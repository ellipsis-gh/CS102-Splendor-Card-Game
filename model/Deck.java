package com.splendor.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Deck {
    private final int level;
    private final LinkedList<Card> cards;

    public Deck(int level, List<Card> cards) {
        this.level = level;
        this.cards = new LinkedList<>(cards);
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card draw() {
        if (cards.isEmpty())
            return null;
        return cards.poll();
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
