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
    private static final int MAX_RESERVED = 3;

    private static final String ESC = "\u001B[";
    private static final String RESET = ESC + "0m";
    private static final String BOLD = ESC + "1m";
    private static final String DIM = ESC + "2m";
    private static final String CLEAR = ESC + "2J" + ESC + "H";

    private static final int CARD_BOX_WIDTH = 24;
    private static final int CARD_BOX_HEIGHT = 6; // includes borders

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

        System.out.println();
        System.out.println(bold("Market"));
        for (int level = 3; level >= 1; level--) {
            System.out.println(dim("Level " + level));
            printMarketRow(level, board.getVisibleCards(level));
            System.out.println();
        }

        Player current = players.get(currentIndex);
        if (current.isHuman() && !current.getHand().isEmpty()) {
            System.out.println(dim("Tip: type 'r' during your turn to view reserved card details."));
        }
    }

    private String formatCard(Card c) {
        return pointsLabel(c.getPrestigePoints()) + "  Bonus: " + tokenLabel(c.getBonus()) + "  Cost: " + formatCost(c.getCost());
    }

    private String formatNoble(Noble n) {
        return pointsLabel(n.getPrestigePoints()) + "  Needs: " + formatCost(n.getCost());
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

    /**
     * Clears the screen and prints the current player's reserved cards with full details.
     *
     * @param player player whose reserved cards to show
     */
    public void displayReservedCards(Player player) {
        Objects.requireNonNull(player, "player");
        clearScreen();

        System.out.println(bold("Reserved Cards") + dim("  (" + player.getHand().size() + "/" + MAX_RESERVED + ")"));
        System.out.println("Player: " + player.getName());
        System.out.println("Bonuses: " + formatTokenMap(player.getBonuses(), false));
        System.out.println();

        if (player.getHand().isEmpty()) {
            System.out.println(dim("(none)"));
            return;
        }

        for (int i = 0; i < player.getHand().size(); i++) {
            Card c = player.getHand().get(i);
            boolean affordable = player.canAffordCard(c);

            System.out.println(bold("[r-" + i + "]") + " " + (affordable ? "" : dim("(not affordable yet) ")));
            System.out.println("  " + formatCard(c));
            System.out.println("  After bonuses: " + formatCostCompact(remainingAfterBonuses(c, player)));
            System.out.println();
        }
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

    private void printMarketRow(int level, Card[] row) {
        List<List<String>> boxes = new java.util.ArrayList<>();
        for (int slot = 0; slot < row.length; slot++) {
            boxes.add(renderCardBox("[" + level + "-" + slot + "]", row[slot]));
        }
        printBoxesSideBySide(boxes);
    }

    private List<String> renderCardBox(String id, Card card) {
        List<String> lines = new java.util.ArrayList<>(CARD_BOX_HEIGHT);
        // ASCII borders render reliably across terminals (including IDE consoles).
        String top = "+" + "-".repeat(CARD_BOX_WIDTH - 2) + "+";
        String bottom = "+" + "-".repeat(CARD_BOX_WIDTH - 2) + "+";
        lines.add(top);

        if (card == null) {
            lines.add(boxLine(padRight(id, CARD_BOX_WIDTH - 2)));
            lines.add(boxLine(padRight("(empty)", CARD_BOX_WIDTH - 2)));
            lines.add(boxLine(padRight("", CARD_BOX_WIDTH - 2)));
            lines.add(boxLine(padRight("", CARD_BOX_WIDTH - 2)));
            lines.add(bottom);
            return lines;
        }

        String header = id + "  " + pointsLabel(card.getPrestigePoints());
        String bonus = "Bonus: " + tokenLabel(card.getBonus());
        String cost = "Cost: " + formatCostCompact(card.getCost());

        lines.add(boxLine(padRight(truncate(header, CARD_BOX_WIDTH - 2), CARD_BOX_WIDTH - 2)));
        lines.add(boxLine(padRight(truncate(bonus, CARD_BOX_WIDTH - 2), CARD_BOX_WIDTH - 2)));
        lines.add(boxLine(padRight(truncate(cost, CARD_BOX_WIDTH - 2), CARD_BOX_WIDTH - 2)));
        lines.add(boxLine(padRight("", CARD_BOX_WIDTH - 2)));
        lines.add(bottom);
        return lines;
    }

    private void printBoxesSideBySide(List<List<String>> boxes) {
        int height = boxes.stream().mapToInt(List::size).min().orElse(0);
        for (int i = 0; i < height; i++) {
            StringBuilder row = new StringBuilder();
            for (int b = 0; b < boxes.size(); b++) {
                if (b > 0) row.append("  ");
                row.append(boxes.get(b).get(i));
            }
            System.out.println(row);
        }
    }

    private String boxLine(String content) {
        return "|" + content + "|";
    }

    private String padRight(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }

    private String truncate(String s, int width) {
        if (s.length() <= width) return s;
        if (width <= 1) return "…".substring(0, width);
        return s.substring(0, width - 1) + "…";
    }

    private String pointsLabel(int points) {
        return points + " pts";
    }

    private String formatCostCompact(Map<Token, Integer> cost) {
        if (cost == null || cost.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Token t : COST_ORDER) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) sb.append(tokenLabel(t)).append(v).append(" ");
        }
        return sb.toString().trim();
    }

    private Map<Token, Integer> remainingAfterBonuses(Card card, Player player) {
        Map<Token, Integer> remaining = new EnumMap<>(Token.class);
        Map<Token, Integer> cost = card.getCost();
        Map<Token, Integer> bonuses = player.getBonuses();
        for (Token t : COST_ORDER) {
            int needed = Math.max(0, cost.getOrDefault(t, 0) - bonuses.getOrDefault(t, 0));
            if (needed > 0) remaining.put(t, needed);
        }
        return remaining;
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
