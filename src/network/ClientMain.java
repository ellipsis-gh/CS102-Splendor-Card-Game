package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


// it asks for the server IP and port
// it connects to the game server
// it listens for messages from the server and prints them
// it reads what the player types
// it sends those typed commands to the server
// it stops when the player types QUIT or the connection closes

public class ClientMain {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String host;
        int port = 5000;

        if (args.length > 0 && !args[0].isBlank()) {
            host = args[0].trim();
            System.out.println("Connecting to " + host + ":" + port + "...");
        } else {
            System.out.print("Enter server IP: ");
            host = sc.nextLine().trim();
        }

        try (
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("Connected to server.");

            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });

            readerThread.setDaemon(true);
            readerThread.start();

            while (true) {
                String input = sc.nextLine();
                out.println(input);

                if ("QUIT".equalsIgnoreCase(input.trim())) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Could not connect: " + e.getMessage());
        }

        sc.close();
    }
}