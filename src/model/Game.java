package model;

import java.util.ArrayList;
import java.util.List;

// manages the overall game state — turns, move validation, and win conditions
public class Game {
    private final Board board;
    private final List<Player> players;
    private int currentPlayerIndex;
    private boolean gameOver;
    private Player winner;

    // set up a new game with a board and a list of players
    public Game(Board board, List<Player> players) {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Game must have at least one player");
        }
        this.board = board;
        this.players = new ArrayList<>(players);
        this.currentPlayerIndex = 0;
        this.gameOver = false;
        this.winner = null;
    }

    // whose turn is it right now?
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    // returns a copy so the caller can't mess with the internal list
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public Board getBoard() {
        return board;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    // null if the game hasn't ended yet
    public Player getWinner() {
        return winner;
    }

    // can this player take 3 different non-gold gems?
    // needs: all 3 different, all available on board, no gold, player stays under 10 tokens
    public boolean canTakeThreeDifferentGems(Player player, Token token1, Token token2, Token token3) {
        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        if (gameOver) {
            return false;
        }

        // all three must be different colors
        if (token1 == token2 || token1 == token3 || token2 == token3) {
            return false;
        }

        // can't take gold this way
        if (token1 == Token.GOLD || token2 == Token.GOLD || token3 == Token.GOLD) {
            return false;
        }

        // all must be available on the board
        if (board.getAvailableTokens().getOrDefault(token1, 0) < 1 ||
            board.getAvailableTokens().getOrDefault(token2, 0) < 1 ||
            board.getAvailableTokens().getOrDefault(token3, 0) < 1) {
            return false;
        }

        // player can't go over 10 tokens total
        int currentTotal = player.getTotalTokenCount();
        if (currentTotal + 3 > 10) {
            return false;
        }

        return true;
    }

    // can this player take 2 of the same gem?
    // needs: not gold, at least 4 of that color on board, player stays under 10
    public boolean canTakeTwoSameGems(Player player, Token token) {
        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        if (gameOver) {
            return false;
        }

        if (token == Token.GOLD) {
            return false;
        }

        // board rule: need 4+ of that color to allow taking 2
        if (!board.canTakeTwo(token)) {
            return false;
        }

        int currentTotal = player.getTotalTokenCount();
        if (currentTotal + 2 > 10) {
            return false;
        }

        return true;
    }

    // take 3 different gems — assumes canTakeThreeDifferentGems already returned true
    public void takeThreeDifferentGems(Player player, Token token1, Token token2, Token token3) {
        if (!canTakeThreeDifferentGems(player, token1, token2, token3)) {
            throw new IllegalArgumentException("Invalid move: cannot take 3 different gems");
        }

        // remove from board, give to player
        board.removeToken(token1, 1);
        board.removeToken(token2, 1);
        board.removeToken(token3, 1);

        player.addTokens(token1, 1);
        player.addTokens(token2, 1);
        player.addTokens(token3, 1);
    }

    // take 2 of the same gem — assumes canTakeTwoSameGems already returned true
    public void takeTwoSameGems(Player player, Token token) {
        if (!canTakeTwoSameGems(player, token)) {
            throw new IllegalArgumentException("Invalid move: cannot take 2 same gems");
        }

        board.removeToken(token, 2);
        player.addTokens(token, 2);
    }

    // is the card at this level/slot available and can the player afford it?
    public boolean canBuyVisibleCard(Player player, int level, int slot) {
        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        if (gameOver) {
            return false;
        }

        if (level < 1 || level > 3 || slot < 0 || slot >= 4) {
            return false;
        }

        Card[] row = board.getVisibleCards(level);
        Card card = row[slot];

        if (card == null) {
            return false;
        }

        return player.canAffordCard(card);
    }

    // buy a face-up card from the market, then refill the empty slot
    public void buyVisibleCard(Player player, int level, int slot) {
        if (!canBuyVisibleCard(player, level, slot)) {
            throw new IllegalArgumentException("Invalid move: cannot buy visible card");
        }

        Card card = board.takeCard(level, slot);
        board.refillMarket();

        player.payForCard(card, board);
        player.buyCard(card);
    }

    // can the player afford one of their reserved cards?
    public boolean canBuyReservedCard(Player player, int reservedIndex) {
        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        if (gameOver) {
            return false;
        }

        if (reservedIndex < 0 || reservedIndex >= player.getHand().size()) {
            return false;
        }

        Card card = player.getHand().get(reservedIndex);
        return player.canAffordCard(card);
    }

    // buy one of the player's reserved cards
    public void buyReservedCard(Player player, int reservedIndex) {
        if (!canBuyReservedCard(player, reservedIndex)) {
            throw new IllegalArgumentException("Invalid move: cannot buy reserved card");
        }

        Card card = player.getHand().get(reservedIndex);

        player.payForCard(card, board);
        player.buyCard(card);
    }

    // can the player reserve a visible card? needs room in hand and the slot isn't empty
    public boolean canReserveVisibleCard(Player player, int level, int slot) {
        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        if (gameOver) {
            return false;
        }

        if (player.getHand().size() >= 3) {
            return false; // hand is full
        }

        if (level < 1 || level > 3 || slot < 0 || slot >= 4) {
            return false;
        }

        Card[] row = board.getVisibleCards(level);
        return row[slot] != null;
    }

    // reserve a visible card and hand the player a gold token if one is available
    public void reserveVisibleCard(Player player, int level, int slot) {
        if (!canReserveVisibleCard(player, level, slot)) {
            throw new IllegalArgumentException("Invalid move: cannot reserve visible card");
        }

        Card card = board.takeCard(level, slot);
        board.refillMarket();

        player.reserveCard(card);

        if (board.getAvailableTokens().getOrDefault(Token.GOLD, 0) > 0) {
            player.addTokens(Token.GOLD, 1);
            board.removeToken(Token.GOLD, 1);
        }
    }

    // can the player blind-reserve from the top of a deck?
    public boolean canReserveDeckCard(Player player, int level) {
        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        if (gameOver) {
            return false;
        }

        if (player.getHand().size() >= 3) {
            return false;
        }

        if (level < 1 || level > 3) {
            return false;
        }

        return board.deckHasCards(level);
    }

    // reserve the top card from a deck (face down) and give gold if available
    public void reserveDeckCard(Player player, int level) {
        if (!canReserveDeckCard(player, level)) {
            throw new IllegalArgumentException("Invalid move: cannot reserve deck card");
        }

        Card card = board.drawFromDeck(level);
        player.reserveCard(card);

        if (board.getAvailableTokens().getOrDefault(Token.GOLD, 0) > 0) {
            player.addTokens(Token.GOLD, 1);
            board.removeToken(Token.GOLD, 1);
        }
    }

    // check if any noble wants to visit after a player's turn and award them if so
    public Noble checkAndAwardNoble(Player player) {
        for (Noble noble : new ArrayList<>(board.getNobles())) {
            if (player.canGetNoble(noble)) {
                player.receiveNoble(noble);
                board.removeNoble(noble);
                return noble;
            }
        }
        return null;
    }

    // does this player need to return tokens? (over the 10-token limit)
    public boolean mustReturnTokens(Player player) {
        return player.getTotalTokenCount() > 10;
    }

    // how many tokens does this player need to give back?
    public int getNumTokensToReturn(Player player) {
        return Math.max(0, player.getTotalTokenCount() - 10);
    }

    // player returns tokens to the board
    public void returnToken(Player player, Token token, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Return count must be positive");
        }

        player.removeTokens(token, count);
        board.addToken(token, count);
    }

    // end the current turn: check for a noble visit, then advance to the next player
    public Noble endTurnAndCheckNoble() {
        Player current = getCurrentPlayer();
        Noble noble = checkAndAwardNoble(current);
        nextTurn();
        return noble;
    }

    // advance to the next player — also checks if the game just ended
    public void nextTurn() {
        checkGameEnd();

        if (!gameOver) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
    }

    // game ends when someone hits 15 points — winner is determined after the round finishes
    private void checkGameEnd() {
        for (Player player : players) {
            if (player.getScore() >= 15) {
                gameOver = true;
                return; // don't set winner yet — need to finish the round first
            }
        }
    }

    // figure out who won — highest score wins, fewer purchased cards breaks ties
    public Player determineWinner() {
        if (!gameOver) {
            return null;
        }

        Player bestPlayer = null;
        int bestScore = -1;
        int fewestCards = Integer.MAX_VALUE;

        for (Player player : players) {
            int score = player.getScore();
            int cardCount = player.getPurchasedCards().size();

            if (score > bestScore) {
                bestPlayer = player;
                bestScore = score;
                fewestCards = cardCount;
            } else if (score == bestScore) {
                // tie-breaker: fewer purchased cards wins
                if (cardCount < fewestCards) {
                    bestPlayer = player;
                    fewestCards = cardCount;
                }
            }
        }

        winner = bestPlayer;
        return winner;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    // returns a rough list of available move types — used by AI and UI
    public List<String> getLegalMoves() {
        List<String> moves = new ArrayList<>();
        Player current = getCurrentPlayer();

        moves.add("TAKE_3_DIFFERENT");

        // check which colors can be taken as 2 of the same
        for (Token token : Token.values()) {
            if (token != Token.GOLD && canTakeTwoSameGems(current, token)) {
                moves.add("TAKE_2_SAME_" + token);
            }
        }

        return moves;
    }

    // force the game to end — mainly useful for testing
    public void endGame() {
        this.gameOver = true;
    }
}
