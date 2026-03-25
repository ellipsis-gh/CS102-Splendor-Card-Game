package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// the physical game board — holds tokens, face-up cards, nobles, and the three decks
public class Board {
    private final Map<Token, Integer> availableTokens = new HashMap<>();
    private       List<Noble> nobles;
    private final Deck deck1; // level 1 deck (cheapest cards)
    private final Deck deck2; // level 2 deck
    private final Deck deck3; // level 3 deck (most expensive / highest value)

    private final Card[] level1Visible = new Card[4]; // 4 face-up slots per level
    private final Card[] level2Visible = new Card[4];
    private final Card[] level3Visible = new Card[4];

    public Board(List<Noble> nobles, Deck d1, Deck d2, Deck d3, int playerCount) {
        this.nobles = nobles;
        this.deck1 = d1;
        this.deck2 = d2;
        this.deck3 = d3;

        initializeTokens(playerCount);
        refillMarket(); // deal the starting face-up cards
    }

    // setup tokens based on player count
    private void initializeTokens(int playerCount) {
        int gemCount;
        if (playerCount == 2)
            gemCount = 4;
        else if (playerCount == 3)
            gemCount = 5;
        else
            gemCount = 7;

        availableTokens.put(Token.GREEN, gemCount);
        availableTokens.put(Token.WHITE, gemCount);
        availableTokens.put(Token.BLUE, gemCount);
        availableTokens.put(Token.BLACK, gemCount);
        availableTokens.put(Token.RED, gemCount);
        availableTokens.put(Token.GOLD, 5); // gold is always 5 regardless of player count
    }

    // fill any empty card slots by drawing from the appropriate deck
    public void refillMarket() {
        for (int i = 0; i < 4; i++) {
            if (level1Visible[i] == null)
                level1Visible[i] = deck1.draw();
            if (level2Visible[i] == null)
                level2Visible[i] = deck2.draw();
            if (level3Visible[i] == null)
                level3Visible[i] = deck3.draw();
        }
    }

    public Map<Token, Integer> getAvailableTokens() {
        return availableTokens;
    }

    // take tokens off the board when a player picks them up
    public void removeToken(Token t, int count) {
        int current = availableTokens.getOrDefault(t, 0);
        if (current < count)
            throw new IllegalArgumentException("Not enough tokens on board");
        availableTokens.put(t, current - count);
    }

    // put tokens back when a player pays or returns them
    public void addToken(Token t, int count) {
        availableTokens.put(t, availableTokens.getOrDefault(t, 0) + count);
    }

    // returns the 4 visible cards for the given level
    public Card[] getVisibleCards(int level) {
        switch (level) {
            case 1:
                return level1Visible;
            case 2:
                return level2Visible;
            case 3:
                return level3Visible;
            default:
                throw new IllegalArgumentException("Invalid level");
        }
    }

    public List<Noble> getNobles() {
        return nobles;
    }

    // remove a noble once a player has claimed it
    public void removeNoble(Noble n) {
        nobles.remove(n);
    }

    // removes the card from its slot and returns it — caller is responsible for refilling
    public Card takeCard(int level, int index) {
        Card[] row = getVisibleCards(level);
        if (index < 0 || index >= row.length)
            return null;
        Card c = row[index];
        row[index] = null; // leave the slot empty until refillMarket() is called
        return c;
    }

    // need at least 4 of a color on the board to allow taking 2 of the same
    public boolean canTakeTwo(Token t) {
        return availableTokens.getOrDefault(t, 0) >= 4;
    }

    // checks that 3 different non-gold tokens are all available on the board
    public boolean canTakeThreeDifferent(Token t1, Token t2, Token t3) {
        if (t1 == Token.GOLD || t2 == Token.GOLD || t3 == Token.GOLD)
            return false;
        if (t1 == t2 || t1 == t3 || t2 == t3)
            return false;
        if (availableTokens.getOrDefault(t1, 0) < 1) return false;
        if (availableTokens.getOrDefault(t2, 0) < 1) return false;
        if (availableTokens.getOrDefault(t3, 0) < 1) return false;
        return true;
    }

    // draws from the top of a deck — used when a player reserves blind
    public Card drawFromDeck(int level) {
        if (level == 1) return deck1.draw();
        if (level == 2) return deck2.draw();
        if (level == 3) return deck3.draw();
        return null;
    }

    // whether there are any cards left to draw from that level's deck
    public boolean deckHasCards(int level) {
        if (level == 1) return !deck1.isEmpty();
        if (level == 2) return !deck2.isEmpty();
        if (level == 3) return !deck3.isEmpty();
        return false;
    }
}
