package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private final Map<Token, Integer> availableTokens = new HashMap<>();
    private       List<Noble> nobles;
    private final Deck deck1;
    private final Deck deck2;
    private final Deck deck3;

    private final Card[] level1Visible = new Card[4];
    private final Card[] level2Visible = new Card[4];
    private final Card[] level3Visible = new Card[4];

    public Board(List<Noble> nobles, Deck d1, Deck d2, Deck d3, int playerCount) {
        this.nobles = nobles;
        this.deck1 = d1;
        this.deck2 = d2;
        this.deck3 = d3;

        initializeTokens(playerCount);
        refillMarket();
    }

    // Setup tokens based on player count
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
        availableTokens.put(Token.GOLD, 5); // Always 5 gold
    }

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

    public void removeToken(Token t, int count) {
        int current = availableTokens.getOrDefault(t, 0);
        if (current < count)
            throw new IllegalArgumentException("Not enough tokens on board");
        availableTokens.put(t, current - count);
    }

    public void addToken(Token t, int count) {
        availableTokens.put(t, availableTokens.getOrDefault(t, 0) + count);
    }

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

    public void removeNoble(Noble n) {
        nobles.remove(n);
    }

    // Returns the card and removes it from the slot (does not refill immediately,
    // GameController handles that or Board does)
    // Here we let specific method remove it
    public Card takeCard(int level, int index) {
        Card[] row = getVisibleCards(level);
        if (index < 0 || index >= row.length)
            return null;
        Card c = row[index];
        row[index] = null;
        return c;
    }

    public boolean canTakeTwo(Token t) {
        return availableTokens.getOrDefault(t, 0) >= 4;
    }
}

