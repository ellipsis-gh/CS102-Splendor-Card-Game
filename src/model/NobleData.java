package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// all 10 nobles from the standard Splendor set
// the game picks a subset depending on player count
public class NobleData {

    // returns the full pool so the game can pick the right number of nobles
    public static List<Noble> nobleDataList() {
        List<Noble> nobleDeck = new ArrayList<>();

        nobleDeck.add(new Noble(3, Map.of(Token.RED, 4, Token.GREEN, 4)));
        nobleDeck.add(new Noble(3, Map.of(Token.RED, 3, Token.BLACK, 3, Token.GREEN, 3)));
        nobleDeck.add(new Noble(3, Map.of(Token.BLUE, 4, Token.WHITE, 4)));
        nobleDeck.add(new Noble(3, Map.of(Token.BLACK, 4, Token.WHITE, 4)));
        nobleDeck.add(new Noble(3, Map.of(Token.BLUE, 4, Token.GREEN, 4)));
        nobleDeck.add(new Noble(3, Map.of(Token.GREEN, 3, Token.BLUE, 3, Token.RED, 3)));
        nobleDeck.add(new Noble(3, Map.of(Token.GREEN, 3, Token.BLUE, 3, Token.WHITE, 3)));
        nobleDeck.add(new Noble(3, Map.of(Token.BLACK, 4, Token.RED, 4)));
        nobleDeck.add(new Noble(3, Map.of(Token.BLACK, 3, Token.BLUE, 3, Token.WHITE, 3)));
        nobleDeck.add(new Noble(3, Map.of(Token.BLACK, 3, Token.RED, 3, Token.GREEN, 3)));

        return nobleDeck;
    }
}
