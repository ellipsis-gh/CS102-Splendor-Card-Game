package ui;

import java.util.List;

import logic.Game;
import model.Noble;
import model.Player;
import model.Token;

/**
 * Abstracts all game output, independent of whether output goes to stdout
 * (local mode) or a network socket (server mode).
 *
 * <h3>Two-tier messaging</h3>
 * <ul>
 *   <li>{@link #renderMessage} — delivered to the <em>current turn player</em>
 *       only (action feedback, error messages).</li>
 *   <li>{@link #renderBroadcast} — delivered to <em>all players</em>
 *       (noble visits, end-game announcements, AI moves).</li>
 * </ul>
 * In local / console mode both methods are identical (print to stdout).
 */
public interface GameRenderer {

    /** Clears the screen and prints the full board. Called at the start of every turn. */
    void renderGameState(Game game, int winScore);

    /** Prints the turn header, e.g. "Player 2's Turn". */
    void renderTurnHeader(Player player);

    /** Prints the action menu [1] Take / [2] Buy / [3] Reserve / [R] / [Q]. */
    void renderActionMenu();

    /**
     * Prints numbered gem colors with board counts plus selection instructions.
     * Called immediately before {@link io.InputHandler#readGemSelection}.
     */
    void renderGemOptions(List<Token> available);

    /**
     * Prints a numbered list of card options with a header.
     * Called immediately before {@link io.InputHandler#readCardIndex}.
     *
     * @param options formatted option strings (same list passed to readCardIndex)
     * @param header  section header, e.g. "Buyable cards:" or "Reserve which card?"
     */
    void renderCardOptions(List<String> options, String header);

    /**
     * Prints the token-return prompt showing numbered held tokens.
     * Called immediately before {@link io.InputHandler#readTokenToReturn}.
     *
     * @param held       tokens the player currently holds (same list passed to readTokenToReturn)
     * @param numToReturn how many must still be returned
     */
    void renderTokenReturnPrompt(Player player, int numToReturn, List<Token> held);

    /** Sends a message to the <em>current turn player</em> only. */
    void renderMessage(String message);

    /** Sends a message to <em>all players</em>. */
    void renderBroadcast(String message);

    /** Clears the screen and prints the final board + winner banner. */
    void renderGameOver(Game game, int winScore);

    /** Prints the reserved cards for the current player. */
    void renderReservedCards(Player player);

    /** Prints an AI-turn description to all players. */
    void renderAIAction(String description);

    /** Prints a noble-visit announcement to all players. */
    void renderNobleVisit(Player player, Noble noble);
}
