package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardLoader {
    private static Token parseColor(String color) {
        switch (color.trim()) {
            case "Black": return Token.BLACK;
            case "Blue":  return Token.BLUE;
            case "Green": return Token.GREEN;
            case "Red":   return Token.RED;
            case "White": return Token.WHITE;
            default:      return Token.GREEN;
        }
    }

    public static List<Card> loadCards(String path) throws IOException {
        List<Card> cards = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 8) continue;
                int level = Integer.parseInt(parts[0].trim());
                Token bonus = parseColor(parts[1]);
                int pv = Integer.parseInt(parts[2].trim());
                Map<Token, Integer> cost = new HashMap<>();
                cost.put(Token.BLACK, Integer.parseInt(parts[3].trim()));
                cost.put(Token.BLUE,  Integer.parseInt(parts[4].trim()));
                cost.put(Token.GREEN, Integer.parseInt(parts[5].trim()));
                cost.put(Token.RED,   Integer.parseInt(parts[6].trim()));
                cost.put(Token.WHITE, Integer.parseInt(parts[7].trim()));
                cards.add(new Card(level, pv, bonus, cost));
            }
        }
        return cards;
    }

    public static List<Noble> createDefaultNobles() {
        List<Noble> nobles = new ArrayList<>();
        Map<Token, Integer> cost;
        cost = new HashMap<>(); cost.put(Token.BLACK, 4); cost.put(Token.BLUE, 4); nobles.add(new Noble(3, cost));
        cost = new HashMap<>(); cost.put(Token.GREEN, 4); cost.put(Token.RED, 4); nobles.add(new Noble(3, cost));
        cost = new HashMap<>(); cost.put(Token.BLUE, 4); cost.put(Token.WHITE, 4); nobles.add(new Noble(3, cost));
        cost = new HashMap<>(); cost.put(Token.BLACK, 3); cost.put(Token.GREEN, 3); cost.put(Token.WHITE, 3); nobles.add(new Noble(3, cost));
        cost = new HashMap<>(); cost.put(Token.BLUE, 3); cost.put(Token.RED, 3); cost.put(Token.WHITE, 3); nobles.add(new Noble(3, cost));
        return nobles;
    }
}
