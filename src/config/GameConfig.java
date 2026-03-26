package config;

import java.io.*;
import java.util.*;

public class GameConfig {

    //file name 
    private static final String CONFIG_FILE = "src/config/config.properties";

    //function to read and retrieve winning points from config.properties
    public static int getWinningPoints() {
        Properties props = new Properties();

        InputStream input = null;

    try {

            input = new FileInputStream(CONFIG_FILE);

        // load a properties file
            props.load(input);

        //get winning points, then turn into an integer to return
            return Integer.parseInt(props.getProperty("winning.points"));

        } catch (NumberFormatException e){
            System.out.println("Wrong formatting of points");
            return 15;// use default values
        } catch (IOException e) {
            System.out.println("Could not load config file, using default.");
            return 15; // fallback to default
        }
    }

    public static int getInitialGems(int playerCount) {
        Properties props = new Properties();

        InputStream input = null;

        try {
            input = new FileInputStream(CONFIG_FILE);
            props.load(input);

            //current key structure requires player count
            String key = "gems." + playerCount;


            //getProperty will scan config file until the corresponding key is found
            //then returning the string that follows
            return Integer.parseInt(props.getProperty(key));

        } catch (FileNotFoundException e) {
            //print stack trace of the error
            e.printStackTrace();

        } catch (IOException e){
            //same as above
            e.printStackTrace();
        } 

        System.out.println("Error detected, using default 2 player settings");
        return Integer.parseInt(props.getProperty("gems.2"));
    }


    public static int getInitialNobles(int playerCount) {
        Properties props = new Properties();

        InputStream input = null;

        try {
            input = new FileInputStream(CONFIG_FILE);
            props.load(input);

            //current key structure requires player count
            String key = "noblesCount." + playerCount;

            //getProperty will scan config file until the corresponding key is found
            //then returning the string that follows
            return Integer.parseInt(props.getProperty(key));

        } catch (FileNotFoundException e) {
            //print stack trace of the error
            e.printStackTrace();

        } catch (IOException e){
            //same as above
            e.printStackTrace();
        } 

        System.out.println("Error detected, using default 2 player settings");
        return Integer.parseInt(props.getProperty("noblesCount.2"));
    }



    public static String getCardFilePath(){
        Properties props = new Properties();

        InputStream input = null;

        try {
            input = new FileInputStream(CONFIG_FILE);
            props.load(input);

            return props.getProperty("deck.filePath");


        } catch (IOException e) {
            throw new RuntimeException("Config error. Could not load Splendor Cards.csv");

        }
    }

    public static String getNobleFilePath(){
        Properties props = new Properties();

        InputStream input = null;

        try {
            input = new FileInputStream(CONFIG_FILE);
            props.load(input);

            return props.getProperty("nobles.filePath");

        } catch (IOException e) {
            throw new RuntimeException("Config error. Could not load Nobles.csv");

        }
    }

    
}