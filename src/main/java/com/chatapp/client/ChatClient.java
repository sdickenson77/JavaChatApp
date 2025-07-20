package com.chatapp.client;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;


public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Consumer<String> onMessageReceived;

    // Server configuration with environment variable support for flexibility
    private static final String SERVER_HOST = System.getenv().getOrDefault("CHAT_SERVER_HOST", "localhost");
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("CHAT_SERVER_PORT", "5000"));

    /**
     * Initializes the connection to the chat server.
     * @throws IOException if connection fails
     */
    public void startConnection() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Creates a new chat client with specified server details and message handler.
     * @param serverAddress The server's IP address or hostname
     * @param serverPort The server's port number
     * @param onMessageReceived Callback for handling received messages
     * @throws IOException if connection fails
     */
    public ChatClient(String serverAddress, int serverPort, Consumer<String> onMessageReceived) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.onMessageReceived = onMessageReceived;
    }

    /**
     * Sends a message to the chat server.
     * @param msg The message to send
     */
    public void sendMessage(String msg) {
        out.println(msg);
    }

    /**
     * Starts a background thread to continuously listen for incoming messages.
     * Received messages are passed to the onMessageReceived callback.
     */
    public void startClient() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    onMessageReceived.accept(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}