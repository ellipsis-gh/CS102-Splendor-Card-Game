import java.util.*;

public class NobleDeck {
    public static List<Noble> NobleDataList(){

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
