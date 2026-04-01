package network;

import util.SplashScreen;

// it reads the port number from the command-line arguments, or uses 5000 by default
// it creates a GameServer
// it calls start() to begin listening for player connections

public final class ServerMain {

    private ServerMain() {}

    public static void main(String[] args) {
        int port = 5000;
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {
            }
        }

        // Show a server-themed ASCII splash before opening the socket.
        SplashScreen.server(2, port);
        new GameServer(port).start();
    }
}

