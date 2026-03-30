package util.ui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import logic.Game;
import model.Board;
import model.Card;
import model.Noble;
import model.Player;
import model.Token;
import util.AnsiSupport;

/**
 * Console UI helper to gather setup input and render the full game state each turn.
 *
 * <p>This class intentionally contains <em>no</em> AI logic. AI moves are computed by the already-existing AI
 * implementation elsewhere in the codebase; this UI only displays the results.</p>
 */
public class ConsoleUI {
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 4;
    private static final int MAX_RESERVED = 3;
    private static final int MIN_WIN_SCORE = 1;
    private static final int MAX_WIN_SCORE = 99;

    private static final String ESC   = "\u001B[";
    private static final String RESET = ESC + "0m";
    private static final String BOLD  = ESC + "1m";
    private static final String DIM   = ESC + "2m";
    private static final String CLEAR = ESC + "2J" + ESC + "H";

    // Unicode box-drawing characters
    private static final String H  = "─";
    private static final String TL = "┌";
    private static final String TR = "┐";
    private static final String BL = "└";
    private static final String BR = "┘";
    private static final String VB = "│";

    private static final int CARD_BOX_WIDTH   = 30;  // 4 per row in the market; slightly wider to avoid truncation
    private static final int NOBLE_BOX_WIDTH  = 34;  // inner=32 fits "Needs: Blk3 Blu3 Grn3 Red3 Wht3"
    private static final int PLAYER_BOX_WIDTH = 54;  // bigger + clearer labels in player profile

    private static final Token[] COST_ORDER = {
            Token.BLACK, Token.BLUE, Token.GREEN, Token.RED, Token.WHITE
    };

    private final Scanner sc;
    private final boolean ansiEnabled;

    public ConsoleUI() {
        this(new Scanner(System.in));
    }

    public ConsoleUI(Scanner scanner) {
        this.sc = Objects.requireNonNull(scanner, "scanner");
        this.ansiEnabled = AnsiSupport.isSupported();
    }

    // -------------------------------------------------------------------------
    // Public rendering API
    // -------------------------------------------------------------------------

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
        StringBuilder out = new StringBuilder();

        Board board          = game.getBoard();
        List<Player> players = game.getPlayers();
        int currentIndex     = game.getCurrentPlayerIndex();
        Player current       = players.get(currentIndex);

        // ── Header ───────────────────────────────────────────────────────────
        out.append(bold("SPLENDOR", ansi))
           .append(dim("  (" + players.size() + " players)", ansi))
           .append(dim("  - first to " + winScore + " pts", ansi))
           .append('\n');
        out.append(dim("Legend: ", ansi))
           .append(colorizeToken(Token.BLACK, "Blk", ansi)).append(' ')
           .append(colorizeToken(Token.BLUE, "Blu", ansi)).append(' ')
           .append(colorizeToken(Token.GREEN, "Grn", ansi)).append(' ')
           .append(colorizeToken(Token.RED, "Red", ansi)).append(' ')
           .append(colorizeToken(Token.WHITE, "Wht", ansi)).append(' ')
           .append(colorizeToken(Token.GOLD, "Gld", ansi))
           .append('\n');

        // ── Player profile boxes ──────────────────────────────────────────────
        List<List<String>> playerBoxes = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            playerBoxes.add(renderPlayerBox(i, players.get(i), i == currentIndex, winScore, ansi));
        }
        out.append(renderBoxesGrid(playerBoxes, players.size() <= 2 ? players.size() : 2));
        out.append('\n');

        // ── Bank ──────────────────────────────────────────────────────────────
        out.append("Bank:  ").append(formatTokenMap(board.getAvailableTokens(), true, ansi)).append('\n');

        // ── Nobles as boxes ───────────────────────────────────────────────────
        out.append(bold("Nobles", ansi)).append('\n');
        List<Noble> nobles = board.getNobles();
        if (nobles.isEmpty()) {
            out.append(dim("(none)", ansi)).append('\n');
        } else {
            List<List<String>> nobleBoxes = new ArrayList<>();
            for (int i = 0; i < nobles.size(); i++) {
                nobleBoxes.add(renderNobleBox(i, nobles.get(i), current, ansi));
            }
            out.append(renderBoxesGrid(nobleBoxes, nobles.size() <= 3 ? nobles.size() : 3));
        }
        out.append('\n');

        // ── Market (levels 3 → 1) ─────────────────────────────────────────────
        out.append(bold("Market", ansi)).append('\n');
        for (int level = 3; level >= 1; level--) {
            int remaining   = board.getDeckRemainingCount(level);
            String deckInfo = remaining > 0
                    ? dim(" [" + remaining + " in deck]", ansi)
                    : dim(" [deck empty]", ansi);
            out.append(dim("Level " + level, ansi)).append(deckInfo).append('\n');
            out.append(buildMarketRowString(level, board.getVisibleCards(level), ansi));
            out.append('\n');
        }

        // ── Reserved-card reminder ────────────────────────────────────────────
        if (current.isHuman() && !current.getHand().isEmpty()) {
            out.append(dim("Tip: choose [R] to view your reserved cards.", ansi)).append('\n');
        }

        return out.toString();
    }

    public void displayGameState(Game game, int winScore) {
        Objects.requireNonNull(game, "game");
        clearScreen();
        System.out.print(renderGameStateString(game, ansiEnabled, winScore));
    }

    /** Convenience overload — uses winScore=10 as fallback. */
    public void displayGameState(Game game) {
        displayGameState(game, 10);
    }

    public void displayReservedCards(Player player) {
        Objects.requireNonNull(player, "player");
        clearScreen();

        System.out.println(bold("Reserved Cards") + dim("  (" + player.getHand().size() + "/" + MAX_RESERVED + ")"));
        System.out.println("Player: " + player.getName());
        System.out.println("Bonuses: " + formatTokenMap(player.getBonuses(), false));
        System.out.println();

        if (player.getHand().isEmpty()) {
            System.out.println(dim("(none)"));
            return;
        }

        for (int i = 0; i < player.getHand().size(); i++) {
            Card c         = player.getHand().get(i);
            boolean afford = player.canAffordCard(c);
            System.out.println(bold("[r-" + i + "]") + " " + (afford ? "" : dim("(not affordable yet) ")));
            System.out.println("  " + formatCard(c));
            System.out.println("  After bonuses: " + formatCostCompact(remainingAfterBonuses(c, player)));
            System.out.println();
        }
    }

    // -------------------------------------------------------------------------
    // Setup prompts
    // -------------------------------------------------------------------------

    public int getWinningPoints(int defaultWinScore) {
        clearScreen();
        System.out.println(bold("S P L E N D O R"));
        System.out.println(dim("Press Enter to accept defaults."));
        System.out.println();

        while (true) {
            System.out.printf("Points to win (%d-%d) [%d]: ", MIN_WIN_SCORE, MAX_WIN_SCORE, defaultWinScore);
            String line = readLine();
            if (line == null) return defaultWinScore;
            line = line.trim();
            if (line.isEmpty()) return defaultWinScore;
            try {
                int n = Integer.parseInt(line);
                if (n >= MIN_WIN_SCORE && n <= MAX_WIN_SCORE) return n;
            } catch (NumberFormatException ignored) { }
            System.out.printf("Please enter a number between %d and %d.%n", MIN_WIN_SCORE, MAX_WIN_SCORE);
        }
    }

    public int getNumberOfPlayers() {
        while (true) {
            System.out.printf("How many players? (%d-%d): ", MIN_PLAYERS, MAX_PLAYERS);
            String line = readLine();
            if (line == null) return MIN_PLAYERS;
            line = line.trim();
            try {
                int n = Integer.parseInt(line);
                if (n >= MIN_PLAYERS && n <= MAX_PLAYERS) return n;
            } catch (NumberFormatException ignored) { }
            System.out.printf("Please enter a number between %d and %d.%n", MIN_PLAYERS, MAX_PLAYERS);
        }
    }

    public boolean[] getPlayerTypes(int numberOfPlayers) {
        boolean[] isAI = new boolean[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            while (true) {
                System.out.print("Is Player " + (i + 1) + " an AI? (y/n): ");
                String resp = readLine();
                if (resp == null) { isAI[i] = false; break; }
                resp = resp.trim().toLowerCase();
                if (resp.startsWith("y")) { isAI[i] = true;  break; }
                if (resp.startsWith("n")) { isAI[i] = false; break; }
                System.out.println("Please answer y or n.");
            }
        }
        return isAI;
    }

    // -------------------------------------------------------------------------
    // Player profile boxes
    // -------------------------------------------------------------------------

    private static List<String> renderPlayerBox(int index, Player player,
                                                boolean isCurrent, int winScore, boolean ansi) {
        int inner = PLAYER_BOX_WIDTH - 2;

        // uniform border colour: keep all player borders the same; highlight current player via marker/text
        String borderColor = ansi ? BOLD : null;
        String top    = borderTop(inner, borderColor, ansi);
        String bottom = borderBottom(inner, borderColor, ansi);
        String lb     = borderLeft(borderColor, ansi);
        String rb     = borderRight(borderColor, ansi);

        List<String> lines = new ArrayList<>();
        lines.add(top);

        // ── name row: bold for current player, <== marker flush right ─────────
        String type      = player.isHuman() ? "Human" : "AI";
        String nameLabel = "P" + (index + 1) + " [" + type + "] " + player.getName();
        if (isCurrent) nameLabel = bold(nameLabel, ansi);
        String marker = isCurrent ? bold("<==", ansi) : "";
        lines.add(lb + fitLine(rightMarkerLine(nameLabel, marker, inner), inner) + rb);

        // ── thin separator ────────────────────────────────────────────────────
        // (separator removed for compact layout)

        // ── score row ─────────────────────────────────────────────────────────
        int toWin = Math.max(0, winScore - player.getScore());
        lines.add(lb + fitLine("Score: " + player.getScore() + "/" + winScore + "   Left: " + toWin, inner) + rb);
        lines.add(lb + fitLine("Reserved: " + player.getHand().size() + "/" + MAX_RESERVED
                + "   Bought: " + player.getPurchasedCards().size()
                + "   Nobles: " + player.getNobles().size(), inner) + rb);

        // ── tokens row ────────────────────────────────────────────────────────
        lines.add(lb + fitLine("Tokens:  " + formatTokenMapFixed(player.getTokens(), true, ansi), inner) + rb);

        // ── bonuses row ───────────────────────────────────────────────────────
        lines.add(lb + fitLine("Bonuses: " + formatTokenMapFixed(player.getBonuses(), false, ansi), inner) + rb);

        // ── stats row ─────────────────────────────────────────────────────────
        // (stats merged into score line)

        lines.add(bottom);
        return lines;
    }

    // -------------------------------------------------------------------------
    // Noble boxes
    // -------------------------------------------------------------------------

    private static List<String> renderNobleBox(int index, Noble noble, Player currentPlayer, boolean ansi) {
        int inner = NOBLE_BOX_WIDTH - 2;
        List<String> lines = new ArrayList<>();
        lines.add(borderTop(inner, null, ansi));
        String lb = borderLeft(null, ansi);
        String rb = borderRight(null, ansi);

        // header: index left, pts right-aligned bold
        String idStr  = "[N" + index + "] " + noble.getName();
        String ptsStr = bold(noble.getPrestigePoints() + " pts", ansi);
        lines.add(lb + fitLine(rightMarkerLine(idStr, ptsStr, inner), inner) + rb);

        // requirements
        String needs = "Needs: " + formatCostCompactStatic(noble.getCost());
        lines.add(lb + fitLine(needs, inner) + rb);

        // current player's progress toward this noble
        Map<Token, Integer> bonuses = currentPlayer.getBonuses();
        StringBuilder progressSb   = new StringBuilder("Need: ");
        boolean ready              = true;
        for (Token t : COST_ORDER) {
            int still = noble.getCost().getOrDefault(t, 0) - bonuses.getOrDefault(t, 0);
            if (still > 0) {
                progressSb.append(colorizeToken(t, tokenLabel(t), ansi)).append(still).append(" ");
                ready = false;
            }
        }
        String progressLine = ready ? bold("*** READY! ***", ansi) : progressSb.toString().trim();
        lines.add(lb + fitLine(progressLine, inner) + rb);

        lines.add(borderBottom(inner, null, ansi));
        return lines;
    }

    // -------------------------------------------------------------------------
    // Card boxes
    // -------------------------------------------------------------------------

    private static List<String> computeWrappedCostChunks(Card card, int inner, boolean ansi) {
        if (card == null) return List.of();
        String prefix = "Bon " + gemChip(card.getBonus(), ansi) + "  Cost ";
        int prefixVis = visLen(prefix);
        int costWidth = Math.max(6, inner - prefixVis);
        List<String> chunks = wrapBySpaces(formatCostCompactStatic(card.getCost()), costWidth);
        return chunks.isEmpty() ? List.of("-") : chunks;
    }

    private static List<String> wrapBySpaces(String text, int width) {
        if (text == null) return List.of();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return List.of();
        int w = Math.max(1, width);

        String[] words = trimmed.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;

            if (current.length() == 0) {
                if (word.length() <= w) {
                    current.append(word);
                } else {
                    int idx = 0;
                    while (idx < word.length()) {
                        int end = Math.min(word.length(), idx + w);
                        lines.add(word.substring(idx, end));
                        idx = end;
                    }
                }
                continue;
            }

            if (current.length() + 1 + word.length() <= w) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                if (word.length() <= w) {
                    current.append(word);
                } else {
                    int idx = 0;
                    while (idx < word.length()) {
                        int end = Math.min(word.length(), idx + w);
                        lines.add(word.substring(idx, end));
                        idx = end;
                    }
                }
            }
        }

        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private static String buildMarketRowString(int level, Card[] row, boolean ansi) {
        List<List<String>> boxes = new ArrayList<>();

        int inner = CARD_BOX_WIDTH - 2;
        int maxCostLines = 1;
        List<List<String>> perCardCostChunks = new ArrayList<>();
        for (int slot = 0; slot < row.length; slot++) {
            Card card = row[slot];
            List<String> chunks = (card == null) ? List.of() : computeWrappedCostChunks(card, inner, ansi);
            perCardCostChunks.add(chunks);
            maxCostLines = Math.max(maxCostLines, Math.max(1, chunks.size()));
        }

        for (int slot = 0; slot < row.length; slot++) {
            boxes.add(renderCardBox("[" + level + "-" + slot + "]", row[slot], ansi, perCardCostChunks.get(slot), maxCostLines));
        }
        String out = joinBoxesSideBySide(boxes);
        return out.endsWith("\n") ? out.substring(0, out.length() - 1) : out;
    }

    private static List<String> renderCardBox(String id, Card card, boolean ansi, List<String> costChunks, int maxCostLines) {
        int inner = CARD_BOX_WIDTH - 2;

        // border colored by the card's gem bonus (apply to all border segments)
        String bc = (ansi && card != null && card.getBonus() != null) ? gemBorderColor(card.getBonus()) : null;
        String top    = borderTop(inner, bc, ansi);
        String bottom = borderBottom(inner, bc, ansi);
        String lb     = borderLeft(bc, ansi);
        String rb     = borderRight(bc, ansi);

        List<String> lines = new ArrayList<>();
        lines.add(top);

        if (card == null) {
            lines.add(lb + fitLine(id, inner) + rb);
            lines.add(lb + fitLine("(empty)", inner) + rb);
            for (int i = 1; i < Math.max(1, maxCostLines); i++) {
                lines.add(lb + fitLine("", inner) + rb);
            }
            lines.add(bottom);
            return lines;
        }

        // header: id left, prestige pts right-aligned bold
        String ptsStr = bold(card.getPrestigePoints() + " pts", ansi);
        lines.add(lb + fitLine(rightMarkerLine(id, ptsStr, inner), inner) + rb);

        // bonus: background-coloured chip — always visible regardless of terminal theme
        String prefix = "Bon " + gemChip(card.getBonus(), ansi) + "  Cost ";
        int prefixVis = visLen(prefix);
        List<String> safeChunks = (costChunks == null || costChunks.isEmpty()) ? List.of("-") : costChunks;

        lines.add(lb + fitLine(prefix + safeChunks.get(0), inner) + rb);
        String indent = " ".repeat(prefixVis);
        for (int i = 1; i < Math.max(1, maxCostLines); i++) {
            String chunk = (i < safeChunks.size()) ? safeChunks.get(i) : "";
            lines.add(lb + fitLine(indent + chunk, inner) + rb);
        }
        lines.add(bottom);
        return lines;
    }

    // -------------------------------------------------------------------------
    // Box layout
    // -------------------------------------------------------------------------

    /**
     * Joins boxes side by side. All boxes are padded to the tallest height so
     * no box is chopped short.
     */
    private static String joinBoxesSideBySide(List<List<String>> boxes) {
        if (boxes.isEmpty()) return "";

        // find the tallest box
        int maxH = boxes.stream().mapToInt(List::size).max().orElse(0);

        // pad shorter boxes with blank content rows (inserted before the last line)
        List<List<String>> normalized = new ArrayList<>();
        for (List<String> box : boxes) {
            if (box.size() == maxH) {
                normalized.add(new ArrayList<>(box));
                continue;
            }
            // infer inner width from the visible length of the top border line
            int boxW  = visLen(box.get(0));
            int inner = boxW - 2;
            List<String> padded = new ArrayList<>(box.subList(0, box.size() - 1)); // drop bottom border
            while (padded.size() < maxH - 1) {
                padded.add(VB + " ".repeat(inner) + VB);                           // blank content row
            }
            padded.add(box.get(box.size() - 1));                                   // re-attach bottom border
            normalized.add(padded);
        }

        // render row by row
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxH; i++) {
            StringBuilder row = new StringBuilder();
            for (int b = 0; b < normalized.size(); b++) {
                if (b > 0) row.append("  ");
                row.append(normalized.get(b).get(i));
            }
            sb.append(row).append('\n');
        }
        return sb.toString();
    }

    private static String renderBoxesGrid(List<List<String>> boxes, int columns) {
        if (boxes == null || boxes.isEmpty()) return "";
        int cols = Math.max(1, columns);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < boxes.size(); i += cols) {
            int end = Math.min(boxes.size(), i + cols);
            if (sb.length() > 0) sb.append('\n');
            String block = joinBoxesSideBySide(boxes.subList(i, end));
            // joinBoxesSideBySide currently ends with a newline; drop it to keep the layout compact
            if (block.endsWith("\n")) block = block.substring(0, block.length() - 1);
            sb.append(block);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Formatting helpers
    // -------------------------------------------------------------------------

    private String formatCard(Card c) { return formatCard(c, ansiEnabled); }

    private static String formatCard(Card c, boolean ansi) {
        return pointsLabel(c.getPrestigePoints()) + "  Bonus: " + tokenLabel(c.getBonus())
                + "  Cost: " + formatCost(c.getCost(), ansi);
    }

    private static String formatCost(Map<Token, Integer> cost, boolean ansi) {
        StringBuilder sb = new StringBuilder();
        for (Token t : COST_ORDER) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) sb.append(colorizeToken(t, tokenLabel(t), ansi)).append(v).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatTokenMap(Map<Token, Integer> map, boolean includeGold) {
        return formatTokenMap(map, includeGold, ansiEnabled);
    }

    /**
     * Formats a token map, skipping colors with a count of zero. Returns "—" when empty.
     */
    static String formatTokenMap(Map<Token, Integer> map, boolean includeGold, boolean ansi) {
        Map<Token, Integer> safe = (map == null) ? new EnumMap<>(Token.class) : map;
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (Token t : COST_ORDER) {
            int v = safe.getOrDefault(t, 0);
            if (v > 0) {
                sb.append(colorizeToken(t, tokenLabel(t), ansi)).append(":").append(v).append(" ");
                any = true;
            }
        }
        if (includeGold) {
            int g = safe.getOrDefault(Token.GOLD, 0);
            if (g > 0) {
                sb.append(colorizeToken(Token.GOLD, tokenLabel(Token.GOLD), ansi)).append(":").append(g);
                any = true;
            }
        }
        if (!any) sb.append("—");
        return sb.toString().trim();
    }

    /**
     * Fixed-width token formatter used inside player boxes so counts never push borders.
     * Always prints all colours in a stable order, with 2-digit counts (00-99).
     */
    private static String formatTokenMapFixed(Map<Token, Integer> map, boolean includeGold, boolean ansi) {
        Map<Token, Integer> safe = (map == null) ? new EnumMap<>(Token.class) : map;
        StringBuilder sb = new StringBuilder();

        for (Token t : COST_ORDER) {
            int v = Math.min(99, safe.getOrDefault(t, 0));
            sb.append(colorizeToken(t, tokenShortLabel(t), ansi))
              .append(String.format("%02d", v))
              .append(' ');
        }
        if (includeGold) {
            int g = Math.min(99, safe.getOrDefault(Token.GOLD, 0));
            sb.append(colorizeToken(Token.GOLD, tokenShortLabel(Token.GOLD), ansi))
              .append(String.format("%02d", g));
        } else if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 1); // drop trailing space
        }
        return sb.toString().trim();
    }

    private static String tokenShortLabel(Token token) {
        if (token == null) return "?";
        return switch (token) {
            case BLACK -> "Blk";
            case BLUE  -> "Blu";
            case GREEN -> "Grn";
            case RED   -> "Red";
            case WHITE -> "Wht";
            case GOLD  -> "Gld";
        };
    }

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

    private static String colorizeToken(Token token, String text, boolean ansi) {
        if (!ansi) return text;
        String color = switch (token) {
            case GREEN -> ESC + "32m";
            case WHITE -> ESC + "1;37m";   // bold — more visible than plain white
            case BLUE  -> ESC + "34m";
            case BLACK -> ESC + "90m";
            case RED   -> ESC + "31m";
            case GOLD  -> ESC + "33m";
        };
        return color + text + RESET;
    }

    /**
     * Returns a background-coloured gem chip for a token — always visible regardless
     * of terminal theme. Used as the bonus indicator inside card boxes.
     * Non-ANSI fallback is the plain 3-char abbreviation.
     */
    private static String gemChip(Token token, boolean ansi) {
        if (!ansi) return tokenLabel(token);
        String code = switch (token) {
            case GREEN -> ESC + "42m";        // green bg
            case WHITE -> ESC + "47;30m";     // white bg + black fg
            case BLUE  -> ESC + "44m";        // blue bg
            case BLACK -> ESC + "40;97m";     // black bg + bright-white fg
            case RED   -> ESC + "41m";        // red bg
            case GOLD  -> ESC + "43;30m";     // yellow bg + black fg
        };
        return code + " " + tokenLabel(token) + " " + RESET;
    }

    private static String gemBorderColor(Token token) {
        return switch (token) {
            case GREEN -> ESC + "1;32m";       // bold green
            case WHITE -> ESC + "1;37m";       // bold white (less overpowering than a white background)
            case BLUE  -> ESC + "1;34m";       // bold blue
            case BLACK -> ESC + "1;90m";       // bright black/gray (no background fill)
            case RED   -> ESC + "1;31m";       // bold red
            case GOLD  -> ESC + "1;33m";       // bold yellow
        };
    }

    private String bold(String text) { return bold(text, ansiEnabled); }
    private static String bold(String text, boolean ansi) {
        return ansi ? (BOLD + text + RESET) : text;
    }

    private String dim(String text) { return dim(text, ansiEnabled); }
    private static String dim(String text, boolean ansi) {
        return ansi ? (DIM + text + RESET) : text;
    }

    private void clearScreen() {
        if (ansiEnabled) {
            System.out.print(CLEAR);
            System.out.flush();
            return;
        }
        System.out.println("\n".repeat(40));
    }

    // -------------------------------------------------------------------------
    // String utilities — all length calculations use visible (ANSI-stripped) length
    // -------------------------------------------------------------------------

    /** Returns the visible character count of s, ignoring ANSI escape sequences. */
    private static int visLen(String s) {
        return stripAnsi(s).length();
    }

    /**
     * Pads s on the right so its visible length equals width.
     * ANSI escape codes are excluded from the length calculation.
     */
    private static String padRight(String s, int width) {
        int vis = visLen(s);
        if (vis >= width) return s;
        return s + " ".repeat(width - vis);
    }

    /** Truncates s to at most width visible characters, appending "…" if cut. */
    private static String truncate(String s, int width) {
        if (s.length() <= width) return s;
        if (width <= 1) return "…";
        return s.substring(0, width - 1) + "…";
    }

    /** Fits s to exactly width visible chars (ANSI-aware): truncate if needed, then pad right. */
    private static String fitLine(String s, int width) {
        if (width <= 0) return "";
        String truncated = truncateVisible(s == null ? "" : s, width);
        int vis = visLen(truncated);
        if (vis < width) truncated = truncated + " ".repeat(width - vis);
        return truncated;
    }

    /** Truncates s to at most width visible chars (ANSI-aware), appending "..." if cut. */
    private static String truncateVisible(String s, int width) {
        String safe = (s == null) ? "" : s;
        if (visLen(safe) <= width) return safe;
        if (width <= 3) return ".".repeat(Math.max(0, width));

        StringBuilder sb = new StringBuilder();
        int vis = 0;
        int target = width - 3; // reserve space for "..."
        for (int i = 0; i < safe.length() && vis < target; ) {
            char c = safe.charAt(i);
            if (c == '\u001B' && i + 1 < safe.length() && safe.charAt(i + 1) == '[') {
                int m = safe.indexOf('m', i);
                if (m < 0) break;
                sb.append(safe, i, m + 1);
                i = m + 1;
                continue;
            }
            sb.append(c);
            vis++;
            i++;
        }
        sb.append("...");
        if (safe.contains(ESC)) sb.append(RESET);
        return sb.toString();
    }

    private static String rightMarkerLine(String left, String right, int innerWidth) {
        String l = (left == null) ? "" : left;
        String r = (right == null) ? "" : right;
        int gap = innerWidth - visLen(l) - visLen(r);
        if (gap < 1) return truncateVisible(l, innerWidth);
        return l + " ".repeat(gap) + r;
    }

    /** Strips ANSI escape sequences for length calculations. */
    private static String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[^m]*m", "");
    }

    private static String pointsLabel(int points) { return points + " pts"; }

    private String formatCostCompact(Map<Token, Integer> cost) { return formatCostCompactStatic(cost); }

    public static String formatCostCompactStatic(Map<Token, Integer> cost) {
        if (cost == null || cost.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Token t : COST_ORDER) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) sb.append(tokenLabel(t)).append(v).append(" ");
        }
        return sb.toString().trim();
    }

    private static String formatCostTight(Map<Token, Integer> cost) {
        if (cost == null || cost.isEmpty()) return "â€”";
        StringBuilder sb = new StringBuilder();
        for (Token t : COST_ORDER) {
            int v = cost.getOrDefault(t, 0);
            if (v > 0) sb.append(tokenShortLabel(t)).append(v).append(" ");
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? "â€”" : out;
    }

    private Map<Token, Integer> remainingAfterBonuses(Card card, Player player) {
        Map<Token, Integer> remaining = new EnumMap<>(Token.class);
        Map<Token, Integer> cost      = card.getCost();
        Map<Token, Integer> bonuses   = player.getBonuses();
        for (Token t : COST_ORDER) {
            int needed = Math.max(0, cost.getOrDefault(t, 0) - bonuses.getOrDefault(t, 0));
            if (needed > 0) remaining.put(t, needed);
        }
        return remaining;
    }

    private String readLine() {
        try {
            if (!sc.hasNextLine()) return null;
            return sc.nextLine();
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Border helpers (ANSI-aware)
    // -------------------------------------------------------------------------

    private static String borderTop(int innerWidth, String borderColor, boolean ansi) {
        String h = H.repeat(Math.max(0, innerWidth));
        if (!ansi || borderColor == null) return TL + h + TR;
        return borderColor + TL + h + TR + RESET;
    }

    private static String borderBottom(int innerWidth, String borderColor, boolean ansi) {
        String h = H.repeat(Math.max(0, innerWidth));
        if (!ansi || borderColor == null) return BL + h + BR;
        return borderColor + BL + h + BR + RESET;
    }

    private static String borderLeft(String borderColor, boolean ansi) {
        if (!ansi || borderColor == null) return VB;
        return borderColor + VB + RESET;
    }

    private static String borderRight(String borderColor, boolean ansi) {
        if (!ansi || borderColor == null) return VB;
        return borderColor + VB + RESET;
    }

    // -------------------------------------------------------------------------
    // Backward-compatible overload used by NetworkFormatter
    // -------------------------------------------------------------------------

    public static String renderGameStateString(Game game, boolean ansi) {
        return renderGameStateString(game, ansi, 10);
    }
}
