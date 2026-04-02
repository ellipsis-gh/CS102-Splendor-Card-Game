package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// It keeps the socket plus the input/output streams in one place for a client
// so the server can easily read from and send to that player.

public class ClientHandler {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private String playerName;

    // set up the connection tools for one client.
    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    // read one line of text sent by the client.
    public String readLine() throws IOException {
        return in.readLine();
    }

    // send one line of text to the client.
    public void send(String message) {
        out.println(message);
    }

    //close the socket when the player disconnects or when the game ends.
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    // return the player name.
    public String getPlayerName() {
        return playerName;
    }

    // set the player name.
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
