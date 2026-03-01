package opgg.ghrami.util;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple TCP server for real-time messaging between Ghrami users.
 * Started as a daemon background thread when the application launches.
 * Clients identify themselves on connect and the server relays messages to recipients.
 *
 * Protocol:
 *   Client → Server (on connect):  REGISTER:<userId>
 *   Client → Server (send msg):    MSG:<fromId>:<toId>:<content>
 *   Server → Client (relay msg):   MSG:<fromId>:<toId>:<content>
 */
public class ChatServer {

    private static final int PORT = 9090;
    private static ChatServer instance;

    private ServerSocket serverSocket;
    private final Map<Long, PrintWriter> clients = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    private ChatServer() {}

    public static synchronized ChatServer getInstance() {
        if (instance == null) instance = new ChatServer();
        return instance;
    }

    /** Start the server in a daemon thread. Safe to call multiple times. */
    public synchronized void start() {
        if (running) return;
        running = true;
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println("[ChatServer] Listening on port " + PORT);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Thread clientThread = new Thread(() -> handleClient(clientSocket));
                        clientThread.setDaemon(true);
                        clientThread.start();
                    } catch (IOException e) {
                        if (running) System.err.println("[ChatServer] Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                if (running) System.err.println("[ChatServer] Failed to start on port " + PORT + ": " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.setName("ChatServer-Main");
        serverThread.start();
    }

    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
    }

    private void handleClient(Socket socket) {
        Long userId = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // First line: "REGISTER:<userId>"
            String line = in.readLine();
            if (line != null && line.startsWith("REGISTER:")) {
                try {
                    userId = Long.parseLong(line.substring(9).trim());
                    clients.put(userId, out);
                    System.out.println("[ChatServer] User " + userId + " registered.");
                } catch (NumberFormatException e) {
                    System.err.println("[ChatServer] Invalid REGISTER line: " + line);
                    return;
                }
            } else {
                return;
            }

            // Read subsequent messages
            while ((line = in.readLine()) != null) {
                if (line.startsWith("MSG:")) {
                    // Format: MSG:<fromId>:<toId>:<content>
                    String[] parts = line.split(":", 4);
                    if (parts.length == 4) {
                        try {
                            long fromId = Long.parseLong(parts[1]);
                            long toId   = Long.parseLong(parts[2]);
                            String content = parts[3];
                            // Relay to recipient if connected
                            PrintWriter recipientOut = clients.get(toId);
                            if (recipientOut != null) {
                                recipientOut.println("MSG:" + fromId + ":" + toId + ":" + content);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            // Client disconnected
        } finally {
            if (userId != null) {
                clients.remove(userId);
                System.out.println("[ChatServer] User " + userId + " disconnected.");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public boolean isRunning() {
        return running;
    }
}
