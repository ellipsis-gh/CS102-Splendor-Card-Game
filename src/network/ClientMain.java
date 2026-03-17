package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String host;
        int port = 5000;

        if (args.length > 0 && !args[0].isBlank()) {
            host = args[0].trim();
            if (args.length > 1 && !args[1].isBlank()) {
                try {
                    port = Integer.parseInt(args[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
            System.out.println("Connecting to " + host + ":" + port + "...");
        } else {
            System.out.print("Enter server IP: ");
            host = sc.nextLine().trim();
            System.out.print("Enter port (default 5000): ");
            String portText = sc.nextLine().trim();
            if (!portText.isEmpty()) {
                try {
                    port = Integer.parseInt(portText);
                } catch (NumberFormatException ignored) {
                }
            }
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