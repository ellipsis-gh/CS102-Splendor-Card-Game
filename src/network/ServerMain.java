package network;

import util.SplashScreen;

// the start file for the server side of the game.
// It reads the port number, shows the server splash screen,
// and starts the GameServer so players can connect.

public final class ServerMain {

    // This is just a utility class, so we do not create ServerMain objects.
    private ServerMain() {}

    // This method starts the server program on port 5000.
    public static void main(String[] args) {
        int port = 5000;

        // Show the server splash screen before opening the socket.
        SplashScreen.server(2, port);
        new GameServer(port).start();
    }
}
