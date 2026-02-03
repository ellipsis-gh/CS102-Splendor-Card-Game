package com.splendor.model;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class Deck {
    private final int level;
    private final ArrayList<Card> cards;

    public Deck(int level, List<Card> cards) {
        this.level = level;
        this.cards = new ArrayList<>(cards);
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

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

    public int getRemainingCount() {
        return cards.size();
    }
}