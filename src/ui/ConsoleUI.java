package ui;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

import model.Board;
import model.Card;
import model.Game;
import model.Noble;
import model.Player;
import model.Token;

/**
 * Minimal console UI helper to gather setup input and display the full game state each turn.
 */
public class ConsoleUI {
    private final Scanner sc;

    // default constructor uses System.in
    public ConsoleUI() {
        this(new Scanner(System.in));
    }

    // allow caller to provide a Scanner (so piping input can be shared and not double-consumed)
    public ConsoleUI(Scanner scanner) {
        this.sc = scanner;
    }

    public int getNumberOfPlayers() {
        while (true) {
            System.out.print("How many players? (2-4): ");
            String line = sc.nextLine().trim();
            try {
                int n = Integer.parseInt(line);
                if (n >= 2 && n <= 4) return n;
            } catch (NumberFormatException ignored) {}
            System.out.println("Please enter a number between 2 and 4.");
        }
    }

    public boolean[] getPlayerTypes(int numberOfPlayers) {
        boolean[] isAI = new boolean[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            while (true) {
                System.out.print("Is Player " + (i + 1) + " an AI? (y/n): ");
                String resp = sc.nextLine().trim().toLowerCase();
                if (resp.startsWith("y")) { isAI[i] = true; break; }
                if (resp.startsWith("n")) { isAI[i] = false; break; }
                System.out.println("Please answer y or n.");
            }
        }
        return isAI;
    }

    public void displayGameState(Game game) {
        System.out.println();
        Board board = game.getBoard();
        List<Player> players = game.getPlayers();
        int currentIndex = game.getCurrentPlayerIndex();

        System.out.println("=== SPLENDOR - " + players.size() + " Player Game ===");
        System.out.println();

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            String role = p.isHuman() ? "(YOU)" : "(AI)";
            String currentMarker = (i == currentIndex) ? " <<< Current" : "";
            System.out.printf("Player %d %s: %d pts | Tokens: %s | Cards: %d %s\n",
                    i + 1,
                    role,
                    p.getScore(),
                    formatTokens(p.getTokens()),
                    p.getPurchasedCards().size(),
                    currentMarker);
        }

        System.out.println();
        System.out.println("Bank: " + formatTokens(board.getAvailableTokens()));

        List<Noble> nobles = board.getNobles();
        System.out.print("Available Nobles: ");
        for (int i = 0; i < nobles.size(); i++) {
            System.out.print("[" + i + ": " + formatNoble(nobles.get(i)) + "] ");
        }
        System.out.println();

        // show visible cards in a compact form
        for (int level = 1; level <= 3; level++) {
            System.out.println("[Level " + level + "]");
            Card[] row = board.getVisibleCards(level);
            for (int s = 0; s < row.length; s++) {
                Card c = row[s];
                if (c != null) {
                    System.out.println("  (" + level + "-" + s + ") " + formatCard(c));
                } else {
                    System.out.println("  (" + level + "-" + s + ") (empty)");
                }
            }
        }
    }

    private String formatTokens(Map<Token, Integer> map) {
        StringBuilder sb = new StringBuilder();
        for (Token t : Token.values()) {
            int n = map.getOrDefault(t, 0);
            if (n > 0) sb.append(t.name().charAt(0)).append(":").append(n).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatCard(Card c) {
        return "PV:" + c.getPrestigePoints() + " " + c.getBonus() + "+ " + formatCost(c.getCost());
    }

    private String formatNoble(Noble n) {
        return n.getPrestigePoints() + "pts needs " + formatCost(n.getCost());
    }

    private String formatCost(Map<Token, Integer> cost) {
        StringBuilder sb = new StringBuilder();
        for (Token t : new Token[]{Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE}) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) sb.append(t.name().charAt(0)).append(v).append(" ");
        }
        return sb.toString().trim();
    }
}
