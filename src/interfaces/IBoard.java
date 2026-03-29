package interfaces;

import java.util.List;
import java.util.Map;
import model.Card;
import model.Noble;
import model.Token;

public interface IBoard {
    void refillMarket();

    Map<Token, Integer> getAvailableTokens();
    
    void removeToken(Token t, int count);
    void addToken(Token t, int count);
    Card[] getVisibleCards(int level);
    List<Noble> getNobles();
    void removeNoble(Noble n);
    Card takeCard(int level, int index);
    boolean canTakeTwo(Token t);
    boolean canTakeThreeDifferent(Token t1, Token t2, Token t3);
    Card drawFromDeck(int level);
    boolean deckHasCards(int level);
}
