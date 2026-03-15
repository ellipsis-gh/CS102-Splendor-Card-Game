package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Game class manages the overall game state, turn logic, and move validation.
 * Handles:
 * - Game initialization with board, decks, nobles, and players
 * - Turn management (current player, next turn)
 * - Move validation (take 3 different gems, take 2 same gems)
 * - Applying moves and updating game state
 */
public class Game {
    private final Board board;
    private final List<Player> players;
    private int currentPlayerIndex;
    private boolean gameOver;
    private Player winner;

    /**
     * Constructor to initialize a new game.
     *
     * @param board   The game board with tokens, decks, and nobles
     * @param players List of players participating in the game
     */
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

    /**
     * Gets the current player whose turn it is.
     *
     * @return The current player
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /**
     * Gets all players in the game.
     *
     * @return List of all players
     */
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    /**
     * Gets the game board.
     *
     * @return The game board
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Checks if the game is over.
     *
     * @return true if game is over, false otherwise
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Gets the winner of the game (null if game is not over or tied).
     *
     * @return The winning player, or null if no winner yet
     */
    public Player getWinner() {
        return winner;
    }

    /**
     * Validates if a player can take 3 different gem tokens.
     * Rules:
     * - Must take exactly 3 different tokens (non-gold)
     * - All tokens must be available on the board
     * - Player must not exceed 10 tokens total after taking
     * - Cannot take gold tokens in this action
     *
     * @param player The player attempting the move
     * @param token1 First token type
     * @param token2 Second token type
     * @param token3 Third token type
     * @return true if the move is valid, false otherwise
     */
    public boolean canTakeThreeDifferentGems(Player player, Token token1, Token token2, Token token3) {
        // Check if it's the player's turn
        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        // Check if game is over
        if (gameOver) {
            return false;
        }

        // All tokens must be different
        if (token1 == token2 || token1 == token3 || token2 == token3) {
            return false;
        }

        // Cannot take gold tokens
        if (token1 == Token.GOLD || token2 == Token.GOLD || token3 == Token.GOLD) {
            return false;
        }

        // Check if tokens are available on board
        if (board.getAvailableTokens().getOrDefault(token1, 0) < 1 ||
            board.getAvailableTokens().getOrDefault(token2, 0) < 1 ||
            board.getAvailableTokens().getOrDefault(token3, 0) < 1) {
            return false;
        }

        // Check if player would exceed 10 tokens total
        int currentTotal = player.getTotalTokenCount();
        if (currentTotal + 3 > 10) {
            return false;
        }

        return true;
    }

    /**
     * Validates if a player can take 2 of the same gem token.
     * Rules:
     * - Must take exactly 2 of the same token (non-gold)
     * - At least 4 of that token must be available on the board
     * - Player must not exceed 10 tokens total after taking
     * - Cannot take gold tokens in this action
     *
     * @param player The player attempting the move
     * @param token  The token type to take 2 of
     * @return true if the move is valid, false otherwise
     */
    public boolean canTakeTwoSameGems(Player player, Token token) {
        // Check if it's the player's turn
        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        // Check if game is over
        if (gameOver) {
            return false;
        }

        // Cannot take gold tokens
        if (token == Token.GOLD) {
            return false;
        }

        // Check if at least 4 tokens are available (required for taking 2)
        if (!board.canTakeTwo(token)) {
            return false;
        }

        // Check if player would exceed 10 tokens total
        int currentTotal = player.getTotalTokenCount();
        if (currentTotal + 2 > 10) {
            return false;
        }

        return true;
    }

    /**
     * Applies the move of taking 3 different gem tokens.
     * This method assumes the move has been validated.
     *
     * @param player The player taking the tokens
     * @param token1 First token type
     * @param token2 Second token type
     * @param token3 Third token type
     * @throws IllegalArgumentException if the move is invalid
     */
    public void takeThreeDifferentGems(Player player, Token token1, Token token2, Token token3) {
        if (!canTakeThreeDifferentGems(player, token1, token2, token3)) {
            throw new IllegalArgumentException("Invalid move: cannot take 3 different gems");
        }

        // Remove tokens from board
        board.removeToken(token1, 1);
        board.removeToken(token2, 1);
        board.removeToken(token3, 1);

        // Add tokens to player
        player.addTokens(token1, 1);
        player.addTokens(token2, 1);
        player.addTokens(token3, 1);
    }

    /**
     * Applies the move of taking 2 of the same gem token.
     * This method assumes the move has been validated.
     *
     * @param player The player taking the tokens
     * @param token  The token type to take 2 of
     * @throws IllegalArgumentException if the move is invalid
     */
    public void takeTwoSameGems(Player player, Token token) {
        if (!canTakeTwoSameGems(player, token)) {
            throw new IllegalArgumentException("Invalid move: cannot take 2 same gems");
        }

        // Remove tokens from board
        board.removeToken(token, 2);

        // Add tokens to player
        player.addTokens(token, 2);
    }

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

public void buyVisibleCard(Player player, int level, int slot) {
    if (!canBuyVisibleCard(player, level, slot)) {
        throw new IllegalArgumentException("Invalid move: cannot buy visible card");
    }

    Card card = board.takeCard(level, slot);
    board.refillMarket();

    player.payForCard(card, board);
    player.buyCard(card);
}

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

public void buyReservedCard(Player player, int reservedIndex) {
    if (!canBuyReservedCard(player, reservedIndex)) {
        throw new IllegalArgumentException("Invalid move: cannot buy reserved card");
    }

    Card card = player.getHand().get(reservedIndex);

    player.payForCard(card, board);
    player.buyCard(card);
}

public boolean canReserveVisibleCard(Player player, int level, int slot) {
    if (!player.equals(getCurrentPlayer())) {
        return false;
    }

    if (gameOver) {
        return false;
    }

    if (player.getHand().size() >= 3) {
        return false;
    }

    if (level < 1 || level > 3 || slot < 0 || slot >= 4) {
        return false;
    }

    Card[] row = board.getVisibleCards(level);
    return row[slot] != null;
}

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


public boolean mustReturnTokens(Player player) {
    return player.getTotalTokenCount() > 10;
}

public int getNumTokensToReturn(Player player) {
    return Math.max(0, player.getTotalTokenCount() - 10);
}

public void returnToken(Player player, Token token, int count) {
    if (count <= 0) {
        throw new IllegalArgumentException("Return count must be positive");
    }

    player.removeTokens(token, count);
    board.addToken(token, count);
}

public Noble endTurnAndCheckNoble() {
    Player current = getCurrentPlayer();
    Noble noble = checkAndAwardNoble(current);
    nextTurn();
    return noble;
}
    /**
     * Moves to the next player's turn.
     * Checks for game end conditions after each turn.
     */
    public void nextTurn() {
        // Check for game end condition
        checkGameEnd();

        if (!gameOver) {
            // Move to next player
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
    }

    /**
     * Checks if the game should end.
     * Game ends when:
     * - A player reaches 15 or more prestige points
     * After the round completes, the player with the highest prestige wins.
     * In case of a tie, the player with fewer purchased cards wins.
     */
    private void checkGameEnd() {
        // Check if any player has reached 15+ prestige points
        for (Player player : players) {
            if (player.getScore() >= 15) {
                gameOver = true;
                // Don't set winner yet - need to finish the round
                return;
            }
        }
    }

    /**
     * Determines the winner after the game ends.
     * Should be called after the round completes when a player reaches 15+ points.
     * Winner is the player with:
     * 1. Highest prestige points
     * 2. If tied, fewer purchased cards
     *
     * @return The winning player
     */
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
                // Tie-breaker: fewer cards wins
                if (cardCount < fewestCards) {
                    bestPlayer = player;
                    fewestCards = cardCount;
                }
            }
        }

        winner = bestPlayer;
        return winner;
    }

    /**
     * Gets the current player index (0-based).
     *
     * @return Current player index
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    /**
     * Gets a list of legal moves for the current player.
     * This is a helper method that can be used by AI or UI to show available moves.
     * Note: This returns basic move types. Full validation should be done before applying moves.
     *
     * @return List of move type descriptions (simplified for now)
     */
    public List<String> getLegalMoves() {
        List<String> moves = new ArrayList<>();
        Player current = getCurrentPlayer();

        // Check take 3 different gems
        // This is simplified - full implementation would check all combinations
        moves.add("TAKE_3_DIFFERENT");

        // Check take 2 same gems for each token type
        for (Token token : Token.values()) {
            if (token != Token.GOLD && canTakeTwoSameGems(current, token)) {
                moves.add("TAKE_2_SAME_" + token);
            }
        }

        // Note: Buy card and reserve card moves would be added by other team members
        // (Tasks F - validate move: buy card, reserve card)

        return moves;
    }

    /**
     * Forces the game to end (useful for testing or manual termination).
     */
    public void endGame() {
        this.gameOver = true;
    }

    
}

