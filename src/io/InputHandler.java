package io;

import java.util.List;
import model.Token;

/**
 * Abstracts all player input, independent of whether input comes from a
 * local Scanner (console) or a network socket.
 *
 * <p>Return values signal intent:
 * <ul>
 *   <li>{@code null} — EOF, disconnect, or the player typed Q to quit.</li>
 *   <li>{@code -1}   — cancel / go back to menu (for index-based methods).</li>
 * </ul>
 * </p>
 */
public interface InputHandler {

    /**
     * Reads the top-level turn menu choice.
     *
     * @return "1", "2", "3", "r", or "q"; {@code null} on EOF / disconnect.
     */
    String readMenuChoice();

    /**
     * Reads a gem selection from the numbered available list.
     * The caller has already rendered the list via {@link ui.GameRenderer#renderGemOptions}.
     *
     * @param available non-gold tokens currently on the board (in display order)
     * @return list of 2 identical Tokens (take-two-same) or 3 Tokens
     *         (take-three-different); {@code null} to cancel.
     */
    List<Token> readGemSelection(List<Token> available);

    /**
     * Reads a card selection from a numbered list the caller has already rendered.
     *
     * @param options formatted option strings (0-based, caller-built)
     * @return 0-based index of the selected option; {@code -1} to cancel.
     */
    int readCardIndex(List<String> options);

    /**
     * Reads which token the player wants to return (one at a time).
     * The caller has already rendered the numbered held-token list.
     *
     * @param held tokens the player currently holds (in display order)
     * @return the chosen Token; {@code null} on EOF / disconnect.
     */
    Token readTokenToReturn(List<Token> held);

    /**
     * Blocks until the player acknowledges (presses Enter or sends any line).
     * Used after AI turns and the reserved-card viewer.
     */
    void waitForAck();
}
