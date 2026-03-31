package util.ui;

import java.util.List;

import model.Player;

/**
 * Renders score-related UI elements (top summary and per-player badges).
 *
 * <p>These helpers are used in multiple places: the top-of-screen banner and the
 * per-player summary boxes.</p>
 */
final class ConsoleScoreView {

    private ConsoleScoreView() {
    }

    /**
     * Returns the highest prestige score among all players.
     * Used to mark the current "leader" for quick scanning.
     */
    static int maxScore(List<Player> players) {
        int best = Integer.MIN_VALUE;
        for (Player p : players) best = Math.max(best, p.getScore());
        return best == Integer.MIN_VALUE ? 0 : best;
    }

    /**
     * Builds a compact summary like:
     * {@code [P1 7/15]  P2 5/15  P3 4/15}
     */
    static String renderScoresSummary(List<Player> players, int currentIndex, int winScore,
                                      int leaderScore, boolean ansi) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            boolean isCurrent = (currentIndex >= 0 && i == currentIndex);
            boolean isLeader  = (p.getScore() == leaderScore);

            String entry = "P" + (i + 1) + " " + p.getScore() + "/" + winScore;
            if (!ansi) {
                if (isLeader && !isCurrent) entry = "*" + entry;
                if (isCurrent) entry = "[" + entry + "]";
            } else {
                if (isCurrent) entry = ConsoleAnsi.bold("[" + entry + "]", true);
                else if (isLeader) entry = ConsoleAnsi.bold(entry, true);
            }

            sb.append(entry);
            if (i < players.size() - 1) sb.append(ConsoleAnsi.dim("  ", ansi));
        }
        return sb.toString();
    }

    /**
     * Prominent badge displayed for a specific player.
     *
     * <p>When ANSI is enabled we highlight the current player (yellow) and the leader (green).</p>
     */
    static String renderScoreBadge(int score, int winScore, int toWin,
                                   boolean isCurrent, boolean isLeader, boolean ansi) {
        String text = "SCORE: " + score + "/" + winScore + "   LEFT: " + toWin;
        if (!ansi) {
            if (isCurrent) return ">> " + text;
            if (isLeader) return "* " + text;
            return text;
        }

        String code = null;
        if (isCurrent) code = ConsoleAnsi.ESC + "1;30;43m";     // bold black-on-yellow (turn highlight)
        else if (isLeader) code = ConsoleAnsi.ESC + "1;30;42m"; // bold black-on-green (leader highlight)

        if (code == null) return ConsoleAnsi.bold(text, true);
        return code + " " + text + " " + ConsoleAnsi.RESET;
    }
}
