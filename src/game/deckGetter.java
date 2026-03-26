package game;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import model.Card;
import model.Token;

// reads the CSV and splits all cards into 3 separate lists by level
public class deckGetter {
    public ArrayList<ArrayList<Card>> getDecksByLevel() {
        ArrayList<Card> deck1Cards = new ArrayList<>();
        ArrayList<Card> deck2Cards = new ArrayList<>();
        ArrayList<Card> deck3Cards = new ArrayList<>();

        // try-with-resources handles opening and closing the file automatically
        try (BufferedReader reader = new BufferedReader(new FileReader("Splendor Cards.csv"))) {

            reader.readLine(); // skip the header row

            String line;
            int lineNum = 2; // start at 2 since line 1 was the header

            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");

                Map<Token, Integer> cost = new HashMap<>();
                cost.put(Token.BLACK, Integer.parseInt(p[3]));
                cost.put(Token.BLUE,  Integer.parseInt(p[4]));
                cost.put(Token.GREEN, Integer.parseInt(p[5]));
                cost.put(Token.RED,   Integer.parseInt(p[6]));
                cost.put(Token.WHITE, Integer.parseInt(p[7]));

                Card c = new Card(Integer.parseInt(p[0]), Integer.parseInt(p[2]), Token.valueOf(p[1].toUpperCase()), cost);

                // assign to the right deck based on line number in the CSV
                // rows 2-41 = level 1, 42-71 = level 2, 72+ = level 3
                if (lineNum <= 41) {
                    deck1Cards.add(c);
                } else if (lineNum <= 71) {
                    deck2Cards.add(c);
                } else {
                    deck3Cards.add(c);
                }
                lineNum++;
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error: The CSV file was not found.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error: An error occurred while reading the file.");
            e.printStackTrace();
        }

        // pack all three lists into one return value
        ArrayList<ArrayList<Card>> allDecks = new ArrayList<>();
        allDecks.add(deck1Cards);
        allDecks.add(deck2Cards);
        allDecks.add(deck3Cards);

        return allDecks;
    }
}
