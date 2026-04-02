package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

import config.GameConfig;
import engine.GameEngine;
import engine.GameSetup;
import io.NetworkInputHandler;
import logic.Game;
import ui.NetworkGameRenderer;

// This file handles the networking side of a 2-player game.
// Its job is to wait for both players to connect, collect their names,
// run the setup steps, and then hand everything to the normal game engine.
// The actual Splendor game rules are not handled here.
public class GameServer {

    private final int port;
    private static final int DEFAULT_WIN_SCORE = GameConfig.getWinningPoints();

    // This saves the port number the server should listen on.
    public GameServer(int port) {
        this.port = port;
    }

    // This starts the server socket and waits for both players to connect.
    // It also asks each player for their name before starting the game.
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            printLocalAddresses();
            System.out.println("Clients: run run_client.bat and enter one of the IPs above");

            System.out.println("Waiting for Player 1...");
            Socket socket1 = serverSocket.accept();
            ClientHandler client1 = new ClientHandler(socket1);
            client1.send("Connected as Player 1.");
            client1.send("Enter your name:");
            String name1 = client1.readLine();
            if (name1 == null || name1.isBlank()) name1 = "Player 1";
            client1.setPlayerName(name1);
            System.out.println(name1 + " connected.");
            client1.send("Waiting for Player 2...");

            System.out.println("Waiting for Player 2...");
            Socket socket2 = serverSocket.accept();
            ClientHandler client2 = new ClientHandler(socket2);
            client2.send("Connected as Player 2.");
            client2.send("Enter your name:");
            String name2 = client2.readLine();
            if (name2 == null || name2.isBlank()) name2 = "Player 2";
            client2.setPlayerName(name2);
            System.out.println(name2 + " connected.");

            runGame(client1, client2);

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // This creates the game objects and connects the two clients to the engine.
    private void runGame(ClientHandler client1, ClientHandler client2) {
        try {
            int winScore = runNetworkSetup(client1, client2);

            Game game = GameSetup.create(
                    2,
                    new boolean[]{false, false},
                    new String[]{client1.getPlayerName(), client2.getPlayerName()});

            ClientHandler[] clients = {client1, client2};

            client1.send("Both players connected. Starting game...");
            client2.send("Both players connected. Starting game...");
            client1.send("Target score: " + winScore + " points.");
            client2.send("Target score: " + winScore + " points.");

            NetworkInputHandler input    = new NetworkInputHandler(clients, game);
            NetworkGameRenderer renderer = new NetworkGameRenderer(clients, game);

            new GameEngine(game, input, renderer, winScore).run();

        } catch (Exception e) {
            client1.send("Server error: " + e.getMessage());
            client2.send("Server error: " + e.getMessage());
        } finally {
            client1.close();
            client2.close();
        }
    }

    // This runs the setup that only happens in network mode.
    // Player 1 chooses the target score, while Player 2 just waits and confirms.
    private int runNetworkSetup(ClientHandler player1, ClientHandler player2) throws IOException {
        sendSetupScreen(player1, "PLAYER 1 SETUP", "You choose the win condition for this match.");
        player1.send("SETUP:WIN_SCORE");

        sendSetupScreen(player2, "PLAYER 2 READY", "Press Enter to continue while Player 1 sets up.");
        player2.send("SETUP:START_ACK");

        // Player 2 only needs to press Enter so both clients stay in sync.
        if (player2.readLine() == null) throw new IOException("Player 2 disconnected during setup.");

        // Player 1 keeps entering a score until it is in the allowed range.
        int winScore = DEFAULT_WIN_SCORE;
        while (true) {
            String line = player1.readLine();
            if (line == null) throw new IOException("Player 1 disconnected during setup.");
            try {
                int chosen = Integer.parseInt(line.trim());
                if (chosen >= 5 && chosen <= 30) {
                    winScore = chosen;
                    break;
                }
            } catch (NumberFormatException ignored) {}
            player1.send("Invalid score. Enter a number from 5 to 30.");
            player1.send("SETUP:WIN_SCORE");
        }

        player1.send("Win condition set to " + winScore + " points.");
        player2.send("Player 1 set win condition to " + winScore + " points.");
        return winScore;
    }

    // This sends a simple setup box to one client.
    private void sendSetupScreen(ClientHandler client, String title, String body) {
        client.send("┌──────────────────────────────────────────────────────────┐");
        client.send("│ " + title);
        client.send("├──────────────────────────────────────────────────────────┤");
        client.send("│ " + body);
        client.send("└──────────────────────────────────────────────────────────┘");
    }

    // This prints the server IP addresses so other players know what to connect to.
    private void printLocalAddresses() {
        System.out.println("Connect using one of these IPs:");
        try {
            System.out.println("  localhost / 127.0.0.1 (this machine only)");
            List<NetworkInterface> ifaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : ifaces) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    String host = addr.getHostAddress();
                    if (host.contains("%")) host = host.split("%")[0];
                    if (!host.contains(":")) { // skip IPv6
                        System.out.println("  " + host + " (port " + port + ")");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("  (could not enumerate: " + e.getMessage() + ")");
        }
    }
}
