import java.util.Scanner;

import org.fusesource.jansi.AnsiConsole;

import config.GameConfig;
import engine.GameEngine;
import engine.GameSetup;
import io.ConsoleInputHandler;
import logic.Game;
import model.Difficulty;
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

        // For each AI player, ask what difficulty level they should be
        Difficulty[] difficulties = new Difficulty[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            if (isAI[i]) difficulties[i] = ui.getAIDifficulty(i + 1);
        }

        // Build game + wire engine
        Game game                    = GameSetup.create(numPlayers, isAI, null, difficulties);
        ConsoleInputHandler  input   = new ConsoleInputHandler(sc);
        ConsoleGameRenderer  renderer = new ConsoleGameRenderer();

        new GameEngine(game, input, renderer, winScore).run();
    }
}
