package com.chatapp.server;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Main server class for the chat application.
 * Handles incoming client connections and manages message broadcasting.
 */
public class ChatServer {
    // Shared list of all connected client handlers
    private static List<ClientHandler> clients = new ArrayList<>();

    // Server port configuration with environment variable support
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "5000"));


     //Continuously accepts new client connections and creates handlers for them.
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        System.out.println("Server started on port " + SERVER_PORT + ". Waiting for clients...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket);

            // Create and start a new handler thread for the client
            ClientHandler clientThread = new ClientHandler(clientSocket, clients);
            clients.add(clientThread);
            new Thread(clientThread).start();
        }
    }
}

/**
 * Handles individual client connections and message broadcasting.
 * Each instance runs in its own thread and manages communication with one client.
 */
class ClientHandler implements Runnable {
    private Socket clientSocket;
    private List<ClientHandler> clients;
    private PrintWriter out;
    private BufferedReader in;

    /**
     * Creates a new client handler.
     * @param socket The client's socket connection
     * @param clients Shared list of all connected clients for broadcasting
     * @throws IOException if stream initialization fails
     */
    public ClientHandler(Socket socket, List<ClientHandler> clients) throws IOException {
        this.clientSocket = socket;
        this.clients = clients;
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    /**
     * Main client handling loop.
     * Reads incoming messages and broadcasts them to all connected clients.
     * Handles client disconnection and cleanup.
     */
    public void run() {
        try {
            String inputLine;
            // Continue reading messages until client disconnects or thread is interrupted
            while (!Thread.currentThread().isInterrupted() && (inputLine = in.readLine()) != null) {
                // Synchronize access to clients list during broadcasting
                synchronized(clients) {
                    // Broadcast message to all connected clients
                    for (ClientHandler aClient : clients) {
                        aClient.out.println(inputLine);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            // Clean up resources when client disconnects
            try {
                clients.remove(this);
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}