package ui;

import java.util.List;

import logic.Game;
import model.Noble;
import model.Player;
import model.Token;
import util.AnsiSupport;
import util.ui.ConsoleRenderer;

/**
 * {@link GameRenderer} implementation that writes to stdout.
 *
 * <p>Wraps the existing {@link ConsoleRenderer} components — no new layout
 * logic here. In local mode, {@code renderMessage} and {@code renderBroadcast}
 * are identical (both print to the same terminal).</p>
 */
public class ConsoleGameRenderer implements GameRenderer {

    private static final String ANSI_CLEAR = "\u001B[2J\u001B[H";

    private final boolean ansi;

    public ConsoleGameRenderer() {
        this.ansi = AnsiSupport.isSupported();
    }

    // -----------------------------------------------------------------------
    // Interface implementation
    // -----------------------------------------------------------------------

    @Override
    public void renderGameState(Game game, int winScore) {
        clearScreen();
        System.out.print(ConsoleRenderer.renderGameStateString(game, ansi, winScore));
        System.out.flush();
    }

    @Override
    public void renderTurnHeader(Player player) {
        System.out.println();
        printLine("-", 52);
        System.out.println("  " + player.getName() + "'s Turn");
        printLine("-", 52);
    }

    @Override
    public void renderActionMenu() {
        System.out.println("┌─ YOUR MOVE ──────────────────────────────────────┐");
        System.out.println("│  [1]  Take gems                                  │");
        System.out.println("│  [2]  Buy a card                                 │");
        System.out.println("│  [3]  Reserve a card                             │");
        System.out.println("│  [R]  View reserved cards       [Q]  Quit        │");
        System.out.println("└──────────────────────────────────────────────────┘");
    }

    @Override
    public void renderGemOptions(List<Token> available) {
        System.out.println();
        System.out.println("Available gem colors:");
        for (int i = 0; i < available.size(); i++) {
            System.out.printf("  [%d] %-5s%n", i + 1, available.get(i));
        }
        System.out.println();
        System.out.println("  Take 3 different → enter 3 numbers e.g.  1 2 3");
        System.out.println("  Take 2 same      → enter same number twice e.g.  2 2");
    }

    @Override
    public void renderCardOptions(List<String> options, String header) {
        System.out.println();
        System.out.println(header);
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + options.get(i));
        }
    }

    @Override
    public void renderTokenReturnPrompt(Player player, int numToReturn, List<Token> held) {
        System.out.println();
        System.out.println("You have " + player.getTotalTokenCount()
                + " tokens (max 10). Return " + numToReturn + ".");
        System.out.println("Which token to return?");
        for (int i = 0; i < held.size(); i++) {
            System.out.printf("  [%d] %s (%d)%n", i + 1, held.get(i),
                    player.getTokenCount(held.get(i)));
        }
    }

    @Override
    public void renderMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void renderBroadcast(String message) {
        System.out.println(message);
    }

    @Override
    public void renderGameOver(Game game, int winScore) {
        clearScreen();
        System.out.print(ConsoleRenderer.renderGameOverString(game, ansi, winScore));
        System.out.flush();
    }

    @Override
    public void renderReservedCards(Player player) {
        clearScreen();
        System.out.print(ConsoleRenderer.renderReservedCardsString(player, ansi));
        System.out.println();
    }

    @Override
    public void renderAIAction(String description) {
        System.out.println(description);
    }

    @Override
    public void renderNobleVisit(Player player, Noble noble) {
        System.out.println("A noble visits " + player.getName() + "! (+3 pts)");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void clearScreen() {
        if (ansi) {
            System.out.print(ANSI_CLEAR);
            System.out.flush();
        } else {
            System.out.println("\n".repeat(40));
        }
    }

    private static void printLine(String ch, int len) {
        System.out.println(ch.repeat(len));
    }
}
