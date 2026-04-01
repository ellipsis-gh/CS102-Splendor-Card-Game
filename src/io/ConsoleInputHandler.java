package io;

import java.util.List;
import java.util.Scanner;

import model.Token;

/**
 * {@link InputHandler} implementation that reads from a local {@link Scanner}.
 *
 * <p>All parsing logic previously scattered across {@code GameApp} is
 * concentrated here. The renderer has already printed the option lists;
 * this class only shows a "Choice:" prompt and parses what the user types.</p>
 */
public class ConsoleInputHandler implements InputHandler {

    private final Scanner sc;

    public ConsoleInputHandler(Scanner sc) {
        this.sc = sc;
    }

    // -----------------------------------------------------------------------
    // Interface implementation
    // -----------------------------------------------------------------------

    @Override
    public String readMenuChoice() {
        System.out.print("Choice: ");
        String line = readLine();
        return (line == null) ? null : line.toLowerCase();
    }

    /**
     * Expects the user to type either:
     * <ul>
     *   <li>Two identical numbers for take-two-same, e.g. {@code 2 2}</li>
     *   <li>Three different numbers for take-three-different, e.g. {@code 1 2 3}</li>
     *   <li>{@code 0} to cancel.</li>
     * </ul>
     */
    @Override
    public List<Token> readGemSelection(List<Token> available) {
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");

        String line = readLine();
        if (line == null || line.equals("0")) return null;

        String[] parts = line.trim().split("\\s+");
        try {
            if (parts.length == 2 && parts[0].equals(parts[1])) {
                int idx = Integer.parseInt(parts[0]) - 1;
                if (idx < 0 || idx >= available.size()) {
                    System.out.println("Invalid number.");
                    return null;
                }
                Token t = available.get(idx);
                return List.of(t, t);

            } else if (parts.length == 3) {
                int i1 = Integer.parseInt(parts[0]) - 1;
                int i2 = Integer.parseInt(parts[1]) - 1;
                int i3 = Integer.parseInt(parts[2]) - 1;
                if (i1 < 0 || i1 >= available.size()
                        || i2 < 0 || i2 >= available.size()
                        || i3 < 0 || i3 >= available.size()) {
                    System.out.println("Invalid numbers.");
                    return null;
                }
                return List.of(available.get(i1), available.get(i2), available.get(i3));
            }
        } catch (NumberFormatException e) {
            System.out.println("Enter numbers only.");
            return null;
        }

        System.out.println("Enter 2 matching numbers or 3 different numbers.");
        return null;
    }

    @Override
    public int readCardIndex(List<String> options) {
        System.out.println("  [0] Cancel");
        System.out.print("Choice: ");
        String line = readLine();
        if (line == null) return -1;
        try {
            int n = Integer.parseInt(line.trim());
            if (n == 0) return -1;
            if (n >= 1 && n <= options.size()) return n - 1;
            System.out.println("Invalid choice.");
        } catch (NumberFormatException e) {
            System.out.println("Enter a number.");
        }
        return -1;
    }

    @Override
    public Token readTokenToReturn(List<Token> held) {
        System.out.print("Choice: ");
        String line = readLine();
        if (line == null) return null;
        try {
            int n = Integer.parseInt(line.trim()) - 1;
            if (n >= 0 && n < held.size()) return held.get(n);
            System.out.println("Invalid choice.");
        } catch (NumberFormatException e) {
            System.out.println("Enter a number.");
        }
        return null;
    }

    @Override
    public void waitForAck() {
        System.out.print("(Press Enter to continue) ");
        readLine();
    }

    // -----------------------------------------------------------------------
    // Internal helper
    // -----------------------------------------------------------------------

    private String readLine() {
        try {
            if (!sc.hasNextLine()) return null;
            return sc.nextLine().trim();
        } catch (Exception e) {
            return null;
        }
    }
}
