package network;

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

        new GameServer(port).start();
    }
}

