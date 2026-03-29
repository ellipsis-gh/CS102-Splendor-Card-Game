package game;

import java.util.List;
import java.util.Map;

import model.Board;
import model.Card;
import model.Noble;
import model.Player;
import model.Token;

// handles printing the game state to the console
public class ConsoleUI {

    // clears the screen then shows the full board and all players
    public void showGameState(Board board, List<Player> players, int currentPlayerIndex) {
        clearScreen();

        System.out.println("============== SPLENDOR ==============");
        System.out.println("Current Player: " + players.get(currentPlayerIndex).getName());
        System.out.println();

        showBoard(board);
        System.out.println();
        showPlayers(players);
        System.out.println("======================================");
    }

    // prints tokens, nobles, and all 3 levels of face-up cards
    public void showBoard(Board board) {
        System.out.println("----- BOARD -----");

        System.out.println("Bank Tokens: " + formatTokenMap(board.getAvailableTokens()));

        System.out.println("Nobles:");
        for (Noble n : board.getNobles()) {
            System.out.println("  - " + n);
        }

        // show each level with slot indices so players can reference them when making moves
        for (int level = 1; level <= 3; level++) {
            System.out.println("Level " + level + " cards:");
            Card[] visible = board.getVisibleCards(level);
            for (int i = 0; i < visible.length; i++) {
                Card c = visible[i];
                if (c == null) {
                    System.out.println("  [" + i + "] (empty)");
                } else {
                    System.out.println("  [" + i + "] " + shortCard(c));
                }
            }
        }
    }

    // print every player's score, tokens, bonuses, and card counts
    public void showPlayers(List<Player> players) {
        System.out.println("----- PLAYERS -----");
        for (Player p : players) {
            System.out.println(p.getName()
                    + " | Score: " + p.getScore()
                    + " | Tokens: " + formatTokenMap(p.getTokens())
                    + " | Bonuses: " + formatTokenMap(p.getBonuses())
                    + " | Reserved: " + p.getHand().size()
                    + " | Purchased: " + p.getPurchasedCards().size()
                    + " | Nobles: " + p.getNobles().size());
        }
    }

    // compact card display — shows points, bonus color, and cost
    private String shortCard(Card c) {
        return "P" + c.getPrestigePoints()
                + " +" + c.getBonus()
                + " cost" + c.getCost();
    }

    // formats a token map in a consistent order (Token.values order)
    private String formatTokenMap(Map<Token, Integer> map) {
        StringBuilder sb = new StringBuilder("{ ");
        for (Token t : Token.values()) {
            int v = map.getOrDefault(t, 0);
            sb.append(t.name()).append("=").append(v).append(" ");
        }
        sb.append("}");
        return sb.toString();
    }

    // cheap cross-platform screen clear — just prints a bunch of blank lines
    private void clearScreen() {
        System.out.print("\n\n\n\n\n\n\n\n\n\n");
    }
}
