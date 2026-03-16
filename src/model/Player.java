package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    private final String name;
    private final boolean isHuman;

    private final Map<Token, Integer> tokens = new HashMap<>();
    private final List<Card> hand = new ArrayList<>(); // Reserved cards
    private final List<Card> purchasedCards = new ArrayList<>();
    private final List<Noble> nobles = new ArrayList<>();

    public Player(String name, boolean isHuman) {
        this.name = name;
        this.isHuman = isHuman;
        // Initialize tokens with 0
        for (Token t : Token.values()) {
            tokens.put(t, 0);
        }
    }

    public String getName() {
        return name;
    }

    public boolean isHuman() {
        return isHuman;
    }

    public Map<Token, Integer> getTokens() {
        return tokens;
    }

    public int getTokenCount(Token t) {
        return tokens.getOrDefault(t, 0);
    }

    public int getTotalTokenCount() {
        return tokens.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void addTokens(Token t, int count) {
        tokens.put(t, tokens.getOrDefault(t, 0) + count);
    }

    public void removeTokens(Token t, int count) {
        int current = tokens.getOrDefault(t, 0);
        if (current < count)
            throw new IllegalArgumentException("Not enough tokens");
        tokens.put(t, current - count);
    }

    public List<Card> getHand() {
        return hand;
    }

    public List<Card> getPurchasedCards() {
        return purchasedCards;
    }

    public List<Noble> getNobles() {
        return nobles;
    }

    public void reserveCard(Card card) {
        if (hand.size() >= 3)
            throw new IllegalStateException("Hand is full (max 3 reserved cards)");
        hand.add(card);
    }

    public void buyCard(Card card) {
        purchasedCards.add(card);
        // If card was in hand (reserved), remove it
        hand.remove(card);
    }

    public void receiveNoble(Noble noble) {
        nobles.add(noble);
    }

    public boolean canGetNoble(Noble noble) {
        Map<Token, Integer> playerBonus = getBonuses();
        Map<Token, Integer> cost = noble.getCost();

        for (Map.Entry<Token, Integer> entry : cost.entrySet()) {
            Token token = entry.getKey();
            int required = entry.getValue();
            if (playerBonus.getOrDefault(token, 0) < required) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if player can afford a card (bonuses reduce cost, gold is wild).
     */
    public boolean canAffordCard(Card card) {
        Map<Token, Integer> cost = card.getCost();
        Map<Token, Integer> bonuses = getBonuses();

        int totalNeeded = 0;
        int totalFromRegular = 0;

        for (Token t : new Token[]{Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE}) {
            int cardCost = cost.getOrDefault(t, 0);
            int bonus = bonuses.getOrDefault(t, 0);
            int effectiveCost = cardCost - bonus;
            if (effectiveCost > 0) {
                totalNeeded += effectiveCost;
                int payWithColor = Math.min(getTokenCount(t), effectiveCost);
                totalFromRegular += payWithColor;
            }
        }

        int goldNeeded = totalNeeded - totalFromRegular;
        return goldNeeded <= getTokenCount(Token.GOLD);
    }

    /**
     * Pay for a card: deduct tokens. Uses regular tokens first, then gold.
     */
    public void payForCard(Card card, Board board) {
        Map<Token, Integer> cost = card.getCost();
        Map<Token, Integer> bonuses = getBonuses();

        for (Token t : new Token[]{Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE}) {
            int cardCost = cost.getOrDefault(t, 0);
            int bonus = bonuses.getOrDefault(t, 0);
            int effectiveCost = cardCost - bonus;
            if (effectiveCost > 0) {
                int toPay = Math.min(getTokenCount(t), effectiveCost);
                if (toPay > 0) {
                    removeTokens(t, toPay);
                    board.addToken(t, toPay);
                }
                int goldNeeded = effectiveCost - toPay;
                if (goldNeeded > 0) {
                    removeTokens(Token.GOLD, goldNeeded);
                    board.addToken(Token.GOLD, goldNeeded);
                }
            }
        }
    }

    public int getScore() {
        int cardPoints = purchasedCards.stream().mapToInt(Card::getPrestigePoints).sum();
        int noblePoints = nobles.stream().mapToInt(Noble::getPrestigePoints).sum();
        return cardPoints + noblePoints;
    }

    public Map<Token, Integer> getBonuses() {
        Map<Token, Integer> bonuses = new HashMap<>();
        for (Token t : Token.values()) {
            if (t == Token.GOLD)
                continue; // Gold is not a bonus
            bonuses.put(t, 0);
        }
        for (Card c : purchasedCards) {
            Token b = c.getBonus();
            if (b != null) {
                bonuses.put(b, bonuses.getOrDefault(b, 0) + 1);
            }
        }
        return bonuses;
    }

    @Override
    public String toString() {
        return name + " [" + getScore() + " pts] Tokens: " + tokens;
    }
}
