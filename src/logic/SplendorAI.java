package logic;

import java.util.List;
import model.Board;
import model.Player;
import model.Token;

/**
 * Backward-compatibility wrapper — delegates everything to {@link BotMain}.
 *
 * Existing callers (GameEngine, GameApp) continue to work unchanged.
 * New code should call BotMain directly.
 */
public class SplendorAI {

    public static String chooseAction(Player p, Board board, List<Player> allPlayers) {
        return BotMain.chooseAction(p, board, allPlayers);
    }

    public static String chooseAction(Player p, Board board) {
        return BotMain.chooseAction(p, board);
    }

    public static List<Token> chooseTokensToReturn(Player p, Board board, int count) {
        return BotMain.chooseTokensToReturn(p, board, count);
    }
}
