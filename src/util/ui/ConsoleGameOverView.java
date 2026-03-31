package util.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import logic.Game;
import model.Player;

/**
 * Final screen when the game ends (board + prominent winner banner).
 *
 * <p>Kept separate from the main state view so changes to the end screen don't
 * risk breaking the normal turn layout.</p>
 */
final class ConsoleGameOverView {

    private ConsoleGameOverView() {
    }

    /**
     * Builds the full "game over" screen:
     * <ol>
     *   <li>Final board state (so the player can still see everything)</li>
     *   <li>Winner banner + final standings</li>
     * </ol>
     */
    static String renderGameOverString(Game game, boolean ansi, int winScore) {
        Objects.requireNonNull(game, "game");
        StringBuilder out = new StringBuilder();
        out.append(ConsoleGameStateView.renderGameStateString(game, ansi, winScore));
        out.append('\n');
        out.append(renderGameOverBanner(game, ansi, winScore));
        return out.toString();
    }

    /**
     * The winner banner is rendered as a gold bordered box so it visually pops.
     * We use ANSI-safe borders (reset after each segment) to prevent colour bleed.
     */
    /**
     * Renders a formatted game over banner displaying the winner and final standings.
     * 
     * @param game the Game object containing player information and winner details
     * @param ansi boolean flag indicating whether to apply ANSI color and formatting codes
     * @param winScore the target prestige score needed to win the game
     * @return a String containing the formatted game over banner with borders, winner info,
     *         final standings, and tie-break rules. The banner includes:
     *         - A gold-bordered box (if ANSI is enabled)
     *         - "GAME OVER" title centered at the top
     *         - Winner's name and prestige score (if determined)
     *         - Congratulations message
     *         - Final standings table sorted by prestige score (descending),
     *           with tie-breaking by purchased card count (ascending)
     *         - Each player entry showing rank, name, prestige, card count, and noble count
     *         - Tie-break rule explanation
     * 
     * Note: The ANSI code "1;33m" represents bold (1) + yellow/gold color (33),
     *       used for highlighting the game over box borders and trophy symbol.
     */
    private static String renderGameOverBanner(Game game, boolean ansi, int winScore) {
        List<Player> players = game.getPlayers();
        Player winner = game.determineWinner();

        int inner = 78;
        String borderColor = ansi ? (ConsoleAnsi.ESC + "1;33m") : null; // gold
        String top = ConsoleBoxes.borderTop(inner, borderColor, ansi);
        String bottom = ConsoleBoxes.borderBottom(inner, borderColor, ansi);
        String lb = ConsoleBoxes.borderLeft(borderColor, ansi);
        String rb = ConsoleBoxes.borderRight(borderColor, ansi);

        String title = ansi ? (ConsoleAnsi.ESC + "1m" + " GAME OVER " + ConsoleAnsi.RESET) : " GAME OVER ";
        String trophy = ansi ? (ConsoleAnsi.ESC + "1;33m" + "★" + ConsoleAnsi.RESET) : "*";

        List<String> lines = new ArrayList<>();
        lines.add(top);
        lines.add(lb + ConsoleText.fitLine(ConsoleText.centerLine(title, inner), inner) + rb);
        lines.add(lb + ConsoleText.fitLine("", inner) + rb);

        if (winner != null) {
            String winLine = trophy + " WINNER: " + winner.getName();
            String scoreLine = "Prestige: " + winner.getScore() + "/" + winScore + " pts";
            lines.add(lb + ConsoleText.fitLine(ansi ? ConsoleAnsi.bold(winLine, true) : winLine, inner) + rb);
            lines.add(lb + ConsoleText.fitLine(ansi ? ConsoleAnsi.bold(scoreLine, true) : scoreLine, inner) + rb);
            lines.add(lb + ConsoleText.fitLine(ConsoleAnsi.dim("Congratulations! Thanks for playing.", ansi), inner) + rb);
        } else {
            lines.add(lb + ConsoleText.fitLine("Winner: (could not be determined)", inner) + rb);
        }

        lines.add(lb + ConsoleText.fitLine("", inner) + rb);
        lines.add(lb + ConsoleText.fitLine(ansi ? ConsoleAnsi.bold("Final standings", true) : "Final standings", inner) + rb);

        List<Player> sorted = new ArrayList<>(players);
        sorted.sort((a, b) -> {
            int scoreCmp = Integer.compare(b.getScore(), a.getScore());
            if (scoreCmp != 0) return scoreCmp;
            return Integer.compare(a.getPurchasedCards().size(), b.getPurchasedCards().size());
        });

        for (int i = 0; i < sorted.size(); i++) {
            Player p = sorted.get(i);
            String line = String.format("  %d) %s  —  %d pts   (cards: %d, nobles: %d)",
                    i + 1, p.getName(), p.getScore(), p.getPurchasedCards().size(), p.getNobles().size());
            lines.add(lb + ConsoleText.fitLine(line, inner) + rb);
        }

        lines.add(lb + ConsoleText.fitLine(ConsoleAnsi.dim("Tie-break: fewer purchased cards wins.", ansi), inner) + rb);
        lines.add(bottom);

        return String.join("\n", lines) + "\n";
    }
}
