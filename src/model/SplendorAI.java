package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple algorithmic AI for Splendor.
 * Strategy: Buy when possible, else reserve good cards, else take gems we need.
 */
public class SplendorAI {

    private static final Token[] GEM_COLORS = {Token.GREEN, Token.WHITE, Token.BLUE, Token.BLACK, Token.RED};

    /**
     * Decide what action the AI should take this turn.
     * Returns a string: "1:t1:t2:t3" or "2:t" or "3:level:slot" or "3:r:idx" or "4:level:slot" or "4:deck:level"
     */
    public static String chooseAction(Player p, Board board) {
        // 1. Can we buy an affordable card? Prefer high points, or helps toward noble
        String buy = tryBuyCard(p, board);
        if (buy != null) return buy;

        // 2. Can we buy from reserved?
        String buyReserved = tryBuyReserved(p, board);
        if (buyReserved != null) return buyReserved;

        // 3. Should we reserve a card? (if we have room and a good target)
        String reserve = tryReserveCard(p, board);
        if (reserve != null) return reserve;

        // 4. Take gems - prefer colors we need for cards we're close to
        String take = tryTakeGems(p, board);
        if (take != null) return take;

        // Fallback: take 3 different of whatever is available
        return takeThreeAvailable(board);
    }

    /**
     * Decide which tokens to return when over 10. Prefer returning colors we need least.
     * Returns list of tokens to return (caller removes them from player, adds to board).
     */
    public static List<Token> chooseTokensToReturn(Player p, Board board, int count) {
        List<Token> result = new ArrayList<>();
        Map<Token, Integer> needScore = howMuchWeNeedEachColor(p, board);
        Map<Token, Integer> have = new HashMap<>();
        for (Token t : GEM_COLORS) have.put(t, p.getTokenCount(t));
        have.put(Token.GOLD, p.getTokenCount(Token.GOLD));

        for (int i = 0; i < count; i++) {
            Token worst = null;
            int worstScore = 999;
            for (Token t : GEM_COLORS) {
                if (have.getOrDefault(t, 0) > 0) {
                    int score = needScore.getOrDefault(t, 0);
                    if (score < worstScore) {
                        worstScore = score;
                        worst = t;
                    }
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

    private static String tryBuyCard(Player p, Board board) {
        int bestScore = -1;
        int bestLevel = -1;
        int bestSlot = -1;

        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                Card c = row[slot];
                if (c == null) continue;
                if (!p.canAffordCard(c)) continue;

                int score = scoreCardForPurchase(c, p, board);
                if (score > bestScore) {
                    bestScore = score;
                    bestLevel = level;
                    bestSlot = slot;
                }
            }
        }
        if (bestLevel >= 0) {
            return "3:" + bestLevel + ":" + bestSlot;
        }
        return null;
    }

    private static String tryBuyReserved(Player p, Board board) {
        List<Card> hand = p.getHand();
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            if (p.canAffordCard(c)) {
                return "3:r:" + i;
            }
        }
        return null;
    }

    private static String tryReserveCard(Player p, Board board) {
        if (p.getHand().size() >= 3) return null;

        int bestScore = -1;
        String bestAction = null;

        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                Card c = row[slot];
                if (c == null) continue;
                if (p.canAffordCard(c)) continue;

                int tokensNeeded = tokensNeededForCard(p, c);
                if (tokensNeeded <= 2 && tokensNeeded > 0) {
                    int score = c.getPrestigePoints() * 3 + (4 - tokensNeeded);
                    if (score > bestScore) {
                        bestScore = score;
                        bestAction = "4:" + level + ":" + slot;
                    }
                }
            }
        }
        if (bestAction != null) return bestAction;

        if (bestScore < 0 && p.getHand().size() < 3) {
            for (int level = 1; level <= 3; level++) {
                if (board.deckHasCards(level)) {
                    return "4:deck:" + level;
                }
            }
        }
        return null;
    }

    private static String tryTakeGems(Player p, Board board) {
        Token[] needed = getTop3NeededColors(p, board);
        if (needed != null && board.canTakeThreeDifferent(needed[0], needed[1], needed[2])) {
            return "1:" + needed[0] + ":" + needed[1] + ":" + needed[2];
        }

        for (Token t : GEM_COLORS) {
            if (board.canTakeTwo(t)) {
                int need = howMuchWeNeedColor(p, board, t);
                if (need >= 2) {
                    return "2:" + t;
                }
            }
        }

        if (needed != null && board.canTakeThreeDifferent(needed[0], needed[1], needed[2])) {
            return "1:" + needed[0] + ":" + needed[1] + ":" + needed[2];
        }
        return null;
    }

    private static String takeThreeAvailable(Board board) {
        List<Token> available = new ArrayList<>();
        for (Token t : GEM_COLORS) {
            if (board.getAvailableTokens().getOrDefault(t, 0) > 0) {
                available.add(t);
            }
        }
        if (available.size() >= 3) {
            return "1:" + available.get(0) + ":" + available.get(1) + ":" + available.get(2);
        }
        return null;
    }

    private static int scoreCardForPurchase(Card c, Player p, Board board) {
        int score = c.getPrestigePoints() * 10;
        if (helpsTowardNoble(p, c, board)) score += 5;
        if (c.getLevel() == 3) score += 2;
        return score;
    }

    private static boolean helpsTowardNoble(Player p, Card c, Board board) {
        Token bonus = c.getBonus();
        if (bonus == null) return false;
        for (Noble n : board.getNobles()) {
            if (n.getCost().containsKey(bonus) && p.getBonuses().getOrDefault(bonus, 0) < n.getCost().get(bonus)) {
                return true;
            }
        }
        return false;
    }

    private static int tokensNeededForCard(Player p, Card c) {
        Map<Token, Integer> cost = c.getCost();
        Map<Token, Integer> bonuses = p.getBonuses();
        int totalNeeded = 0;
        int totalFromTokens = 0;
        for (Token t : GEM_COLORS) {
            int effectiveCost = Math.max(0, cost.getOrDefault(t, 0) - bonuses.getOrDefault(t, 0));
            totalNeeded += effectiveCost;
            totalFromTokens += Math.min(p.getTokenCount(t), effectiveCost);
        }
        int gold = p.getTokenCount(Token.GOLD);
        return Math.max(0, totalNeeded - totalFromTokens - gold);
    }

    private static Map<Token, Integer> howMuchWeNeedEachColor(Player p, Board board) {
        Map<Token, Integer> need = new HashMap<>();
        for (Token t : GEM_COLORS) {
            need.put(t, 0);
        }
        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (Card c : row) {
                if (c == null) continue;
                addCardNeed(need, p, c);
            }
        }
        for (Card c : p.getHand()) {
            addCardNeed(need, p, c);
        }
        return need;
    }

    private static void addCardNeed(Map<Token, Integer> need, Player p, Card c) {
        Map<Token, Integer> cost = c.getCost();
        Map<Token, Integer> bonuses = p.getBonuses();
        for (Token t : GEM_COLORS) {
            int req = cost.getOrDefault(t, 0) - bonuses.getOrDefault(t, 0);
            if (req > 0) {
                need.put(t, need.get(t) + req);
            }
        }
    }

    private static int howMuchWeNeedColor(Player p, Board board, Token color) {
        int need = 0;
        for (Card c : p.getHand()) {
            int cost = c.getCost().getOrDefault(color, 0);
            int bonus = p.getBonuses().getOrDefault(color, 0);
            need += Math.max(0, cost - bonus - p.getTokenCount(color));
        }
        for (int level = 1; level <= 3; level++) {
            for (Card c : board.getVisibleCards(level)) {
                if (c == null || p.canAffordCard(c)) continue;
                int cost = c.getCost().getOrDefault(color, 0);
                int bonus = p.getBonuses().getOrDefault(color, 0);
                need += Math.max(0, cost - bonus - p.getTokenCount(color));
            }
        }
        return need;
    }

    private static Token[] getTop3NeededColors(Player p, Board board) {
        Map<Token, Integer> need = new HashMap<>();
        for (Token t : GEM_COLORS) {
            need.put(t, 0);
        }
        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (Card c : row) {
                if (c == null) continue;
                if (p.canAffordCard(c)) continue;
                Map<Token, Integer> cost = c.getCost();
                Map<Token, Integer> bonuses = p.getBonuses();
                for (Token t : GEM_COLORS) {
                    int req = cost.getOrDefault(t, 0) - bonuses.getOrDefault(t, 0);
                    if (req > p.getTokenCount(t)) {
                        need.put(t, need.get(t) + (req - p.getTokenCount(t)));
                    }
                }
            }
        }
        for (Card c : p.getHand()) {
            Map<Token, Integer> cost = c.getCost();
            Map<Token, Integer> bonuses = p.getBonuses();
            for (Token t : GEM_COLORS) {
                int req = cost.getOrDefault(t, 0) - bonuses.getOrDefault(t, 0);
                if (req > p.getTokenCount(t)) {
                    need.put(t, need.get(t) + (req - p.getTokenCount(t)));
                }
            }
        }

        List<Token> top = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Token best = null;
            int bestNeed = 0;
            for (Token t : GEM_COLORS) {
                if (top.contains(t)) continue;
                if (board.getAvailableTokens().getOrDefault(t, 0) < 1) continue;
                int n = need.getOrDefault(t, 0);
                if (n > bestNeed) {
                    bestNeed = n;
                    best = t;
                }
            }
            if (best != null) top.add(best);
        }
        if (top.size() >= 3) {
            return new Token[]{top.get(0), top.get(1), top.get(2)};
        }
        return null;
    }
}
