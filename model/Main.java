package model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Splendor - Console version
 * A simple card game where players collect gems to buy cards and attract nobles.
 * First to 15 prestige points wins!
 */
public class Main {

    private static final int WIN_SCORE = 15;
    private static final int MAX_TOKENS = 10;

    public static void main(String[] args) {
        System.out.println();
        printLine("=", 50);
        System.out.println("         S P L E N D O R");
        System.out.println("    Collect gems. Buy cards. Win!");
        printLine("=", 50);
        System.out.println();
        System.out.println("  Goal: First to 15 points wins!");
        System.out.println("  - Take gems (3 different OR 2 same)");
        System.out.println("  - Buy cards with gems (bonuses = discounts)");
        System.out.println("  - Reserve cards, get nobles for bonus points");
        System.out.println("  - Max 10 tokens - return extras when over");
        System.out.println();

        // Load cards from CSV
        List<Card> allCards;
        try {
            allCards = CardLoader.loadCards("Splendor Cards.csv");
        } catch (IOException e) {
            System.err.println("Error: Could not load Splendor Cards.csv");
            System.err.println("Make sure the file is in the same folder as the program.");
            return;
        }

        // Split cards by level
        List<Card> level1 = new ArrayList<>();
        List<Card> level2 = new ArrayList<>();
        List<Card> level3 = new ArrayList<>();
        for (Card c : allCards) {
            if (c.getLevel() == 1) level1.add(c);
            else if (c.getLevel() == 2) level2.add(c);
            else level3.add(c);
        }

        // Create and shuffle decks
        Deck d1 = new Deck(1, level1);
        Deck d2 = new Deck(2, level2);
        Deck d3 = new Deck(3, level3);
        d1.shuffle();
        d2.shuffle();
        d3.shuffle();

        // Setup board and players (2 players = 3 nobles)
        List<Noble> allNobles = CardLoader.createDefaultNobles();
        List<Noble> nobles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            nobles.add(allNobles.get(i));
        }
        Board board = new Board(nobles, d1, d2, d3, 2);
        List<Player> players = new ArrayList<>();
        players.add(new Player("Player 1", true));
        players.add(new Player("Player 2", true));

        Scanner sc = new Scanner(System.in);
        int currentPlayer = 0;
        boolean gameOver = false;

        // Main game loop
        while (!gameOver) {
            Player p = players.get(currentPlayer);

            System.out.println();
            printLine("-", 50);
            System.out.println("  " + p.getName() + "'s Turn");
            printLine("-", 50);

            // Show board
            printBoard(board);
            System.out.println();
            printPlayerStatus(p);
            System.out.println();

            // Get player action
            boolean validAction = false;
            while (!validAction) {
                System.out.println("What would you like to do?");
                System.out.println("  1 = Take 3 different gems");
                System.out.println("  2 = Take 2 same gems (need 4+ of that color)");
                System.out.println("  3 = Buy a card");
                System.out.println("  4 = Reserve a card");
                System.out.println("  q = Quit game");
                System.out.print("Your choice: ");

                String input = sc.nextLine().trim().toLowerCase();

                if (input.equals("q")) {
                    System.out.println("\nThanks for playing! Goodbye.");
                    sc.close();
                    return;
                }

                if (input.equals("1")) {
                    validAction = doTakeThreeGems(p, board, sc);
                } else if (input.equals("2")) {
                    validAction = doTakeTwoSameGems(p, board, sc);
                } else if (input.equals("3")) {
                    validAction = doBuyCard(p, board, sc);
                } else if (input.equals("4")) {
                    validAction = doReserveCard(p, board, sc);
                } else {
                    System.out.println("Invalid choice. Please enter 1, 2, 3, 4, or q.");
                }
            }

            // Return excess tokens if over 10
            returnExcessTokens(p, board, sc);

            // Check for noble visit
            checkNobleVisit(p, board, sc);

            // Check win condition
            if (p.getScore() >= WIN_SCORE) {
                System.out.println();
                printLine("*", 50);
                System.out.println("  " + p.getName() + " WINS with " + p.getScore() + " points!");
                printLine("*", 50);
                gameOver = true;
            } else {
                currentPlayer = (currentPlayer + 1) % players.size();
            }
        }
        sc.close();
    }

    // ---- Helper methods for printing ----

    private static void printLine(String ch, int len) {
        for (int i = 0; i < len; i++) {
            System.out.print(ch);
        }
        System.out.println();
    }

    private static void printBoard(Board board) {
        System.out.println("+-- GEMS ON BOARD --+");
        System.out.println("  " + formatTokens(board.getAvailableTokens()));
        System.out.println();

        System.out.println("+-- NOBLES (3 pts each) --+");
        List<Noble> nobles = board.getNobles();
        for (int i = 0; i < nobles.size(); i++) {
            System.out.println("  [" + i + "] " + formatNoble(nobles.get(i)));
        }
        System.out.println();

        for (int level = 1; level <= 3; level++) {
            System.out.println("+-- LEVEL " + level + " CARDS --+");
            Card[] row = board.getVisibleCards(level);
            for (int i = 0; i < row.length; i++) {
                if (row[i] != null) {
                    System.out.println("  [" + level + "-" + i + "] " + formatCard(row[i]));
                } else {
                    System.out.println("  [" + level + "-" + i + "] (empty)");
                }
            }
            System.out.println();
        }
    }

    private static String formatCard(Card c) {
        return "PV:" + c.getPrestigePoints() + " " + c.getBonus() + "+ " + formatCost(c.getCost());
    }

    private static String formatNoble(Noble n) {
        return "3 pts - needs " + formatCost(n.getCost());
    }

    private static String formatCost(Map<Token, Integer> cost) {
        StringBuilder sb = new StringBuilder("Cost:");
        for (Token t : new Token[]{Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE}) {
            int n = cost.getOrDefault(t, 0);
            if (n > 0) sb.append(" ").append(t).append("x").append(n);
        }
        return sb.toString();
    }

    private static String formatTokens(Map<Token, Integer> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : Token.values()) {
            int n = tokens.getOrDefault(t, 0);
            if (n > 0) sb.append(t).append(":").append(n).append("  ");
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? "none" : s;
    }

    private static void printPlayerStatus(Player p) {
        System.out.println("+-- " + p.getName() + " --+");
        System.out.println("  Score: " + p.getScore() + " pts");
        System.out.println("  Tokens: " + formatTokens(p.getTokens()));
        System.out.println("  Bonuses: " + formatTokens(p.getBonuses()));
        if (!p.getHand().isEmpty()) {
            System.out.println("  Reserved: ");
            for (int i = 0; i < p.getHand().size(); i++) {
                System.out.println("    [" + i + "] " + formatCard(p.getHand().get(i)));
            }
        }
    }

    // ---- Action methods ----

    private static boolean doTakeThreeGems(Player p, Board board, Scanner sc) {
        System.out.print("Enter 3 different colors (e.g. green blue red): ");
        String line = sc.nextLine().trim().toLowerCase();
        String[] parts = line.split("\\s+");
        if (parts.length != 3) {
            System.out.println("You must enter exactly 3 colors.");
            return false;
        }
        Token t1 = parseToken(parts[0]);
        Token t2 = parseToken(parts[1]);
        Token t3 = parseToken(parts[2]);

        if (t1 == null || t2 == null || t3 == null) {
            System.out.println("Invalid color. Use: green, white, blue, black, red");
            return false;
        }
        if (t1 == Token.GOLD || t2 == Token.GOLD || t3 == Token.GOLD) {
            System.out.println("Cannot take gold when taking gems.");
            return false;
        }
        if (t1 == t2 || t1 == t3 || t2 == t3) {
            System.out.println("All 3 colors must be different.");
            return false;
        }
        if (!board.canTakeThreeDifferent(t1, t2, t3)) {
            System.out.println("Not enough gems of those colors on the board.");
            return false;
        }

        board.removeToken(t1, 1);
        board.removeToken(t2, 1);
        board.removeToken(t3, 1);
        p.addTokens(t1, 1);
        p.addTokens(t2, 1);
        p.addTokens(t3, 1);
        System.out.println("Took 1 " + t1 + ", 1 " + t2 + ", 1 " + t3);
        return true;
    }

    private static boolean doTakeTwoSameGems(Player p, Board board, Scanner sc) {
        System.out.print("Enter color (need 4+ on board): ");
        String line = sc.nextLine().trim().toLowerCase();
        Token t = parseToken(line);
        if (t == null || t == Token.GOLD) {
            System.out.println("Invalid color. Use: green, white, blue, black, red");
            return false;
        }
        if (!board.canTakeTwo(t)) {
            System.out.println("Need at least 4 " + t + " gems on board. There are " + board.getAvailableTokens().getOrDefault(t, 0) + ".");
            return false;
        }

        board.removeToken(t, 2);
        p.addTokens(t, 2);
        System.out.println("Took 2 " + t);
        return true;
    }

    private static boolean doBuyCard(Player p, Board board, Scanner sc) {
        System.out.print("Enter card (e.g. 1-0 for level 1 slot 0, or r-0 for reserved): ");
        String line = sc.nextLine().trim().toLowerCase();
        Card card = null;
        boolean fromReserved = false;
        int level = 0;
        int slot = 0;

        if (line.startsWith("r")) {
            fromReserved = true;
            String num = line.replace("r", "").replace("-", "").trim();
            int idx = 0;
            try {
                idx = Integer.parseInt(num);
            } catch (NumberFormatException e) {
                System.out.println("Invalid format. Use r-0, r-1, or r-2");
                return false;
            }
            if (idx >= 0 && idx < p.getHand().size()) {
                card = p.getHand().get(idx);
            }
        } else {
            String[] parts = line.split("-");
            if (parts.length != 2) {
                System.out.println("Invalid format. Use level-slot (e.g. 1-0, 2-2)");
                return false;
            }
            try {
                level = Integer.parseInt(parts[0].trim());
                slot = Integer.parseInt(parts[1].trim());
                if (level >= 1 && level <= 3 && slot >= 0 && slot < 4) {
                    Card[] row = board.getVisibleCards(level);
                    card = row[slot];
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid numbers.");
                return false;
            }
        }

        if (card == null) {
            System.out.println("Invalid card or slot.");
            return false;
        }

        if (!p.canAffordCard(card)) {
            System.out.println("You cannot afford this card. Need: " + formatCost(card.getCost()));
            return false;
        }

        if (!fromReserved) {
            board.takeCard(level, slot);
            board.refillMarket();
        }
        p.payForCard(card, board);
        p.buyCard(card);
        System.out.println("Purchased: " + formatCard(card) + " (+" + card.getPrestigePoints() + " pts)");
        return true;
    }

    private static boolean doReserveCard(Player p, Board board, Scanner sc) {
        if (p.getHand().size() >= 3) {
            System.out.println("You already have 3 reserved cards. Buy one first.");
            return false;
        }
        System.out.print("Enter card (e.g. 1-0) or 'deck 1', 'deck 2', 'deck 3' for top of deck: ");
        String line = sc.nextLine().trim().toLowerCase();
        Card card = null;

        if (line.startsWith("deck")) {
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                try {
                    int level = Integer.parseInt(parts[1]);
                    if (level >= 1 && level <= 3) {
                        card = board.drawFromDeck(level);
                    }
                } catch (NumberFormatException e) {
                }
            }
            if (card == null) {
                System.out.println("Use: deck 1, deck 2, or deck 3");
                return false;
            }
        } else {
            String[] parts = line.split("-");
            if (parts.length != 2) {
                System.out.println("Use level-slot (e.g. 1-0) or deck 1");
                return false;
            }
            try {
                int level = Integer.parseInt(parts[0].trim());
                int slot = Integer.parseInt(parts[1].trim());
                if (level >= 1 && level <= 3 && slot >= 0 && slot < 4) {
                    card = board.takeCard(level, slot);
                    board.refillMarket();
                }
            } catch (NumberFormatException e) {
            }
        }

        if (card == null) {
            System.out.println("Invalid choice.");
            return false;
        }

        p.reserveCard(card);

        if (board.getAvailableTokens().getOrDefault(Token.GOLD, 0) > 0) {
            System.out.print("Take 1 gold? (y/n): ");
            if (sc.nextLine().trim().toLowerCase().equals("y")) {
                board.removeToken(Token.GOLD, 1);
                p.addTokens(Token.GOLD, 1);
                System.out.println("Took 1 gold.");
            }
        }
        System.out.println("Reserved: " + formatCard(card));
        return true;
    }

    private static void returnExcessTokens(Player p, Board board, Scanner sc) {
        int total = p.getTotalTokenCount();
        if (total <= MAX_TOKENS) return;

        int toReturn = total - MAX_TOKENS;
        System.out.println("You have " + total + " tokens. Max is " + MAX_TOKENS + ". Return " + toReturn + ".");

        int returned = 0;
        while (returned < toReturn) {
            System.out.print("Color to return (green/white/blue/black/red/gold): ");
            String line = sc.nextLine().trim().toLowerCase();
            Token t = parseToken(line);
            if (t == null && line.equals("gold")) t = Token.GOLD;
            if (t != null && p.getTokenCount(t) > 0) {
                int have = p.getTokenCount(t);
                int need = toReturn - returned;
                int give = Math.min(have, need);
                p.removeTokens(t, give);
                board.addToken(t, give);
                returned += give;
                System.out.println("Returned " + give + " " + t + ". " + (toReturn - returned) + " more to go.");
            } else {
                System.out.println("Invalid or you don't have that color.");
            }
        }
    }

    private static void checkNobleVisit(Player p, Board board, Scanner sc) {
        List<Noble> nobles = board.getNobles();
        for (int i = 0; i < nobles.size(); i++) {
            Noble n = nobles.get(i);
            if (p.canGetNoble(n)) {
                System.out.println("A noble visits! " + n + " (+3 pts)");
                p.receiveNoble(n);
                board.removeNoble(n);
                break;
            }
        }
    }

    private static Token parseToken(String s) {
        if (s == null) return null;
        switch (s) {
            case "green": return Token.GREEN;
            case "white": return Token.WHITE;
            case "blue":  return Token.BLUE;
            case "black": return Token.BLACK;
            case "red":   return Token.RED;
            default:      return null;
        }
    }
}
