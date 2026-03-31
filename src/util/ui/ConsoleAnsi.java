package util.ui;

import model.Token;

/**
 * ANSI styling helpers (colors, bold/dim, gem chips).
 *
 * <p>All renderers pass an {@code ansi} flag. When false, these helpers return
 * plain text without escape sequences so output is readable in any console.</p>
 *
 * <p>Note on codes like {@code 90m}: ANSI uses numbers to represent colors.
 * {@code 90m} is "bright black" (often rendered as gray). If a console does not
 * interpret escape sequences, you might see raw text like {@code [90m} in the
 * output — that is why local/network clients enable Jansi and also allow ANSI
 * to be disabled.</p>
 */
final class ConsoleAnsi {

    static final String ESC   = "\u001B[";
    static final String RESET = ESC + "0m";
    private static final String BOLD = ESC + "1m";
    private static final String DIM  = ESC + "2m";

    private ConsoleAnsi() {
    }

    /** Applies bold styling when ANSI is enabled. */
    static String bold(String text, boolean ansi) {
        return ansi ? (BOLD + text + RESET) : text;
    }

    /** Applies dim styling when ANSI is enabled (used for hints/secondary labels). */
    static String dim(String text, boolean ansi) {
        return ansi ? (DIM + text + RESET) : text;
    }

    /** 3-character abbreviation used in tight layouts (e.g., "Blk", "Blu"). */
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

    /** User-facing token label (currently identical to {@link #tokenShortLabel(Token)}). */
    static String tokenLabel(Token token) {
        if (token == null) return "(none)";
        return tokenShortLabel(token);
    }

    /**
     * Colours a short token label.
     *
     * <p>We colour only the abbreviation, not the count, so alignment stays predictable.</p>
     */
    static String colorizeToken(Token token, String text, boolean ansi) {
        if (!ansi) return text;
        String color = switch (token) {
            case GREEN -> ESC + "32m";
            case WHITE -> ESC + "97m";
            case BLUE  -> ESC + "34m";
            case BLACK -> ESC + "90m";
            case RED   -> ESC + "31m";
            case GOLD  -> ESC + "33m";
        };
        return color + text + RESET;
    }

    /**
     * Background-coloured chip for a token — used as the bonus indicator inside card boxes.
     * Non-ANSI fallback is the plain 3-char abbreviation.
     */
    static String gemChip(Token token, boolean ansi) {
        if (!ansi) return tokenLabel(token);
        String code = switch (token) {
            case GREEN -> ESC + "42m";        // green bg
            case WHITE -> ESC + "47;30m";     // white bg + black fg
            case BLUE  -> ESC + "44m";        // blue bg
            case BLACK -> ESC + "40;97m";     // black bg + bright-white fg
            case RED   -> ESC + "41m";        // red bg
            case GOLD  -> ESC + "43;30m";     // yellow bg + black fg
        };
        return code + " " + tokenLabel(token) + " " + RESET;
    }

    /**
     * Border highlight colour for a card, based on its gem bonus.
     */
    static String gemBorderColor(Token token) {
        return switch (token) {
            case GREEN -> ESC + "1;32m";       // bold green
            case WHITE -> ESC + "1;37m";       // bold white
            case BLUE  -> ESC + "1;34m";       // bold blue
            case BLACK -> ESC + "1;90m";       // bright black/gray
            case RED   -> ESC + "1;31m";       // bold red
            case GOLD  -> ESC + "1;33m";       // bold yellow
        };
    }
}
