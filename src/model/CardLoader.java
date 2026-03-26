package model;

import java.io.*;
import java.util.*;


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
        try {
            File input = new File(path);
            BufferedReader br = new BufferedReader(new FileReader(input));
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
        } catch (FileNotFoundException e){
            throw new FileNotFoundException();
        }

        return cards;
    }

    // reads all nobles from csv - expected format: pv,white,blue,green,red,black
    public static List<Noble> loadNobles(String path) throws IOException {
        List<Noble> nobles = new ArrayList<Noble>();
        Scanner in = null;

        try {
            File input = new File(path);
            in = new Scanner(input);
            // Skip first line with headers

            in.nextLine();
            while (in.hasNextLine()) {
                String line = in.nextLine();
   
                Scanner lineSc = new Scanner(line);
                lineSc.useDelimiter(";");

                int pv = Integer.parseInt(lineSc.next());
                Map<Token, Integer> cost = new HashMap<>();
                cost.put(Token.WHITE, Integer.parseInt(lineSc.next()));
                cost.put(Token.BLUE,  Integer.parseInt(lineSc.next()));
                cost.put(Token.GREEN, Integer.parseInt(lineSc.next()));
                cost.put(Token.RED,   Integer.parseInt(lineSc.next()));
                cost.put(Token.BLACK, Integer.parseInt(lineSc.next()));
                nobles.add(new Noble(pv, cost));
       
                lineSc.close();
            }
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        } catch (NoSuchElementException e) {
            System.out.println("File has wrong format, null returned");
    
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return nobles;

    }
}
