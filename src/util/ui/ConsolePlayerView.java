package util.ui;

import java.util.ArrayList;
import java.util.List;

import model.Player;

/**
 * Renders the per-player summary boxes shown near the top of the board.
 *
 * <p>This is the "at-a-glance" area: scores, tokens, and bonuses are always visible
 * so players don't need to scroll or remember state between turns.</p>
 */
final class ConsolePlayerView {

    private ConsolePlayerView() {
    }

    /**
     * Builds the grid of player boxes.
     *
     * <p>We keep this in a separate helper so the main board renderer reads like a
     * high-level "story" (header → players → bank → nobles → market).</p>
     */
    static String renderPlayersGrid(List<Player> players, int currentIndex,
                                    int winScore, int leaderScore, boolean ansi) {
        List<List<String>> playerBoxes = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            playerBoxes.add(renderPlayerBox(i, players.get(i), i == currentIndex, winScore, leaderScore, ansi));
        }
        return ConsoleBoxes.renderBoxesGrid(playerBoxes, players.size() <= 2 ? players.size() : 2) + "\n";
    }

    private static List<String> renderPlayerBox(int index, Player player,
                                                boolean isCurrent, int winScore, int leaderScore, boolean ansi) {
        int inner = ConsoleConstants.PLAYER_BOX_WIDTH - 2;

        // Uniform border colour: highlight current player via marker/text, not border colour.
        String borderColor = ansi ? (ConsoleAnsi.ESC + "1m") : null;
        String top    = ConsoleBoxes.borderTop(inner, borderColor, ansi);
        String bottom = ConsoleBoxes.borderBottom(inner, borderColor, ansi);
        String lb     = ConsoleBoxes.borderLeft(borderColor, ansi);
        String rb     = ConsoleBoxes.borderRight(borderColor, ansi);

        List<String> lines = new ArrayList<>();
        lines.add(top);

        // Name row: bold for current player, "<==" marker flush right
        String type      = player.isHuman() ? "Human" : "AI";
        String nameLabel = "P" + (index + 1) + " [" + type + "] " + player.getName();
        if (isCurrent) nameLabel = ConsoleAnsi.bold(nameLabel, ansi);
        String marker = isCurrent ? ConsoleAnsi.bold("<==", ansi) : "";
        lines.add(lb + ConsoleText.fitLine(ConsoleText.rightMarkerLine(nameLabel, marker, inner), inner) + rb);

        // Score row (prominent)
        int toWin = Math.max(0, winScore - player.getScore());
        lines.add(lb + ConsoleText.fitLine(ConsoleScoreView.renderScoreBadge(
                player.getScore(), winScore, toWin, isCurrent, player.getScore() == leaderScore, ansi), inner) + rb);
        lines.add(lb + ConsoleText.fitLine("Reserved: " + player.getHand().size() + "/" + ConsoleConstants.MAX_RESERVED
                + "   Bought: " + player.getPurchasedCards().size()
                + "   Nobles: " + player.getNobles().size(), inner) + rb);

        // Tokens + bonuses (fixed width so borders never shift across turns)
        lines.add(lb + ConsoleText.fitLine("Tokens:  " + ConsoleTokenFormat.formatTokenMapFixed(player.getTokens(), true, ansi), inner) + rb);
        lines.add(lb + ConsoleText.fitLine("Bonuses: " + ConsoleTokenFormat.formatTokenMapFixed(player.getBonuses(), false, ansi), inner) + rb);

        lines.add(bottom);
        return lines;
    }
}
