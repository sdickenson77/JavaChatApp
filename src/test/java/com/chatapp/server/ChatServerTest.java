package com.chatapp.server;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ChatServerTest {
    private static final int TEST_PORT = 5001;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private List<ClientHandler> clients;
    private List<Socket> testSockets;

    @BeforeEach
    void setUp() throws IOException {
        executorService = Executors.newFixedThreadPool(3);
        clients = new ArrayList<>();
        testSockets = new ArrayList<>();
        serverSocket = new ServerSocket(TEST_PORT);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Close all test sockets
        for (Socket socket : testSockets) {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

        // Clear the clients list
        clients.clear();

        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Executor service did not terminate");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @Timeout(5) // 5 second timeout
    void testClientConnection() throws IOException, InterruptedException {
        CountDownLatch connectionLatch = new CountDownLatch(1);

        executorService.submit(() -> {
            try {
                Socket clientSocket = serverSocket.accept();
                testSockets.add(clientSocket);
                assertTrue(clientSocket.isConnected());
                connectionLatch.countDown();
            } catch (IOException e) {
                fail("Server failed to accept connection: " + e.getMessage());
            }
        });

        Socket clientSocket = new Socket("localhost", TEST_PORT);
        testSockets.add(clientSocket);
        assertTrue(clientSocket.isConnected());

        assertTrue(connectionLatch.await(3, TimeUnit.SECONDS), "Connection timed out");
    }

    @Test
    @Timeout(5)
    void testMessageBroadcast() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch messageLatch = new CountDownLatch(2);

        // Start server handler
        executorService.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && messageLatch.getCount() > 0) {
                    Socket clientSocket = serverSocket.accept();
                    testSockets.add(clientSocket);
                    ClientHandler clientHandler = new ClientHandler(clientSocket, clients);
                    clients.add(clientHandler);
                    executorService.submit(clientHandler);
                }
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    fail("Server failed: " + e.getMessage());
                }
            }
        });

        // Create two test clients
        Socket client1 = new Socket("localhost", TEST_PORT);
        Socket client2 = new Socket("localhost", TEST_PORT);
        testSockets.add(client1);
        testSockets.add(client2);

        // Wait for clients to be properly connected
        TimeUnit.MILLISECONDS.sleep(500);

        PrintWriter client1Writer = new PrintWriter(client1.getOutputStream(), true);
        BufferedReader client1Reader = new BufferedReader(new InputStreamReader(client1.getInputStream()));
        BufferedReader client2Reader = new BufferedReader(new InputStreamReader(client2.getInputStream()));

        String testMessage = "Hello, everyone!";
        client1Writer.println(testMessage);

        CompletableFuture<String> client1Future = CompletableFuture.supplyAsync(() -> {
            try {
                String message = client1Reader.readLine();
                messageLatch.countDown();
                return message;
            } catch (IOException e) {
                return null;
            }
        });

        CompletableFuture<String> client2Future = CompletableFuture.supplyAsync(() -> {
            try {
                String message = client2Reader.readLine();
                messageLatch.countDown();
                return message;
            } catch (IOException e) {
                return null;
            }
        });

        assertTrue(messageLatch.await(3, TimeUnit.SECONDS), "Message broadcast timed out");

        assertEquals(testMessage, client1Future.get(1, TimeUnit.SECONDS));
        assertEquals(testMessage, client2Future.get(1, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(5)
    void testClientDisconnection() throws IOException, InterruptedException {
        CountDownLatch connectionLatch = new CountDownLatch(1);

        executorService.submit(() -> {
            try {
                Socket clientSocket = serverSocket.accept();
                testSockets.add(clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket, clients);
                clients.add(clientHandler);
                executorService.submit(clientHandler);
                connectionLatch.countDown();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    fail("Server failed: " + e.getMessage());
                }
            }
        });

        Socket clientSocket = new Socket("localhost", TEST_PORT);
        testSockets.add(clientSocket);
        assertTrue(connectionLatch.await(3, TimeUnit.SECONDS), "Connection timed out");
        assertTrue(clientSocket.isConnected());

        clientSocket.close();
        assertTrue(clientSocket.isClosed());
    }

    @Test
    @Timeout(5)
    void testServerStartup() {
        assertDoesNotThrow(() -> {
            ServerSocket testServer = new ServerSocket(TEST_PORT + 1);
            assertNotNull(testServer);
            assertTrue(testServer.isBound());
            testServer.close();
        });
    }
}