package util.ui;

import java.util.Objects;

import model.Card;
import model.Player;

/**
 * Reserved-card screen (local mode shortcut: R).
 *
 * <p>This view answers the common question: "What can I buy from my reserved cards right now?"
 * It shows affordability and the remaining cost after permanent bonuses.</p>
 */
final class ConsoleReservedView {

    private ConsoleReservedView() {
    }

    /**
     * Renders the reserved cards owned by {@code player}.
     *
     * <p>Slot tags are {@code [r-0]}, {@code [r-1]}, ... so the rest of the game can refer to them
     * consistently (local input + network protocol).</p>
     */
    static String renderReservedCardsString(Player player, boolean ansi) {
        Objects.requireNonNull(player, "player");
        StringBuilder out = new StringBuilder();

        out.append(ConsoleAnsi.bold("Reserved Cards", ansi))
           .append(ConsoleAnsi.dim("  (" + player.getHand().size() + "/" + ConsoleConstants.MAX_RESERVED + ")", ansi))
           .append('\n');
        out.append("Player: ").append(player.getName()).append('\n');
        out.append("Bonuses: ").append(ConsoleTokenFormat.formatTokenMap(player.getBonuses(), false, ansi)).append('\n');
        out.append('\n');

        if (player.getHand().isEmpty()) {
            out.append(ConsoleAnsi.dim("(none)", ansi)).append('\n');
            return out.toString();
        }

        for (int i = 0; i < player.getHand().size(); i++) {
            Card c         = player.getHand().get(i);
            boolean afford = player.canAffordCard(c);
            out.append(ConsoleAnsi.bold("[r-" + i + "]", ansi))
               .append(' ')
               .append(afford ? "" : ConsoleAnsi.dim("(not affordable yet) ", ansi))
               .append('\n');
            out.append("  ").append(ConsoleTokenFormat.formatCard(c, ansi)).append('\n');
            out.append("  After bonuses: ")
               .append(ConsoleTokenFormat.formatCostCompactStatic(ConsoleTokenFormat.remainingAfterBonuses(c, player)))
               .append('\n');
            out.append('\n');
        }

        return out.toString();
    }
}
