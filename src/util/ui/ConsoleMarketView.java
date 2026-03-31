package util.ui;

import java.util.ArrayList;
import java.util.List;

import model.Card;

/**
 * Renders the market rows and card boxes.
 *
 * <p>Cards are rendered as boxes so each slot tag (e.g. {@code [2-1]}) lines up
 * with its cost and bonus, matching how the physical market is arranged.</p>
 */
final class ConsoleMarketView {

    private ConsoleMarketView() {
    }

    /**
     * Builds a single market row (one level) with 4 card boxes side-by-side.
     *
     * @param level card level (1–3)
     * @param row visible cards at that level (null entries represent empty slots)
     */
    static String buildMarketRowString(int level, Card[] row, boolean ansi) {
        List<List<String>> boxes = new ArrayList<>();

        int inner = ConsoleConstants.CARD_BOX_WIDTH - 2;
        int maxCostLines = 1;
        List<List<String>> perCardCostChunks = new ArrayList<>();
        for (int slot = 0; slot < row.length; slot++) {
            Card card = row[slot];
            List<String> chunks = (card == null) ? List.of() : computeWrappedCostChunks(card, inner, ansi);
            perCardCostChunks.add(chunks);
            maxCostLines = Math.max(maxCostLines, Math.max(1, chunks.size()));
        }

        for (int slot = 0; slot < row.length; slot++) {
            boxes.add(renderCardBox("[" + level + "-" + slot + "]", row[slot], ansi, perCardCostChunks.get(slot), maxCostLines));
        }
        String out = ConsoleBoxes.joinBoxesSideBySide(boxes);
        return out.endsWith("\n") ? out.substring(0, out.length() - 1) : out;
    }

    private static List<String> computeWrappedCostChunks(Card card, int inner, boolean ansi) {
        if (card == null) return List.of();
        // Cost is wrapped so long costs don't push the card box wider than CARD_BOX_WIDTH.
        String prefix = "Bon " + ConsoleAnsi.gemChip(card.getBonus(), ansi) + "  Cost ";
        int prefixVis = ConsoleText.visLen(prefix);
        int costWidth = Math.max(6, inner - prefixVis);
        List<String> chunks = ConsoleText.wrapBySpaces(ConsoleTokenFormat.formatCostCompactStatic(card.getCost()), costWidth);
        return chunks.isEmpty() ? List.of("-") : chunks;
    }

    /**
     * Creates a single card box.
     *
     * <p>The border colour is derived from the card's bonus to make scanning faster.</p>
     */
    private static List<String> renderCardBox(String id, Card card, boolean ansi, List<String> costChunks, int maxCostLines) {
        int inner = ConsoleConstants.CARD_BOX_WIDTH - 2;

        String bc = (ansi && card != null && card.getBonus() != null) ? ConsoleAnsi.gemBorderColor(card.getBonus()) : null;
        String top    = ConsoleBoxes.borderTop(inner, bc, ansi);
        String bottom = ConsoleBoxes.borderBottom(inner, bc, ansi);
        String lb     = ConsoleBoxes.borderLeft(bc, ansi);
        String rb     = ConsoleBoxes.borderRight(bc, ansi);

        List<String> lines = new ArrayList<>();
        lines.add(top);

        if (card == null) {
            lines.add(lb + ConsoleText.fitLine(id, inner) + rb);
            lines.add(lb + ConsoleText.fitLine("(empty)", inner) + rb);
            for (int i = 1; i < Math.max(1, maxCostLines); i++) {
                lines.add(lb + ConsoleText.fitLine("", inner) + rb);
            }
            lines.add(bottom);
            return lines;
        }

        String ptsStr = ConsoleAnsi.bold(card.getPrestigePoints() + " pts", ansi);
        lines.add(lb + ConsoleText.fitLine(ConsoleText.rightMarkerLine(id, ptsStr, inner), inner) + rb);

        String prefix = "Bon " + ConsoleAnsi.gemChip(card.getBonus(), ansi) + "  Cost ";
        int prefixVis = ConsoleText.visLen(prefix);
        List<String> safeChunks = (costChunks == null || costChunks.isEmpty()) ? List.of("-") : costChunks;

        lines.add(lb + ConsoleText.fitLine(prefix + safeChunks.get(0), inner) + rb);
        String indent = " ".repeat(prefixVis);

        for (int i = 1; i < Math.max(1, maxCostLines); i++) {
            String chunk = (i < safeChunks.size()) ? safeChunks.get(i) : "";
            lines.add(lb + ConsoleText.fitLine(indent + chunk, inner) + rb);
        }
        lines.add(bottom);
        return lines;
    }
}
