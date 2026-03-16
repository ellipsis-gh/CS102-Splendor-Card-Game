package model;

import java.util.EnumMap;
import java.util.Map;

public class Bank {

    private final Map<Token, Integer> tokens;

    public Bank(int playerCount) {
        tokens = new EnumMap<>(Token.class);
        initializeTokens(playerCount);
    }

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
                tokens.put(t, 5);
            } else {
                tokens.put(t, normalCount);
            }
        }
    }

    public boolean hasEnough(Token type, int amount) {
        return tokens.get(type) >= amount;
    }

    public void take(Token type, int amount) {
        if (!hasEnough(type, amount)) {
            throw new IllegalArgumentException("Not enough tokens in bank");
        }
        tokens.put(type, tokens.get(type) - amount);
    }

    public void giveBack(Token type, int amount) {
        tokens.put(type, tokens.get(type) + amount);
    }

    public int getAvailable(Token type) {
        return tokens.get(type);
    }

    public Map<Token, Integer> getAllTokens() {
        return new EnumMap<>(tokens);
    }
}
