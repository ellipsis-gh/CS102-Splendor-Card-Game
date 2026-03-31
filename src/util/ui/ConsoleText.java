package util.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Text utilities used by multiple renderers.
 *
 * <p>Key rule: any width calculation must use visible length (ANSI stripped),
 * otherwise colours break alignment.</p>
 */
final class ConsoleText {

    private ConsoleText() {
    }

    /**
     * Removes ANSI escape sequences so text width can be measured correctly.
     * Example sequence: {@code "\u001B[90m"}.
     */
    static String stripAnsi(String s) {
        if (s == null) return "";
        return s.replaceAll("\u001B\\[[^m]*m", "");
    }

    /** Visible character length (ANSI stripped). */
    static int visLen(String s) {
        return stripAnsi(s).length();
    }

    /** Pads on the right so the visible length becomes {@code width}. */
    static String padRight(String s, int width) {
        int vis = visLen(s);
        if (vis >= width) return s;
        return s + " ".repeat(width - vis);
    }

    /**
     * Truncates to a maximum visible width, preserving ANSI codes.
     * Adds {@code "..."} when truncation happens.
     */
    static String truncateVisible(String s, int width) {
        String safe = (s == null) ? "" : s;
        if (visLen(safe) <= width) return safe;
        if (width <= 3) return ".".repeat(Math.max(0, width));

        StringBuilder sb = new StringBuilder();
        int vis = 0;
        int target = width - 3; // reserve space for "..."
        for (int i = 0; i < safe.length() && vis < target; ) {
            char c = safe.charAt(i);
            if (c == '\u001B' && i + 1 < safe.length() && safe.charAt(i + 1) == '[') {
                int m = safe.indexOf('m', i);
                if (m < 0) break;
                sb.append(safe, i, m + 1);
                i = m + 1;
                continue;
            }
            sb.append(c);
            vis++;
            i++;
        }
        sb.append("...");
        if (safe.contains(ConsoleAnsi.ESC)) sb.append(ConsoleAnsi.RESET);
        return sb.toString();
    }

    /**
     * Ensures a line is exactly {@code width} visible characters:
     * truncate if needed, otherwise pad with spaces.
     */
    static String fitLine(String s, int width) {
        if (width <= 0) return "";
        String truncated = truncateVisible(s == null ? "" : s, width);
        int vis = visLen(truncated);
        if (vis < width) truncated = truncated + " ".repeat(width - vis);
        return truncated;
    }

    /**
     * Combines a left label and a right marker so the marker is flush-right.
     * Used for "ID ... pts" headers.
     */
    static String rightMarkerLine(String left, String right, int innerWidth) {
        String l = (left == null) ? "" : left;
        String r = (right == null) ? "" : right;
        int gap = innerWidth - visLen(l) - visLen(r);
        if (gap < 1) return truncateVisible(l, innerWidth);
        return l + " ".repeat(gap) + r;
    }

    /** Centers text inside a given visible width. */
    static String centerLine(String text, int width) {
        String t = (text == null) ? "" : text;
        int pad = Math.max(0, (width - visLen(t)) / 2);
        return " ".repeat(pad) + t;
    }

    /**
     * Wraps a string by spaces to fit a maximum line width.
     * Used to wrap long costs inside a fixed-width card box.
     */
    static List<String> wrapBySpaces(String text, int width) {
        if (text == null) return List.of();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return List.of();
        int w = Math.max(1, width);

        String[] words = trimmed.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;

            if (current.length() == 0) {
                appendWordOrSplit(lines, current, word, w);
                continue;
            }

            if (current.length() + 1 + word.length() <= w) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                appendWordOrSplit(lines, current, word, w);
            }
        }

        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private static void appendWordOrSplit(List<String> lines, StringBuilder current, String word, int w) {
        if (word.length() <= w) {
            current.append(word);
            return;
        }
        int idx = 0;
        while (idx < word.length()) {
            int end = Math.min(word.length(), idx + w);
            lines.add(word.substring(idx, end));
            idx = end;
        }
    }
}
