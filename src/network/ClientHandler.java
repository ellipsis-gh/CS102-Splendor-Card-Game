package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// it stores that player’s Socket
// it creates a BufferedReader to read messages from the player
// it creates a PrintWriter to send messages to the player
// readLine() reads one line from the client
// send(String message) sends one line to the client
// close() closes the connection
// it also stores the player’s name with getPlayerName() and setPlayerName()

public class ClientHandler {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private String playerName;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String readLine() throws IOException {
        return in.readLine();
    }

    public void send(String message) {
        out.println(message);
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}