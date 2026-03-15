package network;

import model.Board;
import model.Card;
import model.Noble;
import model.Player;
import model.Token;

import java.util.List;
import java.util.Map;

public class NetworkFormatter {

    public static String formatBoard(Board board) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== BOARD ===\n");
        sb.append("Tokens: ").append(formatTokens(board.getAvailableTokens())).append("\n\n");

        sb.append("Nobles:\n");
        List<Noble> nobles = board.getNobles();
        for (int i = 0; i < nobles.size(); i++) {
            sb.append("[").append(i).append("] ")
              .append("3 pts - ").append(formatCost(nobles.get(i).getCost()))
              .append("\n");
        }

        sb.append("\n");
        for (int level = 1; level <= 3; level++) {
            sb.append("Level ").append(level).append(":\n");
            Card[] row = board.getVisibleCards(level);
            for (int i = 0; i < row.length; i++) {
                if (row[i] != null) {
                    sb.append("[").append(level).append("-").append(i).append("] ")
                      .append(formatCard(row[i])).append("\n");
                } else {
                    sb.append("[").append(level).append("-").append(i).append("] empty\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String formatPlayer(Player p) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(p.getName()).append(" ===\n");
        sb.append("Score: ").append(p.getScore()).append("\n");
        sb.append("Tokens: ").append(formatTokens(p.getTokens())).append("\n");
        sb.append("Bonuses: ").append(formatTokens(p.getBonuses())).append("\n");

        if (!p.getHand().isEmpty()) {
            sb.append("Reserved:\n");
            for (int i = 0; i < p.getHand().size(); i++) {
                sb.append("  [r-").append(i).append("] ")
                  .append(formatCard(p.getHand().get(i))).append("\n");
            }
        }

        return sb.toString();
    }

    private static String formatCard(Card c) {
        return "PV:" + c.getPrestigePoints() + " " + c.getBonus() + " " + formatCost(c.getCost());
    }

    private static String formatCost(Map<Token, Integer> cost) {
        StringBuilder sb = new StringBuilder("Cost:");
        for (Token t : new Token[]{Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE}) {
            int n = cost.getOrDefault(t, 0);
            if (n > 0) {
                sb.append(" ").append(t).append("x").append(n);
            }
        }
        return sb.toString();
    }

    private static String formatTokens(Map<Token, Integer> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : Token.values()) {
            int n = tokens.getOrDefault(t, 0);
            if (n > 0) {
                sb.append(t).append(":").append(n).append(" ");
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? "none" : result;
    }
}