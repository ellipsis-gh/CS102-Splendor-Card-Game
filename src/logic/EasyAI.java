package logic;

import java.util.*;
import model.Board;
import model.Card;
import model.Player;
import model.Token;

/**
 * EASY difficulty — naive/beginner behavior.
 *
 * Strategy:
 *  1. Buy the first affordable visible card (no scoring, lowest level first)
 *  2. Buy the first affordable reserved card
 *  3. Take the first 3 available gem colors (ignores actual need)
 *  4. Take 2 of any gem color that has 4+ on the board
 */
class EasyAI {

    static String chooseAction(Player p, Board board) {
        // 1. Buy first affordable visible card
        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                if (row[slot] != null && p.canAffordCard(row[slot]))
                    return "3:" + level + ":" + slot;
            }
        }

        // 2. Buy first affordable reserved card
        for (int i = 0; i < p.getHand().size(); i++) {
            if (p.canAffordCard(p.getHand().get(i))) return "3:r:" + i;
        }

        // 3. Take first 3 available gem colors (no strategy)
        int headroom = 10 - p.getTotalTokenCount();
        List<Token> avail = AIHelpers.availableColors(board);
        if (headroom >= 3 && avail.size() >= 3)
            return "1:" + avail.get(0) + ":" + avail.get(1) + ":" + avail.get(2);

        // 4. Take 2 of any color with 4+ on the board
        if (headroom >= 2) {
            for (Token t : AIHelpers.GEM_COLORS) {
                if (board.canTakeTwo(t)) return "2:" + t;
            }
        }

        return null; // skip turn
    }

    /** Returns tokens naively: GREEN first, then WHITE, BLUE, BLACK, RED, then GOLD last. */
    static List<Token> chooseTokensToReturn(Player p, int count) {
        List<Token> result = new ArrayList<>();
        for (Token t : AIHelpers.GEM_COLORS) {
            for (int i = 0; i < p.getTokenCount(t) && result.size() < count; i++)
                result.add(t);
        }
        for (int i = 0; i < p.getTokenCount(Token.GOLD) && result.size() < count; i++)
            result.add(Token.GOLD);
        return result;
    }
}
