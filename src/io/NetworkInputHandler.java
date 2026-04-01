package io;

import java.io.IOException;
import java.util.List;

import logic.Game;
import model.Token;
import network.ClientHandler;

/**
 * {@link InputHandler} implementation that reads from a socket-connected client.
 *
 * <p><b>Runs on the SERVER side.</b> Each method:</p>
 * <ol>
 *   <li>Sends {@link #INPUT_NEEDED} to the current player's socket to signal
 *       that the client should read a line from the user and send it back.</li>
 *   <li>Blocks on {@link ClientHandler#readLine()} until the client responds.</li>
 *   <li>Validates the response; re-sends an error + {@code INPUT_NEEDED} if invalid.</li>
 * </ol>
 *
 * <p>The client ({@code ClientMain}) enters active input mode when it receives
 * {@code INPUT_NEEDED}, reads exactly one line from stdin, and forwards it.</p>
 */
public class NetworkInputHandler implements InputHandler {

    /**
     * Protocol sentinel: server → current client.
     * Tells the client to read the next user keystroke and send it back.
     */
    public static final String INPUT_NEEDED = "INPUT_NEEDED";

    private final ClientHandler[] clients;
    private final Game            game;

    /**
     * @param clients array of connected players (index matches player index in game)
     * @param game    live game — used to resolve the current player index
     */
    public NetworkInputHandler(ClientHandler[] clients, Game game) {
        this.clients = clients;
        this.game    = game;
    }

    // -----------------------------------------------------------------------
    // Interface implementation
    // -----------------------------------------------------------------------

    @Override
    public String readMenuChoice() {
        ClientHandler client = currentClient();
        while (true) {
            client.send(INPUT_NEEDED);
            String line = safeRead(client);
            if (line == null) return null;
            String choice = line.trim().toLowerCase();
            if (choice.equals("1") || choice.equals("2") || choice.equals("3")
                    || choice.equals("r") || choice.equals("q")) {
                return choice;
            }
            client.send("Invalid choice. Enter 1, 2, 3, R, or Q.");
        }
    }

    @Override
    public List<Token> readGemSelection(List<Token> available) {
        ClientHandler client = currentClient();
        while (true) {
            client.send(INPUT_NEEDED);
            String line = safeRead(client);
            if (line == null) return null;
            line = line.trim();
            if (line.equals("0")) return null;

            String[] parts = line.split("\\s+");
            try {
                if (parts.length == 2 && parts[0].equals(parts[1])) {
                    int idx = Integer.parseInt(parts[0]) - 1;
                    if (idx >= 0 && idx < available.size()) {
                        Token t = available.get(idx);
                        return List.of(t, t);
                    }
                } else if (parts.length == 3) {
                    int i1 = Integer.parseInt(parts[0]) - 1;
                    int i2 = Integer.parseInt(parts[1]) - 1;
                    int i3 = Integer.parseInt(parts[2]) - 1;
                    if (i1 >= 0 && i1 < available.size()
                            && i2 >= 0 && i2 < available.size()
                            && i3 >= 0 && i3 < available.size()) {
                        return List.of(available.get(i1), available.get(i2), available.get(i3));
                    }
                }
            } catch (NumberFormatException ignored) { /* fall through to error */ }

            client.send("Invalid. Enter 2 matching numbers or 3 different numbers, or 0 to cancel.");
        }
    }

    @Override
    public int readCardIndex(List<String> options) {
        ClientHandler client = currentClient();
        while (true) {
            client.send(INPUT_NEEDED);
            String line = safeRead(client);
            if (line == null) return -1;
            try {
                int n = Integer.parseInt(line.trim());
                if (n == 0) return -1;
                if (n >= 1 && n <= options.size()) return n - 1;
            } catch (NumberFormatException ignored) { /* fall through */ }
            client.send("Invalid choice. Enter 1-" + options.size() + " or 0 to cancel.");
        }
    }

    @Override
    public Token readTokenToReturn(List<Token> held) {
        ClientHandler client = currentClient();
        while (true) {
            client.send(INPUT_NEEDED);
            String line = safeRead(client);
            if (line == null) return null;
            try {
                int n = Integer.parseInt(line.trim()) - 1;
                if (n >= 0 && n < held.size()) return held.get(n);
            } catch (NumberFormatException ignored) { /* fall through */ }
            client.send("Invalid choice. Enter a number from the list.");
        }
    }

    @Override
    public void waitForAck() {
        ClientHandler client = currentClient();
        client.send(INPUT_NEEDED);
        safeRead(client); // discard — just needs any response
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ClientHandler currentClient() {
        return clients[game.getCurrentPlayerIndex()];
    }

    private String safeRead(ClientHandler client) {
        try {
            return client.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
