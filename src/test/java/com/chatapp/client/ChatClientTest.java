package com.chatapp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatClientTest {
    private ServerSocket serverSocket;
    private ChatClient chatClient;
    private Socket serverSideSocket;
    private final int PORT = 5001;

    @BeforeEach
    void setUp() throws IOException {
        serverSocket = new ServerSocket(PORT);

        // Start a thread to accept client connection
        new Thread(() -> {
            try {
                serverSideSocket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (chatClient != null) {
            // Close client resources
        }
        if (serverSideSocket != null) {
            serverSideSocket.close();
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    @Test
    void testClientConnection() throws IOException {
        chatClient = new ChatClient("localhost", PORT, message -> {});
        assertNotNull(chatClient);
    }

    @Test
    void testMessageSending() throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] receivedMessage = {null};

        chatClient = new ChatClient("localhost", PORT, message -> {});

        // Start a thread to read messages on server side
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(serverSideSocket.getInputStream()));
                receivedMessage[0] = in.readLine();
                latch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Send message from client
        String testMessage = "Hello, Server!";
        chatClient.sendMessage(testMessage);

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(testMessage, receivedMessage[0]);
        } catch (InterruptedException e) {
            fail("Test timed out");
        }
    }

    @Test
    void testMessageReceiving() throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        final String testMessage = "Hello, Client!";
        final String[] receivedMessage = {null};

        chatClient = new ChatClient("localhost", PORT, message -> {
            receivedMessage[0] = message;
            latch.countDown();
        });
        chatClient.startClient();

        // Send message from server to client
        PrintWriter out = new PrintWriter(serverSideSocket.getOutputStream(), true);
        out.println(testMessage);

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(testMessage, receivedMessage[0]);
        } catch (InterruptedException e) {
            fail("Test timed out");
        }
    }
}