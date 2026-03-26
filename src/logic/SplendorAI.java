package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Board;
import model.Card;
import model.Noble;
import model.Player;
import model.Token;

// simple greedy AI: buy if possible, reserve a good card, otherwise take gems we need
public class SplendorAI {

    private static final Token[] GEM_COLORS = {Token.GREEN, Token.WHITE, Token.BLUE, Token.BLACK, Token.RED};

    // decides what action to take — returns a command string like:
    // "1:t1:t2:t3" (take 3), "2:t" (take 2), "3:level:slot" or "3:r:idx" (buy), "4:level:slot" or "4:deck:level" (reserve)
    public static String chooseAction(Player p, Board board) {
        // 1. buy an affordable card if we can — prefer high points or noble progress
        String buy = tryBuyCard(p, board);
        if (buy != null) return buy;

        // 2. check if we can buy from our reserved cards
        String buyReserved = tryBuyReserved(p, board);
        if (buyReserved != null) return buyReserved;

        // 3. reserve a card if there's a good target and we have room
        String reserve = tryReserveCard(p, board);
        if (reserve != null) return reserve;

        // 4. take gems — prefer colors we need most for cards we're close to buying
        String take = tryTakeGems(p, board);
        if (take != null) return take;

        // fallback: just take 3 of whatever is available
        return takeThreeAvailable(board);
    }

    // decides which tokens to give back when over 10 — returns the least-needed colors first
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
                // only return gold if there's nothing else to give
                result.add(Token.GOLD);
                have.put(Token.GOLD, have.get(Token.GOLD) - 1);
            } else break;
        }
        return result;
    }

    // scan visible cards and pick the best one we can afford
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

    // check if we can buy any card we already have reserved
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

    // look for a card that's close to affordable and worth reserving
    private static String tryReserveCard(Player p, Board board) {
        if (p.getHand().size() >= 3) return null; // hand full

        int bestScore = -1;
        String bestAction = null;

        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                Card c = row[slot];
                if (c == null) continue;
                if (p.canAffordCard(c)) continue; // no need to reserve what we can already buy

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

        // nothing specific worth reserving — grab a blind card from the highest available deck
        if (bestScore < 0 && p.getHand().size() < 3) {
            for (int level = 1; level <= 3; level++) {
                if (board.deckHasCards(level)) {
                    return "4:deck:" + level;
                }
            }
        }
        return null;
    }

    // try to take gems smartly — pick the 3 we need most, or take 2 if we need a lot of one color
    private static String tryTakeGems(Player p, Board board) {
        Token[] needed = getTop3NeededColors(p, board);
        if (needed != null && board.canTakeThreeDifferent(needed[0], needed[1], needed[2])) {
            return "1:" + needed[0] + ":" + needed[1] + ":" + needed[2];
        }

        // if we really need a lot of one color and the board has 4+, take 2
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

    // last resort: grab 3 of whatever colors are on the board
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

    // score a card for purchase: prestige points matter most, noble progress and high level are bonuses
    private static int scoreCardForPurchase(Card c, Player p, Board board) {
        int score = c.getPrestigePoints() * 10;
        if (helpsTowardNoble(p, c, board)) score += 5;
        if (c.getLevel() == 3) score += 2; // slight preference for high-level cards
        return score;
    }

    // does this card's bonus color help us get closer to one of the nobles on the board?
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

    // how many more tokens would we need to afford this card right now?
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

    // tally how much of each color we need across all visible cards and our reserved cards
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

    // add this card's unmet color requirements to the running need totals
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

    // how many more of this specific color do we need across all our target cards?
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

    // find the 3 colors we need most right now — prioritize ones that are actually on the board
    private static Token[] getTop3NeededColors(Player p, Board board) {
        Map<Token, Integer> need = new HashMap<>();
        for (Token t : GEM_COLORS) {
            need.put(t, 0);
        }
        for (int level = 1; level <= 3; level++) {
            Card[] row = board.getVisibleCards(level);
            for (Card c : row) {
                if (c == null) continue;
                if (p.canAffordCard(c)) continue; // already affordable, no need to count
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

        // pick the top 3 by need score, skipping colors not on the board
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
