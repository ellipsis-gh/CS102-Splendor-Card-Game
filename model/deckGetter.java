package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.splendor.model.Deck;
import com.splendor.model.Token;

import model.*;



public class deckGetter {
    public Map<Integer, Deck> getDecksByLevel(){
        List<Card> deck1Cards = new ArrayList<>();
        List<Card> deck2Cards = new ArrayList<>();
        List<Card> deck3Cards = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader("Splendor Cards.csv"));
        
        reader.readLine(); // Skip header

        String line;
        int lineNum = 2;

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
                list1.add(c);
            } else if (lineNum <= 71) {
                list2.add(c);
            } else {
                list3.add(c);
            }
            lineNum++;
        }
        ArrayList<ArrayList<Card>> allDecks = new ArrayList<>();
        allDecks.add(level1); 
        allDecks.add(level2); 
        allDecks.add(level3); 
    }
}
