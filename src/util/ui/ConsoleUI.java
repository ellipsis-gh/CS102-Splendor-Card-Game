package util.ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import logic.Game;
import model.Board;
import model.Card;
import model.Noble;
import model.Player;
import model.Token;

/**
 * Console UI helper to gather setup input and render the full game state each turn.
 *
 * <p>This class intentionally contains <em>no</em> AI logic. AI moves are computed by the already-existing AI
 * implementation elsewhere in the codebase; this UI only displays the results.</p>
 */
public class ConsoleUI {
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 4;

    private static final String ESC = "\u001B[";
    private static final String RESET = ESC + "0m";
    private static final String BOLD = ESC + "1m";
    private static final String DIM = ESC + "2m";
    private static final String CLEAR = ESC + "2J" + ESC + "H";

    private static final Token[] COST_ORDER = {
            Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE
    };

    private final Scanner sc;
    private final boolean ansiEnabled;

    /**
     * Constructs a UI that reads from {@code System.in}.
     */
    public ConsoleUI() {
        this(new Scanner(System.in));
    }

    /**
     * Constructs a UI that reads input from the provided scanner.
     *
     * @param scanner scanner to read input from
     */
    public ConsoleUI(Scanner scanner) {
        this.sc = Objects.requireNonNull(scanner, "scanner");
        this.ansiEnabled = detectAnsiSupport();
    }

    /**
     * Prompts for the number of players (2–4).
     *
     * @return number of players
     */
    public int getNumberOfPlayers() {
        while (true) {
            System.out.printf("How many players? (%d-%d): ", MIN_PLAYERS, MAX_PLAYERS);
            String line = readLine();
            if (line == null) {
                return MIN_PLAYERS;
            }
            line = line.trim();

            try {
                int n = Integer.parseInt(line);
                if (n >= MIN_PLAYERS && n <= MAX_PLAYERS) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
                // reprompt
            }
            System.out.printf("Please enter a number between %d and %d.%n", MIN_PLAYERS, MAX_PLAYERS);
        }
    }

    /**
     * Prompts whether each player is AI-controlled.
     *
     * @param numberOfPlayers number of players
     * @return boolean array where {@code true} means the player is AI-controlled
     */
    public boolean[] getPlayerTypes(int numberOfPlayers) {
        boolean[] isAI = new boolean[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            while (true) {
                System.out.print("Is Player " + (i + 1) + " an AI? (y/n): ");
                String resp = readLine();
                if (resp == null) {
                    isAI[i] = false;
                    break;
                }
                resp = resp.trim().toLowerCase();

                if (resp.startsWith("y")) {
                    isAI[i] = true;
                    break;
                }

                if (resp.startsWith("n")) {
                    isAI[i] = false;
                    break;
                }

                System.out.println("Please answer y or n.");
            }
        }
        return isAI;
    }

    /**
     * Clears the screen and prints the full board state (players, bank, nobles, and face-up cards).
     *
     * @param game game state to render
     */
    public void displayGameState(Game game) {
        Objects.requireNonNull(game, "game");
        clearScreen();

        Board board = game.getBoard();
        List<Player> players = game.getPlayers();
        int currentIndex = game.getCurrentPlayerIndex();

        System.out.println(bold("SPLENDOR") + dim("  (" + players.size() + " players)"));
        System.out.println(dim("Current: " + players.get(currentIndex).getName()));
        System.out.println();

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            boolean isCurrent = (i == currentIndex);
            String type = p.isHuman() ? "Human" : "AI";
            String marker = isCurrent ? "  <==" : "";

            System.out.printf("P%d %-5s %-18s  %2d pts  Tokens: %s  Bonuses: %s  Reserved:%d  Purchased:%d%s%n",
                    (i + 1),
                    ("[" + type + "]"),
                    p.getName(),
                    p.getScore(),
                    formatTokenMap(p.getTokens(), true),
                    formatTokenMap(p.getBonuses(), false),
                    p.getHand().size(),
                    p.getPurchasedCards().size(),
                    marker);
        }

        System.out.println();
        System.out.println("Bank:   " + formatTokenMap(board.getAvailableTokens(), true));

        List<Noble> nobles = board.getNobles();
        System.out.print("Nobles: ");
        if (nobles.isEmpty()) {
            System.out.println("(none)");
        } else {
            for (int i = 0; i < nobles.size(); i++) {
                System.out.print("[" + i + ": " + formatNoble(nobles.get(i)) + "] ");
            }
            System.out.println();
        }

        for (int level = 1; level <= 3; level++) {
            System.out.println();
            System.out.println(bold("Level " + level));
            Card[] row = board.getVisibleCards(level);
            for (int s = 0; s < row.length; s++) {
                Card c = row[s];
                System.out.println("  [" + level + "-" + s + "] " + (c == null ? dim("(empty)") : formatCard(c)));
            }
        }
    }

    private String formatCard(Card c) {
        return c.getPrestigePoints() + " pts  Bonus: " + tokenLabel(c.getBonus()) + "  Cost: " + formatCost(c.getCost());
    }

    private String formatNoble(Noble n) {
        return n.getPrestigePoints() + " pts  Needs: " + formatCost(n.getCost());
    }

    private String formatCost(Map<Token, Integer> cost) {
        StringBuilder sb = new StringBuilder();
        for (Token t : COST_ORDER) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) {
                sb.append(colorizeToken(t, tokenLabel(t))).append(v).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String formatTokenMap(Map<Token, Integer> map, boolean includeGold) {
        Map<Token, Integer> safe = (map == null) ? new EnumMap<>(Token.class) : map;
        StringBuilder sb = new StringBuilder();
        for (Token t : COST_ORDER) {
            sb.append(colorizeToken(t, tokenLabel(t))).append(":").append(safe.getOrDefault(t, 0)).append(" ");
        }
        if (includeGold) {
            sb.append(colorizeToken(Token.GOLD, tokenLabel(Token.GOLD))).append(":").append(safe.getOrDefault(Token.GOLD, 0));
        }
        return sb.toString().trim();
    }

    private String tokenLabel(Token token) {
        if (token == null) return "(none)";
        return switch (token) {
            case BLACK -> "Blk";
            case BLUE -> "Blu";
            case GREEN -> "Grn";
            case RED -> "Red";
            case WHITE -> "Wht";
            case GOLD -> "Gld";
        };
    }

    private String colorizeToken(Token token, String text) {
        if (!ansiEnabled) {
            return text;
        }
        String color = switch (token) {
            case GREEN -> ESC + "32m";
            case WHITE -> ESC + "37m";
            case BLUE -> ESC + "34m";
            case BLACK -> ESC + "90m";
            case RED -> ESC + "31m";
            case GOLD -> ESC + "33m";
        };
        return color + text + RESET;
    }

    private String bold(String text) {
        return ansiEnabled ? (BOLD + text + RESET) : text;
    }

    private String dim(String text) {
        return ansiEnabled ? (DIM + text + RESET) : text;
    }

    private void clearScreen() {
        if (ansiEnabled) {
            System.out.print(CLEAR);
            System.out.flush();
            return;
        }
        System.out.println("\n".repeat(40));
    }

    private boolean detectAnsiSupport() {
        if (System.console() == null) {
            return false;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            // Be conservative on Windows: many consoles still print raw ESC sequences.
            return System.getenv("WT_SESSION") != null
                    || System.getenv("ANSICON") != null
                    || "ON".equalsIgnoreCase(System.getenv("ConEmuANSI"))
                    || System.getenv("TERM") != null;
        }

        String term = System.getenv("TERM");
        return term != null && !term.equalsIgnoreCase("dumb");
    }

    private String readLine() {
        try {
            if (!sc.hasNextLine()) {
                return null;
            }
            return sc.nextLine();
        } catch (Exception e) {
            return null;
        }
    }
}
