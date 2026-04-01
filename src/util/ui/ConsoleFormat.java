package util.ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import model.Card;
import model.Player;
import model.Token;

/**
 * ConsoleFormat — all token, card, cost, and score formatting in one place.
 *
 * Merges what used to be ConsoleTokenFormat and ConsoleScoreView into a
 * single, easy-to-read file.  Every method here builds a String; nothing
 * prints to the terminal directly.
 *
 * Two kinds of helpers live here:
 * 
 *   Token / cost helpers — turn Token enums and cost maps into
 *       human-readable strings, optionally with ANSI colour.
 *   Score helpers — build score summaries and badges shown at the
 *       top of each player's panel.
 * 
 *
 * All methods are {@code static}.  The class cannot be instantiated.
 */
final class ConsoleFormat {

    private ConsoleFormat() {}

    // =========================================================================
    // SECTION 1 — TOKEN / COST FORMATTING
    // =========================================================================
    //
    // These helpers convert Token enums and Map<Token,Integer> cost/wallet maps
    // into display strings.  They all accept an `ansi` flag:
    //   true  → coloured output using ANSI escape codes (see HelperFunctions)
    //   false → plain text output safe for terminals without ANSI support

    /**
     * Returns a short "N pts" label for a prestige-points value.
     * Example: pointsLabel(3) → "3 pts"
     */
    static String pointsLabel(int points) {
        return points + " pts";
    }

    /**
     * One-line summary of a card, used in reserved-hand views and logs.
     * Example: "3 pts  Bonus: Grn  Cost: Blk2 Blu1"
     *
     * @param c    the card to describe
     * @param ansi true for coloured output
     */
    static String formatCard(Card c, boolean ansi) {
        return pointsLabel(c.getPrestigePoints())
                + "  Bonus: " + HelperFunctions.tokenLabel(c.getBonus())
                + "  Cost: "  + formatCost(c.getCost(), ansi);
    }

    /**
     * Formats a full token cost map with coloured abbreviations.
     * Only tokens with count > 0 are shown.
     * Example (ANSI off): "Blk3 Blu1"
     *
     * @param cost the cost map from a card
     * @param ansi true for coloured token labels
     */
    static String formatCost(Map<Token, Integer> cost, boolean ansi) {
        StringBuilder sb = new StringBuilder();
        // HelperFunctions.COST_ORDER is the canonical token order (no GOLD)
        for (Token t : HelperFunctions.COST_ORDER) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) {
                // colorizeToken wraps the label in ANSI colour codes when ansi=true
                sb.append(HelperFunctions.colorizeToken(t, HelperFunctions.tokenLabel(t), ansi))
                  .append(v)
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Formats any token map (gem wallet, cost, etc.) skipping zeros.
     * Returns "—" when every count is zero.
     *
     * @param map         the token map (null treated as empty)
     * @param includeGold whether to include the GOLD wildcard in output
     * @param ansi        true for coloured labels
     */
    static String formatTokenMap(Map<Token, Integer> map, boolean includeGold, boolean ansi) {
        // Treat null as an empty map to avoid NPE
        Map<Token, Integer> safe = (map == null) ? new EnumMap<>(Token.class) : map;
        StringBuilder sb = new StringBuilder();
        boolean any = false;

        for (Token t : HelperFunctions.COST_ORDER) {
            int v = safe.getOrDefault(t, 0);
            if (v > 0) {
                sb.append(HelperFunctions.colorizeToken(t, HelperFunctions.tokenLabel(t), ansi))
                  .append(":").append(v).append(" ");
                any = true;
            }
        }

        if (includeGold) {
            int g = safe.getOrDefault(Token.GOLD, 0);
            if (g > 0) {
                sb.append(HelperFunctions.colorizeToken(Token.GOLD, HelperFunctions.tokenLabel(Token.GOLD), ansi))
                  .append(":").append(g);
                any = true;
            }
        }

        if (!any) sb.append("—"); // Unicode em-dash: nothing to show
        return sb.toString().trim();
    }

    /**
     * Fixed-width version of formatTokenMap, used inside player boxes.
     *
     * Why fixed-width?  Inside a box with exact border positions, a variable
     * count like "Blk:9" vs "Blk:12" would push the right border.  This method
     * always prints every colour with zero-padded 2-digit counts (00–99) so the
     * total width is constant regardless of gem counts.
     *
     * @param map         the token map (null treated as empty)
     * @param includeGold whether to append the GOLD column at the end
     * @param ansi        true for coloured labels
     */
    static String formatTokenMapFixed(Map<Token, Integer> map, boolean includeGold, boolean ansi) {
        Map<Token, Integer> safe = (map == null) ? new EnumMap<>(Token.class) : map;
        StringBuilder sb = new StringBuilder();

        // tokenShortLabel gives a 3-char abbreviation: Blk Blu Grn Red Wht
        for (Token t : HelperFunctions.COST_ORDER) {
            int v = Math.min(99, safe.getOrDefault(t, 0)); // cap at 99 for formatting
            sb.append(HelperFunctions.colorizeToken(t, HelperFunctions.tokenShortLabel(t), ansi))
              .append(String.format("%02d", v)) // always 2 digits: 00, 01, … 99
              .append(' ');
        }

        if (includeGold) {
            int g = Math.min(99, safe.getOrDefault(Token.GOLD, 0));
            sb.append(HelperFunctions.colorizeToken(Token.GOLD, HelperFunctions.tokenShortLabel(Token.GOLD), ansi))
              .append(String.format("%02d", g));
        } else if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // trim the trailing space after the last colour
        }

        return sb.toString().trim();
    }

    /**
     * Compact cost string with NO ANSI colour.
     *
     * Used by GameEngine when building the action-menu option list, which is
     * plain text shared across local and network modes.
     *
     * Example: "Blk2 Blu1 Grn1"
     */
    static String formatCostCompactStatic(Map<Token, Integer> cost) {
        if (cost == null || cost.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Token t : HelperFunctions.COST_ORDER) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) {
                // tokenLabel with no ANSI — always safe for network clients
                sb.append(HelperFunctions.tokenLabel(t)).append(v).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Computes the remaining gem cost after applying a player's permanent bonuses.
     *
     * In Splendor, each purchased card grants a permanent discount equal to its
     * bonus gem colour.  This method subtracts those bonuses from the card's raw
     * cost.  Gold tokens can later cover whatever remains — but that deduction
     * happens at buy-time in the game logic, not here.
     *
     * @param card   the card the player wants to buy
     * @param player the player (their bonuses map is used for discounts)
     * @return a new map containing only the colours still needed (zero-cost colours omitted)
     */
    static Map<Token, Integer> remainingAfterBonuses(Card card, Player player) {
        Map<Token, Integer> remaining = new EnumMap<>(Token.class);
        Map<Token, Integer> cost      = card.getCost();
        Map<Token, Integer> bonuses   = player.getBonuses();

        for (Token t : HelperFunctions.COST_ORDER) {
            // Bonus reduces the required count; never goes below 0
            int needed = Math.max(0, cost.getOrDefault(t, 0) - bonuses.getOrDefault(t, 0));
            if (needed > 0) remaining.put(t, needed);
        }
        return remaining;
    }

    // =========================================================================
    // SECTION 2 — SCORE FORMATTING
    // =========================================================================
    //
    // Score helpers build the small status lines that appear at the top of the
    // board and inside each player panel.  They use HelperFunctions for ANSI
    // bold/dim so they don't need to know about raw escape codes.

    /**
     * Returns the highest prestige score among all players.
     * Used to mark the current "leader" in the score summary.
     */
    static int maxScore(List<Player> players) {
        int best = Integer.MIN_VALUE;
        for (Player p : players) best = Math.max(best, p.getScore());
        return best == Integer.MIN_VALUE ? 0 : best;
    }

    /**
     * Builds a compact multi-player score summary for the top of the screen.
     * Current player is highlighted in brackets; leader is bolded.
     *
     * Example (2 players, P1 is current leader):
     *   [P1 7/15]  P2 5/15
     *
     * @param players      all players in turn order
     * @param currentIndex whose turn it is (–1 = game over, no highlight)
     * @param winScore     the target winning score
     * @param leaderScore  highest current score (from maxScore())
     * @param ansi         true for bold/dim ANSI formatting
     */
    static String renderScoresSummary(List<Player> players, int currentIndex, int winScore,
                                      int leaderScore, boolean ansi) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            Player  p         = players.get(i);
            boolean isCurrent = (currentIndex >= 0 && i == currentIndex);
            boolean isLeader  = (p.getScore() == leaderScore);

            String entry = "P" + (i + 1) + " " + p.getScore() + "/" + winScore;

            if (!ansi) {
                // Plain text: use symbols since we can't change colour
                if (isLeader && !isCurrent) entry = "*" + entry;  // star = leader
                if (isCurrent) entry = "[" + entry + "]";          // brackets = active player
            } else {
                // ANSI: bold current player and leader
                if (isCurrent) entry = HelperFunctions.bold("[" + entry + "]", true);
                else if (isLeader) entry = HelperFunctions.bold(entry, true);
            }

            sb.append(entry);
            if (i < players.size() - 1) {
                // dim double-space separator between players
                sb.append(HelperFunctions.dim("  ", ansi));
            }
        }
        return sb.toString();
    }

    /**
     * Prominent badge shown in a player's panel for their own score.
     *
     * Shows: "SCORE: 7/15   LEFT: 8"
     * When ANSI is on, the current player gets yellow highlighting and the
     * leader gets green highlighting.
     *
     * @param score     this player's current prestige score
     * @param winScore  points needed to win
     * @param toWin     how many more points this player needs
     * @param isCurrent true if it is currently this player's turn
     * @param isLeader  true if this player has the highest score
     * @param ansi      true for coloured output
     */
    static String renderScoreBadge(int score, int winScore, int toWin,
                                   boolean isCurrent, boolean isLeader, boolean ansi) {
        String text = "SCORE: " + score + "/" + winScore + "   LEFT: " + toWin;

        if (!ansi) {
            // Plain text fallback: prefix symbols to distinguish status
            if (isCurrent) return ">> " + text;  // ">>" = your turn
            if (isLeader)  return "* " + text;   // "*"  = you're winning
            return text;
        }

        // ANSI colour codes for the badge background:
        //   bold black text on yellow = current player's turn
        //   bold black text on green  = leader (but not current)
        String code = null;
        if (isCurrent) code = HelperFunctions.ESC + "1;30;43m"; // yellow bg
        else if (isLeader) code = HelperFunctions.ESC + "1;30;42m"; // green bg

        if (code == null) return HelperFunctions.bold(text, true); // neither: just bold
        return code + " " + text + " " + HelperFunctions.RESET;
    }
}
