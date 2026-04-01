package util.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import model.Card;
import model.Noble;
import model.Player;
import model.Token;

/**
 * ConsoleViews — every view section for the board in one file.
 *
 * <p>Merges what used to be four separate files:</p>
 * <ul>
 *   <li>ConsoleMarketView  — card rows with coloured box borders</li>
 *   <li>ConsoleNobleView   — noble grid with progress-toward indicators</li>
 *   <li>ConsolePlayerView  — per-player summary boxes (score, tokens, bonuses)</li>
 *   <li>ConsoleReservedView — reserved-hand popup (local R shortcut)</li>
 * </ul>
 *
 * <p>Each section is clearly labelled.  All methods return Strings; nothing
 * prints to the terminal directly — the caller decides when to flush.</p>
 *
 * <p>All methods are {@code static}.  The class cannot be instantiated.</p>
 */
final class ConsoleViews {

    // Prevent instantiation — purely static utilities
    private ConsoleViews() {}

    // =========================================================================
    // SECTION 1 — MARKET VIEW  (card rows for levels 1, 2, 3)
    // =========================================================================
    //
    // The market is displayed as 3 horizontal rows, one row per card level.
    // Each row shows 4 card boxes side-by-side.  Card borders are coloured to
    // match the card's bonus gem colour, so you can scan by colour quickly.

    /**
     * Builds a complete row for one card level, ready to print.
     *
     * <p>All card boxes in the row are forced to the same height so that the
     * borders line up in a clean grid, even when some cards have longer costs
     * that need wrapping onto multiple lines.</p>
     *
     * @param level 1, 2, or 3
     * @param row   the 4 visible card slots at this level (null = empty slot)
     * @param ansi  true for coloured output
     * @return multi-line string (no trailing newline)
     */
    static String buildMarketRowString(int level, Card[] row, boolean ansi) {
        // Step 1: figure out how many lines the cost section needs in each box.
        // We wrap long cost strings so they don't push the box wider.
        int inner = HelperFunctions.CARD_BOX_WIDTH - 2; // usable width inside borders
        int maxCostLines = 1; // all boxes must be at least 1 cost-line tall
        List<List<String>> perCardCostChunks = new ArrayList<>();

        for (int slot = 0; slot < row.length; slot++) {
            List<String> chunks = computeWrappedCostChunks(row[slot], inner, ansi);
            perCardCostChunks.add(chunks);
            maxCostLines = Math.max(maxCostLines, Math.max(1, chunks.size()));
        }

        // Step 2: build each card box, then join them side by side.
        List<List<String>> boxes = new ArrayList<>();
        for (int slot = 0; slot < row.length; slot++) {
            boxes.add(renderCardBox(
                    "[" + level + "-" + slot + "]",  // slot tag like [2-1]
                    row[slot],
                    ansi,
                    perCardCostChunks.get(slot),
                    maxCostLines));
        }

        // joinBoxesSideBySide places the boxes next to each other horizontally
        String out = HelperFunctions.joinBoxesSideBySide(boxes);
        // Remove any trailing newline — the caller adds spacing as needed
        return out.endsWith("\n") ? out.substring(0, out.length() - 1) : out;
    }

    /**
     * Splits a card's cost string into wrapped chunks that fit inside the box.
     * Returns a single "-" for cards with no cost.
     */
    private static List<String> computeWrappedCostChunks(Card card, int inner, boolean ansi) {
        if (card == null) return List.of();

        // "Bon ♦  Cost " is the prefix before the actual cost tokens.
        // We measure its visible width (ignoring ANSI codes) to know how much
        // room is left for the cost tokens on the same line.
        String prefix    = "Bon " + HelperFunctions.gemChip(card.getBonus(), ansi) + "  Cost ";
        int prefixVis    = HelperFunctions.visLen(prefix); // ANSI-safe width measurement
        int costWidth    = Math.max(6, inner - prefixVis); // at least 6 chars for cost tokens

        // wrapBySpaces breaks at word boundaries so "Blk3 Blu1 Grn2" never gets
        // split mid-token (e.g. never "Blk" on one line and "3" on the next).
        List<String> chunks = HelperFunctions.wrapBySpaces(
                ConsoleFormat.formatCostCompactStatic(card.getCost()), costWidth);
        return chunks.isEmpty() ? List.of("-") : chunks;
    }

    /**
     * Renders one card as a box (list of fixed-width lines).
     *
     * @param id           slot tag e.g. "[2-1]"
     * @param card         the card (null = empty slot)
     * @param ansi         true for coloured output
     * @param costChunks   pre-computed wrapped cost lines
     * @param maxCostLines height to pad to (keeps all boxes in a row the same height)
     */
    private static List<String> renderCardBox(String id, Card card, boolean ansi,
                                              List<String> costChunks, int maxCostLines) {
        int inner = HelperFunctions.CARD_BOX_WIDTH - 2;

        // The border colour comes from the card's bonus gem — e.g. green border for green cards.
        String bc  = (ansi && card != null && card.getBonus() != null)
                     ? HelperFunctions.gemBorderColor(card.getBonus()) : null;
        String top    = HelperFunctions.borderTop(inner, bc, ansi);
        String bottom = HelperFunctions.borderBottom(inner, bc, ansi);
        String lb     = HelperFunctions.borderLeft(bc, ansi);
        String rb     = HelperFunctions.borderRight(bc, ansi);

        List<String> lines = new ArrayList<>();
        lines.add(top);

        if (card == null) {
            // Empty slot: show the tag and "(empty)" then pad to match row height
            lines.add(lb + HelperFunctions.fitLine(id, inner) + rb);
            lines.add(lb + HelperFunctions.fitLine("(empty)", inner) + rb);
            for (int i = 1; i < Math.max(1, maxCostLines); i++) {
                lines.add(lb + HelperFunctions.fitLine("", inner) + rb);
            }
            lines.add(bottom);
            return lines;
        }

        // First line: slot tag on the left, prestige points on the right
        String ptsStr = HelperFunctions.bold(card.getPrestigePoints() + " pts", ansi);
        lines.add(lb + HelperFunctions.fitLine(
                HelperFunctions.rightMarkerLine(id, ptsStr, inner), inner) + rb);

        // Second (and possibly more) lines: bonus gem chip + cost tokens
        String prefix    = "Bon " + HelperFunctions.gemChip(card.getBonus(), ansi) + "  Cost ";
        int prefixVis    = HelperFunctions.visLen(prefix);
        List<String> safeChunks = (costChunks == null || costChunks.isEmpty()) ? List.of("-") : costChunks;

        // First chunk on the same line as the prefix
        lines.add(lb + HelperFunctions.fitLine(prefix + safeChunks.get(0), inner) + rb);

        // Continuation lines (if cost wrapped): indent to align with first chunk
        String indent = " ".repeat(prefixVis);
        for (int i = 1; i < Math.max(1, maxCostLines); i++) {
            String chunk = (i < safeChunks.size()) ? safeChunks.get(i) : "";
            lines.add(lb + HelperFunctions.fitLine(indent + chunk, inner) + rb);
        }

        lines.add(bottom);
        return lines;
    }

    // =========================================================================
    // SECTION 2 — NOBLE VIEW
    // =========================================================================
    //
    // Nobles award prestige points when a player collects enough permanent bonuses.
    // The noble view shows each noble's requirement and how close the current
    // player is to claiming it.

    /**
     * Renders the full nobles section: title, then a grid of noble boxes.
     *
     * @param nobles        nobles still on the board
     * @param currentPlayer used to show progress toward each noble
     * @param ansi          true for coloured output
     * @return multi-line string with trailing newline
     */
    static String renderNoblesSection(List<Noble> nobles, Player currentPlayer, boolean ansi) {
        StringBuilder out = new StringBuilder();
        out.append(HelperFunctions.bold("Nobles", ansi)).append('\n');

        if (nobles == null || nobles.isEmpty()) {
            out.append(HelperFunctions.dim("(none)", ansi)).append('\n');
            return out.toString();
        }

        // Build each noble as a box, then arrange in a grid (max 3 per row)
        List<List<String>> nobleBoxes = new ArrayList<>();
        for (int i = 0; i < nobles.size(); i++) {
            nobleBoxes.add(renderNobleBox(i, nobles.get(i), currentPlayer, ansi));
        }
        out.append(HelperFunctions.renderBoxesGrid(
                nobleBoxes, nobles.size() <= 3 ? nobles.size() : 3));
        out.append('\n');
        return out.toString();
    }

    /**
     * Renders a single noble as a box.
     *
     * <p>Each box shows:
     * <ol>
     *   <li>Noble name + prestige points (right-aligned)</li>
     *   <li>The bonus requirement (e.g. "Needs: Grn3 Red2")</li>
     *   <li>How many more bonuses the current player needs ("Need: Grn1") or "READY!"</li>
     * </ol>
     * </p>
     */
    private static List<String> renderNobleBox(int index, Noble noble,
                                               Player currentPlayer, boolean ansi) {
        int inner = HelperFunctions.NOBLE_BOX_WIDTH - 2;
        List<String> lines = new ArrayList<>();
        lines.add(HelperFunctions.borderTop(inner, null, ansi));
        String lb = HelperFunctions.borderLeft(null, ansi);
        String rb = HelperFunctions.borderRight(null, ansi);

        // Title row: noble index + name on the left, points on the right
        String idStr  = "[N" + index + "] " + noble.getName();
        String ptsStr = HelperFunctions.bold(noble.getPrestigePoints() + " pts", ansi);
        lines.add(lb + HelperFunctions.fitLine(
                HelperFunctions.rightMarkerLine(idStr, ptsStr, inner), inner) + rb);

        // Requirement row: always shows the full cost regardless of player bonuses
        String needs = "Needs: " + ConsoleFormat.formatCostCompactStatic(noble.getCost());
        lines.add(lb + HelperFunctions.fitLine(needs, inner) + rb);

        // Progress row: how many more bonuses does the current player need?
        Map<Token, Integer> bonuses = currentPlayer.getBonuses();
        StringBuilder progressSb   = new StringBuilder("Need: ");
        boolean ready              = true;

        for (Token t : HelperFunctions.COST_ORDER) {
            // still = required bonuses - already-owned bonuses (floor at 0)
            int still = noble.getCost().getOrDefault(t, 0) - bonuses.getOrDefault(t, 0);
            if (still > 0) {
                progressSb.append(HelperFunctions.colorizeToken(t, HelperFunctions.tokenLabel(t), ansi))
                          .append(still).append(" ");
                ready = false;
            }
        }
        String progressLine = ready
                ? HelperFunctions.bold("*** READY! ***", ansi)
                : progressSb.toString().trim();
        lines.add(lb + HelperFunctions.fitLine(progressLine, inner) + rb);
        lines.add(HelperFunctions.borderBottom(inner, null, ansi));
        return lines;
    }

    // =========================================================================
    // SECTION 3 — PLAYER VIEW  (per-player summary boxes)
    // =========================================================================
    //
    // The player grid shows every player's score, tokens, and bonuses in a
    // compact, fixed-width box.  Fixed-width token formatting is used so that
    // the box borders don't shift when gem counts change.

    /**
     * Builds the grid of player summary boxes (up to 2 per row).
     *
     * @param players      all players in order
     * @param currentIndex whose turn it is (used to bold/highlight their box)
     * @param winScore     winning score (shown in score badge)
     * @param leaderScore  current highest score (highlighted in green)
     * @param ansi         true for coloured output
     * @return multi-line string with trailing newline
     */
    static String renderPlayersGrid(List<Player> players, int currentIndex,
                                    int winScore, int leaderScore, boolean ansi) {
        List<List<String>> playerBoxes = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            playerBoxes.add(renderPlayerBox(
                    i, players.get(i),
                    i == currentIndex,
                    winScore, leaderScore, ansi));
        }
        // 2 players per row keeps boxes side-by-side on a standard 80-col terminal
        return HelperFunctions.renderBoxesGrid(playerBoxes, players.size() <= 2 ? players.size() : 2) + "\n";
    }

    /**
     * Renders a single player's summary box.
     *
     * <p>Contents:
     * <ol>
     *   <li>Name row — bold when it's this player's turn; "<== " marker on the right</li>
     *   <li>Score badge — colour-coded (yellow = active turn, green = leader)</li>
     *   <li>Reserved / bought / nobles counts</li>
     *   <li>Token counts (fixed-width, includes GOLD)</li>
     *   <li>Bonus counts (fixed-width, no GOLD)</li>
     * </ol>
     * </p>
     */
    private static List<String> renderPlayerBox(int index, Player player,
                                                boolean isCurrent, int winScore,
                                                int leaderScore, boolean ansi) {
        int inner = HelperFunctions.PLAYER_BOX_WIDTH - 2;

        // Bold border (ESC[1m) for all players — the active player is distinguished
        // by text content (bold name + "<==" marker), not by a different border colour.
        String borderColor = ansi ? (HelperFunctions.ESC + "1m") : null;
        String top    = HelperFunctions.borderTop(inner, borderColor, ansi);
        String bottom = HelperFunctions.borderBottom(inner, borderColor, ansi);
        String lb     = HelperFunctions.borderLeft(borderColor, ansi);
        String rb     = HelperFunctions.borderRight(borderColor, ansi);

        List<String> lines = new ArrayList<>();
        lines.add(top);

        // Name row
        String type      = player.isHuman() ? "Human" : "AI";
        String nameLabel = "P" + (index + 1) + " [" + type + "] " + player.getName();
        if (isCurrent) nameLabel = HelperFunctions.bold(nameLabel, ansi);
        String marker = isCurrent ? HelperFunctions.bold("<==", ansi) : "";
        lines.add(lb + HelperFunctions.fitLine(
                HelperFunctions.rightMarkerLine(nameLabel, marker, inner), inner) + rb);

        // Score badge (shows points + how many left to win)
        int toWin = Math.max(0, winScore - player.getScore());
        lines.add(lb + HelperFunctions.fitLine(ConsoleFormat.renderScoreBadge(
                player.getScore(), winScore, toWin,
                isCurrent, player.getScore() == leaderScore, ansi), inner) + rb);

        // Card counts row
        lines.add(lb + HelperFunctions.fitLine(
                "Reserved: " + player.getHand().size() + "/" + HelperFunctions.MAX_RESERVED
                + "   Bought: "  + player.getPurchasedCards().size()
                + "   Nobles: "  + player.getNobles().size(), inner) + rb);

        // Token row — fixed-width so borders never shift when counts change
        lines.add(lb + HelperFunctions.fitLine(
                "Tokens:  " + ConsoleFormat.formatTokenMapFixed(player.getTokens(), true, ansi),
                inner) + rb);

        // Bonus row — no GOLD column (bonuses are only the 5 regular colours)
        lines.add(lb + HelperFunctions.fitLine(
                "Bonuses: " + ConsoleFormat.formatTokenMapFixed(player.getBonuses(), false, ansi),
                inner) + rb);

        lines.add(bottom);
        return lines;
    }

    // =========================================================================
    // SECTION 4 — RESERVED CARDS VIEW
    // =========================================================================
    //
    // The reserved-card popup is shown when a player presses "R" locally.
    // It answers: "What cards do I have reserved, and can I buy any of them?"

    /**
     * Builds the reserved-cards popup for the given player.
     *
     * <p>Shows each reserved card with:
     * <ul>
     *   <li>Slot tag [r-0], [r-1], [r-2]</li>
     *   <li>"(not affordable yet)" when the player can't buy it this turn</li>
     *   <li>Full card summary (prestige + bonus + cost)</li>
     *   <li>Remaining cost after applying the player's current bonuses</li>
     * </ul>
     * </p>
     *
     * @param player the player whose reserved hand to show
     * @param ansi   true for coloured output
     * @return multi-line string with trailing newline
     */
    static String renderReservedCardsString(Player player, boolean ansi) {
        Objects.requireNonNull(player, "player"); // defensive: catch accidental null at call site
        StringBuilder out = new StringBuilder();

        // Header line: bold title + dim count badge
        out.append(HelperFunctions.bold("Reserved Cards", ansi))
           .append(HelperFunctions.dim(
                   "  (" + player.getHand().size() + "/" + HelperFunctions.MAX_RESERVED + ")", ansi))
           .append('\n');
        out.append("Player: ").append(player.getName()).append('\n');
        out.append("Bonuses: ")
           .append(ConsoleFormat.formatTokenMap(player.getBonuses(), false, ansi))
           .append('\n');
        out.append('\n');

        if (player.getHand().isEmpty()) {
            out.append(HelperFunctions.dim("(none)", ansi)).append('\n');
            return out.toString();
        }

        for (int i = 0; i < player.getHand().size(); i++) {
            Card    c      = player.getHand().get(i);
            boolean afford = player.canAffordCard(c);

            // Slot tag, with optional "not affordable" note
            out.append(HelperFunctions.bold("[r-" + i + "]", ansi))
               .append(' ')
               .append(afford ? "" : HelperFunctions.dim("(not affordable yet) ", ansi))
               .append('\n');

            // One-line card summary
            out.append("  ").append(ConsoleFormat.formatCard(c, ansi)).append('\n');

            // Remaining cost after applying permanent bonuses (what you'd actually pay)
            out.append("  After bonuses: ")
               .append(ConsoleFormat.formatCostCompactStatic(
                       ConsoleFormat.remainingAfterBonuses(c, player)))
               .append('\n');
            out.append('\n');
        }

        return out.toString();
    }
}
