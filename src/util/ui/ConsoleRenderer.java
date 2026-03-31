package util.ui;

import java.util.Map;
import java.util.Objects;

import logic.Game;
import model.Player;
import model.Token;

/**
 * Stateless string renderer for the game's console UI.
 *
 * <p>This is a small facade that keeps the public API stable while the actual
 * rendering is split into focused component classes (each ~200 lines or less).
 * This makes the codebase much easier to explain in an oral review.</p>
 */
public final class ConsoleRenderer {

    private ConsoleRenderer() {
    }

    /**
     * Renders the full game state as a string.
     *
     * @param game     current game state
     * @param ansi     whether to embed ANSI colour sequences
     * @param winScore prestige points needed to win
     * @return multi-line text ready to print
     */
    public static String renderGameStateString(Game game, boolean ansi, int winScore) {
        Objects.requireNonNull(game, "game");
        return ConsoleGameStateView.renderGameStateString(game, ansi, winScore);
    }

    /**
     * Renders the final screen shown when the game is over: full state + winner banner.
     * Intended to be identical between local and network modes.
     */
    public static String renderGameOverString(Game game, boolean ansi, int winScore) {
        Objects.requireNonNull(game, "game");
        return ConsoleGameOverView.renderGameOverString(game, ansi, winScore);
    }

    /**
     * Reserved-card view shown in local mode when the player presses {@code R}.
     */
    public static String renderReservedCardsString(Player player, boolean ansi) {
        Objects.requireNonNull(player, "player");
        return ConsoleReservedView.renderReservedCardsString(player, ansi);
    }

    /**
     * Shared compact cost formatter used by the UI and by {@code GameApp} logs.
     */
    public static String formatCostCompactStatic(Map<Token, Integer> cost) {
        return ConsoleTokenFormat.formatCostCompactStatic(cost);
    }

    /**
     * Backward-compatible overload — uses winScore=10 as fallback.
     */
    public static String renderGameStateString(Game game, boolean ansi) {
        return renderGameStateString(game, ansi, 10);
    }
}

