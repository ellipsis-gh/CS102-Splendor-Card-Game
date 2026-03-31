package util.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Box/border helpers for the console UI.
 *
 * <p>All borders are ANSI-aware: when a border colour is supplied, each border
 * segment is wrapped and reset so the colour doesn't "leak" into content.</p>
 */
final class ConsoleBoxes {

    private ConsoleBoxes() {
    }

    /**
     * Top border line for a box.
     *
     * @param innerWidth number of visible characters between the vertical borders
     * @param borderColor optional ANSI colour code (e.g., {@code "\u001B[1;33m"})
     */
    static String borderTop(int innerWidth, String borderColor, boolean ansi) {
        String h = ConsoleConstants.H.repeat(Math.max(0, innerWidth));
        if (!ansi || borderColor == null) return ConsoleConstants.TL + h + ConsoleConstants.TR;
        return borderColor + ConsoleConstants.TL + h + ConsoleConstants.TR + ConsoleAnsi.RESET;
    }

    /** Bottom border line for a box. */
    static String borderBottom(int innerWidth, String borderColor, boolean ansi) {
        String h = ConsoleConstants.H.repeat(Math.max(0, innerWidth));
        if (!ansi || borderColor == null) return ConsoleConstants.BL + h + ConsoleConstants.BR;
        return borderColor + ConsoleConstants.BL + h + ConsoleConstants.BR + ConsoleAnsi.RESET;
    }

    /** Left border character (ANSI-safe). */
    static String borderLeft(String borderColor, boolean ansi) {
        if (!ansi || borderColor == null) return ConsoleConstants.VB;
        return borderColor + ConsoleConstants.VB + ConsoleAnsi.RESET;
    }

    /** Right border character (ANSI-safe). */
    static String borderRight(String borderColor, boolean ansi) {
        if (!ansi || borderColor == null) return ConsoleConstants.VB;
        return borderColor + ConsoleConstants.VB + ConsoleAnsi.RESET;
    }

    /**
     * Joins boxes side by side. All boxes are padded to the tallest height so no
     * box is chopped short.
     */
    static String joinBoxesSideBySide(List<List<String>> boxes) {
        if (boxes == null || boxes.isEmpty()) return "";

        int maxH = boxes.stream().mapToInt(List::size).max().orElse(0);

        List<List<String>> normalized = new ArrayList<>();
        for (List<String> box : boxes) {
            if (box.size() == maxH) {
                normalized.add(new ArrayList<>(box));
                continue;
            }
            int boxW  = ConsoleText.visLen(box.get(0));
            int inner = boxW - 2;
            List<String> padded = new ArrayList<>(box.subList(0, box.size() - 1)); // drop bottom border
            while (padded.size() < maxH - 1) {
                padded.add(ConsoleConstants.VB + " ".repeat(inner) + ConsoleConstants.VB);
            }
            padded.add(box.get(box.size() - 1)); // re-attach bottom border
            normalized.add(padded);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxH; i++) {
            StringBuilder row = new StringBuilder();
            for (int b = 0; b < normalized.size(); b++) {
                if (b > 0) row.append("  ");
                row.append(normalized.get(b).get(i));
            }
            sb.append(row).append('\n');
        }
        return sb.toString();
    }

    /**
     * Renders a list of boxes into a multi-row grid.
     *
     * <p>Used for player boxes (2 columns) and nobles (up to 3 columns).</p>
     */
    static String renderBoxesGrid(List<List<String>> boxes, int columns) {
        if (boxes == null || boxes.isEmpty()) return "";
        int cols = Math.max(1, columns);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < boxes.size(); i += cols) {
            int end = Math.min(boxes.size(), i + cols);
            if (sb.length() > 0) sb.append('\n');
            String block = joinBoxesSideBySide(boxes.subList(i, end));
            if (block.endsWith("\n")) block = block.substring(0, block.length() - 1);
            sb.append(block);
        }
        return sb.toString();
    }
}
