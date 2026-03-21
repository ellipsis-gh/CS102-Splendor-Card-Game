package model;

import java.util.HashMap;
import java.util.Map;

// the bank holds all the tokens available in the game
public class Bank {

    private final Map<Token, Integer> tokens;      // maps each token type to how many are left

    // sets up the bank with the right number of tokens for the given player count
    public Bank(int playerCount) {
        tokens = new HashMap<>();
        initializeTokens(playerCount);
    }

    // fills the token map based on splendor rules — more players means more tokens
    private void initializeTokens(int playerCount) {
        int normalCount;

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
                tokens.put(t, 5); // gold is always 5 regardless of player count
            } else {
                tokens.put(t, normalCount);
            }
        }
    }

    // checks if the bank has at least this many tokens of the given type
    public boolean hasEnough(Token type, int amount) {
        return tokens.get(type) >= amount;
    }

    // removes tokens from the bank when a player takes them
    public void take(Token type, int amount) {
        if (!hasEnough(type, amount)) {
            throw new IllegalArgumentException("Not enough tokens in bank");
        }
        tokens.put(type, tokens.get(type) - amount);
    }

    // adds tokens back to the bank when a player returns them
    public void giveBack(Token type, int amount) {
        tokens.put(type, tokens.get(type) + amount);
    }

    // returns how many of a specific token are currently in the bank
    public int getAvailable(Token type) {
        return tokens.get(type);
    }

    // returns a copy so nothing outside can mess with the bank directly
    public Map<Token, Integer> getAllTokens() {
        return new HashMap<>(tokens);
    }
}
