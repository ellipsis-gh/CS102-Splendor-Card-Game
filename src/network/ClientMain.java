package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.fusesource.jansi.AnsiConsole;

import util.AnsiSupport;

/**
 * Network client for the multiplayer game.
 *
 * This version keeps the helpful numbered menus, but the control flow is kept
 * simple:
 * 1. a reader thread listens to the server
 * 2. the main loop checks what kind of input is needed
 * 3. the player's menu choice is translated into a server command string
 */
public class ClientMain {
    private static final int SCREEN_CLEAR_LINES = 60;
    private static final String ANSI_CLEAR = "\u001B[2J\u001B[H";

    private static final String[] GEM_NAMES = {"black", "blue", "green", "red", "white"};
    private static final String[] GEM_ABBR = {"Blk", "Blu", "Grn", "Red", "Wht"};

    private enum InputMode {
        NONE,
        NAME,
        TURN,
        RETURN
    }

    private static volatile String lastState = "";
    private static volatile List<String> lastBuyableSlots = new ArrayList<>();
    private static volatile InputMode currentMode = InputMode.NONE;
    private static volatile boolean connected = true;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        Scanner sc = new Scanner(System.in);

        String host = getHost(args, sc);
        int port = getPort(args);

        try (
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("Connected to server.");

            Thread readerThread = new Thread(() -> readServerMessages(in));
            readerThread.setDaemon(true);
            readerThread.start();

            while (connected) {
                if (currentMode == InputMode.NONE) {
                    pauseBriefly();
                    continue;
                }

                if (currentMode == InputMode.NAME) {
                    handleNameInput(sc, out);
                } else if (currentMode == InputMode.TURN) {
                    handleTurnInput(sc, out);
                } else if (currentMode == InputMode.RETURN) {
                    handleReturnInput(sc, out);
                }
            }

        } catch (IOException e) {
            System.out.println("Could not connect: " + e.getMessage());
        }

        sc.close();
    }

    private static String getHost(String[] args, Scanner sc) {
        if (args.length >= 1 && !args[0].isBlank()) {
            String host = args[0].trim();
            System.out.println("Connecting to " + host + ":" + getPort(args) + "...");
            return host;
        }

        System.out.print("Enter server IP: ");
        return sc.nextLine().trim();
    }

    private static int getPort(String[] args) {
        int port = 5080;
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return port;
    }

    private static void readServerMessages(BufferedReader in) {
        try {
            String line;
            boolean readingState = false;
            boolean readingBuyable = false;
            StringBuilder stateBuffer = new StringBuilder();
            List<String> buyableBuffer = new ArrayList<>();

            while ((line = in.readLine()) != null) {
                if (NetworkFormatter.STATE_BEGIN.equals(line)) {
                    readingState = true;
                    stateBuffer.setLength(0);
                    continue;
                }

                if (NetworkFormatter.STATE_END.equals(line)) {
                    readingState = false;
                    lastState = stateBuffer.toString();
                    clearScreen();
                    System.out.print(lastState);
                    if (!lastState.endsWith("\n")) {
                        System.out.println();
                    }
                    continue;
                }

                if (NetworkFormatter.BUYABLE_BEGIN.equals(line)) {
                    readingBuyable = true;
                    buyableBuffer.clear();
                    continue;
                }

                if (NetworkFormatter.BUYABLE_END.equals(line)) {
                    readingBuyable = false;
                    lastBuyableSlots = new ArrayList<>(buyableBuffer);
                    continue;
                }

                if (readingState) {
                    stateBuffer.append(line).append('\n');
                    continue;
                }

                if (readingBuyable) {
                    String clean = stripAnsi(line).trim();
                    if (!clean.isEmpty()) {
                        buyableBuffer.add(clean);
                    }
                    continue;
                }

                System.out.println(line);
                updateModeFromServerMessage(line);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        } finally {
            connected = false;
        }
    }

    private static void updateModeFromServerMessage(String line) {
        if (line.equalsIgnoreCase("Enter your name:")) {
            currentMode = InputMode.NAME;
        } else if (line.startsWith("--- YOUR TURN")) {
            currentMode = InputMode.TURN;
        } else if (line.startsWith("Invalid move")) {
            currentMode = InputMode.TURN;
        } else if (line.startsWith("TOKEN LIMIT:")) {
            currentMode = InputMode.RETURN;
        } else if (line.startsWith("Invalid return")) {
            currentMode = InputMode.RETURN;
        }
    }

    private static void handleNameInput(Scanner sc, PrintWriter out) {
        System.out.print("Name: ");
        if (!sc.hasNextLine()) {
            connected = false;
            return;
        }

        String name = sc.nextLine().trim();
        out.println(name.isBlank() ? "Player" : name);
        currentMode = InputMode.NONE;
    }

    private static void handleTurnInput(Scanner sc, PrintWriter out) {
        while (connected) {
            printActionMenu();
            if (!sc.hasNextLine()) {
                connected = false;
                return;
            }

            String input = sc.nextLine().trim().toLowerCase();
            String command = translateTopLevelChoice(input, sc);
            if (command == null) {
                continue;
            }

            out.println(command);
            currentMode = InputMode.NONE;

            if ("QUIT".equalsIgnoreCase(command)) {
                connected = false;
            }
            return;
        }
    }

    private static void handleReturnInput(Scanner sc, PrintWriter out) {
        while (connected) {
            System.out.print("Return tokens (example: red 1) or Q: ");
            if (!sc.hasNextLine()) {
                connected = false;
                return;
            }

            String input = sc.nextLine().trim();
            if (input.equalsIgnoreCase("q")) {
                out.println("QUIT");
                connected = false;
                return;
            }

            String[] parts = input.split("\\s+");
            if (parts.length != 2) {
                System.out.println("Enter: <color> <count>");
                continue;
            }

            out.println("RETURN " + parts[0] + " " + parts[1]);
            currentMode = InputMode.NONE;
            return;
        }
    }

    private static void printActionMenu() {
        System.out.println("Choose your move:");
        System.out.println("  [1] Take gems");
        System.out.println("  [2] Buy a card");
        System.out.println("  [3] Reserve a card");
        System.out.println("  [Q] Quit");
        System.out.print("Choice: ");
    }

    private static String translateTopLevelChoice(String input, Scanner sc) {
        if (input.equals("1")) {
            return translateTakeGems(sc);
        }
        if (input.equals("2")) {
            return translateBuyCard(sc);
        }
        if (input.equals("3")) {
            return translateReserveCard(sc);
        }
        if (input.equals("q")) {
            return "QUIT";
        }

        System.out.println("Enter 1, 2, 3, or Q.");
        return null;
    }

    private static String translateTakeGems(Scanner sc) {
        List<String> availableGems = parseAvailableGems(lastState);

        if (availableGems.isEmpty()) {
            System.out.print("Type command directly (TAKE3 ... or TAKE2 ...): ");
            return sc.hasNextLine() ? sc.nextLine().trim() : null;
        }

        System.out.println("Available gem colors:");
        for (int i = 0; i < availableGems.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + availableGems.get(i));
        }
        System.out.println("Take 3 different: enter 3 numbers, example: 1 2 3");
        System.out.println("Take 2 same: enter the same number twice, example: 2 2");
        System.out.println("[0] Cancel");
        System.out.print("Choice: ");

        if (!sc.hasNextLine()) {
            return null;
        }

        String line = sc.nextLine().trim();
        if (line.equals("0")) {
            return null;
        }

        String[] parts = line.split("\\s+");
        try {
            if (parts.length == 2 && parts[0].equals(parts[1])) {
                int index = Integer.parseInt(parts[0]) - 1;
                if (!isValidIndex(index, availableGems.size())) {
                    System.out.println("Invalid choice.");
                    return null;
                }
                return "TAKE2 " + getGemColorName(availableGems.get(index));
            }

            if (parts.length == 3) {
                int first = Integer.parseInt(parts[0]) - 1;
                int second = Integer.parseInt(parts[1]) - 1;
                int third = Integer.parseInt(parts[2]) - 1;

                if (!isValidIndex(first, availableGems.size())
                        || !isValidIndex(second, availableGems.size())
                        || !isValidIndex(third, availableGems.size())) {
                    System.out.println("Invalid choice.");
                    return null;
                }

                return "TAKE3 "
                        + getGemColorName(availableGems.get(first)) + " "
                        + getGemColorName(availableGems.get(second)) + " "
                        + getGemColorName(availableGems.get(third));
            }
        } catch (NumberFormatException e) {
            System.out.println("Enter numbers only.");
            return null;
        }

        System.out.println("Enter either 2 matching numbers or 3 numbers.");
        return null;
    }

    private static String translateBuyCard(Scanner sc) {
        List<String> slots = new ArrayList<>(lastBuyableSlots);

        System.out.println("Buy which card?");
        if (slots.isEmpty()) {
            System.out.println("No affordable cards were listed by the server.");
            System.out.print("Type a slot directly (example: 1-0 or r0), or 0 to cancel: ");
            if (!sc.hasNextLine()) {
                return null;
            }

            String line = sc.nextLine().trim();
            if (line.equals("0")) {
                return null;
            }

            if (line.startsWith("r")) {
                return "BUYR " + line.replace("r", "").replace("-", "").trim();
            }
            return "BUY " + line;
        }

        for (int i = 0; i < slots.size(); i++) {
            String slot = slots.get(i);
            String label = slot.startsWith("r-") ? slot + " (reserved)" : slot;
            System.out.println("  [" + (i + 1) + "] " + label);
        }
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");

        if (!sc.hasNextLine()) {
            return null;
        }

        String line = sc.nextLine().trim();
        if (line.equals("0")) {
            return null;
        }

        try {
            int choice = Integer.parseInt(line) - 1;
            if (!isValidIndex(choice, slots.size())) {
                System.out.println("Invalid choice.");
                return null;
            }

            String slot = slots.get(choice);
            if (slot.startsWith("r-")) {
                return "BUYR " + slot.substring(2);
            }
            return "BUY " + slot;
        } catch (NumberFormatException e) {
            System.out.println("Enter a number.");
            return null;
        }
    }

    private static String translateReserveCard(Scanner sc) {
        List<String> slots = parseVisibleCardSlots(lastState);

        System.out.println("Reserve which card?");
        for (int i = 0; i < slots.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + slots.get(i));
        }
        System.out.println("  [D1] Reserve top card from deck 1");
        System.out.println("  [D2] Reserve top card from deck 2");
        System.out.println("  [D3] Reserve top card from deck 3");
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");

        if (!sc.hasNextLine()) {
            return null;
        }

        String line = sc.nextLine().trim().toLowerCase();
        if (line.equals("0")) {
            return null;
        }

        if (line.equals("d1") || line.equals("d2") || line.equals("d3")) {
            return "RESERVEDECK " + line.substring(1);
        }

        try {
            int choice = Integer.parseInt(line) - 1;
            if (!isValidIndex(choice, slots.size())) {
                System.out.println("Invalid choice.");
                return null;
            }
            return "RESERVE " + slots.get(choice);
        } catch (NumberFormatException e) {
            System.out.println("Enter a number or D1/D2/D3.");
            return null;
        }
    }

    private static List<String> parseAvailableGems(String state) {
        List<String> result = new ArrayList<>();

        for (String line : state.split("\n")) {
            String clean = stripAnsi(line);
            if (!clean.startsWith("Bank:")) {
                continue;
            }

            for (int i = 0; i < GEM_NAMES.length; i++) {
                String shortName = GEM_ABBR[i];
                int index = clean.indexOf(shortName + ":");
                if (index < 0) {
                    continue;
                }

                try {
                    int start = index + shortName.length() + 1;
                    int end = clean.indexOf(' ', start);
                    String numberText = (end < 0) ? clean.substring(start) : clean.substring(start, end);
                    int count = Integer.parseInt(numberText.trim());
                    if (count > 0) {
                        result.add(GEM_NAMES[i] + " (" + count + " on board)");
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            break;
        }

        return result;
    }

    private static List<String> parseVisibleCardSlots(String state) {
        Set<String> slots = new LinkedHashSet<>();

        for (String line : state.split("\n")) {
            String clean = stripAnsi(line);
            int start = 0;

            while ((start = clean.indexOf('[', start)) >= 0) {
                int end = clean.indexOf(']', start + 1);
                if (end < 0) {
                    break;
                }

                String tag = clean.substring(start + 1, end).trim();
                if (tag.matches("\\d-\\d")) {
                    slots.add(tag);
                }
                start = end + 1;
            }
        }

        return new ArrayList<>(slots);
    }

    private static boolean isValidIndex(int index, int size) {
        return index >= 0 && index < size;
    }

    private static String getGemColorName(String entry) {
        int space = entry.indexOf(' ');
        return (space < 0) ? entry : entry.substring(0, space);
    }

    private static void clearScreen() {
        if (AnsiSupport.isSupported()) {
            System.out.print(ANSI_CLEAR);
            return;
        }

        for (int i = 0; i < SCREEN_CLEAR_LINES; i++) {
            System.out.println();
        }
    }

    private static void pauseBriefly() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            connected = false;
        }
    }

    private static String stripAnsi(String text) {
        return text == null ? "" : text.replaceAll("\u001B\\[[^m]*m", "");
    }
}
