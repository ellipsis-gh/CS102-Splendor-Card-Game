package interfaces;

import java.util.List;
import java.util.Map;
import model.Board;
import model.Card;
import model.Noble;
import model.Token;

public interface IPlayer {
    String getName();
    boolean isHuman();
    Map<Token, Integer> getTokens();
    int getTokenCount(Token t);
    int getTotalTokenCount();
    void addTokens(Token t, int count);
    void removeTokens(Token t, int count);
    List<Card> getHand();
    List<Card> getPurchasedCards();
    List<Noble> getNobles();
    void reserveCard(Card card);
    void buyCard(Card card);
    void receiveNoble(Noble noble);
    boolean canGetNoble(Noble noble);
    boolean canAffordCard(Card card);
    void payForCard(Card card, Board board);
    int getScore();
    Map<Token, Integer> getBonuses();
}
