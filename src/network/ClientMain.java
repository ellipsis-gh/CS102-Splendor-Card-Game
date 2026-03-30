package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.fusesource.jansi.AnsiConsole;

import util.AnsiSupport;

/**
 * Network game client.
 *
 * Connects to the game server via TCP, renders the game state on screen, and
 * presents numbered menus so the player never has to type raw command strings.
 * Selections are translated to the server's text protocol before being sent.
 */
public class ClientMain {
    private static final int    SCREEN_CLEAR_LINES = 60;
    private static final String ANSI_CLEAR         = "\u001B[2J\u001B[H";

    // These are the five non-gold gem colors in display order
    private static final String[] GEM_NAMES = {"black", "blue", "green", "red", "white"};
    private static final String[] GEM_ABBR  = {"Blk", "Blu", "Grn", "Red", "Wht"};

    // Last received state text — used by the input thread to build menus
    private static volatile String lastState = "";

    private enum InputMode { NONE, NAME, TURN, RETURN }

    private static final Object MODE_LOCK = new Object();
    private static volatile InputMode mode = InputMode.NONE;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        Scanner sc = new Scanner(System.in);

        String host;
        int port = 5000;

        if (args.length >= 2) {
            try { port = Integer.parseInt(args[1].trim()); } catch (NumberFormatException ignored) { }
        }

        if (args.length >= 1 && !args[0].isBlank()) {
            host = args[0].trim();
            System.out.println("Connecting to " + host + ":" + port + "...");
        } else {
            System.out.print("Enter server IP: ");
            host = sc.nextLine().trim();
        }

        try (
            Socket socket          = new Socket(host, port);
            BufferedReader in      = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out     = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("Connected to server.");

            // ── Reader thread: receives server messages and redraws the screen ──
            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    boolean inState          = false;
                    StringBuilder stateBuffer = new StringBuilder();

                    while ((line = in.readLine()) != null) {
                        if (NetworkFormatter.STATE_BEGIN.equals(line)) {
                            inState = true;
                            stateBuffer.setLength(0);
                            continue;
                        }
                        if (NetworkFormatter.STATE_END.equals(line)) {
                            inState = false;
                            lastState = stateBuffer.toString();
                            clearScreen();
                            System.out.print(lastState);
                            if (lastState.length() > 0 && lastState.charAt(lastState.length() - 1) != '\n') {
                                System.out.println();
                            }
                            System.out.flush();
                            continue;
                        }
                        if (inState) {
                            stateBuffer.append(line).append('\n');
                        } else {
                            System.out.println(line);
                            if (line.equalsIgnoreCase("Enter your name:")) {
                                setMode(InputMode.NAME);
                            } else if (line.startsWith("--- YOUR TURN")) {
                                setMode(InputMode.TURN);
                            } else if (line.startsWith("Invalid move")) {
                                setMode(InputMode.TURN);
                            } else if (line.startsWith("TOKEN LIMIT:")) {
                                setMode(InputMode.RETURN);
                            } else if (line.startsWith("Invalid return")) {
                                setMode(InputMode.RETURN);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // ── Input thread: presents numbered menus and translates to commands ──
            boolean running = true;
            while (running) {
                InputMode current = waitForMode();
                switch (current) {
                    case NAME -> {
                        System.out.print("Name: ");
                        if (!sc.hasNextLine()) { running = false; break; }
                        String name = sc.nextLine().trim();
                        out.println(name.isBlank() ? "Player" : name);
                    }
                    case TURN -> {
                        while (true) {
                            printActionMenu();
                            if (!sc.hasNextLine()) { running = false; break; }
                            String input = sc.nextLine().trim().toLowerCase();

                            String command = translateInput(input, sc);
                            if (command == null) continue; // cancelled/invalid — reprompt locally

                            out.println(command);
                            if ("QUIT".equalsIgnoreCase(command)) running = false;
                            break;
                        }
                    }
                    case RETURN -> {
                        while (true) {
                            System.out.print("Return tokens (e.g. red 1) or Q: ");
                            if (!sc.hasNextLine()) { running = false; break; }
                            String line = sc.nextLine().trim();
                            if (line.equalsIgnoreCase("q")) {
                                out.println("QUIT");
                                running = false;
                                break;
                            }
                            String[] parts = line.split("\\s+");
                            if (parts.length != 2) {
                                System.out.println("Enter: <color> <count>");
                                continue;
                            }
                            out.println("RETURN " + parts[0] + " " + parts[1]);
                            break;
                        }
                    }
                    case NONE -> running = false;
                }
            }

            try { readerThread.join(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        } catch (IOException e) {
            System.out.println("Could not connect: " + e.getMessage());
        }

        sc.close();
    }

    // -------------------------------------------------------------------------
    // Numbered menus → server command strings
    // -------------------------------------------------------------------------

    private static void printActionMenu() {
        System.out.println("┌─ YOUR MOVE ──────────────────────────────────────┐");
        System.out.println("│  [1]  Take gems                                  │");
        System.out.println("│  [2]  Buy a card                                 │");
        System.out.println("│  [3]  Reserve a card                             │");
        System.out.println("│  [Q]  Quit                                       │");
        System.out.println("└──────────────────────────────────────────────────┘");
        System.out.print("Choice: ");
    }

    /**
     * Translates a top-level menu choice into a server command string.
     * Returns null if the player cancelled or entered something invalid.
     */
    private static String translateInput(String input, Scanner sc) {
        return switch (input) {
            case "1"    -> translateTakeGems(sc);
            case "2"    -> translateBuyCard(sc);
            case "3"    -> translateReserve(sc);
            case "q"    -> "QUIT";
            default     -> { System.out.println("Enter 1, 2, 3, or Q."); yield null; }
        };
    }

    // ── Take gems ────────────────────────────────────────────────────────────

    private static String translateTakeGems(Scanner sc) {
        // Extract available gem counts from the last printed state
        List<String> available = parseAvailableGems(lastState);

        if (available.isEmpty()) {
            System.out.println("No gem info available. Type command directly: TAKE3 <c> <c> <c> or TAKE2 <c>");
            System.out.print("Command: ");
            return sc.hasNextLine() ? sc.nextLine().trim() : null;
        }

        System.out.println();
        System.out.println("Available gem colors:");
        for (int i = 0; i < available.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + available.get(i));
        }
        System.out.println();
        System.out.println("  Take 3 different → enter 3 numbers  e.g.  1 2 3");
        System.out.println("  Take 2 same      → enter same number twice  e.g.  2 2");
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");

        if (!sc.hasNextLine()) return null;
        String line  = sc.nextLine().trim();
        if (line.equals("0")) return null;

        String[] parts = line.split("\\s+");
        try {
            if (parts.length == 2 && parts[0].equals(parts[1])) {
                int idx = Integer.parseInt(parts[0]) - 1;
                if (idx < 0 || idx >= available.size()) { System.out.println("Invalid."); return null; }
                String color = gemColorName(available.get(idx));
                return "TAKE2 " + color;

            } else if (parts.length == 3) {
                int i1 = Integer.parseInt(parts[0]) - 1;
                int i2 = Integer.parseInt(parts[1]) - 1;
                int i3 = Integer.parseInt(parts[2]) - 1;
                if (i1 < 0 || i1 >= available.size() ||
                    i2 < 0 || i2 >= available.size() ||
                    i3 < 0 || i3 >= available.size()) { System.out.println("Invalid."); return null; }
                return "TAKE3 " + gemColorName(available.get(i1))
                        + " " + gemColorName(available.get(i2))
                        + " " + gemColorName(available.get(i3));

            } else {
                System.out.println("Enter 2 numbers (same) or 3 different numbers.");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Enter valid numbers.");
            return null;
        }
    }

    // ── Buy card ─────────────────────────────────────────────────────────────

    private static String translateBuyCard(Scanner sc) {
        List<String> slots = parseCardSlots(lastState);

        System.out.println();
        System.out.println("Buy which card?");

        if (slots.isEmpty()) {
            System.out.println("  (No card info parsed — enter slot directly e.g. 1-0 or r0)");
        } else {
            for (int i = 0; i < slots.size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + slots.get(i));
            }
        }
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");

        if (!sc.hasNextLine()) return null;
        String line = sc.nextLine().trim();
        if (line.equals("0")) return null;

        if (slots.isEmpty()) {
            // fallback: the user types a raw slot like "1-0" or "r0"
            return line.startsWith("r") ? "BUYR " + line.replace("r", "").replace("-", "").trim()
                                        : "BUY " + line;
        }

        try {
            int choice = Integer.parseInt(line) - 1;
            if (choice < 0 || choice >= slots.size()) { System.out.println("Invalid."); return null; }
            String slot = slots.get(choice);
            if (slot.startsWith("r-")) {
                return "BUYR " + slot.substring(2);
            } else {
                return "BUY " + slot.split(" ")[0]; // e.g. "1-0 ..."
            }
        } catch (NumberFormatException e) {
            System.out.println("Enter a number.");
            return null;
        }
    }

    // ── Reserve card ─────────────────────────────────────────────────────────

    private static String translateReserve(Scanner sc) {
        List<String> slots = parseCardSlots(lastState);

        System.out.println();
        System.out.println("Reserve which card?");

        if (slots.isEmpty()) {
            System.out.println("  (No card info parsed — enter slot directly e.g. 1-0 or DECK 2)");
        } else {
            for (int i = 0; i < slots.size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + slots.get(i));
            }
            // offer deck options
            System.out.println("  [D1] Top of Deck 1  [D2] Top of Deck 2  [D3] Top of Deck 3");
        }
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");

        if (!sc.hasNextLine()) return null;
        String line = sc.nextLine().trim().toLowerCase();
        if (line.equals("0")) return null;

        if (line.startsWith("d") && line.length() == 2) {
            try {
                int level = Integer.parseInt(line.substring(1));
                return "RESERVEDECK " + level;
            } catch (NumberFormatException e) {
                System.out.println("Invalid deck choice.");
                return null;
            }
        }

        if (slots.isEmpty()) {
            return line.startsWith("deck") ? "RESERVEDECK " + line.replace("deck", "").trim()
                                           : "RESERVE " + line;
        }

        try {
            int choice = Integer.parseInt(line) - 1;
            if (choice < 0 || choice >= slots.size()) { System.out.println("Invalid."); return null; }
            String slot = slots.get(choice).split(" ")[0]; // e.g. "2-1"
            return "RESERVE " + slot;
        } catch (NumberFormatException e) {
            System.out.println("Enter a number or D1/D2/D3.");
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // State parsing helpers
    // -------------------------------------------------------------------------

    /**
     * Scans the last received state string for the bank line and returns
     * a list of "COLOR (N on board)" entries for colors with count > 0.
     */
    private static List<String> parseAvailableGems(String state) {
        List<String> result = new ArrayList<>();
        for (String line : state.split("\n")) {
            String clean = stripAnsi(line);
            if (!clean.startsWith("Bank:")) continue;
            // Bank line looks like: "Bank:   Blk:3  Blu:4  Grn:2  ..."
            for (int i = 0; i < GEM_NAMES.length; i++) {
                String gem  = GEM_NAMES[i];
                String abbr = GEM_ABBR[i];
                // match e.g. "Blk:3"
                int idx = clean.indexOf(abbr + ":");
                if (idx < 0) continue;
                try {
                    int start = idx + abbr.length() + 1;
                    int end   = clean.indexOf(' ', start);
                    String ns = end < 0 ? clean.substring(start)
                                        : clean.substring(start, end);
                    int count = Integer.parseInt(ns.trim());
                    if (count > 0) result.add(gem + " (" + count + " on board)");
                } catch (NumberFormatException ignored) { }
            }
            break;
        }
        return result;
    }

    /**
     * Scans the last state string for card slot identifiers like "[1-0]", "[2-3]" etc.
     * Returns them as a list in the order they appear on screen.
     */
    private static List<String> parseCardSlots(String state) {
        List<String> slots = new ArrayList<>();
        for (String line : state.split("\n")) {
            // Card boxes show the slot id at the start of the header row, e.g. "│ [1-0] ..."
            int start = line.indexOf('[');
            int end   = line.indexOf(']', start + 1);
            if (start < 0 || end < 0) continue;
            String tag = line.substring(start + 1, end);
            // matches "1-0", "2-3", etc.  Skip noble tags "[N0]"
            if (tag.matches("\\d-\\d") && !slots.contains(tag)) {
                slots.add(tag);
            }
        }
        return slots;
    }

    /** Extracts the bare color name from an entry like "green (4 on board)". */
    private static String gemColorName(String entry) {
        int space = entry.indexOf(' ');
        return space < 0 ? entry : entry.substring(0, space);
    }

    // -------------------------------------------------------------------------
    // Screen helpers
    // -------------------------------------------------------------------------

    private static void clearScreen() {
        if (AnsiSupport.isSupported()) {
            System.out.print(ANSI_CLEAR);
            return;
        }
        for (int i = 0; i < SCREEN_CLEAR_LINES; i++) System.out.println();
    }

    private static void setMode(InputMode newMode) {
        synchronized (MODE_LOCK) {
            mode = newMode;
            MODE_LOCK.notifyAll();
        }
    }

    private static InputMode waitForMode() {
        synchronized (MODE_LOCK) {
            while (mode == InputMode.NONE) {
                try {
                    MODE_LOCK.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return InputMode.NONE;
                }
            }
            InputMode current = mode;
            mode = InputMode.NONE;
            return current;
        }
    }

    private static String stripAnsi(String s) {
        return s == null ? "" : s.replaceAll("\u001B\\[[^m]*m", "");
    }
}
