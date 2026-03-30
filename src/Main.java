
import java.util.Scanner;

import org.fusesource.jansi.AnsiConsole;

import logic.Game;
import util.GameApp;
import util.ui.ConsoleUI;



public class Main{
    
    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        // Use a single Scanner for all input and pass it to ConsoleUI so input isn't consumed by multiple scanners
            Scanner sc = new Scanner(System.in);
            ConsoleUI ui = new ConsoleUI(sc);

            // setup: get player count and which are AI
            int numPlayers = ui.getNumberOfPlayers();
            boolean[] isAI = ui.getPlayerTypes(numPlayers);

            // build game
            Game game = GameApp.setupGame(numPlayers, isAI);

            // run the main loop using the same Scanner and UI
            GameApp.runGameLoop(game, sc, ui);
        
    }

}