package logic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import model.Board;
import model.Card;
import model.Noble;
import model.Player;
import model.Token;

/**
 * INSANE difficulty — sends the full board state to the OpenAI API and lets
 * the model choose the optimal move. Falls back to HardAI on any error.
 */
class InsaneAI {

    // -----------------------------------------------------------------------
    // OpenAI API settings — hard-coded as requested
    // -----------------------------------------------------------------------
    static final String API_KEY = "key_here";
    static final String MODEL   = "gpt-4o";

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    static String chooseAction(Player p, Board board, List<Player> allPlayers) {
        try {
            String state    = buildBoardText(p, board, allPlayers);
            String response = callOpenAI(state);
            if (response != null) {
                String cmd = extractCmd(response.trim());
                if (cmd != null) return cmd;
            }
        } catch (Exception e) {
            System.err.println("[INSANE AI] OpenAI call failed (" + e.getMessage() + ") — falling back to HARD");
        }
        return HardAI.chooseAction(p, board, allPlayers);
    }

    // -----------------------------------------------------------------------
    // Board state description
    // -----------------------------------------------------------------------

    private static String buildBoardText(Player p, Board board, List<Player> allPlayers) {
        StringBuilder sb = new StringBuilder();

        // Current player
        sb.append("=== YOUR STATE ===\n");
        sb.append("Name: ").append(p.getName())
          .append(" | Score: ").append(p.getScore()).append(" pts\n");
        sb.append("Tokens: ");
        for (Token t : Token.values()) {
            int n = p.getTokenCount(t);
            if (n > 0) sb.append(t).append("=").append(n).append(" ");
        }
        sb.append("| Total: ").append(p.getTotalTokenCount()).append("/10\n");
        sb.append("Permanent bonuses (discounts): ");
        for (Token t : AIHelpers.GEM_COLORS) {
            int b = p.getBonuses().getOrDefault(t, 0);
            if (b > 0) sb.append(t).append("=").append(b).append(" ");
        }
        sb.append("\n");
        if (!p.getHand().isEmpty()) {
            sb.append("Reserved cards (hand):\n");
            for (int i = 0; i < p.getHand().size(); i++) {
                Card c = p.getHand().get(i);
                sb.append("  [").append(i).append("] ").append(cardText(c));
                sb.append(p.canAffordCard(c) ? " [AFFORDABLE NOW]" : "").append("\n");
            }
        }

        // Opponents
        if (allPlayers != null) {
            sb.append("\n=== OPPONENTS ===\n");
            for (Player opp : allPlayers) {
                if (opp == p) continue;
                sb.append(opp.getName()).append(": ").append(opp.getScore()).append(" pts | bonuses: ");
                for (Token t : AIHelpers.GEM_COLORS) {
                    int b = opp.getBonuses().getOrDefault(t, 0);
                    if (b > 0) sb.append(t).append("=").append(b).append(" ");
                }
                sb.append("| tokens=").append(opp.getTotalTokenCount())
                  .append(" | hand=").append(opp.getHand().size()).append("\n");
            }
        }

        // Board tokens
        sb.append("\n=== BOARD TOKENS ===\n");
        for (Token t : Token.values()) {
            sb.append(t).append("=").append(board.getAvailableTokens().getOrDefault(t, 0)).append(" ");
        }
        sb.append("\n");

        // Market — highest level first (most valuable)
        sb.append("\n=== MARKET ===\n");
        for (int level = 3; level >= 1; level--) {
            sb.append("Level ").append(level)
              .append(" (").append(board.getDeckRemainingCount(level)).append(" remaining in deck):\n");
            Card[] row = board.getVisibleCards(level);
            for (int slot = 0; slot < row.length; slot++) {
                Card c = row[slot];
                if (c == null) { sb.append("  slot ").append(slot).append(": [empty]\n"); continue; }
                sb.append("  slot ").append(slot).append(": ").append(cardText(c));
                if (p.canAffordCard(c)) sb.append(" [YOU CAN BUY THIS]");
                sb.append("\n");
            }
        }

        // Nobles
        sb.append("\n=== NOBLES ===\n");
        for (Noble n : board.getNobles()) {
            sb.append("  ").append(n.getName()).append(" (").append(n.getPrestigePoints())
              .append(" pts) requires: ").append(n.getCost()).append("\n");
        }

        return sb.toString();
    }

    private static String cardText(Card c) {
        StringBuilder sb = new StringBuilder();
        sb.append("L").append(c.getLevel()).append(" ")
          .append(c.getPrestigePoints()).append("pts bonus:").append(c.getBonus())
          .append(" cost:[");
        boolean first = true;
        for (Map.Entry<Token, Integer> e : c.getCost().entrySet()) {
            if (e.getValue() > 0) {
                if (!first) sb.append(",");
                sb.append(e.getKey()).append("=").append(e.getValue());
                first = false;
            }
        }
        return sb.append("]").toString();
    }

    // -----------------------------------------------------------------------
    // OpenAI API call
    // -----------------------------------------------------------------------

    private static String callOpenAI(String boardState) throws Exception {
        String body = "{"
                + "\"model\":" + jsonStr(MODEL) + ","
                + "\"max_tokens\":60,"
                + "\"messages\":["
                +   "{\"role\":\"system\",\"content\":" + jsonStr(buildSystemPrompt()) + "},"
                +   "{\"role\":\"user\",\"content\":" + jsonStr(boardState) + "}"
                + "]}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());

        return parseOpenAIResponse(resp.body());
    }

    private static String buildSystemPrompt() {
        return "You are an expert Splendor player. Respond with EXACTLY ONE move command — "
             + "no explanation, no punctuation before or after, just the command string.\n\n"
             + "Valid commands:\n"
             + "  1:C1:C2:C3  — take 1 each of 3 different gem colors (e.g. 1:GREEN:BLUE:RED)\n"
             + "  2:C         — take 2 of same color (board needs 4+) (e.g. 2:WHITE)\n"
             + "  3:LV:SLOT   — buy visible card at level LV (1-3), slot SLOT (0-3) (e.g. 3:2:1)\n"
             + "  3:r:IDX     — buy reserved card at index IDX (0-2) (e.g. 3:r:0)\n"
             + "  4:LV:SLOT   — reserve visible card (e.g. 4:3:0)\n"
             + "  4:deck:LV   — reserve top of deck LV (1-3) (e.g. 4:deck:3)\n"
             + "Colors: GREEN WHITE BLUE BLACK RED\n\n"
             + "Strategy priorities:\n"
             + "1. Buy affordable cards with the most prestige points\n"
             + "2. Build a discount engine — buy cards that give bonuses you need for expensive future cards\n"
             + "3. Target nobles (3 pts each) — they require specific bonus combinations\n"
             + "4. In endgame (anyone near 15 pts), maximize prestige per turn\n"
             + "5. Block opponents near winning by reserving their best affordable card\n"
             + "6. Take gems you are most short on for your closest target cards";
    }

    /**
     * Extracts the assistant's message content from an OpenAI chat-completions response.
     * Finds "choices" in the JSON, then the first "content" field after it.
     */
    private static String parseOpenAIResponse(String json) {
        int choicesIdx = json.indexOf("\"choices\"");
        if (choicesIdx < 0) return null;
        int contentIdx = json.indexOf("\"content\":", choicesIdx);
        if (contentIdx < 0) return null;
        int i = contentIdx + 10; // skip past "content":
        while (i < json.length() && json.charAt(i) == ' ') i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char ch = json.charAt(i);
            if (ch == '\\' && i + 1 < json.length()) {
                char nx = json.charAt(i + 1);
                switch (nx) {
                    case '"'  -> { sb.append('"');  i += 2; }
                    case 'n'  -> { sb.append('\n'); i += 2; }
                    case 'r'  -> { sb.append('\r'); i += 2; }
                    case 't'  -> { sb.append('\t'); i += 2; }
                    case '\\' -> { sb.append('\\'); i += 2; }
                    default   -> { sb.append(ch);   i++;    }
                }
            } else if (ch == '"') {
                break;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // JSON / command helpers
    // -----------------------------------------------------------------------

    private static String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t")
             + "\"";
    }

    /**
     * Scans each line of the model's response for a valid command pattern.
     * Strips backticks/quotes and handles uppercase variants gracefully.
     */
    static String extractCmd(String raw) {
        if (raw == null) return null;
        for (String line : raw.split("[\\n\\r]+")) {
            String s = line.trim().replaceAll("[`'\"]", "");
            if (s.matches("1:[A-Z]+:[A-Z]+:[A-Z]+")) return s;
            if (s.matches("2:[A-Z]+"))               return s;
            if (s.matches("3:[123]:[0123]"))          return s;
            if (s.matches("3:r:[012]"))               return s;
            if (s.matches("4:[123]:[0123]"))          return s;
            if (s.matches("4:deck:[123]"))            return s;
            // case-insensitive fallback
            String u = s.toUpperCase();
            if (u.matches("1:[A-Z]+:[A-Z]+:[A-Z]+")) return u;
            if (u.matches("2:[A-Z]+"))                return u;
            if (u.matches("3:[123]:[0123]"))          return u;
            if (u.matches("3:R:[012]"))               return u.replace("3:R:", "3:r:");
            if (u.matches("4:[123]:[0123]"))          return u;
            if (u.matches("4:DECK:[123]"))            return u.replace("4:DECK:", "4:deck:");
        }
        return null;
    }
}
