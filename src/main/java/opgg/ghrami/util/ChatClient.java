package opgg.ghrami.util;

import java.io.*;
import java.net.*;
import java.util.function.BiConsumer;

/**
 * TCP client for real-time messaging. One instance per logged-in session.
 * Connects to ChatServer, registers the current user, and listens for incoming messages.
 *
 * Protocol:
 *   On connect:  REGISTER:<userId>
 *   Send msg:    MSG:<fromId>:<toId>:<content>
 *   Receive msg: MSG:<fromId>:<toId>:<content>
 */
public class ChatClient {

    private static final String HOST = "localhost";
    private static final int PORT = 9090;

    private static ChatClient instance;

    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;
    private long userId;

    /** Called when an incoming message arrives: (fromUserId, messageContent) */
    private BiConsumer<Long, String> messageListener;

    private ChatClient() {}

    public static synchronized ChatClient getInstance() {
        if (instance == null) instance = new ChatClient();
        return instance;
    }

    /**
     * Connect to the ChatServer and register this user.
     * Safe to call multiple times — won't reconnect if already connected.
     */
    public synchronized void connect(long userId) {
        if (connected) return;
        this.userId = userId;
        try {
            socket = new Socket(HOST, PORT);
            socket.setKeepAlive(true);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // Register with server
            out.println("REGISTER:" + userId);
            connected = true;
            System.out.println("[ChatClient] Connected to chat server as user " + userId);

            // Start background listener thread
            Thread listenerThread = new Thread(() -> {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("MSG:")) {
                            String[] parts = line.split(":", 4);
                            if (parts.length == 4 && messageListener != null) {
                                try {
                                    long fromId = Long.parseLong(parts[1]);
                                    String content = parts[3];
                                    messageListener.accept(fromId, content);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                } catch (IOException e) {
                    // Server closed or disconnected
                } finally {
                    connected = false;
                    System.out.println("[ChatClient] Disconnected from chat server.");
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.setName("ChatClient-Listener");
            listenerThread.start();

        } catch (IOException e) {
            System.err.println("[ChatClient] Cannot connect to chat server: " + e.getMessage());
            connected = false;
        }
    }

    /**
     * Send a message to another user through the server.
     *
     * @param toId    Recipient's userId
     * @param content Message text
     */
    public void send(long toId, String content) {
        if (connected && out != null) {
            // Escape colons in content to avoid protocol breakage
            out.println("MSG:" + userId + ":" + toId + ":" + content);
        }
    }

    /**
     * Register a listener that is called on the calling thread when a message arrives.
     * The listener receives (fromUserId, messageContent).
     * Use Platform.runLater inside the listener to update JavaFX UI.
     */
    public void setMessageListener(BiConsumer<Long, String> listener) {
        this.messageListener = listener;
    }

    public synchronized void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return connected;
    }

    public long getUserId() {
        return userId;
    }
}
