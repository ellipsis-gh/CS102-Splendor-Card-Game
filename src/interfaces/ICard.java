package interfaces;

import java.util.Map;
import model.Token;

public interface ICard {
    int getLevel();
    int getPrestigePoints();
    Token getBonus();
    Map<Token, Integer> getCost();
}
