package interfaces;

import java.util.Map;
import model.Token;

public interface INoble {
    
    int getPrestigePoints();
    
    Map<Token, Integer> getCost();
}
