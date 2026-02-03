package model;

import java.util.List;
import java.util.Map;

public class ConsoleUI {

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

    public void showBoard(Board board) {
        System.out.println("----- BOARD -----");

        System.out.println("Bank Tokens: " + formatTokenMap(board.getAvailableTokens()));

        System.out.println("Nobles:");
        for (Noble n : board.getNobles()) {
            System.out.println("  - " + n); 
        }

        // 3) Visible cards (market)
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

    private String shortCard(Card c) {
        // Example: "P1 +BLUE cost{GREEN=2, WHITE=1}"
        return "P" + c.getPrestigePoints()
                + " +" + c.getBonus()
                + " cost" + c.getCost();
    }

    private String formatTokenMap(Map<Token, Integer> map) {
        // Prints in a stable order (Token.values order)
        StringBuilder sb = new StringBuilder("{ ");
        for (Token t : Token.values()) {
            int v = map.getOrDefault(t, 0);
            sb.append(t.name()).append("=").append(v).append(" ");
        }
        sb.append("}");
        return sb.toString();
    }

    private void clearScreen() {
        // simple cross-platform-ish way
        System.out.print("\n\n\n\n\n\n\n\n\n\n");
    }
}
