package logic;

import java.util.*;
import model.Board;
import model.Card;
import model.Player;
import model.Token;

/**
 * HARD difficulty — full board analysis.
 *
 * Capabilities beyond MEDIUM:
 *  - Discount engine scoring: cards whose bonus covers lots of future unmet
 *    costs score higher, regardless of prestige points alone
 *  - Endgame urgency: prestige weight doubles once any player reaches 12 pts
 *  - Opponent threat blocking: reserves the highest-value card an opponent at
 *    12+ pts can currently afford, to prevent them from winning
 *  - Weighted gem need: level-3 cards counted at 25% weight (far from reachable),
 *    level-1 at 100% — avoids hoarding gems for distant cards
 *  - Conservative blind draws: only draws from deck when hand is completely empty
 */
class HardAI {

    static String chooseAction(Player p, Board board, List<Player> allPlayers) {
        boolean endgame = AIHelpers.anyPlayerAt(allPlayers, 12);

        String buy = buy(p, board, endgame);
        if (buy != null) return buy;

        String buyRes = buyReserved(p, board, endgame);
        if (buyRes != null) return buyRes;

        String reserve = reserve(p, board, allPlayers, endgame);
        if (reserve != null) return reserve;

        String take = takeGems(p, board);
        if (take != null) return take;

        return AIHelpers.fallbackTake(p, board);
    }

    static List<Token> chooseTokensToReturn(Player p, Board board, int count) {
        return AIHelpers.smartReturn(p, board, count);
    }

    // -----------------------------------------------------------------------
    // Private logic
    // -----------------------------------------------------------------------

    private static String buy(Player p, Board board, boolean endgame) {
        int bScore = -1, bLevel = -1, bSlot = -1;
        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                Card c = row[slot];
                if (c == null || !p.canAffordCard(c)) continue;
                int score = buyScore(c, p, board, endgame);
                if (score > bScore) { bScore = score; bLevel = level; bSlot = slot; }
            }
        }
        return bLevel >= 0 ? "3:" + bLevel + ":" + bSlot : null;
    }

    private static String buyReserved(Player p, Board board, boolean endgame) {
        int bScore = -1, bIdx = -1;
        for (int i = 0; i < p.getHand().size(); i++) {
            Card c = p.getHand().get(i);
            if (!p.canAffordCard(c)) continue;
            int score = buyScore(c, p, board, endgame);
            if (score > bScore) { bScore = score; bIdx = i; }
        }
        return bIdx >= 0 ? "3:r:" + bIdx : null;
    }

    /**
     * Hard buy score:
     *   prestige × 10 (×2 in endgame) + noble progress + discount engine value × 4 + level bonus.
     *
     * Discount engine value measures how much this card's bonus reduces the unmet costs
     * of all visible and reserved target cards — cards that build future affordability
     * score much higher here than they would with prestige alone.
     */
    static int buyScore(Card c, Player p, Board board, boolean endgame) {
        int score = c.getPrestigePoints() * (endgame ? 20 : 10);
        if (AIHelpers.helpsNoble(p, c, board)) score += 8;
        score += discountEngineValue(c.getBonus(), p, board) * 4;
        if (c.getLevel() == 3) score += 3;
        return score;
    }

    /**
     * Counts the total unmet demand for a bonus color across all visible and reserved
     * cards. A bonus that satisfies a lot of future need is worth more as a permanent discount.
     */
    private static int discountEngineValue(Token bonus, Player p, Board board) {
        if (bonus == null) return 0;
        int demand = 0;
        for (int level = 1; level <= 3; level++) {
            for (Card c : board.getVisibleCards(level)) {
                if (c == null) continue;
                demand += Math.max(0,
                        c.getCost().getOrDefault(bonus, 0) - p.getBonuses().getOrDefault(bonus, 0));
            }
        }
        for (Card c : p.getHand()) {
            demand += Math.max(0,
                    c.getCost().getOrDefault(bonus, 0) - p.getBonuses().getOrDefault(bonus, 0));
        }
        return demand;
    }

    private static String reserve(Player p, Board board, List<Player> allPlayers, boolean endgame) {
        if (p.getHand().size() >= 3) return null;

        // Endgame: try to block the leading opponent before reserving for ourselves
        if (endgame && allPlayers != null) {
            String block = blockOpponent(p, board, allPlayers);
            if (block != null) return block;
        }

        // Reserve the best reachable high-value card
        int bScore = -1;
        String best = null;
        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                Card c = row[slot];
                if (c == null || p.canAffordCard(c)) continue;
                int needed = AIHelpers.tokensNeeded(p, c);
                if (needed >= 1 && needed <= 2) {
                    int score = buyScore(c, p, board, endgame) + (4 - needed) * 2;
                    if (score > bScore) { bScore = score; best = "4:" + level + ":" + slot; }
                }
            }
        }
        if (best != null) return best;

        // Blind draw from highest deck only when hand is totally empty (conservative)
        if (p.getHand().isEmpty()) {
            for (int level = 3; level >= 1; level--) {
                if (board.deckHasCards(level)) return "4:deck:" + level;
            }
        }
        return null;
    }

    /**
     * Identifies the highest-value card any opponent at 12+ pts can currently afford
     * and reserves it to deny them that purchase. Only blocks cards worth 2+ pts.
     */
    private static String blockOpponent(Player me, Board board, List<Player> allPlayers) {
        int bPts = -1, bLevel = -1, bSlot = -1;
        for (Player opp : allPlayers) {
            if (opp == me || opp.getScore() < 12) continue;
            for (int level = 1; level <= 3; level++) {
                Card[] row = board.getVisibleCards(level);
                for (int slot = 0; slot < row.length; slot++) {
                    Card c = row[slot];
                    if (c == null) continue;
                    if (opp.canAffordCard(c) && c.getPrestigePoints() > bPts) {
                        bPts = c.getPrestigePoints();
                        bLevel = level;
                        bSlot = slot;
                    }
                }
            }
        }
        return (bLevel >= 0 && bPts >= 2) ? "4:" + bLevel + ":" + bSlot : null;
    }

    private static String takeGems(Player p, Board board) {
        int headroom = 10 - p.getTotalTokenCount();
        if (headroom <= 0) return null;

        Map<Token, Integer> need = AIHelpers.weightedNeed(p, board);

        // Take 2 of the most urgently needed color (need > 2 threshold)
        if (headroom >= 2) {
            Token best2 = null;
            int best2Need = 2;
            for (Token t : AIHelpers.GEM_COLORS) {
                int n = need.getOrDefault(t, 0);
                if (board.canTakeTwo(t) && n > best2Need) { best2Need = n; best2 = t; }
            }
            if (best2 != null) return "2:" + best2;
        }

        // Take the top-3 most needed colors
        if (headroom >= 3) {
            Token[] top = AIHelpers.topN(need, board, 3);
            if (top != null) return "1:" + top[0] + ":" + top[1] + ":" + top[2];
        }

        // Take-2 fallback for any color
        if (headroom >= 2) {
            for (Token t : AIHelpers.GEM_COLORS) {
                if (board.canTakeTwo(t)) return "2:" + t;
            }
        }
        return null;
    }
}
