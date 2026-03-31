package util.ui;

import model.Token;

/**
 * Shared constants for console rendering.
 *
 * <p>Kept in a separate file so the main renderer facade stays small and the
 * values are easy to find/justify in a code review.</p>
 */
final class ConsoleConstants {

    /** Splendor rule: a player may reserve at most 3 cards. */
    static final int MAX_RESERVED = 3;

    // Box widths (including borders). These values are tuned so the layout fits
    // typical terminal widths while keeping text readable.
    static final int CARD_BOX_WIDTH   = 30;  // 4 per row in the market
    static final int NOBLE_BOX_WIDTH  = 34;  // inner=32 fits "Needs: Blk3 Blu3 Grn3 Red3 Wht3"
    static final int PLAYER_BOX_WIDTH = 54;  // wider for clear player summaries

    // Unicode box-drawing characters (used instead of ASCII to look cleaner).
    static final String H  = "─";
    static final String TL = "┌";
    static final String TR = "┐";
    static final String BL = "└";
    static final String BR = "┘";
    static final String VB = "│";

    // Tokens appear in a consistent order everywhere (matches the legend line).
    static final Token[] COST_ORDER = {
            Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE
    };

    private ConsoleConstants() {
    }
}
