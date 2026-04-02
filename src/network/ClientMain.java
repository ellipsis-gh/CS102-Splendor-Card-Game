package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import org.fusesource.jansi.AnsiConsole;

import io.NetworkInputHandler;
import util.AnsiSupport;
import util.SplashScreen;


// AI assisted
// the file shows whatever the server sends and sends back what the player type.
public class ClientMain {

    private static final String ANSI_CLEAR        = "\u001B[2J\u001B[H";
    private static final int    SCREEN_CLEAR_LINES = 60;

    private enum InputMode { NONE, NAME, START_ACK, WIN_SCORE, ACTIVE }

    private static volatile InputMode currentMode = InputMode.NONE;
    private static volatile boolean   connected   = true;

    // start the client, and connect to the server,
    // it keeps looping until the connection ends.
    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        Scanner sc = new Scanner(System.in);

        SplashScreen.client();
        System.out.print("Press Enter to continue...");
        if (sc.hasNextLine()) sc.nextLine();

        String host = getHost(args, sc);
        int    port = getPort(args);

        try (
            Socket         socket = new Socket(host, port);
            BufferedReader in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out    = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("Connected to server.");

            Thread reader = new Thread(() -> readServerMessages(in));
            reader.setDaemon(true);
            reader.start();

            while (connected) {
                switch (currentMode) {
                    case NONE:
                        pauseBriefly();
                        break;
                    case NAME:
                        handleNameInput(sc, out);
                        break;
                    case START_ACK:
                        handleStartAckInput(sc, out);
                        break;
                    case WIN_SCORE:
                        handleWinScoreInput(sc, out);
                        break;
                    case ACTIVE:
                        // When the server asks for input, send one line back.
                        if (!sc.hasNextLine()) {
                            connected = false;
                            break;
                        }
                        out.println(sc.nextLine());
                        currentMode = InputMode.NONE;
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("Could not connect: " + e.getMessage());
        }

        sc.close();
    }

    // it keeps reading messages from the server in the background.
    // It also handles full-screen redraw blocks and input prompts.
    private static void readServerMessages(BufferedReader in) {
        try {
            String        line;
            boolean       readingState = false;
            StringBuilder stateBuffer  = new StringBuilder();

            while ((line = in.readLine()) != null) {

                // Ignore these old markers if they ever appear.
                if (NetworkFormatter.BUYABLE_BEGIN.equals(line)
                        || NetworkFormatter.BUYABLE_END.equals(line)) continue;

                // Collect the full board screen before printing it all at once.
                if (NetworkFormatter.STATE_BEGIN.equals(line)) {
                    readingState = true;
                    stateBuffer.setLength(0);
                    continue;
                }
                if (NetworkFormatter.STATE_END.equals(line)) {
                    readingState = false;
                    clearScreen();
                    System.out.print(stateBuffer);
                    if (!stateBuffer.toString().endsWith("\n")) System.out.println();
                    continue;
                }
                if (readingState) {
                    stateBuffer.append(line).append('\n');
                    continue;
                }

                // The server is waiting, so let the main loop accept player input.
                if (NetworkInputHandler.INPUT_NEEDED.equals(line)) {
                    currentMode = InputMode.ACTIVE;
                    continue;
                }

                // Print normal messages and see if the setup mode should change.
                System.out.println(line);
                updateMode(line);
            }

        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        } finally {
            connected = false;
        }
    }

    //  change the client input mode based on setup messages from the server.
    private static void updateMode(String line) {
        if (line.equalsIgnoreCase("Enter your name:")) {
            currentMode = InputMode.NAME;
        } else if (line.equalsIgnoreCase("SETUP:START_ACK")) {
            currentMode = InputMode.START_ACK;
        } else if (line.equalsIgnoreCase("SETUP:WIN_SCORE")) {
            currentMode = InputMode.WIN_SCORE;
        }
    }

    //  asks the player for their name and sends it to the server.
    private static void handleNameInput(Scanner sc, PrintWriter out) {
        System.out.print("Name: ");
        if (!sc.hasNextLine()) { connected = false; return; }
        String name = sc.nextLine().trim();
        out.println(name.isBlank() ? "Player" : name);
        currentMode = InputMode.NONE;
    }

    // wait for the player to press Enter during setup.
    private static void handleStartAckInput(Scanner sc, PrintWriter out) {
        System.out.print("Press Enter to continue...");
        if (!sc.hasNextLine()) { connected = false; return; }
        sc.nextLine();
        out.println("READY");
        currentMode = InputMode.NONE;
    }

    //  lets Player 1 type in the target score for the match.
    private static void handleWinScoreInput(Scanner sc, PrintWriter out) {
        System.out.println("┌───────────────────────────────┐");
        System.out.println("│ Player 1 Setup                │");
        System.out.println("├───────────────────────────────┤");
        System.out.println("│ Select points needed to win.  │");
        System.out.println("│ Allowed range: 5 to 30        │");
        System.out.println("└───────────────────────────────┘");
        System.out.print("Choice (5-30, Enter for default 15): ");
        if (!sc.hasNextLine()) 
            { connected = false; return; }
        String line = sc.nextLine().trim();
        out.println(line.isEmpty() ? "15" : line);
        currentMode = InputMode.NONE;
    }

    // gets the server IP either from the command line or by asking the user.
    private static String getHost(String[] args, Scanner sc) {
        if (args.length >= 1 && !args[0].isBlank()) {
            System.out.println("Connecting to " + args[0].trim() + ":" + getPort(args) + "...");
            return args[0].trim();
        }
        System.out.print("Enter server IP: ");
        return sc.nextLine().trim();
    }

    // use default port 5000 for the client connection.
    private static int getPort(String[] args) {
        return 5000;
    }

    //  clears the terminal before printing a new board view.
    private static void clearScreen() {
        if (AnsiSupport.isSupported()) {
            System.out.print(ANSI_CLEAR);
        } else {
            for (int i = 0; i < SCREEN_CLEAR_LINES; i++) System.out.println();
        }
    }

    //  briefly sleeps(like a timer) so the loop does not run too fast while waiting.
    private static void pauseBriefly() {
        try { Thread.sleep(100); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            connected = false;
        }
    }
}
