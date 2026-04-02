package logic;

import java.util.List;
import model.Board;
import model.Difficulty;
import model.Player;
import model.Token;

/**
 * Main AI interface — routes each decision to the correct difficulty implementation.
 *
 * <p>All game-engine code should call this class. The four difficulty classes
 * ({@link EasyAI}, {@link MediumAI}, {@link HardAI}, {@link InsaneAI}) are
 * package-private; this is the only public entry point.</p>
 *
 * <h3>Difficulty tiers</h3>
 * <table border="1">
 *   <tr><th>Tier</th><th>Strategy</th></tr>
 *   <tr><td>EASY</td><td>Buys the first affordable card, takes the first 3 available gems — no strategy at all</td></tr>
 *   <tr><td>MEDIUM</td><td>Scores and picks the best card, smart gem collection, fixes original AI bugs</td></tr>
 *   <tr><td>HARD</td><td>Discount engine, weighted gem need, endgame urgency, opponent blocking</td></tr>
 *   <tr><td>INSANE</td><td>Sends the board to OpenAI ({@value InsaneAI#MODEL}) — falls back to HARD on error</td></tr>
 * </table>
 */
public class BotMain {

    // -----------------------------------------------------------------------
    // Action selection
    // -----------------------------------------------------------------------

    /**
     * Chooses the best action for {@code p} at the current game state.
     *
     * @param p          the AI-controlled player whose turn it is
     * @param board      the current board state
     * @param allPlayers all players in the game (used by HARD/INSANE for opponent tracking)
     * @return a command string, or {@code null} to skip the turn
     */
    public static String chooseAction(Player p, Board board, List<Player> allPlayers) {
        Difficulty diff = p.getDifficulty();
        if (diff == null) diff = Difficulty.MEDIUM;
        return switch (diff) {
            case EASY   -> EasyAI.chooseAction(p, board);
            case MEDIUM -> MediumAI.chooseAction(p, board);
            case HARD   -> HardAI.chooseAction(p, board, allPlayers);
            case INSANE -> InsaneAI.chooseAction(p, board, allPlayers);
        };
    }

    /** Backward-compatible overload — uses the player's own difficulty, defaults to MEDIUM. */
    public static String chooseAction(Player p, Board board) {
        return chooseAction(p, board, null);
    }

    // -----------------------------------------------------------------------
    // Token return
    // -----------------------------------------------------------------------

    /**
     * Chooses which {@code count} tokens to return when the player is over the 10-token limit.
     * EASY returns naively; all other difficulties use the weighted-need strategy.
     *
     * @param p     the player who must return tokens
     * @param board the current board (used for need scoring)
     * @param count how many tokens must be returned
     * @return list of tokens to hand back to the board
     */
    public static List<Token> chooseTokensToReturn(Player p, Board board, int count) {
        Difficulty diff = p.getDifficulty();
        if (diff == Difficulty.EASY) return EasyAI.chooseTokensToReturn(p, count);
        return AIHelpers.smartReturn(p, board, count);
    }
}
