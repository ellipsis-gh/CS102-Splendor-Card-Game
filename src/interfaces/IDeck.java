package interfaces;

import model.Card;

public interface IDeck {
    void shuffle();
    Card draw();
    boolean isEmpty();
    int getLevel();
    int getRemainingCount();
}
