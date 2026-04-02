package logic;

import java.util.*;
import model.Board;
import model.Card;
import model.Noble;
import model.Player;
import model.Token;

/**
 * Shared static utilities used by all four AI difficulty classes.
 * Package-private — accessed only within the logic package via BotMain.
 */
class AIHelpers {

    static final Token[] GEM_COLORS = {
        Token.GREEN, Token.WHITE, Token.BLUE, Token.BLACK, Token.RED
    };

    // -----------------------------------------------------------------------
    // Noble / card helpers
    // -----------------------------------------------------------------------

    /** True if buying this card's bonus color advances progress toward any noble on the board. */
    static boolean helpsNoble(Player p, Card c, Board board) {
        Token bonus = c.getBonus();
        if (bonus == null) return false;
        for (Noble n : board.getNobles()) {
            if (n.getCost().containsKey(bonus)
                    && p.getBonuses().getOrDefault(bonus, 0) < n.getCost().get(bonus))
                return true;
        }
        return false;
    }

    /**
     * How many more tokens the player still needs to buy this card,
     * after accounting for bonuses, current token holdings, and gold.
     */
    static int tokensNeeded(Player p, Card c) {
        int deficit = 0;
        for (Token t : GEM_COLORS) {
            int eff = Math.max(0, c.getCost().getOrDefault(t, 0) - p.getBonuses().getOrDefault(t, 0));
            deficit += Math.max(0, eff - p.getTokenCount(t));
        }
        return Math.max(0, deficit - p.getTokenCount(Token.GOLD));
    }

    // -----------------------------------------------------------------------
    // Gem-need helpers
    // -----------------------------------------------------------------------

    /** Total unmet demand for one color across all visible + reserved cards. */
    static int colorNeed(Player p, Board board, Token color) {
        int need = 0;
        for (Card c : p.getHand()) {
            need += Math.max(0, c.getCost().getOrDefault(color, 0)
                    - p.getBonuses().getOrDefault(color, 0) - p.getTokenCount(color));
        }
        for (int level = 1; level <= 3; level++) {
            for (Card c : board.getVisibleCards(level)) {
                if (c == null || p.canAffordCard(c)) continue;
                need += Math.max(0, c.getCost().getOrDefault(color, 0)
                        - p.getBonuses().getOrDefault(color, 0) - p.getTokenCount(color));
            }
        }
        return need;
    }

    /** Returns the 3 most-needed gem colors that are currently available on the board. */
    static Token[] top3Needed(Player p, Board board) {
        Map<Token, Integer> need = new HashMap<>();
        for (Token t : GEM_COLORS) need.put(t, 0);
        for (int level = 1; level <= 3; level++) {
            for (Card c : board.getVisibleCards(level)) {
                if (c == null || p.canAffordCard(c)) continue;
                for (Token t : GEM_COLORS) {
                    int req = c.getCost().getOrDefault(t, 0) - p.getBonuses().getOrDefault(t, 0);
                    if (req > p.getTokenCount(t))
                        need.put(t, need.get(t) + req - p.getTokenCount(t));
                }
            }
        }
        for (Card c : p.getHand()) {
            for (Token t : GEM_COLORS) {
                int req = c.getCost().getOrDefault(t, 0) - p.getBonuses().getOrDefault(t, 0);
                if (req > p.getTokenCount(t))
                    need.put(t, need.get(t) + req - p.getTokenCount(t));
            }
        }
        return topN(need, board, 3);
    }

    /** Returns the top-N most-needed colors available on the board from the given need map. */
    static Token[] topN(Map<Token, Integer> need, Board board, int n) {
        List<Token> top = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Token best = null;
            int bestNeed = 0;
            for (Token t : GEM_COLORS) {
                if (top.contains(t)) continue;
                if (board.getAvailableTokens().getOrDefault(t, 0) < 1) continue;
                if (need.getOrDefault(t, 0) > bestNeed) { bestNeed = need.get(t); best = t; }
            }
            if (best != null) top.add(best);
        }
        return top.size() >= n ? top.toArray(new Token[0]) : null;
    }

    /**
     * Builds a need map weighted by card reachability:
     *   level-1 → 100%, level-2 → 75%, level-3 → 25% (far from reachable).
     * Reserved cards always counted at 100%.
     */
    static Map<Token, Integer> weightedNeed(Player p, Board board) {
        Map<Token, Integer> need = new HashMap<>();
        for (Token t : GEM_COLORS) need.put(t, 0);
        double[] w = {1.0, 0.75, 0.25};
        for (int level = 1; level <= 3; level++) {
            for (Card c : board.getVisibleCards(level)) {
                if (c == null || p.canAffordCard(c)) continue;
                for (Token t : GEM_COLORS) {
                    int req = c.getCost().getOrDefault(t, 0) - p.getBonuses().getOrDefault(t, 0);
                    if (req > p.getTokenCount(t))
                        need.put(t, (int)(need.get(t) + (req - p.getTokenCount(t)) * w[level - 1]));
                }
            }
        }
        for (Card c : p.getHand()) {
            for (Token t : GEM_COLORS) {
                int req = c.getCost().getOrDefault(t, 0) - p.getBonuses().getOrDefault(t, 0);
                if (req > p.getTokenCount(t))
                    need.put(t, need.get(t) + (req - p.getTokenCount(t)));
            }
        }
        return need;
    }

    /** Returns tokens with the lowest weighted need score first; gold is returned last. */
    static List<Token> smartReturn(Player p, Board board, int count) {
        Map<Token, Integer> need = weightedNeed(p, board);
        Map<Token, Integer> have = new HashMap<>();
        for (Token t : GEM_COLORS) have.put(t, p.getTokenCount(t));
        have.put(Token.GOLD, p.getTokenCount(Token.GOLD));

        List<Token> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Token worst = null;
            int worstNeed = Integer.MAX_VALUE;
            for (Token t : GEM_COLORS) {
                if (have.getOrDefault(t, 0) > 0 && need.getOrDefault(t, 0) < worstNeed) {
                    worstNeed = need.get(t);
                    worst = t;
                }
            }
            if (worst != null) {
                result.add(worst);
                have.put(worst, have.get(worst) - 1);
            } else if (have.getOrDefault(Token.GOLD, 0) > 0) {
                result.add(Token.GOLD);
                have.put(Token.GOLD, have.get(Token.GOLD) - 1);
            } else break;
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Misc helpers
    // -----------------------------------------------------------------------

    /** True if any player in the list has reached the given score threshold. */
    static boolean anyPlayerAt(List<Player> players, int threshold) {
        if (players == null) return false;
        for (Player pl : players) if (pl.getScore() >= threshold) return true;
        return false;
    }

    /** List of gem colors that have at least 1 token on the board. */
    static List<Token> availableColors(Board board) {
        List<Token> avail = new ArrayList<>();
        for (Token t : GEM_COLORS) {
            if (board.getAvailableTokens().getOrDefault(t, 0) > 0) avail.add(t);
        }
        return avail;
    }

    /** Last-resort fallback: takes any 3 available gem colors if there is headroom. */
    static String fallbackTake(Player p, Board board) {
        int headroom = 10 - p.getTotalTokenCount();
        List<Token> avail = availableColors(board);
        if (headroom >= 3 && avail.size() >= 3)
            return "1:" + avail.get(0) + ":" + avail.get(1) + ":" + avail.get(2);
        if (headroom >= 2) {
            for (Token t : GEM_COLORS) if (board.canTakeTwo(t)) return "2:" + t;
        }
        return null;
    }
}
