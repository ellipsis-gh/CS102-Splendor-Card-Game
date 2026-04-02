package util.ui;

import java.util.ArrayList;
import java.util.List;

import model.Token;

/**
 * HelperFunctions — the shared foundation used by every file in this package.
 *
 * This file is split into four clear sections:
 *
 *   1. ANSI CONSTANTS   — the raw escape codes that produce colors/styles
 *   2. LAYOUT CONSTANTS — fixed numbers that control box widths and token order
 *   3. ANSI HELPERS     — methods that apply color/style to text
 *   4. TEXT UTILITIES   — methods for padding, trimming, wrapping strings
 *   5. BOX DRAWING      — methods that draw the ┌─┐ style borders
 *
 * All methods are static (no instances needed).
 * All methods are package-private (only used inside util.ui).
 */
final class HelperFunctions {

    // =========================================================================
    // 1. ANSI CONSTANTS
    //    ANSI escape codes are special character sequences that tell the terminal
    //    to change text color or style. They start with ESC (\u001B) followed by
    //    "[" and end with "m". Example: "\u001B[1m" means "bold text".
    //    If the terminal doesn't support ANSI, we skip these entirely.
    // =========================================================================

    /** Start of every ANSI escape sequence. */
    static final String ESC   = "\u001B[";

    /** Resets ALL styles back to normal. Always append this after a color/style. */
    static final String RESET = ESC + "0m";

    /** Makes text bold / brighter. */
    private static final String BOLD = ESC + "1m";

    /** Makes text dimmer / less prominent (good for hints and secondary info). */
    private static final String DIM  = ESC + "2m";

    // =========================================================================
    // 2. LAYOUT CONSTANTS
    //    Fixed pixel/character widths for boxes. Tuned to fit a standard terminal.
    //    Box width includes the two border characters (│ on each side).
    // =========================================================================

    /** Max reserved cards a player can hold (Splendor rule). */
    static final int MAX_RESERVED = 3;

    /** Width of each card box in the market (4 cards fit side by side). */
    static final int CARD_BOX_WIDTH   = 30;

    /** Width of each noble box. Inner space fits full cost lines. */
    static final int NOBLE_BOX_WIDTH  = 34;

    /** Width of each player summary box. Wider so all info fits in one row. */
    static final int PLAYER_BOX_WIDTH = 54;

    // Unicode box-drawing characters — these look cleaner than plain ASCII +/-
    static final String H  = "─";   // horizontal line
    static final String TL = "┌";   // top-left corner
    static final String TR = "┐";   // top-right corner
    static final String BL = "└";   // bottom-left corner
    static final String BR = "┘";   // bottom-right corner
    static final String VB = "│";   // vertical bar (left/right side)

    /**
     * Tokens always appear in this order everywhere on screen.
     * This makes it easy for players to scan costs at a glance.
     */
    static final Token[] COST_ORDER = {
            Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE
    };

    // Private constructor — no one should create an instance of this class.
    private HelperFunctions() {}

    // =========================================================================
    // 3. ANSI HELPERS
    //    These methods wrap text in ANSI codes, but only when ansi=true.
    //    When ansi=false they return the plain text unchanged, so the game
    //    still works in terminals that don't support colors.
    // =========================================================================

    /** Makes text bold. Falls back to plain text when ansi is off. */
    static String bold(String text, boolean ansi) {
        return ansi ? (BOLD + text + RESET) : text;
    }

    /** Makes text dimmer (for hints, secondary labels). Falls back to plain. */
    static String dim(String text, boolean ansi) {
        return ansi ? (DIM + text + RESET) : text;
    }

    /**
     * Returns the 3-character abbreviation for a token color.
     * Example: BLACK → "Blk", BLUE → "Blu"
     * Used in tight spaces where the full name won't fit.
     */
    static String tokenShortLabel(Token token) {
        if (token == null) return "?";
        return switch (token) {
            case BLACK -> "Blk";
            case BLUE  -> "Blu";
            case GREEN -> "Grn";
            case RED   -> "Red";
            case WHITE -> "Wht";
            case GOLD  -> "Gld";
        };
    }

    /**
     * Returns the token label used in most places.
     * Currently the same as tokenShortLabel but returns "(none)" for null.
     */
    static String tokenLabel(Token token) {
        if (token == null) return "(none)";
        return tokenShortLabel(token);
    }

    /**
     * Wraps a token's label in the matching foreground color.
     * Example: "Blk" becomes gray text, "Red" becomes red text.
     * Falls back to plain text when ansi is off.
     */
    static String colorizeToken(Token token, String text, boolean ansi) {
        if (!ansi) return text;
        if (token == Token.BLACK) return text;
        // ANSI foreground color codes for each gem type
        String color = switch (token) {
            case GREEN -> ESC + "32m";   // green
            case WHITE -> ESC + "97m";   // bright white
            case BLUE  -> ESC + "34m";   // blue
            case BLACK -> ESC + "37m";
            case RED   -> ESC + "31m";   // red
            case GOLD  -> ESC + "33m";   // yellow/gold
        };
        return color + text + RESET;
    }

    /**
     * Draws a background-colored "chip" for a token.
     * Used inside card boxes as the bonus indicator (e.g. a green chip for green bonus).
     * Falls back to the plain 3-char label when ansi is off.
     */
    static String gemChip(Token token, boolean ansi) {
        if (!ansi) return tokenLabel(token);
        // ANSI background color codes — the text sits on a colored background
        String code = switch (token) {
            case GREEN -> ESC + "42m";        // green background
            case WHITE -> ESC + "47;30m";     // white background + black text
            case BLUE  -> ESC + "44m";        // blue background
            case BLACK -> ESC + "40;97m";     // black background + bright white text
            case RED   -> ESC + "41m";        // red background
            case GOLD  -> ESC + "43;30m";     // yellow background + black text
        };
        return code + " " + tokenLabel(token) + " " + RESET;
    }

    /**
     * Returns a bold foreground color code for use on card box borders.
     * The border color matches the card's bonus gem, making card types
     * easy to identify at a glance.
     */
    static String gemBorderColor(Token token) {
        return switch (token) {
            case GREEN -> ESC + "1;32m";   // bold green
            case WHITE -> ESC + "1;37m";   // bold white
            case BLUE  -> ESC + "1;34m";   // bold blue
            case BLACK -> ESC + "1;37m";   // use bold white for better terminal support
            case RED   -> ESC + "1;31m";   // bold red
            case GOLD  -> ESC + "1;33m";   // bold yellow
        };
    }

    // =========================================================================
    // 4. TEXT UTILITIES
    //    String helpers that are needed by multiple renderers.
    //
    //    KEY RULE: All width calculations must use visLen() not .length().
    //    ANSI escape codes add invisible characters that make .length() wrong.
    //    visLen() strips those codes before counting so alignment stays correct.
    // =========================================================================

    /**
     * Removes all ANSI escape sequences from a string.
     * Example: "\u001B[1mHello\u001B[0m" becomes "Hello".
     * Used before measuring string width.
     */
    static String stripAnsi(String s) {
        if (s == null) return "";
        return s.replaceAll("\u001B\\[[^m]*m", "");
    }

    /**
     * Returns the visible (printed) length of a string — ANSI codes don't count.
     * Use this instead of s.length() whenever measuring strings for alignment.
     */
    static int visLen(String s) {
        return stripAnsi(s).length();
    }

    /**
     * Adds spaces on the right until the visible length equals 'width'.
     * If the string is already at or past 'width', returns it unchanged.
     */
    static String padRight(String s, int width) {
        int vis = visLen(s);
        if (vis >= width) return s;
        return s + " ".repeat(width - vis);
    }

    /**
     * Cuts a string down so it fits in 'width' visible characters.
     * Adds "..." when the string is truncated.
     * Handles ANSI escape codes correctly so colors aren't cut mid-sequence.
     */
    static String truncateVisible(String s, int width) {
        String safe = (s == null) ? "" : s;
        // Nothing to do if it already fits
        if (visLen(safe) <= width) return safe;
        if (width <= 3) return ".".repeat(Math.max(0, width));

        StringBuilder sb = new StringBuilder();
        int vis    = 0;
        int target = width - 3; // leave 3 chars for "..."

        for (int i = 0; i < safe.length() && vis < target; ) {
            char c = safe.charAt(i);
            // If we hit an ANSI escape code, copy it whole (it has no visible width)
            if (c == '\u001B' && i + 1 < safe.length() && safe.charAt(i + 1) == '[') {
                int m = safe.indexOf('m', i);
                if (m < 0) break;
                sb.append(safe, i, m + 1);
                i = m + 1;
                continue;
            }
            // Normal visible character
            sb.append(c);
            vis++;
            i++;
        }
        sb.append("...");
        // If we left an ANSI code open, reset it so colors don't bleed
        if (safe.contains(ESC)) sb.append(RESET);
        return sb.toString();
    }

    /**
     * Makes a string exactly 'width' visible characters long.
     * Truncates if too long, pads with spaces if too short.
     * Used to fill every line inside a box to the same width.
     */
    static String fitLine(String s, int width) {
        if (width <= 0) return "";
        String truncated = truncateVisible(s == null ? "" : s, width);
        int vis = visLen(truncated);
        if (vis < width) truncated = truncated + " ".repeat(width - vis);
        return truncated;
    }

    /**
     * Places 'left' text on the left and 'right' text flush against the right edge,
     * filling the gap with spaces. Used for lines like "Card ID ... 3 pts".
     * Falls back to just the left text if there's no room for both.
     */
    static String rightMarkerLine(String left, String right, int innerWidth) {
        String l   = (left  == null) ? "" : left;
        String r   = (right == null) ? "" : right;
        int    gap = innerWidth - visLen(l) - visLen(r);
        if (gap < 1) return truncateVisible(l, innerWidth);
        return l + " ".repeat(gap) + r;
    }

    /**
     * Centers text within 'width' by adding spaces on the left.
     * If text is wider than 'width', returns it as-is.
     */
    static String centerLine(String text, int width) {
        String t   = (text == null) ? "" : text;
        int    pad = Math.max(0, (width - visLen(t)) / 2);
        return " ".repeat(pad) + t;
    }

    /**
     * Splits a string into multiple lines that each fit within 'width' characters.
     * Splits only at spaces — won't break a word in the middle.
     * Used to wrap long cost strings inside a narrow card box.
     */
    static List<String> wrapBySpaces(String text, int width) {
        if (text == null) return List.of();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return List.of();
        int w = Math.max(1, width);

        String[]      words   = trimmed.split("\\s+");
        List<String>  lines   = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (current.length() == 0) {
                // First word on this line — split if the word itself is too long
                appendWordOrSplit(lines, current, word, w);
            } else if (current.length() + 1 + word.length() <= w) {
                // Word fits on the current line
                current.append(' ').append(word);
            } else {
                // Word doesn't fit — start a new line
                lines.add(current.toString());
                current.setLength(0);
                appendWordOrSplit(lines, current, word, w);
            }
        }

        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    /** Helper for wrapBySpaces — splits a single word if it exceeds the width. */
    private static void appendWordOrSplit(List<String> lines, StringBuilder current, String word, int w) {
        if (word.length() <= w) {
            current.append(word);
            return;
        }
        // Word is longer than the line width — force-split it character by character
        int idx = 0;
        while (idx < word.length()) {
            int end = Math.min(word.length(), idx + w);
            lines.add(word.substring(idx, end));
            idx = end;
        }
    }

    // =========================================================================
    // 5. BOX DRAWING
    //    Methods that draw ┌─┐ style borders around content.
    //
    //    Each box has:
    //      - A top border:    ┌──────┐
    //      - Content lines:   │ text │
    //      - A bottom border: └──────┘
    //
    //    An optional 'borderColor' ANSI code can color the border lines.
    //    Pass null to get plain (uncolored) borders.
    // =========================================================================

    /**
     * Builds the top border of a box.
     * Example (no color): ┌──────────────┐
     * 'innerWidth' is the number of ─ characters between the corners.
     */
    static String borderTop(int innerWidth, String borderColor, boolean ansi) {
        String h = H.repeat(Math.max(0, innerWidth));
        if (!ansi || borderColor == null) return TL + h + TR;
        return borderColor + TL + h + TR + RESET;
    }

    /** Builds the bottom border of a box. Example: └──────────────┘ */
    static String borderBottom(int innerWidth, String borderColor, boolean ansi) {
        String h = H.repeat(Math.max(0, innerWidth));
        if (!ansi || borderColor == null) return BL + h + BR;
        return borderColor + BL + h + BR + RESET;
    }

    /** Returns a left-side border character │ with optional color. */
    static String borderLeft(String borderColor, boolean ansi) {
        if (!ansi || borderColor == null) return VB;
        return borderColor + VB + RESET;
    }

    /** Returns a right-side border character │ with optional color. */
    static String borderRight(String borderColor, boolean ansi) {
        if (!ansi || borderColor == null) return VB;
        return borderColor + VB + RESET;
    }

    /**
     * Places multiple boxes side by side, horizontally.
     * Each box is a List of strings (one per line).
     * All boxes are padded to the same height so no box looks cut short.
     *
     * Example: [Card A box] + [Card B box] → one row with both boxes.
     */
    static String joinBoxesSideBySide(List<List<String>> boxes) {
        if (boxes == null || boxes.isEmpty()) return "";

        // Find the tallest box so we can pad shorter ones to match
        int maxH = boxes.stream().mapToInt(List::size).max().orElse(0);

        // Normalize every box to maxH lines
        List<List<String>> normalized = new ArrayList<>();
        for (List<String> box : boxes) {
            if (box.size() == maxH) {
                normalized.add(new ArrayList<>(box));
                continue;
            }
            // Box is shorter — insert blank content lines before the bottom border
            int          boxW   = visLen(box.get(0));
            int          inner  = boxW - 2;
            List<String> padded = new ArrayList<>(box.subList(0, box.size() - 1)); // drop bottom border
            while (padded.size() < maxH - 1) {
                padded.add(VB + " ".repeat(inner) + VB); // blank content line
            }
            padded.add(box.get(box.size() - 1)); // re-attach bottom border
            normalized.add(padded);
        }

        // Build the final string by combining row-i of each box on the same line
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxH; i++) {
            StringBuilder row = new StringBuilder();
            for (int b = 0; b < normalized.size(); b++) {
                if (b > 0) row.append("  "); // 2-space gap between boxes
                row.append(normalized.get(b).get(i));
            }
            sb.append(row).append('\n');
        }
        return sb.toString();
    }

    /**
     * Arranges a list of boxes into a grid with a fixed number of columns.
     * Used for player boxes (2 columns) and noble boxes (up to 3 columns).
     *
     * Example with 4 boxes and 2 columns:
     *   [Box 1] [Box 2]
     *   [Box 3] [Box 4]
     */
    static String renderBoxesGrid(List<List<String>> boxes, int columns) {
        if (boxes == null || boxes.isEmpty()) return "";
        int cols = Math.max(1, columns);
        StringBuilder sb = new StringBuilder();

        // Process 'cols' boxes at a time (one grid row)
        for (int i = 0; i < boxes.size(); i += cols) {
            int    end   = Math.min(boxes.size(), i + cols);
            String block = joinBoxesSideBySide(boxes.subList(i, end));
            if (sb.length() > 0) sb.append('\n'); // blank line between grid rows
            // Strip trailing newline from block (will be added by the loop)
            if (block.endsWith("\n")) block = block.substring(0, block.length() - 1);
            sb.append(block);
        }
        return sb.toString();
    }
}
