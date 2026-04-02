package network;

import logic.Game;
import util.ui.ConsoleUI;

// prepares game state text for network clients.
// It reuses the same board layout as the local console version
// so both local and online games look consistent.
public class NetworkFormatter {

    // The server sends this before a full board redraw starts.
    // The client uses it to know when to begin buffering the screen.
    public static final String STATE_BEGIN = "<<STATE_BEGIN>>";

    // The server sends this after the full board redraw is finished.
    public static final String STATE_END = "<<STATE_END>>";

    // These mark the beginning and end of a list of cards the player can buy.
    // The values between them are just simple slot labels.
    public static final String BUYABLE_BEGIN = "<<BUYABLE_BEGIN>>";
    public static final String BUYABLE_END   = "<<BUYABLE_END>>";

    // This class only provides helper methods and constants.
    private NetworkFormatter() {
    }

    // This builds the full board view that gets sent to both clients.
    // It includes the current win score so the UI can display it properly.
    public static String formatGameState(Game game, int winScore) {
        return ConsoleUI.renderGameStateString(game, true, winScore);
    }
    
    // This is a simpler version that uses a default win score.
    public static String formatGameState(Game game) {
        return formatGameState(game, 10);
    }
}
