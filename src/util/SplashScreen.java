package util;

/**
 * Displays the Splendor ASCII-art title screen at startup.
 *
 * Three public methods handle the three game modes:
 *   SplashScreen.local()               — single-machine game
 *   SplashScreen.server(players, port) — multiplayer server
 *   SplashScreen.client()              — multiplayer client
 *
 * On Windows, call AnsiConsole.systemInstall() before this so that
 * colour codes are interpreted correctly.
 */
public final class SplashScreen {

    // ── ANSI escape codes ──────────────────────────────────────────────────────
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String DIM     = "\u001B[2m";
    private static final String GOLD    = "\u001B[1;33m";  // bold yellow — the "gold" token colour
    private static final String C_BLACK = "\u001B[90m";    // gem colours
    private static final String C_BLUE  = "\u001B[34m";
    private static final String C_GREEN = "\u001B[32m";
    private static final String C_RED   = "\u001B[31m";
    private static final String C_WHITE = "\u001B[1;37m";
    private static final String CLEAR   = "\u001B[2J\u001B[H";

    // ── Big ASCII-art title (ANSI Shadow / block font) ─────────────────────────
    // Each row is exactly 67 visible characters wide.
    // Characters: S P L E N D O R
    private static final String[] TITLE = {
        "███████╗██████╗ ██╗     ███████╗███╗   ██╗██████╗  ██████╗ ██████╗ ",
        "██╔════╝██╔══██╗██║     ██╔════╝████╗  ██║██╔══██╗██╔═══██╗██╔══██╗",
        "███████╗██████╔╝██║     █████╗  ██╔██╗ ██║██║  ██║██║   ██║██████╔╝",
        "╚════██║██╔═══╝ ██║     ██╔══╝  ██║╚██╗██║██║  ██║██║   ██║██╔══██╗",
        "███████║██║     ███████╗███████╗██║ ╚████║██████╔╝╚██████╔╝██║  ██║",
        "╚══════╝╚═╝     ╚══════╝╚══════╝╚═╝  ╚═══╝╚═════╝  ╚═════╝ ╚═╝  ╚═╝"
    };

    // Visible width of each TITLE row (must match the strings above)
    private static final int TITLE_WIDTH = 67;

    // Width of the content area between the ║ box borders
    private static final int INNER = 95;

    // Left padding so the title is centred inside the box.
    private static final int TITLE_PAD = (INNER - TITLE_WIDTH) / 2;

    private SplashScreen() {}

    // ── Public entry points ────────────────────────────────────────────────────

    /** Title screen for the local (single-machine) game. */
    public static void local() {
        render(AnsiSupport.isSupported(),
               "LOCAL GAME",
               "2 – 4 Players  |  First to reach the goal wins!");
    }

    /** Title screen for the multiplayer game server. */
    public static void server(int numPlayers, int port) {
        render(AnsiSupport.isSupported(),
               "SERVER  ·  " + numPlayers + "-Player Game",
               "Port " + port + "  |  Waiting for players to connect...");
    }

    /** Title screen for the multiplayer game client. */
    public static void client() {
        render(AnsiSupport.isSupported(),
               "MULTIPLAYER CLIENT",
               "Connect to a running Splendor server");
    }

    // ── Core renderer ──────────────────────────────────────────────────────────

    private static void render(boolean ansi, String modeLine, String detailLine) {
        clearScreen(ansi);

        // Reusable box parts
        String hRule  = "═".repeat(INNER);
        String blank  = "║" + " ".repeat(INNER) + "║";
        String top    = "╔" + hRule + "╗";
        String bottom = "╚" + hRule + "╝";

        // Style the border itself if ANSI is available
        top    = ansi ? GOLD + top    + RESET : top;
        bottom = ansi ? GOLD + bottom + RESET : bottom;

        // ── Top border ─────────────────────────────────────────────────────────
        println(top);
        println(blank);
        println(blank);

        // ── Big block-letter title ─────────────────────────────────────────────
        String leftPad  = " ".repeat(TITLE_PAD);
        String rightPad = " ".repeat(INNER - TITLE_PAD - TITLE_WIDTH);
        for (String row : TITLE) {
            String styled = ansi ? (GOLD + row + RESET) : row;
            println("║" + leftPad + styled + rightPad + "║");
        }

        println(blank);
        println(blank);

        // ── Gem-colour legend ──────────────────────────────────────────────────
        println("║" + centred(gemLegend(ansi), INNER, ansi) + "║");

        println(blank);
        println(blank);

        // ── Tagline ────────────────────────────────────────────────────────────
        String tagline = "◆  Collect gems.  Buy cards.  Win!  ◆";
        String styledTag = ansi ? (BOLD + tagline + RESET) : tagline;
        println("║" + centred(styledTag, INNER, ansi) + "║");

        println(blank);
        println(blank);

        // ── Mode / detail ──────────────────────────────────────────────────────
        String modeText   = ansi ? (BOLD + modeLine + RESET) : modeLine;
        String detailText = ansi ? (DIM  + detailLine + RESET) : detailLine;
        println("║" + centred(modeText, INNER, ansi) + "║");
        println("║" + centred(detailText, INNER, ansi) + "║");

        println(blank);

        // ── Bottom border ──────────────────────────────────────────────────────
        println(bottom);
        println("");   // breathing room before the next prompt
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Builds the gem-colour legend row.
     * With ANSI each dot (●) is printed in the matching gem colour.
     */
    private static String gemLegend(boolean ansi) {
        if (!ansi) {
            return "● Black   ● Blue   ● Green   ● Red   ● White   ★ Gold";
        }
        return C_BLACK + "●" + RESET + " Black   "
             + C_BLUE  + "●" + RESET + " Blue    "
             + C_GREEN + "●" + RESET + " Green   "
             + C_RED   + "●" + RESET + " Red     "
             + C_WHITE + "●" + RESET + " White   "
             + GOLD    + "★" + RESET + " Gold";
    }

    /**
     * Centres text inside a field of width visible characters.
     * ANSI escape codes are stripped before measuring length so colour codes
     * do not affect the padding calculation.
     */
    private static String centred(String text, int width, boolean ansi) {
        int len      = visLen(text);
        int leftPad  = Math.max(0, (width - len) / 2);
        int rightPad = Math.max(0, width - len - leftPad);
        return " ".repeat(leftPad) + text + " ".repeat(rightPad);
    }

    /** Visible character count: strips ANSI escape sequences before measuring. */
    private static int visLen(String s) {
        return s == null ? 0 : s.replaceAll("\u001B\\[[^m]*m", "").length();
    }

    private static void println(String s) { System.out.println(s); }

    private static void clearScreen(boolean ansi) {
        if (ansi) {
            System.out.print(CLEAR);
            System.out.flush();
        } else {
            // Fallback: scroll past old content
            for (int i = 0; i < 40; i++) System.out.println();
        }
    }
}
