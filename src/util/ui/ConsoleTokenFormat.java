package util.ui;

import java.util.EnumMap;
import java.util.Map;

import model.Card;
import model.Player;
import model.Token;

/**
 * Token/cost formatting utilities (shared across the UI).
 *
 * <p>This file contains only string formatting; it has no printing and no game logic.</p>
 */
final class ConsoleTokenFormat {

    private ConsoleTokenFormat() {
    }

    /** Human-friendly points label (used in some card displays). */
    static String pointsLabel(int points) {
        return points + " pts";
    }

    /** Short one-line card summary used in reserved view and logs. */
    static String formatCard(Card c, boolean ansi) {
        return pointsLabel(c.getPrestigePoints()) + "  Bonus: " + ConsoleAnsi.tokenLabel(c.getBonus())
                + "  Cost: " + formatCost(c.getCost(), ansi);
    }

    /**
     * Formats the full token cost with coloured abbreviations when ANSI is enabled.
     * Example: {@code Blk3 Blu1}.
     */
    static String formatCost(Map<Token, Integer> cost, boolean ansi) {
        StringBuilder sb = new StringBuilder();
        for (Token t : ConsoleConstants.COST_ORDER) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) sb.append(ConsoleAnsi.colorizeToken(t, ConsoleAnsi.tokenLabel(t), ansi)).append(v).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Formats a token map, skipping colors with a count of zero. Returns "—" when empty.
     */
    static String formatTokenMap(Map<Token, Integer> map, boolean includeGold, boolean ansi) {
        Map<Token, Integer> safe = (map == null) ? new EnumMap<>(Token.class) : map;
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (Token t : ConsoleConstants.COST_ORDER) {
            int v = safe.getOrDefault(t, 0);
            if (v > 0) {
                sb.append(ConsoleAnsi.colorizeToken(t, ConsoleAnsi.tokenLabel(t), ansi)).append(":").append(v).append(" ");
                any = true;
            }
        }
        if (includeGold) {
            int g = safe.getOrDefault(Token.GOLD, 0);
            if (g > 0) {
                sb.append(ConsoleAnsi.colorizeToken(Token.GOLD, ConsoleAnsi.tokenLabel(Token.GOLD), ansi)).append(":").append(g);
                any = true;
            }
        }
        if (!any) sb.append("—");
        return sb.toString().trim();
    }

    /**
     * Fixed-width token formatter used inside player boxes so counts never push borders.
     * Always prints all colours in a stable order, with 2-digit counts (00-99).
     */
    static String formatTokenMapFixed(Map<Token, Integer> map, boolean includeGold, boolean ansi) {
        Map<Token, Integer> safe = (map == null) ? new EnumMap<>(Token.class) : map;
        StringBuilder sb = new StringBuilder();

        for (Token t : ConsoleConstants.COST_ORDER) {
            int v = Math.min(99, safe.getOrDefault(t, 0));
            sb.append(ConsoleAnsi.colorizeToken(t, ConsoleAnsi.tokenShortLabel(t), ansi))
              .append(String.format("%02d", v))
              .append(' ');
        }
        if (includeGold) {
            int g = Math.min(99, safe.getOrDefault(Token.GOLD, 0));
            sb.append(ConsoleAnsi.colorizeToken(Token.GOLD, ConsoleAnsi.tokenShortLabel(Token.GOLD), ansi))
              .append(String.format("%02d", g));
        } else if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // drop trailing space
        }
        return sb.toString().trim();
    }

    static String formatCostCompactStatic(Map<Token, Integer> cost) {
        if (cost == null || cost.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Token t : ConsoleConstants.COST_ORDER) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) sb.append(ConsoleAnsi.tokenLabel(t)).append(v).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Computes remaining cost after permanent bonuses (discounts).
     *
     * <p>Splendor rule: bonuses reduce the cost of buying cards; gold can cover any remainder.</p>
     */
    static Map<Token, Integer> remainingAfterBonuses(Card card, Player player) {
        Map<Token, Integer> remaining = new EnumMap<>(Token.class);
        Map<Token, Integer> cost      = card.getCost();
        Map<Token, Integer> bonuses   = player.getBonuses();
        for (Token t : ConsoleConstants.COST_ORDER) {
            int needed = Math.max(0, cost.getOrDefault(t, 0) - bonuses.getOrDefault(t, 0));
            if (needed > 0) remaining.put(t, needed);
        }
        return remaining;
    }
}
