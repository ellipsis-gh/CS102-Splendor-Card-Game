import java.util.Scanner;

import org.fusesource.jansi.AnsiConsole;

import config.GameConfig;
import engine.GameEngine;
import engine.GameSetup;
import io.ConsoleInputHandler;
import logic.Game;
import ui.ConsoleGameRenderer;
import util.SplashScreen;
import util.ui.ConsoleUI;

/**
 * Entry point for local (single-machine) play.
 *
 * <p>Responsibilities: show splash, collect setup options, wire the
 * concrete {@link ConsoleInputHandler} and {@link ConsoleGameRenderer}
 * into a {@link GameEngine}, and call {@code run()}.</p>
 */
public class Main {

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        Scanner sc = new Scanner(System.in);

        SplashScreen.local();
        System.out.print("Press Enter to continue...");
        if (sc.hasNextLine()) sc.nextLine();

        // Setup — still uses ConsoleUI for the interactive setup screens
        ConsoleUI ui       = new ConsoleUI(sc);
        int winScore       = ui.getWinningPoints(GameConfig.getWinningPoints());
        int numPlayers     = ui.getNumberOfPlayers();
        boolean[] isAI     = ui.getPlayerTypes(numPlayers);

        // Build game + wire engine
        Game game                    = GameSetup.create(numPlayers, isAI);
        ConsoleInputHandler  input   = new ConsoleInputHandler(sc);
        ConsoleGameRenderer  renderer = new ConsoleGameRenderer();

        new GameEngine(game, input, renderer, winScore).run();
    }
}
