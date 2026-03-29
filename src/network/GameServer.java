package network;

import config.GameConfig;
import game.CardLoader;
import logic.Game;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Board;
import model.Card;
import model.Deck;
import model.Noble;
import model.Player;
import model.Token;

public class GameServer {
    private final int port;

    //retrieving points from config.properties
    private static final int WIN_SCORE = GameConfig.getWinningPoints();

    //retrieving cards file path from config.properties
    private static final String CARDS_FILEPATH = GameConfig.getCardFilePath();

    //retrieving nobles file path from config.properties
    private static final String NOBLES_FILEPATH = GameConfig.getNobleFilePath();

    public GameServer(int port) {
        this.port = port;
    }
    //printlocal addressess is to know the server ip for the client to connect to the server
    private void printLocalAddresses() {
        System.out.println("Connect using one of these IPs:");
        try {
            System.out.println("  localhost / 127.0.0.1 (this machine only)");
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : interfaces) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    String host = addr.getHostAddress();
                    if (host.contains("%")) host = host.split("%")[0];
                    if (addr.getHostAddress().indexOf(':') < 0) {
                        System.out.println("  " + host + " (port " + port + ")");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("  (could not enumerate: " + e.getMessage() + ")");
        }
    }
    // the main strtup method for the server
    // it runs the ServerSocketand client for player1
    // it waits for player 2 to connect
    // call runGame(client1, client2) once both players are ready.
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            printLocalAddresses();
            System.out.println("Clients: run run_client.bat and enter one of the IPs above");
            System.out.println("Waiting for Player 1...");

            Socket socket1 = serverSocket.accept();
            ClientHandler client1 = new ClientHandler(socket1);
            client1.send("Connected as Player 1.");
            client1.send("Enter your name:");

            String name1 = client1.readLine();
            if (name1 == null || name1.isBlank()) {
                name1 = "Player 1";
            }
            client1.setPlayerName(name1);

            System.out.println(name1 + " connected.");

            client1.send("Waiting for Player 2...");

            System.out.println("Waiting for Player 2...");
            Socket socket2 = serverSocket.accept();
            ClientHandler client2 = new ClientHandler(socket2);
            client2.send("Connected as Player 2.");
            client2.send("Enter your name:");

            String name2 = client2.readLine();
            if (name2 == null || name2.isBlank()) {
                name2 = "Player 2";
            }
            client2.setPlayerName(name2);

            System.out.println(name2 + " connected.");

            runGame(client1, client2);

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
    //it creates the actual Game object by calling createGame()
    //it ends the latest board/player state to both clients using broadcastState()

    private void runGame(ClientHandler client1, ClientHandler client2) {
        try {
            Game game = createGame(client1.getPlayerName(), client2.getPlayerName());

            client1.send("Both players connected. Starting game...");
            client2.send("Both players connected. Starting game...");

            boolean gameOver = false;

            while (!gameOver) {
                Player current = game.getCurrentPlayer();
                ClientHandler currentClient = current.getName().equals(client1.getPlayerName()) ? client1 : client2;
                ClientHandler otherClient = currentClient == client1 ? client2 : client1;

                broadcastState(game, client1, client2);

                currentClient.send("YOUR TURN");
                currentClient.send("Commands:");
                currentClient.send("TAKE3 green blue red");
                currentClient.send("TAKE2 red");
                currentClient.send("BUY 1-0");
                currentClient.send("BUYR 0");
                currentClient.send("RESERVE 1-0");
                currentClient.send("RESERVEDECK 1");
                currentClient.send("RETURN red 1   (only when over token limit)");
                currentClient.send("QUIT");

                otherClient.send("WAITING FOR " + current.getName());

                boolean validMove = false;
                while (!validMove) {
                    String command = currentClient.readLine();

                    if (command == null) {
                        otherClient.send(current.getName() + " disconnected. Game over.");
                        client1.close();
                        client2.close();
                        return;
                    }

                    command = command.trim();

                    if (command.equalsIgnoreCase("QUIT")) {
                        currentClient.send("You quit the game.");
                        otherClient.send(current.getName() + " quit the game.");
                        client1.close();
                        client2.close();
                        return;
                    }

                    validMove = handleCommand(game, current, command, currentClient);

                    if (!validMove) {
                        currentClient.send("Invalid move. Try again.");
                    }
                }

                enforceTokenLimit(game, current, currentClient, otherClient);

                if (game.checkAndAwardNoble(current) != null) {
                    currentClient.send("A noble visits you.");
                    otherClient.send(current.getName() + " received a noble.");
                }

                if (current.getScore() >= 15) {
                    broadcastState(game, client1, client2);
                    client1.send("WINNER: " + current.getName());
                    client2.send("WINNER: " + current.getName());
                    gameOver = true;
                } else {
                    game.nextTurn();
                }
            }

            client1.close();
            client2.close();

        } catch (Exception e) {
            client1.send("Server game error: " + e.getMessage());
            client2.send("Server game error: " + e.getMessage());
            client1.close();
            client2.close();
        }
    }
    // it makes sure the current player is not holding more than 10 tokens
    private void enforceTokenLimit(Game game, Player player, ClientHandler currentClient, ClientHandler otherClient)
            throws IOException {
        while (game.mustReturnTokens(player)) {
            int mustReturn = game.getNumTokensToReturn(player);
            broadcastState(game, currentClient, otherClient);
            currentClient.send("TOKEN LIMIT: You have " + player.getTotalTokenCount() + " tokens (max 10).");
            currentClient.send("Return " + mustReturn + " token(s) with: RETURN <color> <count>");
            currentClient.send("Example: RETURN red 1");
            otherClient.send("Waiting: " + player.getName() + " must return " + mustReturn + " token(s).");

            boolean ok = false;
            while (!ok) {
                String line = currentClient.readLine();
                if (line == null) {
                    otherClient.send(player.getName() + " disconnected. Game over.");
                    currentClient.close();
                    otherClient.close();
                    throw new IOException("Player disconnected");
                }

                line = line.trim();
                if (line.equalsIgnoreCase("QUIT")) {
                    currentClient.send("You quit the game.");
                    otherClient.send(player.getName() + " quit the game.");
                    currentClient.close();
                    otherClient.close();
                    throw new IOException("Player quit");
                }

                ok = handleReturnCommand(game, player, line, currentClient);
                if (!ok) {
                    currentClient.send("Invalid return. Use: RETURN <color> <count>");
                }
            }
        }
    }
    //it print the current board and players info, then sends the same state to both clients.
    private void broadcastState(Game game, ClientHandler c1, ClientHandler c2) {
        String boardText = NetworkFormatter.formatBoard(game.getBoard());
        String p1Text = NetworkFormatter.formatPlayer(game.getPlayers().get(0));
        String p2Text = NetworkFormatter.formatPlayer(game.getPlayers().get(1));

        c1.send(boardText);
        c1.send(p1Text);
        c1.send(p2Text);

        c2.send(boardText);
        c2.send(p1Text);
        c2.send(p2Text);
    }
    //it sets up a Splendor match.
    private Game createGame(String player1Name, String player2Name) throws IOException {
        // load cards from CSV
        List<Card> allCards = null;
        try {
            allCards = CardLoader.loadCards(CARDS_FILEPATH);
        } catch (IOException e) {
            System.err.println("Error: Could not load Splendor Cards.csv");
            System.err.println("Make sure the file is in the same folder as the program.");
            e.getStackTrace();
        }

        List<Card> level1 = new ArrayList<>();
        List<Card> level2 = new ArrayList<>();
        List<Card> level3 = new ArrayList<>();

        for (Card c : allCards) {
            if (c.getLevel() == 1) {
                level1.add(c);
            } else if (c.getLevel() == 2) {
                level2.add(c);
            } else {
                level3.add(c);
            }
        }

        Deck d1 = new Deck(1, level1);
        Deck d2 = new Deck(2, level2);
        Deck d3 = new Deck(3, level3);
        d1.shuffle();
        d2.shuffle();
        d3.shuffle();

        // setup board with 3 nobles for a 2-player game
        List<Noble> allNobles = new ArrayList<Noble>();

        //load nobles from CSV
        try {
            allNobles = CardLoader.loadNobles(NOBLES_FILEPATH);
        } catch (IOException e) {
            System.err.println("Error: Could not load Nobles.csv");
            System.err.println("Make sure the file is in the same folder as the program.");
            e.printStackTrace();
        }

        //pull out 3 nobles for the board
        List<Noble> nobles = new ArrayList<>();
        //shuffle the existing nobles
        Collections.shuffle(allNobles);
        for (int i = 0; i < 3; i++) {
            nobles.add(allNobles.get(i));
        }

        Board board = new Board(nobles, d1, d2, d3, 2);

        List<Player> players = new ArrayList<>();
        players.add(new Player(player1Name, true));
        players.add(new Player(player2Name, true));

        return new Game(board, players);
    }
    // it parses and executes the main turn commands entered by the player.
    private boolean handleCommand(Game game, Player p, String command, ClientHandler client) {
        String[] parts = command.split("\\s+");
        if (parts.length == 0) {
            return false;
        }

        try {
            switch (parts[0].toUpperCase()) {
                case "TAKE3":
                    if (parts.length != 4) return false;
                    Token t1 = parseToken(parts[1]);
                    Token t2 = parseToken(parts[2]);
                    Token t3 = parseToken(parts[3]);

                    if (t1 == null || t2 == null || t3 == null) return false;
                    if (!game.canTakeThreeDifferentGems(p, t1, t2, t3)) return false;

                    game.takeThreeDifferentGems(p, t1, t2, t3);
                    client.send("OK: Took " + t1 + " " + t2 + " " + t3);
                    return true;

                case "TAKE2":
                    if (parts.length != 2) return false;
                    Token t = parseToken(parts[1]);

                    if (t == null || !game.canTakeTwoSameGems(p, t)) return false;

                    game.takeTwoSameGems(p, t);
                    client.send("OK: Took 2 " + t);
                    return true;

                case "BUY":
                    if (parts.length != 2) return false;
                    String[] buyParts = parts[1].split("-");
                    if (buyParts.length != 2) return false;

                    int level = Integer.parseInt(buyParts[0]);
                    int slot = Integer.parseInt(buyParts[1]);

                    if (!game.canBuyVisibleCard(p, level, slot)) return false;

                    game.buyVisibleCard(p, level, slot);
                    client.send("OK: Bought visible card.");
                    return true;

                case "BUYR":
                    if (parts.length != 2) return false;
                    int reservedIndex = Integer.parseInt(parts[1]);

                    if (!game.canBuyReservedCard(p, reservedIndex)) return false;

                    game.buyReservedCard(p, reservedIndex);
                    client.send("OK: Bought reserved card.");
                    return true;

                case "RESERVE":
                    if (parts.length != 2) return false;
                    String[] reserveParts = parts[1].split("-");
                    if (reserveParts.length != 2) return false;

                    int reserveLevel = Integer.parseInt(reserveParts[0]);
                    int reserveSlot = Integer.parseInt(reserveParts[1]);

                    if (!game.canReserveVisibleCard(p, reserveLevel, reserveSlot)) return false;

                    game.reserveVisibleCard(p, reserveLevel, reserveSlot);
                    client.send("OK: Reserved visible card.");
                    return true;

                case "RESERVEDECK":
                    if (parts.length != 2) return false;
                    int deckLevel = Integer.parseInt(parts[1]);
                    if (!game.canReserveDeckCard(p, deckLevel)) return false;
                    game.reserveDeckCard(p, deckLevel);
                    client.send("OK: Reserved deck card.");
                    return true;

                default:
                    return false;
            }
        } catch (Exception e) {
            client.send("ERROR: " + e.getMessage());
            return false;
        }
    }
    // Similar idea to handleCommand(), but only for return command and  used when the player has too many tokens.
    private boolean handleReturnCommand(Game game, Player p, String line, ClientHandler client) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3) return false;
        if (!"RETURN".equalsIgnoreCase(parts[0])) return false;

        Token token = parseTokenAllowGold(parts[1]);
        if (token == null) return false;

        int count;
        try {
            count = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (count <= 0) return false;

        int have = p.getTokens().getOrDefault(token, 0);
        if (have < count) return false;

        int mustReturn = game.getNumTokensToReturn(p);
        if (count > mustReturn) return false;

        game.returnToken(p, token, count);
        client.send("OK: Returned " + count + " " + token);
        return true;
    }
    // it Converts a text color like "red" or "blue" into the matching Token enum.
    private Token parseToken(String s) {
        if (s == null) return null;

        switch (s.toLowerCase()) {
            case "green": return Token.GREEN;
            case "white": return Token.WHITE;
            case "blue": return Token.BLUE;
            case "black": return Token.BLACK;
            case "red": return Token.RED;
            default: return null;
        }
    }
    //Same idea as parseToken(), but to accepts "gold".
    private Token parseTokenAllowGold(String s) {
        if (s == null) return null;
        if ("gold".equalsIgnoreCase(s)) return Token.GOLD;
        return parseToken(s);
    }
}
