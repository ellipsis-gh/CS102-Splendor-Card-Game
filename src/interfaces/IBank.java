package interfaces;

import java.util.Map;
import model.Token;

public interface IBank {
    boolean hasEnough(Token type, int amount);
    void take(Token type, int amount);
    void giveBack(Token type, int amount);
    int getAvailable(Token type);
    Map<Token, Integer> getAllTokens();
}
