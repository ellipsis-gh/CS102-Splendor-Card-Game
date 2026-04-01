package util.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import logic.Game;
import model.Board;
import model.Player;
import model.Token;

/**
 * ConsoleRenderer — the single public entry point for all console rendering.
 *
 * <p>This class is the only {@code public} class in the util.ui package.
 * All other helper classes (HelperFunctions, ConsoleFormat, ConsoleViews) are
 * package-private — callers outside this package only need to know about
 * ConsoleRenderer.</p>
 *
 * <p>Three major screens live here:</p>
 * <ol>
 *   <li>{@link #renderGameStateString} — the normal in-game board view</li>
 *   <li>{@link #renderGameOverString} — final screen (board + winner banner)</li>
 *   <li>{@link #renderReservedCardsString} — reserved-card popup (local R shortcut)</li>
 * </ol>
 *
 * <p>Everything returns a {@code String}; nothing prints to the terminal directly.
 * The caller decides when and where to flush.</p>
 */
public final class ConsoleRenderer {

    // Prevent instantiation — purely static public facade
    private ConsoleRenderer() {}

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Renders the full in-game board view.
     *
     * <p>Sections (top to bottom):
     * <ol>
     *   <li>Title + token legend</li>
     *   <li>Turn banner + score summary</li>
     *   <li>Player summary boxes (score, tokens, bonuses)</li>
     *   <li>Bank (available gems)</li>
     *   <li>Noble grid</li>
     *   <li>Market (levels 3 → 1 with card boxes)</li>
     *   <li>Reserved-card reminder for human players</li>
     * </ol>
     * </p>
     *
     * @param game     the current game state (must not be null)
     * @param ansi     true to embed ANSI colour/bold sequences
     * @param winScore prestige points needed to win
     * @return multi-line string ready to print
     */
    public static String renderGameStateString(Game game, boolean ansi, int winScore) {
        Objects.requireNonNull(game, "game");
        return buildGameStateString(game, ansi, winScore);
    }

    /**
     * Convenience overload that uses winScore=10.
     * Kept for backward compatibility; prefer the 3-argument version.
     */
    public static String renderGameStateString(Game game, boolean ansi) {
        return renderGameStateString(game, ansi, 10);
    }

    /**
     * Renders the final screen: the board state followed by a winner banner.
     *
     * @param game     the finished game (must not be null)
     * @param ansi     true for ANSI formatting
     * @param winScore the score that triggered end-game
     * @return multi-line string ready to print
     */
    public static String renderGameOverString(Game game, boolean ansi, int winScore) {
        Objects.requireNonNull(game, "game");
        // Show the board, then append the winner banner underneath
        return buildGameStateString(game, ansi, winScore)
                + '\n'
                + buildGameOverBanner(game, ansi, winScore);
    }

    /**
     * Renders the reserved-card popup shown when a local player presses R.
     *
     * @param player the player whose reserved hand to display (must not be null)
     * @param ansi   true for ANSI formatting
     * @return multi-line string ready to print
     */
    public static String renderReservedCardsString(Player player, boolean ansi) {
        Objects.requireNonNull(player, "player");
        // ConsoleViews contains the reserved-card section
        return ConsoleViews.renderReservedCardsString(player, ansi);
    }

    /**
     * Compact cost formatter used by GameEngine when building action-menu option lists.
     *
     * <p>Returns a plain-text string like "Blk2 Blu1" with no ANSI codes, so it is
     * safe to send over the network to clients that may not support ANSI.</p>
     */
    public static String formatCostCompactStatic(Map<Token, Integer> cost) {
        return ConsoleFormat.formatCostCompactStatic(cost);
    }

    // =========================================================================
    // PRIVATE — GAME STATE SCREEN
    // =========================================================================

    /**
     * Builds the full game-state screen string section by section.
     *
     * <p>All rendering details are delegated to ConsoleViews (for the player/noble/market
     * sections) and ConsoleFormat (for token/score strings).  This method is the
     * "director" that decides the order and spacing of sections.</p>
     */
    private static String buildGameStateString(Game game, boolean ansi, int winScore) {
        StringBuilder out = new StringBuilder();

        Board        board        = game.getBoard();
        List<Player> players      = game.getPlayers();
        int          currentIndex = game.getCurrentPlayerIndex();
        Player       current      = players.get(currentIndex);
        int          leaderScore  = ConsoleFormat.maxScore(players);

        // ── 1. Title row + token-colour legend ──────────────────────────────
        out.append(HelperFunctions.bold("SPLENDOR", ansi))
           .append(HelperFunctions.dim("  (" + players.size() + " players)", ansi))
           .append(HelperFunctions.dim("  - first to " + winScore + " pts", ansi))
           .append('\n');

        // Coloured token names so players can quickly map abbreviations to colours
        out.append(HelperFunctions.dim("Legend: ", ansi))
           .append(HelperFunctions.colorizeToken(Token.BLACK, "Blk", ansi)).append(' ')
           .append(HelperFunctions.colorizeToken(Token.BLUE,  "Blu", ansi)).append(' ')
           .append(HelperFunctions.colorizeToken(Token.GREEN, "Grn", ansi)).append(' ')
           .append(HelperFunctions.colorizeToken(Token.RED,   "Red", ansi)).append(' ')
           .append(HelperFunctions.colorizeToken(Token.WHITE, "Wht", ansi)).append(' ')
           .append(HelperFunctions.colorizeToken(Token.GOLD,  "Gld", ansi))
           .append('\n');

        // ── 2. Turn / score banner ───────────────────────────────────────────
        if (game.isGameOver()) {
            // No active turn — show "FINAL: Game Over" instead of a player name
            out.append(HelperFunctions.bold("FINAL", ansi))
               .append(": ")
               .append(HelperFunctions.bold("Game Over", ansi))
               .append('\n');
            out.append(HelperFunctions.bold("SCORES", ansi))
               .append(": ")
               .append(ConsoleFormat.renderScoresSummary(players, -1, winScore, leaderScore, ansi))
               .append('\n');
        } else {
            // Active turn: bold the current player's name + score badge
            int toWinCurrent = Math.max(0, winScore - current.getScore());
            out.append(HelperFunctions.bold("TURN", ansi))
               .append(": ")
               .append(HelperFunctions.bold("P" + (currentIndex + 1) + " " + current.getName(), ansi))
               .append("  ")
               .append(ConsoleFormat.renderScoreBadge(
                       current.getScore(), winScore, toWinCurrent,
                       true, current.getScore() == leaderScore, ansi))
               .append('\n');
            out.append(HelperFunctions.bold("SCORES", ansi))
               .append(": ")
               .append(ConsoleFormat.renderScoresSummary(players, currentIndex, winScore, leaderScore, ansi))
               .append('\n');
        }

        // ── 3. Player summary boxes ──────────────────────────────────────────
        out.append(ConsoleViews.renderPlayersGrid(players, currentIndex, winScore, leaderScore, ansi));

        // ── 4. Bank ─────────────────────────────────────────────────────────
        out.append("Bank:  ")
           .append(ConsoleFormat.formatTokenMap(board.getAvailableTokens(), true, ansi))
           .append('\n');

        // ── 5. Nobles ────────────────────────────────────────────────────────
        out.append(ConsoleViews.renderNoblesSection(board.getNobles(), current, ansi));
        out.append('\n');

        // ── 6. Market (level 3 at top, level 1 at bottom — same as the board game) ──
        out.append(HelperFunctions.bold("Market", ansi)).append('\n');
        for (int level = 3; level >= 1; level--) {
            int remaining = board.getDeckRemainingCount(level);
            // Show how many cards remain in the hidden deck next to the level label
            String deckInfo = remaining > 0
                    ? HelperFunctions.dim(" [" + remaining + " in deck]", ansi)
                    : HelperFunctions.dim(" [deck empty]", ansi);
            out.append(HelperFunctions.dim("Level " + level, ansi)).append(deckInfo).append('\n');
            out.append(ConsoleViews.buildMarketRowString(level, board.getVisibleCards(level), ansi));
            out.append('\n');
        }

        // ── 7. Reserved-card reminder (only for humans with reserved cards) ──
        if (current.isHuman() && !current.getHand().isEmpty()) {
            out.append(HelperFunctions.dim("Tip: choose [R] to view your reserved cards.", ansi))
               .append('\n');
        }

        return out.toString();
    }

    // =========================================================================
    // PRIVATE — GAME OVER BANNER
    // =========================================================================

    /**
     * Builds the gold-bordered winner banner shown at the end of a game.
     *
     * <p>The banner shows:
     * <ul>
     *   <li>"GAME OVER" centred at the top</li>
     *   <li>Winner name + prestige score</li>
     *   <li>Final standings, sorted by prestige score (descending);
     *       ties broken by purchased card count (ascending — fewer cards is better)</li>
     * </ul>
     * </p>
     *
     * <p>ANSI code "1;33m" = bold (1) + yellow/gold foreground (33).
     * This makes the border visually stand out from the rest of the screen.</p>
     */
    private static String buildGameOverBanner(Game game, boolean ansi, int winScore) {
        List<Player> players = game.getPlayers();
        Player       winner  = game.determineWinner();

        int    inner       = 78; // inner width for the banner box
        String borderColor = ansi ? (HelperFunctions.ESC + "1;33m") : null; // bold gold
        String top         = HelperFunctions.borderTop(inner, borderColor, ansi);
        String bottom      = HelperFunctions.borderBottom(inner, borderColor, ansi);
        String lb          = HelperFunctions.borderLeft(borderColor, ansi);
        String rb          = HelperFunctions.borderRight(borderColor, ansi);

        // Title text and trophy icon — bold+gold when ANSI is enabled
        String title  = ansi ? (HelperFunctions.ESC + "1m" + " GAME OVER " + HelperFunctions.RESET) : " GAME OVER ";
        String trophy = ansi ? (HelperFunctions.ESC + "1;33m" + "★" + HelperFunctions.RESET) : "*";

        List<String> lines = new ArrayList<>();
        lines.add(top);
        // Centre the title inside the box
        lines.add(lb + HelperFunctions.fitLine(HelperFunctions.centerLine(title, inner), inner) + rb);
        lines.add(lb + HelperFunctions.fitLine("", inner) + rb); // blank spacer

        if (winner != null) {
            String winLine   = trophy + " WINNER: " + winner.getName();
            String scoreLine = "Prestige: " + winner.getScore() + "/" + winScore + " pts";
            lines.add(lb + HelperFunctions.fitLine(
                    ansi ? HelperFunctions.bold(winLine, true) : winLine, inner) + rb);
            lines.add(lb + HelperFunctions.fitLine(
                    ansi ? HelperFunctions.bold(scoreLine, true) : scoreLine, inner) + rb);
            lines.add(lb + HelperFunctions.fitLine(
                    HelperFunctions.dim("Congratulations! Thanks for playing.", ansi), inner) + rb);
        } else {
            lines.add(lb + HelperFunctions.fitLine("Winner: (could not be determined)", inner) + rb);
        }

        lines.add(lb + HelperFunctions.fitLine("", inner) + rb); // blank spacer
        lines.add(lb + HelperFunctions.fitLine(
                ansi ? HelperFunctions.bold("Final standings", true) : "Final standings", inner) + rb);

        // Sort: highest prestige first; on tie, player with fewer purchased cards wins
        List<Player> sorted = new ArrayList<>(players);
        sorted.sort((a, b) -> {
            int scoreCmp = Integer.compare(b.getScore(), a.getScore()); // descending
            if (scoreCmp != 0) return scoreCmp;
            return Integer.compare(a.getPurchasedCards().size(), b.getPurchasedCards().size()); // ascending
        });

        for (int i = 0; i < sorted.size(); i++) {
            Player p    = sorted.get(i);
            String line = String.format("  %d) %s  —  %d pts   (cards: %d, nobles: %d)",
                    i + 1, p.getName(), p.getScore(),
                    p.getPurchasedCards().size(), p.getNobles().size());
            lines.add(lb + HelperFunctions.fitLine(line, inner) + rb);
        }

        lines.add(lb + HelperFunctions.fitLine(
                HelperFunctions.dim("Tie-break: fewer purchased cards wins.", ansi), inner) + rb);
        lines.add(bottom);

        return String.join("\n", lines) + "\n";
    }
}
