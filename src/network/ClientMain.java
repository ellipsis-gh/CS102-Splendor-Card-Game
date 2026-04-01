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

// ai generated
/**
 * Network client — thin terminal.
 *
 * <p>The client has two jobs:</p>
 * <ol>
 *   <li><b>Reader thread</b> — displays everything the server sends, buffering
 *       {@code <<STATE_BEGIN>>} … {@code <<STATE_END>>} blocks for atomic
 *       screen redraws.</li>
 *   <li><b>Main loop</b> — forwards one line of user input whenever the server
 *       sends {@link NetworkInputHandler#INPUT_NEEDED}.</li>
 * </ol>
 *
 * <p>All menus, prompts, and validation now live on the server side inside
 * {@link io.NetworkInputHandler} and {@link ui.NetworkGameRenderer}.
 * The client is intentionally dumb — it only displays and forwards.</p>
 */
public class ClientMain {

    private static final String ANSI_CLEAR        = "\u001B[2J\u001B[H";
    private static final int    SCREEN_CLEAR_LINES = 60;

    private enum InputMode { NONE, NAME, START_ACK, WIN_SCORE, ACTIVE }

    private static volatile InputMode currentMode = InputMode.NONE;
    private static volatile boolean   connected   = true;

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

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
                    case NONE      -> pauseBriefly();
                    case NAME      -> handleNameInput(sc, out);
                    case START_ACK -> handleStartAckInput(sc, out);
                    case WIN_SCORE -> handleWinScoreInput(sc, out);
                    case ACTIVE    -> {
                        // Server is waiting — forward exactly one line
                        if (!sc.hasNextLine()) { connected = false; break; }
                        out.println(sc.nextLine());
                        currentMode = InputMode.NONE;
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Could not connect: " + e.getMessage());
        }

        sc.close();
    }

    // -----------------------------------------------------------------------
    // Server reader thread
    // -----------------------------------------------------------------------

    private static void readServerMessages(BufferedReader in) {
        try {
            String        line;
            boolean       readingState = false;
            StringBuilder stateBuffer  = new StringBuilder();

            while ((line = in.readLine()) != null) {

                // Silently absorb legacy BUYABLE markers (no longer sent by server)
                if (NetworkFormatter.BUYABLE_BEGIN.equals(line)
                        || NetworkFormatter.BUYABLE_END.equals(line)) continue;

                // Buffer state blocks for atomic screen redraws
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

                // INPUT_NEEDED: server is blocking for user input — activate main loop
                if (NetworkInputHandler.INPUT_NEEDED.equals(line)) {
                    currentMode = InputMode.ACTIVE;
                    continue;
                }

                // Print all other lines and check for setup mode-switches
                System.out.println(line);
                updateMode(line);
            }

        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        } finally {
            connected = false;
        }
    }

    /** Handles setup-phase mode changes detected in server messages. */
    private static void updateMode(String line) {
        if (line.equalsIgnoreCase("Enter your name:")) {
            currentMode = InputMode.NAME;
        } else if (line.equalsIgnoreCase("SETUP:START_ACK")) {
            currentMode = InputMode.START_ACK;
        } else if (line.equalsIgnoreCase("SETUP:WIN_SCORE")) {
            currentMode = InputMode.WIN_SCORE;
        }
    }

    // -----------------------------------------------------------------------
    // Setup input handlers
    // -----------------------------------------------------------------------

    private static void handleNameInput(Scanner sc, PrintWriter out) {
        System.out.print("Name: ");
        if (!sc.hasNextLine()) { connected = false; return; }
        String name = sc.nextLine().trim();
        out.println(name.isBlank() ? "Player" : name);
        currentMode = InputMode.NONE;
    }

    private static void handleStartAckInput(Scanner sc, PrintWriter out) {
        System.out.print("Press Enter to continue...");
        if (!sc.hasNextLine()) { connected = false; return; }
        sc.nextLine();
        out.println("READY");
        currentMode = InputMode.NONE;
    }

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

    // -----------------------------------------------------------------------
    // Connection helpers
    // -----------------------------------------------------------------------

    private static String getHost(String[] args, Scanner sc) {
        if (args.length >= 1 && !args[0].isBlank()) {
            System.out.println("Connecting to " + args[0].trim() + ":" + getPort(args) + "...");
            return args[0].trim();
        }
        System.out.print("Enter server IP: ");
        return sc.nextLine().trim();
    }

    private static int getPort(String[] args) {
        if (args.length >= 2) {
            try { return Integer.parseInt(args[1].trim()); }
            catch (NumberFormatException ignored) {}
        }
        return 5000;
    }

    // -----------------------------------------------------------------------
    // Display utilities
    // -----------------------------------------------------------------------

    private static void clearScreen() {
        if (AnsiSupport.isSupported()) {
            System.out.print(ANSI_CLEAR);
        } else {
            for (int i = 0; i < SCREEN_CLEAR_LINES; i++) System.out.println();
        }
    }

    private static void pauseBriefly() {
        try { Thread.sleep(100); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            connected = false;
        }
    }
}
