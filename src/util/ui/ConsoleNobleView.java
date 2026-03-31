package util.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import model.Noble;
import model.Player;
import model.Token;

/**
 * Renders the noble section (boxes + progress toward each noble).
 *
 * <p>Nobles are important because they award free prestige points once a player
 * has enough permanent bonuses.</p>
 */
final class ConsoleNobleView {

    private ConsoleNobleView() {
    }

    /**
     * Renders the entire nobles section (title + grid of boxes).
     *
     * @param nobles nobles currently available on the board
     * @param currentPlayer used to display "how far away" the current player is from each noble
     */
    static String renderNoblesSection(List<Noble> nobles, Player currentPlayer, boolean ansi) {
        StringBuilder out = new StringBuilder();
        out.append(ConsoleAnsi.bold("Nobles", ansi)).append('\n');

        if (nobles == null || nobles.isEmpty()) {
            out.append(ConsoleAnsi.dim("(none)", ansi)).append('\n');
            return out.toString();
        }

        List<List<String>> nobleBoxes = new ArrayList<>();
        for (int i = 0; i < nobles.size(); i++) {
            nobleBoxes.add(renderNobleBox(i, nobles.get(i), currentPlayer, ansi));
        }
        out.append(ConsoleBoxes.renderBoxesGrid(nobleBoxes, nobles.size() <= 3 ? nobles.size() : 3));
        out.append('\n');
        return out.toString();
    }

    private static List<String> renderNobleBox(int index, Noble noble, Player currentPlayer, boolean ansi) {
        int inner = ConsoleConstants.NOBLE_BOX_WIDTH - 2;
        List<String> lines = new ArrayList<>();
        lines.add(ConsoleBoxes.borderTop(inner, null, ansi));
        String lb = ConsoleBoxes.borderLeft(null, ansi);
        String rb = ConsoleBoxes.borderRight(null, ansi);

        String idStr  = "[N" + index + "] " + noble.getName();
        String ptsStr = ConsoleAnsi.bold(noble.getPrestigePoints() + " pts", ansi);
        lines.add(lb + ConsoleText.fitLine(ConsoleText.rightMarkerLine(idStr, ptsStr, inner), inner) + rb);

        String needs = "Needs: " + ConsoleTokenFormat.formatCostCompactStatic(noble.getCost());
        lines.add(lb + ConsoleText.fitLine(needs, inner) + rb);

        // Show the current player's remaining requirement (based on permanent bonuses).
        Map<Token, Integer> bonuses = currentPlayer.getBonuses();
        StringBuilder progressSb   = new StringBuilder("Need: ");
        boolean ready              = true;
        for (Token t : ConsoleConstants.COST_ORDER) {
            int still = noble.getCost().getOrDefault(t, 0) - bonuses.getOrDefault(t, 0);
            if (still > 0) {
                progressSb.append(ConsoleAnsi.colorizeToken(t, ConsoleAnsi.tokenLabel(t), ansi)).append(still).append(" ");
                ready = false;
            }
        }
        String progressLine = ready ? ConsoleAnsi.bold("*** READY! ***", ansi) : progressSb.toString().trim();
        lines.add(lb + ConsoleText.fitLine(progressLine, inner) + rb);

        lines.add(ConsoleBoxes.borderBottom(inner, null, ansi));
        return lines;
    }
}
