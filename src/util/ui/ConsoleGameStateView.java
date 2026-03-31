package util.ui;

import java.util.List;
import java.util.Objects;

import logic.Game;
import model.Board;
import model.Player;
import model.Token;

/**
 * Renders the main "board" view shown every turn (shared by local + network mode).
 *
 * <p>This class deliberately contains <b>no printing</b> and <b>no input</b>.
 * It only builds strings so both local and networking modes can display the same UI.</p>
 */
final class ConsoleGameStateView {

    private ConsoleGameStateView() {
    }

    /**
     * Builds the full board view for the current game state.
     *
     * <p>Everything here is deterministic from {@code game}:
     * calling it twice with the same game state produces identical output.</p>
     */
    static String renderGameStateString(Game game, boolean ansi, int winScore) {
        Objects.requireNonNull(game, "game");
        StringBuilder out = new StringBuilder();

        Board board          = game.getBoard();
        List<Player> players = game.getPlayers();
        int currentIndex     = game.getCurrentPlayerIndex();
        Player current       = players.get(currentIndex);
        int leaderScore      = ConsoleScoreView.maxScore(players);

        // ── Header: title + legend ───────────────────────────────────────────
        out.append(ConsoleAnsi.bold("SPLENDOR", ansi))
           .append(ConsoleAnsi.dim("  (" + players.size() + " players)", ansi))
           .append(ConsoleAnsi.dim("  - first to " + winScore + " pts", ansi))
           .append('\n');
        out.append(ConsoleAnsi.dim("Legend: ", ansi))
           .append(ConsoleAnsi.colorizeToken(Token.BLACK, "Blk", ansi)).append(' ')
           .append(ConsoleAnsi.colorizeToken(Token.BLUE, "Blu", ansi)).append(' ')
           .append(ConsoleAnsi.colorizeToken(Token.GREEN, "Grn", ansi)).append(' ')
           .append(ConsoleAnsi.colorizeToken(Token.RED, "Red", ansi)).append(' ')
           .append(ConsoleAnsi.colorizeToken(Token.WHITE, "Wht", ansi)).append(' ')
           .append(ConsoleAnsi.colorizeToken(Token.GOLD, "Gld", ansi))
           .append('\n');

        // ── Turn / score banner: always on top so scores are prominent ───────
        if (game.isGameOver()) {
            out.append(ConsoleAnsi.bold("FINAL", ansi)).append(": ").append(ConsoleAnsi.bold("Game Over", ansi)).append('\n');
            out.append(ConsoleAnsi.bold("SCORES", ansi))
               .append(": ")
               .append(ConsoleScoreView.renderScoresSummary(players, -1, winScore, leaderScore, ansi))
               .append('\n');
        } else {
            int toWinCurrent = Math.max(0, winScore - current.getScore());
            out.append(ConsoleAnsi.bold("TURN", ansi))
               .append(": ")
               .append(ConsoleAnsi.bold("P" + (currentIndex + 1) + " " + current.getName(), ansi))
               .append("  ")
               .append(ConsoleScoreView.renderScoreBadge(current.getScore(), winScore, toWinCurrent, true,
                       current.getScore() == leaderScore, ansi))
               .append('\n');
            out.append(ConsoleAnsi.bold("SCORES", ansi))
               .append(": ")
               .append(ConsoleScoreView.renderScoresSummary(players, currentIndex, winScore, leaderScore, ansi))
               .append('\n');
        }

        // ── Player profile boxes: per-player snapshot (score/tokens/bonuses) ─
        out.append(ConsolePlayerView.renderPlayersGrid(players, currentIndex, winScore, leaderScore, ansi));

        // ── Bank: available tokens remaining on the board ────────────────────
        out.append("Bank:  ").append(ConsoleTokenFormat.formatTokenMap(board.getAvailableTokens(), true, ansi)).append('\n');

        // ── Nobles: targets that grant free points ───────────────────────────
        out.append(ConsoleNobleView.renderNoblesSection(board.getNobles(), current, ansi));
        out.append('\n');

        // ── Market: visible cards by level (3 → 1 like the physical game) ────
        out.append(ConsoleAnsi.bold("Market", ansi)).append('\n');
        for (int level = 3; level >= 1; level--) {
            int remaining   = board.getDeckRemainingCount(level);
            String deckInfo = remaining > 0
                    ? ConsoleAnsi.dim(" [" + remaining + " in deck]", ansi)
                    : ConsoleAnsi.dim(" [deck empty]", ansi);
            out.append(ConsoleAnsi.dim("Level " + level, ansi)).append(deckInfo).append('\n');
            out.append(ConsoleMarketView.buildMarketRowString(level, board.getVisibleCards(level), ansi));
            out.append('\n');
        }

        // ── Reserved-card reminder: only shown to humans (local shortcut) ────
        if (current.isHuman() && !current.getHand().isEmpty()) {
            out.append(ConsoleAnsi.dim("Tip: choose [R] to view your reserved cards.", ansi)).append('\n');
        }

        return out.toString();
    }
}
