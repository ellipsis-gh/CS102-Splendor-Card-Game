package engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import config.GameConfig;
import game.CardLoader;
import logic.Game;
import model.Board;
import model.Card;
import model.Deck;
import model.Noble;
import model.Player;

/**
 * Pure factory for creating a configured {@link Game} instance.
 *
 * <p>Contains no I/O — all setup choices are passed as parameters.
 * This replaces {@code GameApp.setupGame()} and is shared by local
 * and network modes.</p>
 */
public class GameSetup {

    private static final String CARDS_FILEPATH  = GameConfig.getCardFilePath();
    private static final String NOBLES_FILEPATH = GameConfig.getNobleFilePath();

    private GameSetup() {}

    /** Convenience overload — all players use default names. */
    public static Game create(int numPlayers, boolean[] isAI) {
        return create(numPlayers, isAI, null);
    }

    /**
     * Builds decks, selects nobles, and creates Player objects before
     * constructing the {@link Game}.
     *
     * @param numPlayers  total number of players (2-4)
     * @param isAI        {@code true} at index i → player i is AI-controlled
     * @param playerNames optional custom names; null or blank entries fall back
     *                    to "Player: N" / "AI: N"
     */
    public static Game create(int numPlayers, boolean[] isAI, String[] playerNames) {
        // --- Cards ---
        List<Card> allCards = new ArrayList<>();
        try {
            allCards = CardLoader.loadCards(CARDS_FILEPATH);
        } catch (IOException e) {
            System.err.println("Error: Could not load Splendor Cards.csv");
            System.err.println("Make sure the file is in the same folder as the program.");
        }

        List<Card> level1 = new ArrayList<>(), level2 = new ArrayList<>(), level3 = new ArrayList<>();
        for (Card c : allCards) {
            if      (c.getLevel() == 1) level1.add(c);
            else if (c.getLevel() == 2) level2.add(c);
            else                        level3.add(c);
        }

        Deck d1 = new Deck(1, level1), d2 = new Deck(2, level2), d3 = new Deck(3, level3);
        d1.shuffle(); d2.shuffle(); d3.shuffle();

        // --- Nobles ---
        List<Noble> allNobles = new ArrayList<>();
        try {
            allNobles = CardLoader.loadNobles(NOBLES_FILEPATH);
        } catch (IOException e) {
            System.err.println("Error: Could not load Nobles.csv");
            System.err.println("Make sure the file is in the same folder as the program.");
        }

        Collections.shuffle(allNobles);
        int noblesToShow = GameConfig.getInitialNobles(numPlayers);
        List<Noble> nobles = new ArrayList<>();
        for (int i = 0; i < noblesToShow && i < allNobles.size(); i++) {
            nobles.add(allNobles.get(i));
        }

        // --- Board + Players ---
        Board board = new Board(nobles, d1, d2, d3, numPlayers);

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String name;
            if (playerNames != null && i < playerNames.length
                    && playerNames[i] != null && !playerNames[i].isBlank()) {
                name = playerNames[i];
            } else if (isAI[i]) {
                name = "AI: " + (i + 1);
            } else {
                name = "Player: " + (i + 1);
            }
            players.add(new Player(name, !isAI[i]));
        }

        return new Game(board, players);
    }
}
