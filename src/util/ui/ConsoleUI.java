package util.ui;

import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import logic.Game;
import model.Difficulty;
import model.Player;
import model.Token;
import util.AnsiSupport;

/**
 * Public UI facade used by the rest of the project.
 *
 * Important design choice: this class is responsible for interaction
 * (reading input + printing). It delegates all layout/string-building to
 * {@link ConsoleRenderer}. This keeps the UI code easy to explain and ensures
 * local and networking mode share the same board rendering.
 */
public class ConsoleUI {

    // ANSI escape used to clear the screen and move the cursor to top-left.
    // Jansi (AnsiConsole.systemInstall()) enables these sequences on Windows terminals.
    private static final String ANSI_CLEAR = "\u001B[2J\u001B[H";

    // Local setup bounds (kept here so Main/GameApp don't need to change).
    private static final int MIN_PLAYERS   = 2;
    private static final int MAX_PLAYERS   = 4;
    private static final int MIN_WIN_SCORE = 5;
    private static final int MAX_WIN_SCORE = 30;

    private static final String ESC   = "\u001B[";
    private static final String RESET = ESC + "0m";
    private static final String BOLD  = ESC + "1m";
    private static final String DIM   = ESC + "2m";

    private final Scanner sc;
    private final boolean ansiEnabled;

    /**
     * Returned by {@link #runLocalSetupMenu(int, int)} to keep Main clean:
     * it can pass the chosen values straight into game setup.
     */
    public static final class SetupOptions {
        public final int numPlayers;
        public final int winScore;

        public SetupOptions(int numPlayers, int winScore) {
            this.numPlayers = numPlayers;
            this.winScore = winScore;
        }
    }

    public ConsoleUI() {
        this(new Scanner(System.in));
    }

    public ConsoleUI(Scanner scanner) {
        this.sc = Objects.requireNonNull(scanner, "scanner");
        this.ansiEnabled = AnsiSupport.isSupported();
    }

    /**
     * Backward-compatible setup prompt used by {@code Main}.
     *
     * This method exists because older versions of the project asked for the
     * win score first, before asking for player count.
     *
     * @param defaultWinScore config default to show when the user just presses Enter
     * @return chosen winning score within {@link #MIN_WIN_SCORE}..{@link #MAX_WIN_SCORE}
     */
    public int getWinningPoints(int defaultWinScore) {
        clearScreen();
        System.out.println(bold("SPLENDOR") + dim("  ·  Setup"));
        System.out.println(dim("Choose the target prestige points to win."));
        System.out.println(dim("Press enter to use default configured winning points. Current: " + defaultWinScore));
        System.out.println();
        return promptIntInline("Points to win", MIN_WIN_SCORE, MAX_WIN_SCORE,
                clamp(defaultWinScore, MIN_WIN_SCORE, MAX_WIN_SCORE));
    }

    /**
     * Backward-compatible setup prompt used by {@code Main}.
     *
     * @return chosen number of players within {@link #MIN_PLAYERS}..{@link #MAX_PLAYERS}
     */
    public int getNumberOfPlayers() {
        clearScreen();
        System.out.println(bold("SPLENDOR") + dim("  ·  Setup"));
        System.out.println(dim("Choose how many players will play on this computer."));
        System.out.println();
        return promptIntInline("Number of players", MIN_PLAYERS, MAX_PLAYERS, MIN_PLAYERS);
    }

    /**
     * Clears the screen and prints the full game board.
     * Game logic never lives here — this is strictly view code.
     */
    public void displayGameState(Game game, int winScore) {
        Objects.requireNonNull(game, "game");
        clearScreen();
        System.out.print(ConsoleRenderer.renderGameStateString(game, ansiEnabled, winScore));
        System.out.flush();
    }

    /**
     * Clears the screen and prints the final board + winner banner.
     */
    public void displayGameOver(Game game, int winScore) {
        Objects.requireNonNull(game, "game");
        clearScreen();
        System.out.print(ConsoleRenderer.renderGameOverString(game, ansiEnabled, winScore));
        System.out.flush();
    }

    /**
     * Reserved cards are a separate screen in local mode.
     * The actual formatting is shared with networking via {@link ConsoleRenderer}.
     */
    public void displayReservedCards(Player player) {
        Objects.requireNonNull(player, "player");
        clearScreen();
        System.out.print(ConsoleRenderer.renderReservedCardsString(player, ansiEnabled));
        System.out.println();
        pauseForEnter();
    }

    /**
     * Single-screen setup menu requested in the assignment: show the values,
     * then let the user pick what to change from a prompt at the bottom.
     *
     * @param defaultPlayers initial number of players to show
     * @param defaultWinScore initial winning points to show
     * @return chosen options, or null if the user quits
     */
    public SetupOptions runLocalSetupMenu(int defaultPlayers, int defaultWinScore) {
        int players  = clamp(defaultPlayers, MIN_PLAYERS, MAX_PLAYERS);
        int winScore = clamp(defaultWinScore, MIN_WIN_SCORE, MAX_WIN_SCORE);

        while (true) {
            clearScreen();
            System.out.println(bold("SPLENDOR") + dim("  ·  Game Setup"));
            System.out.println(dim("Edit settings, then start. Values update on this screen."));
            System.out.println();
            System.out.println("  [1]  Number of players : " + bold(String.valueOf(players))
                    + dim("  (" + MIN_PLAYERS + "-" + MAX_PLAYERS + ")"));
            System.out.println("  [2]  Points to win     : " + bold(String.valueOf(winScore))
                    + dim("  (" + MIN_WIN_SCORE + "-" + MAX_WIN_SCORE + ")"));
            System.out.println();
            System.out.println("  [S]  Start game");
            System.out.println("  [Q]  Quit");
            System.out.println();
            System.out.print("Select an option (1/2/S/Q): ");

            String choice = readLineLower();
            if (choice == null) return null;
            if (choice.isEmpty()) continue;

            switch (choice) {
                case "1" -> players = promptIntInline("Number of players", MIN_PLAYERS, MAX_PLAYERS, players);
                case "2" -> winScore = promptIntInline("Points to win", MIN_WIN_SCORE, MAX_WIN_SCORE, winScore);
                case "s" -> { return new SetupOptions(players, winScore); }
                case "q" -> { return null; }
                default  -> {
                    System.out.println("Invalid option.");
                    pauseForEnter();
                }
            }
        }
    }

    /**
     * Returns a boolean array where {@code true} means that player is AI.
     * Kept as a simple prompt loop so it is easy to explain and modify.
     */
    public boolean[] getPlayerTypes(int numPlayers) {
        int n = clamp(numPlayers, MIN_PLAYERS, MAX_PLAYERS);
        boolean[] isAI = new boolean[n];

        clearScreen();
        System.out.println(bold("Player Types") + dim("  (y = AI, n = Human)"));
        System.out.println();

        for (int i = 0; i < n; i++) {
            while (true) {
                System.out.printf("Player %d AI? (y/n) [n]: ", (i + 1));
                String resp = readLineLower();
                if (resp == null || resp.isBlank() || resp.startsWith("n")) {
                    isAI[i] = false;
                    break;
                }
                if (resp.startsWith("y")) {
                    isAI[i] = true;
                    break;
                }
                System.out.println("Please type y or n.");
            }
        }
        return isAI;
    }

    /**
     * Prompts for the AI difficulty level for a specific player.
     *
     * @param playerNumber 1-based player number (for display only)
     * @return the chosen {@link Difficulty}
     */
    public Difficulty getAIDifficulty(int playerNumber) {
        clearScreen();
        System.out.println(bold("AI Difficulty") + dim("  ·  Player " + playerNumber));
        System.out.println();
        System.out.println("  [1] Easy   — naive play: random gems, buys first available card");
        System.out.println("  [2] Medium — smart gem collection, scores and picks best cards");
        System.out.println("  [3] Hard   — discount engine, endgame urgency, opponent blocking");
        System.out.println("  [4] Insane — sends board to Claude AI for optimal decisions");
        System.out.println();
        return switch (promptIntInline("Difficulty", 1, 4, 2)) {
            case 1 -> Difficulty.EASY;
            case 3 -> Difficulty.HARD;
            case 4 -> Difficulty.INSANE;
            default -> Difficulty.MEDIUM;
        };
    }

    public static String renderGameStateString(Game game, boolean ansi, int winScore) {
        return ConsoleRenderer.renderGameStateString(game, ansi, winScore);
    }

    public static String renderGameOverString(Game game, boolean ansi, int winScore) {
        return ConsoleRenderer.renderGameOverString(game, ansi, winScore);
    }

    public static String formatCostCompactStatic(Map<Token, Integer> cost) {
        return ConsoleRenderer.formatCostCompactStatic(cost);
    }

    // Clears the terminal so each turn redraw looks like a "single screen".
    private void clearScreen() {
        if (ansiEnabled) {
            System.out.print(ANSI_CLEAR);
            System.out.flush();
            return;
        }
        System.out.println("\n".repeat(40));
    }

    // Reads a full line and normalizes it for menu prompts; null means EOF/input closed.
    private String readLineLower() {
        try {
            if (!sc.hasNextLine()) return null;
            return sc.nextLine().trim().toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    // Prompts for an integer and keeps asking until it is within [min, max].
    private int promptIntInline(String label, int min, int max, int current) {
        while (true) {
            System.out.printf("%s (%d-%d) [%d]: ", label, min, max, current);
            String line = readLineLower();
            if (line == null) return current;
            if (line.isBlank()) return current;
            try {
                int n = Integer.parseInt(line.trim());
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException ignored) { }
            System.out.printf("Please enter a number between %d and %d.%n", min, max);
        }
    }

    // Small pause used after full-screen views so the user can read before continuing.
    private void pauseForEnter() {
        System.out.print(dim("Press Enter to continue..."));
        try { if (sc.hasNextLine()) sc.nextLine(); } catch (Exception ignored) { }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String bold(String text) { return ansiEnabled ? (BOLD + text + RESET) : text; }
    private String dim(String text)  { return ansiEnabled ? (DIM + text + RESET) : text; }
}
