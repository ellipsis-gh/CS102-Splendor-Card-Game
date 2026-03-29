package model;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import interfaces.IDeck;

// a deck of cards for one level — handles shuffling and drawing
public class Deck implements IDeck {
    private final int level;
    private final ArrayList<Card> cards;

    public Deck(int level, List<Card> cards) {
        this.level = level;
        this.cards = new ArrayList<>(cards); // copy so the original list isn't modified
    }

    // randomize before the game starts
    public void shuffle() {
        Collections.shuffle(cards);
    }

    // pull the top card — returns null if the deck ran out
    public Card draw() {
        if (cards.isEmpty())
            return null;
        return cards.remove(0);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int getLevel() {
        return level;
    }

    // handy for showing how many cards are left in a deck on the board
    public int getRemainingCount() {
        return cards.size();
    }
}
