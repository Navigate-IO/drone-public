package com.drone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleHttpServer {

    private static int PORT = 80;
    private static boolean running = true;

    public static void main(String[] args) {
        if (args.length > 0) {
            PORT = Integer.parseInt(args[0]);
        }
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress("0.0.0.0", PORT));
            System.out.println("Server started on port " + PORT);

            // Start a separate thread to listen for user input
            Thread inputThread = new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine(); // Wait for Enter key press
                running = false;
                System.out.println("Stopping server...");
                scanner.close();
            });
            inputThread.start();

            while (running) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }

            executor.shutdown();
            try {
                inputThread.join();
            } catch (InterruptedException e) {
                System.err.println("Input thread interrupted: " + e.getMessage());
            }
            System.out.println("Server stopped.");

        } catch (IOException e) {
            System.err.println("Error starting or running server: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Read the request line
            String requestLine = in.readLine();
            while(requestLine != null) {
                System.out.println("Received request: " + requestLine);
                requestLine = in.readLine();
            }
            String responseContent = "{\\\"ccmd\\\":\\\"hi\\\",\\\"fox\\\":1,\\\"fip\\\":\\\"192.168.1.31\\\",\\\"fts\\\":946893746,\\\"fbv\\\":\\\"F\\\",\\\"fcnt\\\":77,\\\"ftso\\\":946892985,\\\"ftsr\\\":946893745,\\\"wrxs\\\":-26,\\\"lat\\\":64.83,\\\"lon\\\":-147.77,\\\"elev\\\":137}";
            int size = responseContent.length();
            // Simple response
            String response = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/json\r\n"
                + "Content-Length:" + size + "\r\n"
                + responseContent;

            System.out.println("Sending back response: " + response);

            out.println(response);

//            if (requestLine != null) {
//                System.out.println("Received request: " + requestLine);
//
//                // Simple response
//                String response = "HTTP/1.1 200 OK\r\n"
//                    + "Content-Type: text/plain\r\n"
//                    + "\r\n"
//                    + "Hello, World!";
//
//                out.println(response);
//
//
//                // Example of Json response
//                /**
//                HTTP/1.1 200 OK
//                Content-Type: application/json
//                Content-Length: 19
//
//                {"success":"true"}
//                 */
//            }

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}