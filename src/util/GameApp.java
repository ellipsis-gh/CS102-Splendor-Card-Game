package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

/**
 * Splendor - Console version
 * A simple card game where players collect gems to buy cards and attract nobles.
 * First to 15 prestige points wins!
 */
public class GameApp {

    //retrieving points from config.properties
    private static final int WIN_SCORE = GameConfig.getWinningPoints();

    //retrieving cards file path from config.properties
    private static final String CARDS_FILEPATH = GameConfig.getCardFilePath();

    //retrieving nobles file path from config.properties
    private static final String NOBLES_FILEPATH = GameConfig.getNobleFilePath();


    // Builds and returns a fully initialized Game instance. Throws IOException when card load fails.

    //gang..don't need to throw exception again if we already caught everything...

    public static Game setupGame(int numPlayers, boolean[] isAI){

        // load cards from CSV
        List<Card> allCards = new ArrayList<Card>();
        try {
            allCards = CardLoader.loadCards(CARDS_FILEPATH);
        } catch (IOException e) {
            System.err.println("Error: Could not load Splendor Cards.csv");
            System.err.println("Make sure the file is in the same folder as the program.");
            e.getStackTrace();
        }

        // split the full card list into 3 separate level piles
        List<Card> level1 = new ArrayList<>();
        List<Card> level2 = new ArrayList<>();
        List<Card> level3 = new ArrayList<>();

        //edited syntaxing
        for (Card c : allCards) {

            if (c.getLevel() == 1){
                level1.add(c);
            } else if (c.getLevel() == 2) {
                level2.add(c);
            } else {
                level3.add(c);
            }
        }

        // create and shuffle each deck
        Deck d1 = new Deck(1, level1);
        Deck d2 = new Deck(2, level2);
        Deck d3 = new Deck(3, level3);
        d1.shuffle();
        d2.shuffle();
        d3.shuffle();

        // setup board with 3 nobles for a 2-player game
        List<Noble> allNobles = new ArrayList<Noble>();

        //load nobles from CSV
        try {
            allNobles = CardLoader.loadNobles(NOBLES_FILEPATH);
        } catch (IOException e) {
            System.err.println("Error: Could not load Nobles.csv");
            System.err.println("Make sure the file is in the same folder as the program.");
            e.printStackTrace();
        }


        /*setup nobles based on number of players (players + 1)
        retrieve this information from configuration file -> based on project writeup*/


        //pull out 3 nobles for the board
        List<Noble> nobles = new ArrayList<>();
        int noblesToShow = GameConfig.getInitialNobles(numPlayers);

        //shuffle the existing nobles
        Collections.shuffle(allNobles);
        for (int i = 0; i < noblesToShow; i++) {
            nobles.add(allNobles.get(i));
        }


        //setup board
        Board board = new Board(nobles, d1, d2, d3, numPlayers);

        //create a list of players 
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            if (isAI[i]) {
                players.add(new Player("AI: " + (i+1), false));
            } else {
                players.add(new Player("Player: " + (i+1), true));
            }
        }

        return new Game(board, players);
    }

    // Main turn loop extracted from previous main() — prints full game state via ConsoleUI each turn
    public static void runGameLoop(Game game, Scanner sc, ConsoleUI ui) {
        System.out.println();
        printLine("=", 50);
        System.out.println("         S P L E N D O R");
        System.out.println("    Collect gems. Buy cards. Win!");
        printLine("=", 50);
        System.out.println();
        System.out.printf("  Goal: First to %d points wins!", WIN_SCORE);
        System.out.println("  - Take gems (3 different OR 2 same)");
        System.out.println("  - Buy cards with gems (bonuses = discounts)");
        System.out.println("  - Reserve cards, get nobles for bonus points");
        System.out.println("  - Max 10 tokens - return extras when over");
        System.out.println();

        while (!game.isGameOver()) {
            Player p = game.getCurrentPlayer();

            // display full game state (board + all players)
            ui.displayGameState(game);

            System.out.println();
            printLine("-", 50);
            System.out.println("  " + p.getName() + "'s Turn");
            printLine("-", 50);

            // get the player's action — human input or AI decision
            boolean validAction = false;
            if (p.isHuman()) {
                while (!validAction) {
                    System.out.println("What would you like to do?");
                    System.out.println("  1 = Take 3 different gems");
                    System.out.println("  2 = Take 2 same gems (need 4+ of that color)");
                    System.out.println("  3 = Buy a card");
                    System.out.println("  4 = Reserve a card");
                    System.out.println("  q = Quit game");
                    System.out.print("Your choice: ");

                    if (!sc.hasNextLine()) {
                        System.out.println("\nNo input available. Exiting.");
                        return;
                    }
                    String input = sc.nextLine().trim().toLowerCase();

                    if (input.equals("q")) {
                        System.out.println("\nThanks for playing! Goodbye.");
                        return;
                    }

                    if (input.equals("1")) {
                        validAction = doTakeThreeGems(game, p, sc);
                    } else if (input.equals("2")) {
                        validAction = doTakeTwoSameGems(game, p, sc);
                    } else if (input.equals("3")) {
                        validAction = doBuyCard(game, p, sc);
                    } else if (input.equals("4")) {
                        validAction = doReserveCard(game, p, sc);
                    } else {
                        System.out.println("Invalid choice. Please enter 1, 2, 3, 4, or q.");
                    }
                }
            } else {
                validAction = doAITurn(game, p);
                System.out.print("(Press Enter to continue) ");
                if (sc.hasNextLine()) sc.nextLine();
            }

            // after a valid action: return excess tokens if needed, check for noble, check win
            if (validAction) {
                if (p.isHuman()) {
                    returnExcessTokens(game, p, sc);
                } else {
                    returnExcessTokensAI(game, p);
                }

                checkNobleVisit(game, p);

                // if someone reached the win score, trigger the end-of-round sequence
                if (!game.isEndTriggered() && p.getScore() >= WIN_SCORE) {
                    System.out.println();
                    System.out.println("*** " + p.getName() + " has reached " + p.getScore() + " points! The final round will now finish. ***");
                    game.triggerEnd(game.getCurrentPlayerIndex());
                }

                // advance to next player (Game will set gameOver when the final round completes)
                game.nextTurn();
            }
        }

        // game is over — determine and announce winner
        Player winner = game.determineWinner();
        System.out.println();
        printLine("*", 50);
        if (winner != null) {
            System.out.println("  " + winner.getName() + " WINS with " + winner.getScore() + " points!");
        } else {
            System.out.println("  Game finished but no winner could be determined.");
        }
        printLine("*", 50);
    }

    // ---- Helper methods for printing & actions (existing implementations remain) ----

    // prints a repeated character as a divider line
    private static void printLine(String ch, int len) {
        for (int i = 0; i < len; i++) {
            System.out.print(ch);
        }
        System.out.println();
    }

    // prints the full board: gem counts, nobles, and all card slots
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

    // compact card summary: prestige, bonus color, and cost
    private static String formatCard(Card c) {
        return "PV:" + c.getPrestigePoints() + " " + c.getBonus() + "+ " + formatCost(c.getCost());
    }

    // noble display: just the point value and what bonuses are required
    private static String formatNoble(Noble n) {
        return "3 pts - needs " + formatCost(n.getCost());
    }

    // formats a gem cost map, skipping any colors with 0 required
    private static String formatCost(Map<Token, Integer> cost) {
        StringBuilder sb = new StringBuilder("Cost:");
        for (Token t : new Token[]{Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE}) {
            int n = cost.getOrDefault(t, 0);
            if (n > 0) sb.append(" ").append(t).append("x").append(n);
        }
        return sb.toString();
    }

    // formats a token map as a compact string, only showing colors with at least 1
    private static String formatTokens(Map<Token, Integer> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : Token.values()) {
            int n = tokens.getOrDefault(t, 0);
            if (n > 0) sb.append(t).append(":").append(n).append("  ");
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? "none" : s;
    }

    // print the player's current score, tokens, bonuses, and reserved cards
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

    // ---- Action methods (unchanged) ----

    // handle "take 3 different gems" — reads 3 color names from the player
    private static boolean doTakeThreeGems(Game game, Player p, Scanner sc) {
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

        if (!game.canTakeThreeDifferentGems(p, t1, t2, t3)) {
            System.out.println("Invalid move: must be 3 different non-gold colors, available on board, and within token limit.");
            return false;
        }

        game.takeThreeDifferentGems(p, t1, t2, t3);
        System.out.println("Took 1 " + t1 + ", 1 " + t2 + ", 1 " + t3);
        return true;
    }

    // handle "take 2 same gems" — reads one color name
    private static boolean doTakeTwoSameGems(Game game, Player p, Scanner sc) {
        System.out.print("Enter color (need 4+ on board): ");
        String line = sc.nextLine().trim().toLowerCase();
        Token t = parseToken(line);

        if (t == null || t == Token.GOLD) {
            System.out.println("Invalid color. Use: green, white, blue, black, red");
            return false;
        }

        if (!game.canTakeTwoSameGems(p, t)) {
            System.out.println("Invalid move: need at least 4 of that color on board and must stay within token limit.");
            return false;
        }

        game.takeTwoSameGems(p, t);
        System.out.println("Took 2 " + t);
        return true;
    }

    // handle "buy a card" — accepts either "level-slot" (e.g. 1-0) or "r-0" for reserved
    private static boolean doBuyCard(Game game, Player p, Scanner sc) {
        System.out.print("Enter card (e.g. 1-0 for level 1 slot 0, or r-0 for reserved): ");
        String line = sc.nextLine().trim().toLowerCase();

        try {
            if (line.startsWith("r")) {
                // buying from reserved hand
                String num = line.replace("r", "").replace("-", "").trim();
                int reservedIndex = Integer.parseInt(num);

                if (!game.canBuyReservedCard(p, reservedIndex)) {
                    System.out.println("You cannot buy that reserved card.");
                    return false;
                }

                Card card = p.getHand().get(reservedIndex);
                game.buyReservedCard(p, reservedIndex);
                System.out.println("Purchased reserved card: " + formatCard(card) +
                        " (+" + card.getPrestigePoints() + " pts)");
                return true;

            } else {
                // buying from the visible market
                String[] parts = line.split("-");
                if (parts.length != 2) {
                    System.out.println("Invalid format. Use level-slot (e.g. 1-0, 2-2)");
                    return false;
                }

                int level = Integer.parseInt(parts[0].trim());
                int slot = Integer.parseInt(parts[1].trim());

                if (!game.canBuyVisibleCard(p, level, slot)) {
                    System.out.println("You cannot buy that visible card.");
                    return false;
                }

                Card card = game.getBoard().getVisibleCards(level)[slot];
                game.buyVisibleCard(p, level, slot);
                System.out.println("Purchased: " + formatCard(card) +
                        " (+" + card.getPrestigePoints() + " pts)");
                return true;
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid numbers.");
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // handle "reserve a card" — accepts "level-slot" for visible or "deck N" for blind
    private static boolean doReserveCard(Game game, Player p, Scanner sc) {
        if (p.getHand().size() >= 3) {
            System.out.println("You already have 3 reserved cards. Buy one first.");
            return false;
        }

        System.out.print("Enter card (e.g. 1-0) or 'deck 1', 'deck 2', 'deck 3' for top of deck: ");
        String line = sc.nextLine().trim().toLowerCase();

        try {
            if (line.startsWith("deck")) {
                // blind reserve from a deck
                String[] parts = line.split("\\s+");
                if (parts.length != 2) {
                    System.out.println("Use: deck 1, deck 2, or deck 3");
                    return false;
                }

                int level = Integer.parseInt(parts[1]);

                if (!game.canReserveDeckCard(p, level)) {
                    System.out.println("Cannot reserve from that deck.");
                    return false;
                }

                game.reserveDeckCard(p, level);
                System.out.println("Reserved top card from deck " + level + ".");
                return true;

            } else {
                // reserve a specific visible card
                String[] parts = line.split("-");
                if (parts.length != 2) {
                    System.out.println("Use level-slot (e.g. 1-0) or deck 1");
                    return false;
                }

                int level = Integer.parseInt(parts[0].trim());
                int slot = Integer.parseInt(parts[1].trim());

                if (!game.canReserveVisibleCard(p, level, slot)) {
                    System.out.println("Cannot reserve that visible card.");
                    return false;
                }

                Card card = game.getBoard().getVisibleCards(level)[slot];
                game.reserveVisibleCard(p, level, slot);
                System.out.println("Reserved: " + formatCard(card));
                return true;
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid numbers.");
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // let the AI decide and execute its turn, then print what it did
    private static boolean doAITurn(Game game, Player p) {
        Board board = game.getBoard();
        String action = SplendorAI.chooseAction(p, board);

        if (action == null) {
            System.out.println("AI could not decide. Skipping turn.");
            return true;
        }

        String[] parts = action.split(":");
        if (parts.length < 2) return false;

        try {
            if (parts[0].equals("1") && parts.length >= 4) {
                // take 3 different gems
                Token t1 = Token.valueOf(parts[1]);
                Token t2 = Token.valueOf(parts[2]);
                Token t3 = Token.valueOf(parts[3]);

                if (game.canTakeThreeDifferentGems(p, t1, t2, t3)) {
                    game.takeThreeDifferentGems(p, t1, t2, t3);
                    System.out.println("AI takes 1 " + t1 + ", 1 " + t2 + ", 1 " + t3);
                    return true;
                }

            } else if (parts[0].equals("2") && parts.length >= 2) {
                // take 2 of the same
                Token t = Token.valueOf(parts[1]);

                if (game.canTakeTwoSameGems(p, t)) {
                    game.takeTwoSameGems(p, t);
                    System.out.println("AI takes 2 " + t);
                    return true;
                }

            } else if (parts[0].equals("3") && parts.length >= 2) {
                // buy a card (reserved or visible)
                if (parts[1].equals("r") && parts.length >= 3) {
                    int idx = Integer.parseInt(parts[2]);

                    if (game.canBuyReservedCard(p, idx)) {
                        Card card = p.getHand().get(idx);
                        game.buyReservedCard(p, idx);
                        System.out.println("AI buys reserved: " + formatCard(card));
                        return true;
                    }

                } else {
                    int level = Integer.parseInt(parts[1]);
                    int slot = Integer.parseInt(parts[2]);

                    if (game.canBuyVisibleCard(p, level, slot)) {
                        Card card = board.getVisibleCards(level)[slot];
                        game.buyVisibleCard(p, level, slot);
                        System.out.println("AI buys: " + formatCard(card));
                        return true;
                    }
                }

            } else if (parts[0].equals("4") && parts.length >= 3) {
                // reserve a card (deck or visible)
                if (parts[1].equals("deck")) {
                    int level = Integer.parseInt(parts[2]);

                    if (game.canReserveDeckCard(p, level)) {
                        game.reserveDeckCard(p, level);
                        System.out.println("AI reserves from deck " + level);
                        return true;
                    }

                } else {
                    int level = Integer.parseInt(parts[1]);
                    int slot = Integer.parseInt(parts[2]);

                    if (game.canReserveVisibleCard(p, level, slot)) {
                        Card card = board.getVisibleCards(level)[slot];
                        game.reserveVisibleCard(p, level, slot);
                        System.out.println("AI reserves: " + formatCard(card));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("AI move failed: " + e.getMessage());
        }

        return false;
    }

    // AI version: automatically return the least-needed tokens if over 10
    private static void returnExcessTokensAI(Game game, Player p) {
        if (!game.mustReturnTokens(p)) {
            return;
        }

        int toReturn = game.getNumTokensToReturn(p);
        List<Token> tokens = SplendorAI.chooseTokensToReturn(p, game.getBoard(), toReturn);

        for (Token t : tokens) {
            game.returnToken(p, t, 1);
        }

        System.out.println("AI returns " + toReturn + " token(s).");
    }

    // human version: prompt the player to pick which tokens to return until they're at 10
    private static void returnExcessTokens(Game game, Player p, Scanner sc) {
        if (!game.mustReturnTokens(p)) {
            return;
        }

        int toReturn = game.getNumTokensToReturn(p);
        System.out.println("You have " + p.getTotalTokenCount() +
                " tokens. Max is 10. Return " + toReturn + ".");

        int returned = 0;
        while (returned < toReturn) {
            System.out.print("Color to return (green/white/blue/black/red/gold): ");
            String line = sc.nextLine().trim().toLowerCase();

            Token t = parseToken(line);
            if (t == null && line.equals("gold")) {
                t = Token.GOLD; // parseToken skips gold, so handle it separately
            }

            if (t != null && p.getTokenCount(t) > 0) {
                game.returnToken(p, t, 1);
                returned++;
                System.out.println("Returned 1 " + t + ". " +
                        (toReturn - returned) + " more to go.");
            } else {
                System.out.println("Invalid or you don't have that color.");
            }
        }
    }

    // check if a noble wants to visit after the player's turn and print if so
    private static void checkNobleVisit(Game game, Player p) {
        Noble noble = game.checkAndAwardNoble(p);
        if (noble != null) {
            System.out.println("A noble visits! " + noble + " (+3 pts)");
        }
    }

    // converts a color string typed by the player to the matching Token — returns null for unknown input
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
