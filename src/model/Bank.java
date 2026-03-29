package model;

import java.util.HashMap;
import java.util.Map;
import interfaces.IBank;

public class Bank implements IBank {

    private final Map<Token, Integer> tokens; // token type -> how many left

    //sets up the bank for however many players are playing
    public Bank(int playerCount) {
        tokens = new HashMap<>(); // start with an empty map
        initializeTokens(playerCount); // fill it based on player count
    }

    private void initializeTokens(int playerCount) {
        int normalCount;

        // more players = more tokens
        switch (playerCount) {
            case 2:
                normalCount = 4;
                break;
            case 3:
                normalCount = 5;
                break;
            case 4:
                normalCount = 7;
                break;
            default:
                throw new IllegalArgumentException("Player count must be 2–4");
        }

        for (Token t : Token.values()) {
            if (t == Token.GOLD) {
                tokens.put(t, 5); // gold is always 5
            } else {
                tokens.put(t, normalCount);//if its not gold 
            }
        }
    }

    // checking if we have enough of this token?
    public boolean hasEnough(Token type, int amount) {
        return tokens.get(type) >= amount;
    }

    // player takes tokens from the bank
    public void take(Token type, int amount) {
        if (!hasEnough(type, amount)) {
            throw new IllegalArgumentException("Not enough tokens in bank");
        } else {
            tokens.put(type, tokens.get(type) - amount);
        }
    }

    // player gives tokens back
    public void giveBack(Token type, int amount) {
        tokens.put(type, tokens.get(type) + amount);
    }

    // how many of this token are left
    public int getAvailable(Token type) {
        return tokens.get(type);
    }

    // returns a copy so the original can't be modified
    public Map<Token, Integer> getAllTokens() {
        return new HashMap<>(tokens);
    }
}
