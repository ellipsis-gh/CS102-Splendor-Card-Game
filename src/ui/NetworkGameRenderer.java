package ui;

import java.util.ArrayList;
import java.util.List;

import logic.Game;
import model.Noble;
import model.Player;
import model.Token;
import network.ClientHandler;
import network.NetworkFormatter;

/**
 * {@link GameRenderer} implementation that sends output over sockets.
 *
 * <p><b>Runs on the SERVER side.</b></p>
 * <ul>
 *   <li>{@link #renderGameState} / {@link #renderBroadcast} / {@link #renderAIAction} /
 *       {@link #renderNobleVisit} — sent to <em>all</em> connected clients.</li>
 *   <li>{@link #renderMessage} and all per-turn prompt methods — sent to the
 *       <em>current player's</em> client only.</li>
 *   <li>{@link #renderTurnHeader} — sends "YOUR TURN" to the current player and
 *       "WAITING FOR …" to the other.</li>
 * </ul>
 */
public class NetworkGameRenderer implements GameRenderer {

    private final ClientHandler[] clients;
    private final Game            game;

    /**
     * @param clients array indexed by player position (matches game player order)
     * @param game    live game — used to resolve the current player index
     */
    public NetworkGameRenderer(ClientHandler[] clients, Game game) {
        this.clients = clients;
        this.game    = game;
    }

    // -----------------------------------------------------------------------
    // Interface implementation
    // -----------------------------------------------------------------------

    @Override
    public void renderGameState(Game game, int winScore) {
        String state = NetworkFormatter.formatGameState(game, winScore);
        for (ClientHandler c : clients) sendState(c, state);
    }

    @Override
    public void renderTurnHeader(Player player) {
        currentClient().send("--- YOUR TURN: " + player.getName() + " ---");
        otherClient().send("--- WAITING FOR: " + player.getName() + " ---");
    }

    @Override
    public void renderActionMenu() {
        ClientHandler c = currentClient();
        c.send("┌───────────────────────────────┐");
        c.send("│ Choose Your Move              │");
        c.send("├───────────────────────────────┤");
        c.send("│ [1] Take gems                 │");
        c.send("│ [2] Buy a card                │");
        c.send("│ [3] Reserve a card            │");
        c.send("│ [R] View reserved cards       │");
        c.send("│ [Q] Quit                      │");
        c.send("└───────────────────────────────┘");
    }

    @Override
    public void renderGemOptions(List<Token> available) {
        ClientHandler c = currentClient();
        c.send("Available gem colors:");
        for (int i = 0; i < available.size(); i++) {
            c.send("  [" + (i + 1) + "] " + available.get(i));
        }
        c.send("  Take 3 different → enter 3 numbers e.g. 1 2 3");
        c.send("  Take 2 same      → enter same number twice e.g. 2 2");
        c.send("  [0] Cancel");
    }

    @Override
    public void renderCardOptions(List<String> options, String header) {
        ClientHandler c = currentClient();
        c.send(header);
        for (int i = 0; i < options.size(); i++) {
            c.send("  [" + (i + 1) + "] " + options.get(i));
        }
        c.send("  [0] Cancel");
    }

    @Override
    public void renderTokenReturnPrompt(Player player, int numToReturn, List<Token> held) {
        ClientHandler c     = currentClient();
        ClientHandler other = otherClient();
        c.send("TOKEN LIMIT: You have " + player.getTotalTokenCount()
                + " tokens (max 10). Return " + numToReturn + ".");
        c.send("Which token to return?");
        for (int i = 0; i < held.size(); i++) {
            c.send("  [" + (i + 1) + "] " + held.get(i)
                    + " (" + player.getTokenCount(held.get(i)) + ")");
        }
        other.send("Waiting: " + player.getName() + " must return " + numToReturn + " token(s).");
    }

    @Override
    public void renderMessage(String message) {
        currentClient().send(message);
    }

    @Override
    public void renderBroadcast(String message) {
        for (ClientHandler c : clients) c.send(message);
    }

    @Override
    public void renderGameOver(Game game, int winScore) {
        String state = NetworkFormatter.formatGameState(game, winScore);
        for (ClientHandler c : clients) sendState(c, state);
    }

    @Override
    public void renderReservedCards(Player player) {
        ClientHandler c = currentClient();
        c.send("--- Reserved Cards for " + player.getName() + " ---");
        List<model.Card> hand = player.getHand();
        if (hand.isEmpty()) {
            c.send("  (none)");
        } else {
            for (int i = 0; i < hand.size(); i++) {
                model.Card card = hand.get(i);
                c.send(String.format("  [r-%d] L%d  %d pts  Bonus: %s",
                        i, card.getLevel(), card.getPrestigePoints(), card.getBonus()));
            }
        }
    }

    @Override
    public void renderAIAction(String description) {
        for (ClientHandler c : clients) c.send(description);
    }

    @Override
    public void renderNobleVisit(Player player, Noble noble) {
        for (ClientHandler c : clients) {
            c.send("A noble visits " + player.getName() + "! (+3 pts)");
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void sendState(ClientHandler client, String stateText) {
        client.send(NetworkFormatter.STATE_BEGIN);
        for (String line : stateText.split("\n")) {
            client.send(line);
        }
        client.send(NetworkFormatter.STATE_END);
    }

    private ClientHandler currentClient() {
        return clients[game.getCurrentPlayerIndex()];
    }

    /** Works for the current 2-player network implementation. */
    private ClientHandler otherClient() {
        return clients[1 - game.getCurrentPlayerIndex()];
    }
}
