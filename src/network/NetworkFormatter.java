package network;

import logic.Game;
import util.ui.ConsoleUI;

/**
 * Formats game state for TCP clients. Uses the same layout and colours as the local {@link ConsoleUI}.
 */
public class NetworkFormatter {

    /**
     * Marker line sent by the server before a full state dump.
     * The client uses this to buffer the state and redraw it as one coherent screen.
     */
    public static final String STATE_BEGIN = "<<STATE_BEGIN>>";

    /**
     * Marker line sent by the server after a full state dump.
     */
    public static final String STATE_END = "<<STATE_END>>";

    /**
     * Marker lines sent by the server around a list of buyable card slots for the active player.
     * Lines between these markers are plain slot tags like "2-1" or "r-0".
     */
    public static final String BUYABLE_BEGIN = "<<BUYABLE_BEGIN>>";
    public static final String BUYABLE_END   = "<<BUYABLE_END>>";

    private NetworkFormatter() {
    }

    /**
     * Full board view matching the local client; includes ANSI colours for gem labels.
     *
     * @param game    current game state
     * @param winScore prestige points needed to win
     */

    public static String formatGameState(Game game, int winScore) {
        return ConsoleUI.renderGameStateString(game, true, winScore);
    }
    
    //default version
    /** Backward-compatible overload — assumes default win score of 10. */
    public static String formatGameState(Game game) {
        return formatGameState(game, 10);
    }
}
