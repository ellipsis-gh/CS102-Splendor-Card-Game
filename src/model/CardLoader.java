package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// handles reading cards from a CSV file and building the default noble pool
public class CardLoader {

    // converts the color string from the CSV to the matching Token
    private static Token parseColor(String color) {
        switch (color.trim()) {
            case "Black": return Token.BLACK;
            case "Blue":  return Token.BLUE;
            case "Green": return Token.GREEN;
            case "Red":   return Token.RED;
            case "White": return Token.WHITE;
            default:      return Token.GREEN; // fallback — shouldn't normally happen
        }
    }

    // reads all cards from a CSV — expected columns: level, color, pv, black, blue, green, red, white
    public static List<Card> loadCards(String path) throws IOException {
        List<Card> cards = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String header = br.readLine(); // skip the header row
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 8) continue; // skip any malformed rows
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

    public static List<Noble> loadNobles(String path) throws IOException {
        List<Noble> nobles = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String header = br.readLine(); // skip the header row
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 6) continue;

                int prestigePoints = Integer.parseInt(parts[0].trim());
                Map<Token, Integer> cost = new HashMap<>();

                int black = Integer.parseInt(parts[1].trim());
                int blue = Integer.parseInt(parts[2].trim());
                int green = Integer.parseInt(parts[3].trim());
                int red = Integer.parseInt(parts[4].trim());
                int white = Integer.parseInt(parts[5].trim());

                if (black > 0) cost.put(Token.BLACK, black);
                if (blue > 0) cost.put(Token.BLUE, blue);
                if (green > 0) cost.put(Token.GREEN, green);
                if (red > 0) cost.put(Token.RED, red);
                if (white > 0) cost.put(Token.WHITE, white);

                nobles.add(new Noble(prestigePoints, cost));
            }
        }
        return nobles;
    }

    // loads nobles from CSV so the data is kept outside the code
    public static List<Noble> createDefaultNobles() {
        try {
            return loadNobles("NobleData.csv");
        } catch (IOException e) {
            throw new RuntimeException("Could not load NobleData.csv", e);
        }
    }
}
