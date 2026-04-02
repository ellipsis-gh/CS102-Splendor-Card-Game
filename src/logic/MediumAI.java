package logic;

import java.util.*;
import model.Board;
import model.Card;
import model.Player;
import model.Token;

/**
 * MEDIUM difficulty — fixed and improved greedy AI.
 *
 * Bugs fixed versus the original SplendorAI (per AI_ANALYSIS.md):
 *  - tryBuyReserved now scores and picks the BEST affordable card, not just the first
 *  - Blind deck reservation now draws from the HIGHEST available deck (not lowest)
 *  - Dead canTakeThreeDifferent check removed from gem-taking logic
 *  - Token headroom checked before every take-3 or take-2 attempt
 */
class MediumAI {

    static String chooseAction(Player p, Board board) {
        String buy = buy(p, board);
        if (buy != null) return buy;

        String buyRes = buyReserved(p, board);
        if (buyRes != null) return buyRes;

        String reserve = reserve(p, board);
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

    /** Buys the highest-scored visible card (prestige × 10 + noble bonus + level-3 bonus). */
    private static String buy(Player p, Board board) {
        int bScore = -1, bLevel = -1, bSlot = -1;
        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                Card c = row[slot];
                if (c == null || !p.canAffordCard(c)) continue;
                int score = buyScore(c, p, board);
                if (score > bScore) { bScore = score; bLevel = level; bSlot = slot; }
            }
        }
        return bLevel >= 0 ? "3:" + bLevel + ":" + bSlot : null;
    }

    /** Buys the best (not just first) affordable reserved card. */
    private static String buyReserved(Player p, Board board) {
        int bScore = -1, bIdx = -1;
        for (int i = 0; i < p.getHand().size(); i++) {
            Card c = p.getHand().get(i);
            if (!p.canAffordCard(c)) continue;
            int score = buyScore(c, p, board);
            if (score > bScore) { bScore = score; bIdx = i; }
        }
        return bIdx >= 0 ? "3:r:" + bIdx : null;
    }

    /** Reserves a card 1–2 tokens from affordable; blind draw from the HIGHEST available deck. */
    private static String reserve(Player p, Board board) {
        if (p.getHand().size() >= 3) return null;
        int bScore = -1;
        String best = null;
        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                Card c = row[slot];
                if (c == null || p.canAffordCard(c)) continue;
                int needed = AIHelpers.tokensNeeded(p, c);
                if (needed >= 1 && needed <= 2) {
                    int score = c.getPrestigePoints() * 3 + (4 - needed);
                    if (score > bScore) { bScore = score; best = "4:" + level + ":" + slot; }
                }
            }
        }
        if (best != null) return best;
        // Blind draw from HIGHEST available deck (fixes original lowest-deck bug)
        for (int level = 3; level >= 1; level--) {
            if (board.deckHasCards(level)) return "4:deck:" + level;
        }
        return null;
    }

    private static String takeGems(Player p, Board board) {
        int headroom = 10 - p.getTotalTokenCount();
        if (headroom <= 0) return null;
        // Take 2 of same if we heavily need one color
        if (headroom >= 2) {
            for (Token t : AIHelpers.GEM_COLORS) {
                if (board.canTakeTwo(t) && AIHelpers.colorNeed(p, board, t) >= 2) return "2:" + t;
            }
        }
        // Take 3 most-needed colors
        if (headroom >= 3) {
            Token[] top3 = AIHelpers.top3Needed(p, board);
            if (top3 != null) return "1:" + top3[0] + ":" + top3[1] + ":" + top3[2];
        }
        // Take-2 fallback
        if (headroom >= 2) {
            for (Token t : AIHelpers.GEM_COLORS) {
                if (board.canTakeTwo(t)) return "2:" + t;
            }
        }
        return null;
    }

    /** Score for buying: prestige × 10 + noble-progress bonus + level-3 tiebreaker. */
    private static int buyScore(Card c, Player p, Board board) {
        int score = c.getPrestigePoints() * 10;
        if (AIHelpers.helpsNoble(p, c, board)) score += 5;
        if (c.getLevel() == 3) score += 2;
        return score;
    }
}
