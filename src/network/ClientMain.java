package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


// it asks for the server IP and port
// it connects to the game server
// it listens for messages from the server and prints them
// it reads what the player types
// it sends those typed commands to the server
// it stops when the player types QUIT or the connection closes

public class ClientMain {
    private static final int SCREEN_CLEAR_LINES = 60;
    private static final String ANSI_CLEAR = "\u001B[2J\u001B[H";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String host;
        int port = 5000;

        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1].trim());
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        if (args.length >= 1 && !args[0].isBlank()) {
            host = args[0].trim();
            System.out.println("Connecting to " + host + ":" + port + "...");
        } else {
            System.out.print("Enter server IP: ");
            host = sc.nextLine().trim();
        }

        try (
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("Connected to server.");

            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    boolean inState = false;
                    StringBuilder stateBuffer = new StringBuilder();

                    while ((line = in.readLine()) != null) {
                        if (NetworkFormatter.STATE_BEGIN.equals(line)) {
                            inState = true;
                            stateBuffer.setLength(0);
                            continue;
                        }

                        if (NetworkFormatter.STATE_END.equals(line)) {
                            inState = false;
                            clearScreen();
                            System.out.print(stateBuffer);
                            if (stateBuffer.length() > 0 && stateBuffer.charAt(stateBuffer.length() - 1) != '\n') {
                                System.out.println();
                            }
                            System.out.flush();
                            continue;
                        }

                        if (inState) {
                            stateBuffer.append(line).append('\n');
                        } else {
                            System.out.println(line);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });

            readerThread.setDaemon(true);
            readerThread.start();

            while (sc.hasNextLine()) {
                String input = sc.nextLine();
                out.println(input);

                if ("QUIT".equalsIgnoreCase(input.trim())) {
                    return;
                }
            }

            // If stdin closes (e.g., piped input), keep listening so state updates are still shown.
            try {
                readerThread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

        } catch (IOException e) {
            System.out.println("Could not connect: " + e.getMessage());
        }

        sc.close();
    }

    private static void clearScreen() {
        if (supportsAnsi()) {
            System.out.print(ANSI_CLEAR);
            return;
        }
        for (int i = 0; i < SCREEN_CLEAR_LINES; i++) {
            System.out.println();
        }
    }

    private static boolean supportsAnsi() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return System.getenv("WT_SESSION") != null
                    || System.getenv("ANSICON") != null
                    || "ON".equalsIgnoreCase(System.getenv("ConEmuANSI"))
                    || System.getenv("TERM") != null;
        }
        String term = System.getenv("TERM");
        return term != null && !term.equalsIgnoreCase("dumb");
    }
}
