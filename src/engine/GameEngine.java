package engine;

import java.util.ArrayList;
import java.util.List;

import io.InputHandler;
import logic.BotMain;
import logic.Game;
import model.Board;
import model.Card;
import model.Noble;
import model.Player;
import model.Token;
import ui.GameRenderer;
import util.ui.ConsoleUI;

/**
 * Orchestrates the Splendor game loop.
 *
 * <p>All I/O is delegated to the injected {@link InputHandler} and
 * {@link GameRenderer}, so this class runs identically in local and
 * networked modes — only the implementations differ (Strategy pattern).</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * new GameEngine(game, inputHandler, renderer, winScore).run();
 * }</pre>
 */
public class GameEngine {

    private final Game         game;
    private final InputHandler input;
    private final GameRenderer renderer;
    private final int          winScore;

    public GameEngine(Game game, InputHandler input, GameRenderer renderer, int winScore) {
        this.game     = game;
        this.input    = input;
        this.renderer = renderer;
        this.winScore = winScore;
    }

    // -----------------------------------------------------------------------
    // Main loop
    // -----------------------------------------------------------------------

    /** Runs until the game ends or a player quits. */
    public void run() {
        renderer.renderBroadcast("");
        renderer.renderBroadcast("=".repeat(52));
        renderer.renderBroadcast("           S P L E N D O R");
        renderer.renderBroadcast("      Collect gems. Buy cards. Win!");
        renderer.renderBroadcast("=".repeat(52));
        renderer.renderBroadcast("");
        renderer.renderBroadcast("  Goal: First to " + winScore + " prestige points wins!");

        while (!game.isGameOver()) {
            Player p = game.getCurrentPlayer();
            renderer.renderGameState(game, winScore);
            renderer.renderTurnHeader(p);

            boolean validAction;
            if (p.isHuman()) {
                validAction = executeHumanTurn(p);
                if (!validAction) {
                    renderer.renderBroadcast(p.getName() + " quit the game. Goodbye.");
                    return;
                }
            } else {
                validAction = executeAITurn(p);
                input.waitForAck();
            }

            if (validAction) {
                handleTokenReturn(p);
                checkNobleVisit(p);
                checkEndTrigger(p);
                game.nextTurn();
            }
        }

        Player winner = game.determineWinner();
        renderer.renderGameOver(game, winScore);
        renderer.renderBroadcast("*".repeat(52));
        if (winner != null) {
            renderer.renderBroadcast("  " + winner.getName() + " WINS with "
                    + winner.getScore() + " points!");
        } else {
            renderer.renderBroadcast("  Game finished — no winner could be determined.");
        }
        renderer.renderBroadcast("*".repeat(52));
    }

    // -----------------------------------------------------------------------
    // Human turn
    // -----------------------------------------------------------------------

    /**
     * Loops the action menu until the player takes a valid action.
     *
     * @return {@code true} if a valid action was performed; {@code false} on quit/disconnect.
     */
    private boolean executeHumanTurn(Player p) {
        while (true) {
            renderer.renderActionMenu();
            String choice = input.readMenuChoice();

            if (choice == null || choice.equals("q")) return false;

            boolean valid = switch (choice) {
                case "1" -> handleTakeGems(p);
                case "2" -> handleBuyCard(p);
                case "3" -> handleReserveCard(p);
                case "r" -> {
                    renderer.renderReservedCards(p);
                    input.waitForAck();
                    renderer.renderGameState(game, winScore);
                    renderer.renderTurnHeader(p);
                    yield false; // not a game action — loop back to menu
                }
                default -> {
                    renderer.renderMessage("Invalid choice. Enter 1, 2, 3, R, or Q.");
                    yield false;
                }
            };

            if (valid) return true;
        }
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    private boolean handleTakeGems(Player p) {
        Board board = game.getBoard();
        var avail = board.getAvailableTokens();

        List<Token> colors = new ArrayList<>();
        for (Token t : new Token[]{Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE}) {
            if (avail.getOrDefault(t, 0) > 0) colors.add(t);
        }

        if (colors.isEmpty()) {
            renderer.renderMessage("No gems available on the board.");
            return false;
        }

        renderer.renderGemOptions(colors);
        List<Token> selected = input.readGemSelection(colors);
        if (selected == null) return false;

        if (selected.size() == 2 && selected.get(0) == selected.get(1)) {
            Token t = selected.get(0);
            if (!game.canTakeTwoSameGems(p, t)) {
                renderer.renderMessage("Need at least 4 of that color on the board (or token limit exceeded).");
                return false;
            }
            game.takeTwoSameGems(p, t);
            renderer.renderMessage("Took 2 " + t + ".");
            return true;

        } else if (selected.size() == 3) {
            Token t1 = selected.get(0), t2 = selected.get(1), t3 = selected.get(2);
            if (!game.canTakeThreeDifferentGems(p, t1, t2, t3)) {
                renderer.renderMessage("Must be 3 different colors, all available, within 10-token limit.");
                return false;
            }
            game.takeThreeDifferentGems(p, t1, t2, t3);
            renderer.renderMessage("Took 1 " + t1 + ", 1 " + t2 + ", 1 " + t3 + ".");
            return true;
        }

        renderer.renderMessage("Invalid gem selection.");
        return false;
    }

    private boolean handleBuyCard(Player p) {
        List<String>   options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        for (int level = 1; level <= 3; level++) {
            Card[] row = game.getBoard().getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                if (game.canBuyVisibleCard(p, level, slot)) {
                    Card c = row[slot];
                    final int lv = level, sl = slot;
                    options.add(String.format("L%d slot %d — %d pts  Bonus:%-3s  Cost: %s",
                            level, slot, c.getPrestigePoints(),
                            tokenLabel(c.getBonus()),
                            ConsoleUI.formatCostCompactStatic(c.getCost())));
                    actions.add(() -> game.buyVisibleCard(p, lv, sl));
                }
            }
        }

        for (int i = 0; i < p.getHand().size(); i++) {
            if (game.canBuyReservedCard(p, i)) {
                Card c        = p.getHand().get(i);
                final int idx = i;
                options.add(String.format("Reserved [r-%d] — %d pts  Bonus:%-3s  Cost: %s",
                        i, c.getPrestigePoints(),
                        tokenLabel(c.getBonus()),
                        ConsoleUI.formatCostCompactStatic(c.getCost())));
                actions.add(() -> game.buyReservedCard(p, idx));
            }
        }

        if (options.isEmpty()) {
            renderer.renderMessage("You cannot afford any card right now.");
            return false;
        }

        renderer.renderCardOptions(options, "Buyable cards:");
        int idx = input.readCardIndex(options);
        if (idx == -1) return false;

        actions.get(idx).run();
        renderer.renderMessage("Purchased: " + options.get(idx));
        return true;
    }

    private boolean handleReserveCard(Player p) {
        if (p.getHand().size() >= 3) {
            renderer.renderMessage("Hand full (max 3 reserved cards). Buy one first.");
            return false;
        }

        List<String>   options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        for (int level = 3; level >= 1; level--) {
            Card[] row = game.getBoard().getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                if (game.canReserveVisibleCard(p, level, slot)) {
                    Card c = row[slot];
                    final int lv = level, sl = slot;
                    options.add(String.format("L%d slot %d — %d pts  Bonus:%-3s  Cost: %s",
                            level, slot, c.getPrestigePoints(),
                            tokenLabel(c.getBonus()),
                            ConsoleUI.formatCostCompactStatic(c.getCost())));
                    actions.add(() -> game.reserveVisibleCard(p, lv, sl));
                }
            }
        }

        for (int level = 1; level <= 3; level++) {
            if (game.canReserveDeckCard(p, level)) {
                final int lv      = level;
                int       remaining = game.getBoard().getDeckRemainingCount(level);
                options.add("Top of Deck " + level + " (face-down, " + remaining + " remaining)");
                actions.add(() -> game.reserveDeckCard(p, lv));
            }
        }

        if (options.isEmpty()) {
            renderer.renderMessage("No cards available to reserve.");
            return false;
        }

        renderer.renderCardOptions(options, "Reserve which card?");
        int idx = input.readCardIndex(options);
        if (idx == -1) return false;

        actions.get(idx).run();
        renderer.renderMessage("Reserved: " + options.get(idx));
        return true;
    }

    // -----------------------------------------------------------------------
    // Token return
    // -----------------------------------------------------------------------

    private void handleTokenReturn(Player p) {
        if (!p.isHuman()) {
            // AI: return tokens in one batch using the smart return logic
            int toReturn = game.getNumTokensToReturn(p);
            if (toReturn > 0) {
                List<Token> returning = BotMain.chooseTokensToReturn(p, game.getBoard(), toReturn);
                for (Token t : returning) game.returnToken(p, t, 1);
                renderer.renderAIAction(p.getName() + " (AI) returns " + toReturn + " token(s).");
            }
            return;
        }

        // Human: interactive one-at-a-time return
        while (game.mustReturnTokens(p)) {
            int toReturn = game.getNumTokensToReturn(p);

            List<Token> held = new ArrayList<>();
            for (Token t : Token.values()) {
                if (p.getTokenCount(t) > 0) held.add(t);
            }

            renderer.renderTokenReturnPrompt(p, toReturn, held);
            Token t = input.readTokenToReturn(held);
            if (t == null) return; // disconnect / EOF

            game.returnToken(p, t, 1);
            int left = game.getNumTokensToReturn(p);
            renderer.renderMessage("Returned 1 " + t + "."
                    + (left > 0 ? "  " + left + " more to go." : ""));
        }
    }

    // -----------------------------------------------------------------------
    // Noble & end-game
    // -----------------------------------------------------------------------

    private void checkNobleVisit(Player p) {
        Noble noble = game.checkAndAwardNoble(p);
        if (noble != null) {
            renderer.renderNobleVisit(p, noble);
        }
    }

    private void checkEndTrigger(Player p) {
        if (!game.isEndTriggered() && p.getScore() >= winScore) {
            renderer.renderBroadcast("");
            renderer.renderBroadcast("*** " + p.getName() + " has reached " + p.getScore()
                    + " points! The final round will now finish. ***");
            game.triggerEnd(game.getCurrentPlayerIndex());
        }
    }

    // -----------------------------------------------------------------------
    // AI turn
    // -----------------------------------------------------------------------

    private boolean executeAITurn(Player p) {
        Board  board  = game.getBoard();
        String action = BotMain.chooseAction(p, board, game.getPlayers());

        if (action == null) {
            renderer.renderAIAction(p.getName() + " (AI) could not decide. Skipping turn.");
            return true;
        }

        String[] parts = action.split(":");
        if (parts.length < 2) return false;

        try {
            switch (parts[0]) {
                case "1" -> {
                    if (parts.length < 4) return false;
                    Token t1 = Token.valueOf(parts[1]);
                    Token t2 = Token.valueOf(parts[2]);
                    Token t3 = Token.valueOf(parts[3]);
                    if (!game.canTakeThreeDifferentGems(p, t1, t2, t3)) return false;
                    game.takeThreeDifferentGems(p, t1, t2, t3);
                    renderer.renderAIAction(p.getName() + " (AI) takes 1 " + t1 + ", 1 " + t2 + ", 1 " + t3);
                    return true;
                }
                case "2" -> {
                    if (parts.length < 2) return false;
                    Token t = Token.valueOf(parts[1]);
                    if (!game.canTakeTwoSameGems(p, t)) return false;
                    game.takeTwoSameGems(p, t);
                    renderer.renderAIAction(p.getName() + " (AI) takes 2 " + t);
                    return true;
                }
                case "3" -> {
                    if (parts[1].equals("r") && parts.length >= 3) {
                        int idx = Integer.parseInt(parts[2]);
                        if (!game.canBuyReservedCard(p, idx)) return false;
                        Card card = p.getHand().get(idx);
                        game.buyReservedCard(p, idx);
                        renderer.renderAIAction(p.getName() + " (AI) buys reserved: " + cardShort(card));
                        return true;
                    } else if (parts.length >= 3) {
                        int lv = Integer.parseInt(parts[1]), sl = Integer.parseInt(parts[2]);
                        if (!game.canBuyVisibleCard(p, lv, sl)) return false;
                        Card card = board.getVisibleCards(lv)[sl];
                        game.buyVisibleCard(p, lv, sl);
                        renderer.renderAIAction(p.getName() + " (AI) buys: " + cardShort(card));
                        return true;
                    }
                }
                case "4" -> {
                    if (parts.length >= 3 && parts[1].equals("deck")) {
                        int lv = Integer.parseInt(parts[2]);
                        if (!game.canReserveDeckCard(p, lv)) return false;
                        game.reserveDeckCard(p, lv);
                        renderer.renderAIAction(p.getName() + " (AI) reserves from deck " + lv);
                        return true;
                    } else if (parts.length >= 3) {
                        int lv = Integer.parseInt(parts[1]), sl = Integer.parseInt(parts[2]);
                        if (!game.canReserveVisibleCard(p, lv, sl)) return false;
                        Card card = board.getVisibleCards(lv)[sl];
                        game.reserveVisibleCard(p, lv, sl);
                        renderer.renderAIAction(p.getName() + " (AI) reserves: " + cardShort(card));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            renderer.renderAIAction(p.getName() + " (AI) move failed: " + e.getMessage());
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Static helpers
    // -----------------------------------------------------------------------

    private static String tokenLabel(Token token) {
        if (token == null) return "(none)";
        return switch (token) {
            case BLACK -> "Blk";
            case BLUE  -> "Blu";
            case GREEN -> "Grn";
            case RED   -> "Red";
            case WHITE -> "Wht";
            case GOLD  -> "Gld";
        };
    }

    private static String cardShort(Card c) {
        return "PV:" + c.getPrestigePoints() + " " + c.getBonus()
                + "+ " + ConsoleUI.formatCostCompactStatic(c.getCost());
    }
}
