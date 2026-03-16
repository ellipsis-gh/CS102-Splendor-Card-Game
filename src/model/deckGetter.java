package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException; 
import java.io.FileNotFoundException; 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class deckGetter {
    public ArrayList<ArrayList<Card>> getDecksByLevel() {
        ArrayList<Card> deck1Cards = new ArrayList<>();
        ArrayList<Card> deck2Cards = new ArrayList<>();
        ArrayList<Card> deck3Cards = new ArrayList<>();

        // 1. Try-with-resources handles the creation and automatic closing of the reader
        try (BufferedReader reader = new BufferedReader(new FileReader("Splendor Cards.csv"))) {
            
            // 2. This call to readLine() is now inside the try block
            reader.readLine(); 

            String line;
            int lineNum = 2;

            // 3. This loop is also protected by the try block
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");

                Map<Token, Integer> cost = new HashMap<>();
                cost.put(Token.BLACK, Integer.parseInt(p[3]));
                cost.put(Token.BLUE,  Integer.parseInt(p[4]));
                cost.put(Token.GREEN, Integer.parseInt(p[5]));
                cost.put(Token.RED,   Integer.parseInt(p[6]));
                cost.put(Token.WHITE, Integer.parseInt(p[7]));

                Card c = new Card(Integer.parseInt(p[0]), Integer.parseInt(p[2]), Token.valueOf(p[1].toUpperCase()), cost);

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

        ArrayList<ArrayList<Card>> allDecks = new ArrayList<>();
        allDecks.add(deck1Cards); 
        allDecks.add(deck2Cards); 
        allDecks.add(deck3Cards); 

        return allDecks;
    }

}