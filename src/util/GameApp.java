package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import config.GameConfig;
import game.CardLoader;
import logic.Game;
import logic.SplendorAI;
import model.Board;
import model.Card;
import model.Deck;
import model.Noble;
import model.Player;
import model.Token;
import util.ui.ConsoleUI;

public class GameApp {

    private static final int    WIN_SCORE       = GameConfig.getWinningPoints();
    private static final String CARDS_FILEPATH  = GameConfig.getCardFilePath();
    private static final String NOBLES_FILEPATH = GameConfig.getNobleFilePath();

    // -------------------------------------------------------------------------
    // Game setup
    // -------------------------------------------------------------------------

    public static Game setupGame(int numPlayers, boolean[] isAI) {
        return setupGame(numPlayers, isAI, null);
    }

    public static Game setupGame(int numPlayers, boolean[] isAI, String[] playerNames) {
        List<Card> allCards = new ArrayList<>();
        try {
            allCards = CardLoader.loadCards(CARDS_FILEPATH);
        } catch (IOException e) {
            System.err.println("Error: Could not load Splendor Cards.csv");
            System.err.println("Make sure the file is in the same folder as the program.");
        }

        List<Card> level1 = new ArrayList<>();
        List<Card> level2 = new ArrayList<>();
        List<Card> level3 = new ArrayList<>();
        for (Card c : allCards) {
            if      (c.getLevel() == 1) level1.add(c);
            else if (c.getLevel() == 2) level2.add(c);
            else                        level3.add(c);
        }

        Deck d1 = new Deck(1, level1);
        Deck d2 = new Deck(2, level2);
        Deck d3 = new Deck(3, level3);
        d1.shuffle();
        d2.shuffle();
        d3.shuffle();

        List<Noble> allNobles = new ArrayList<>();
        try {
            allNobles = CardLoader.loadNobles(NOBLES_FILEPATH);
        } catch (IOException e) {
            System.err.println("Error: Could not load Nobles.csv");
            System.err.println("Make sure the file is in the same folder as the program.");
        }

        Collections.shuffle(allNobles);
        int noblesToShow = GameConfig.getInitialNobles(numPlayers);
        List<Noble> nobles = new ArrayList<>();
        for (int i = 0; i < noblesToShow && i < allNobles.size(); i++) {
            nobles.add(allNobles.get(i));
        }

        Board board = new Board(nobles, d1, d2, d3, numPlayers);

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String name;
            if (playerNames != null && i < playerNames.length
                    && playerNames[i] != null && !playerNames[i].isBlank()) {
                name = playerNames[i];
            } else if (isAI[i]) {
                name = "AI: " + (i + 1);
            } else {
                name = "Player: " + (i + 1);
            }
            players.add(new Player(name, !isAI[i]));
        }

        return new Game(board, players);
    }

    // -------------------------------------------------------------------------
    // Main game loop
    // -------------------------------------------------------------------------

    public static void runGameLoop(Game game, Scanner sc, ConsoleUI ui) {
        System.out.println();
        printLine("=", 52);
        System.out.println("           S P L E N D O R");
        System.out.println("      Collect gems. Buy cards. Win!");
        printLine("=", 52);
        System.out.println();
        System.out.printf("  Goal: First to %d prestige points wins!%n%n", WIN_SCORE);

        while (!game.isGameOver()) {
            Player p = game.getCurrentPlayer();

            ui.displayGameState(game, WIN_SCORE);

            System.out.println();
            printLine("-", 52);
            System.out.println("  " + p.getName() + "'s Turn");
            printLine("-", 52);

            boolean validAction = false;

            if (p.isHuman()) {
                while (!validAction) {
                    printActionMenu(p);
                    String input = readLine(sc);

                    if (input == null || input.equals("q")) {
                        System.out.println("\nThanks for playing! Goodbye.");
                        return;
                    }

                    switch (input) {
                        case "1" -> validAction = doTakeGems(game, p, sc);
                        case "2" -> validAction = doBuyCard(game, p, sc);
                        case "3" -> validAction = doReserveCard(game, p, sc);
                        case "r", "R" -> {
                            ui.displayReservedCards(p);
                            System.out.print("(Press Enter to return) ");
                            readLine(sc);
                            ui.displayGameState(game, WIN_SCORE);
                        }
                        default -> System.out.println("Invalid choice. Enter 1, 2, 3, R, or Q.");
                    }
                }
            } else {
                validAction = doAITurn(game, p);
                System.out.print("(Press Enter to continue) ");
                readLine(sc);
            }

            if (validAction) {
                if (p.isHuman()) {
                    returnExcessTokens(game, p, sc);
                } else {
                    returnExcessTokensAI(game, p);
                }

                checkNobleVisit(game, p);

                if (!game.isEndTriggered() && p.getScore() >= WIN_SCORE) {
                    System.out.println();
                    System.out.println("*** " + p.getName() + " has reached " + p.getScore()
                            + " points! The final round will now finish. ***");
                    game.triggerEnd(game.getCurrentPlayerIndex());
                }

                game.nextTurn();
            }
        }

        Player winner = game.determineWinner();
        System.out.println();
        printLine("*", 52);
        if (winner != null) {
            System.out.println("  " + winner.getName() + " WINS with " + winner.getScore() + " points!");
        } else {
            System.out.println("  Game finished but no winner could be determined.");
        }
        printLine("*", 52);
    }

    // -------------------------------------------------------------------------
    // Action menu
    // -------------------------------------------------------------------------

    private static void printActionMenu(Player p) {
        System.out.println("┌─ YOUR MOVE ──────────────────────────────────────┐");
        System.out.println("│  [1]  Take gems                                  │");
        System.out.println("│  [2]  Buy a card                                 │");
        System.out.println("│  [3]  Reserve a card                             │");
        System.out.println("│  [R]  View reserved cards       [Q]  Quit        │");
        System.out.println("└──────────────────────────────────────────────────┘");
        System.out.print("Choice: ");
    }

    // -------------------------------------------------------------------------
    // Take gems — numbered color picker
    // -------------------------------------------------------------------------

    private static boolean doTakeGems(Game game, Player p, Scanner sc) {
        Board board = game.getBoard();
        var avail   = board.getAvailableTokens();

        List<Token> colors = new ArrayList<>();
        for (Token t : new Token[]{Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE}) {
            if (avail.getOrDefault(t, 0) > 0) colors.add(t);
        }

        if (colors.isEmpty()) {
            System.out.println("No gems available on the board.");
            return false;
        }

        System.out.println();
        System.out.println("Available gem colors:");
        for (int i = 0; i < colors.size(); i++) {
            Token t = colors.get(i);
            System.out.printf("  [%d] %-5s  (%d on board)%n", i + 1, t, avail.get(t));
        }
        System.out.println();
        System.out.println("  Take 3 different → enter 3 numbers separated by spaces  e.g.  1 2 3");
        System.out.println("  Take 2 same      → enter same number twice               e.g.  2 2");
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");

        String line = readLine(sc);
        if (line == null || line.equals("0")) return false;

        String[] parts = line.trim().split("\\s+");

        try {
            if (parts.length == 2 && parts[0].equals(parts[1])) {
                // take 2 same
                int idx = Integer.parseInt(parts[0]) - 1;
                if (idx < 0 || idx >= colors.size()) { System.out.println("Invalid number."); return false; }
                Token t = colors.get(idx);
                if (!game.canTakeTwoSameGems(p, t)) {
                    System.out.println("Need at least 4 of that color on the board (or token limit exceeded).");
                    return false;
                }
                game.takeTwoSameGems(p, t);
                System.out.println("Took 2 " + t + ".");
                return true;

            } else if (parts.length == 3) {
                // take 3 different
                int i1 = Integer.parseInt(parts[0]) - 1;
                int i2 = Integer.parseInt(parts[1]) - 1;
                int i3 = Integer.parseInt(parts[2]) - 1;
                if (i1 < 0 || i1 >= colors.size() ||
                    i2 < 0 || i2 >= colors.size() ||
                    i3 < 0 || i3 >= colors.size()) {
                    System.out.println("Invalid number.");
                    return false;
                }
                Token t1 = colors.get(i1);
                Token t2 = colors.get(i2);
                Token t3 = colors.get(i3);
                if (!game.canTakeThreeDifferentGems(p, t1, t2, t3)) {
                    System.out.println("Must be 3 different colors, all available, and stay within 10-token limit.");
                    return false;
                }
                game.takeThreeDifferentGems(p, t1, t2, t3);
                System.out.println("Took 1 " + t1 + ", 1 " + t2 + ", 1 " + t3 + ".");
                return true;

            } else {
                System.out.println("Enter 2 numbers (same) or 3 different numbers.");
                return false;
            }
        } catch (NumberFormatException e) {
            System.out.println("Enter valid numbers.");
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Buy card — numbered list of affordable cards
    // -------------------------------------------------------------------------

    private static boolean doBuyCard(Game game, Player p, Scanner sc) {
        List<String>   options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        for (int level = 1; level <= 3; level++) {
            Card[] row = game.getBoard().getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                if (game.canBuyVisibleCard(p, level, slot)) {
                    Card c = row[slot];
                    final int lv = level, sl = slot;
                    options.add(String.format("L%d slot %d — %d pts  Bonus:%-3s  Cost: %s",
                            level, slot, c.getPrestigePoints(),
                            tokenLabel(c.getBonus()), ConsoleUI.formatCostCompactStatic(c.getCost())));
                    actions.add(() -> game.buyVisibleCard(p, lv, sl));
                }
            }
        }

        for (int i = 0; i < p.getHand().size(); i++) {
            if (game.canBuyReservedCard(p, i)) {
                Card c     = p.getHand().get(i);
                final int idx = i;
                options.add(String.format("Reserved [r-%d] — %d pts  Bonus:%-3s  Cost: %s",
                        i, c.getPrestigePoints(),
                        tokenLabel(c.getBonus()), ConsoleUI.formatCostCompactStatic(c.getCost())));
                actions.add(() -> game.buyReservedCard(p, idx));
            }
        }

        if (options.isEmpty()) {
            System.out.println("You cannot afford any card right now.");
            return false;
        }

        System.out.println();
        System.out.println("Buyable cards:");
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + options.get(i));
        }
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");

        try {
            String line = readLine(sc);
            if (line == null) return false;
            int choice = Integer.parseInt(line.trim());
            if (choice == 0) return false;
            if (choice < 1 || choice > options.size()) { System.out.println("Invalid choice."); return false; }
            actions.get(choice - 1).run();
            System.out.println("Purchased: " + options.get(choice - 1));
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Enter a number.");
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Reserve card — numbered list
    // -------------------------------------------------------------------------

    private static boolean doReserveCard(Game game, Player p, Scanner sc) {
        if (p.getHand().size() >= 3) {
            System.out.println("Hand full (max 3 reserved cards). Buy one first.");
            return false;
        }

        List<String>   options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        for (int level = 3; level >= 1; level--) {
            Card[] row = game.getBoard().getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                if (game.canReserveVisibleCard(p, level, slot)) {
                    Card c = row[slot];
                    final int lv = level, sl = slot;
                    options.add(String.format("L%d slot %d — %d pts  Bonus:%-3s  Cost: %s",
                            level, slot, c.getPrestigePoints(),
                            tokenLabel(c.getBonus()), ConsoleUI.formatCostCompactStatic(c.getCost())));
                    actions.add(() -> game.reserveVisibleCard(p, lv, sl));
                }
            }
        }

        for (int level = 1; level <= 3; level++) {
            if (game.canReserveDeckCard(p, level)) {
                final int lv = level;
                int remaining = game.getBoard().getDeckRemainingCount(level);
                options.add("Top of Deck " + level + " (face-down, " + remaining + " remaining)");
                actions.add(() -> game.reserveDeckCard(p, lv));
            }
        }

        if (options.isEmpty()) {
            System.out.println("No cards available to reserve.");
            return false;
        }

        System.out.println();
        System.out.println("Reserve which card?");
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + options.get(i));
        }
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");

        try {
            String line = readLine(sc);
            if (line == null) return false;
            int choice = Integer.parseInt(line.trim());
            if (choice == 0) return false;
            if (choice < 1 || choice > options.size()) { System.out.println("Invalid choice."); return false; }
            actions.get(choice - 1).run();
            System.out.println("Reserved: " + options.get(choice - 1));
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Enter a number.");
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Token return — numbered list of what the player holds
    // -------------------------------------------------------------------------

    private static void returnExcessTokens(Game game, Player p, Scanner sc) {
        if (!game.mustReturnTokens(p)) return;

        int toReturn = game.getNumTokensToReturn(p);
        System.out.println();
        System.out.println("You have " + p.getTotalTokenCount() + " tokens (max 10). Return " + toReturn + ".");

        while (game.mustReturnTokens(p)) {
            List<Token> held = new ArrayList<>();
            for (Token t : Token.values()) {
                if (p.getTokenCount(t) > 0) held.add(t);
            }

            System.out.println("Which token to return?");
            for (int i = 0; i < held.size(); i++) {
                System.out.printf("  [%d] %s (%d)%n", i + 1, held.get(i), p.getTokenCount(held.get(i)));
            }
            System.out.print("Choice: ");

            try {
                String line = readLine(sc);
                if (line == null) return;
                int choice = Integer.parseInt(line.trim());
                if (choice < 1 || choice > held.size()) { System.out.println("Invalid choice."); continue; }
                Token t = held.get(choice - 1);
                game.returnToken(p, t, 1);
                int left = game.getNumTokensToReturn(p);
                System.out.println("Returned 1 " + t + "." + (left > 0 ? "  " + left + " more to go." : ""));
            } catch (NumberFormatException e) {
                System.out.println("Enter a number.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // AI helpers
    // -------------------------------------------------------------------------

    private static boolean doAITurn(Game game, Player p) {
        Board board  = game.getBoard();
        String action = SplendorAI.chooseAction(p, board);

        if (action == null) {
            System.out.println("AI could not decide. Skipping turn.");
            return true;
        }

        String[] parts = action.split(":");
        if (parts.length < 2) return false;

        try {
            if (parts[0].equals("1") && parts.length >= 4) {
                Token t1 = Token.valueOf(parts[1]);
                Token t2 = Token.valueOf(parts[2]);
                Token t3 = Token.valueOf(parts[3]);
                if (game.canTakeThreeDifferentGems(p, t1, t2, t3)) {
                    game.takeThreeDifferentGems(p, t1, t2, t3);
                    System.out.println("AI takes 1 " + t1 + ", 1 " + t2 + ", 1 " + t3);
                    return true;
                }

            } else if (parts[0].equals("2") && parts.length >= 2) {
                Token t = Token.valueOf(parts[1]);
                if (game.canTakeTwoSameGems(p, t)) {
                    game.takeTwoSameGems(p, t);
                    System.out.println("AI takes 2 " + t);
                    return true;
                }

            } else if (parts[0].equals("3") && parts.length >= 2) {
                if (parts[1].equals("r") && parts.length >= 3) {
                    int idx = Integer.parseInt(parts[2]);
                    if (game.canBuyReservedCard(p, idx)) {
                        Card card = p.getHand().get(idx);
                        game.buyReservedCard(p, idx);
                        System.out.println("AI buys reserved: " + formatCardShort(card));
                        return true;
                    }
                } else {
                    int level = Integer.parseInt(parts[1]);
                    int slot  = Integer.parseInt(parts[2]);
                    if (game.canBuyVisibleCard(p, level, slot)) {
                        Card card = board.getVisibleCards(level)[slot];
                        game.buyVisibleCard(p, level, slot);
                        System.out.println("AI buys: " + formatCardShort(card));
                        return true;
                    }
                }

            } else if (parts[0].equals("4") && parts.length >= 3) {
                if (parts[1].equals("deck")) {
                    int level = Integer.parseInt(parts[2]);
                    if (game.canReserveDeckCard(p, level)) {
                        game.reserveDeckCard(p, level);
                        System.out.println("AI reserves from deck " + level);
                        return true;
                    }
                } else {
                    int level = Integer.parseInt(parts[1]);
                    int slot  = Integer.parseInt(parts[2]);
                    if (game.canReserveVisibleCard(p, level, slot)) {
                        Card card = board.getVisibleCards(level)[slot];
                        game.reserveVisibleCard(p, level, slot);
                        System.out.println("AI reserves: " + formatCardShort(card));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("AI move failed: " + e.getMessage());
        }
        return false;
    }

    private static void returnExcessTokensAI(Game game, Player p) {
        if (!game.mustReturnTokens(p)) return;
        int toReturn = game.getNumTokensToReturn(p);
        List<Token> tokens = SplendorAI.chooseTokensToReturn(p, game.getBoard(), toReturn);
        for (Token t : tokens) game.returnToken(p, t, 1);
        System.out.println("AI returns " + toReturn + " token(s).");
    }

    private static void checkNobleVisit(Game game, Player p) {
        Noble noble = game.checkAndAwardNoble(p);
        if (noble != null) {
            System.out.println("A noble visits " + p.getName() + "! (+3 pts)");
        }
    }

    // -------------------------------------------------------------------------
    // Formatting helpers
    // -------------------------------------------------------------------------

    private static void printLine(String ch, int len) {
        System.out.println(ch.repeat(len));
    }

    private static String tokenLabel(Token token) {
        if (token == null) return "(none)";
        return switch (token) {
            case BLACK -> "Blk";
            case BLUE  -> "Blu";
            case GREEN -> "Grn";
            case RED   -> "Red";
            case WHITE -> "Wht";
            case GOLD  -> "Gld";
        };
    }

    private static String formatCardShort(Card c) {
        return "PV:" + c.getPrestigePoints() + " " + c.getBonus() + "+ "
                + ConsoleUI.formatCostCompactStatic(c.getCost());
    }

    private static String readLine(Scanner sc) {
        if (!sc.hasNextLine()) return null;
        return sc.nextLine().trim().toLowerCase();
    }
}
