import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CollabServer extends WebSocketServer {

    // A thread-safe set to keep track of all connected users
    private final Set<WebSocket> activeConnections;

    public CollabServer(int port) {
        super(new InetSocketAddress(port));
        // We use synchronizedSet to handle concurrent users safely (no blocking!)
        this.activeConnections = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        activeConnections.add(conn);
        System.out.println("🟢 NEW CONNECTION: " + conn.getRemoteSocketAddress());
        System.out.println("Total active users: " + activeConnections.size());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        activeConnections.remove(conn);
        System.out.println("🔴 DISCONNECTED: " + conn.getRemoteSocketAddress());
        System.out.println("Total active users: " + activeConnections.size());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("📩 RECEIVED from " + conn.getRemoteSocketAddress() + ": " + message);

        // BROADCAST: Send the message to all clients EXCEPT the one who sent it
        // (The sender already updated their own UI locally, so they don't need it echoed back)
        synchronized (activeConnections) {
            for (WebSocket client : activeConnections) {
                if (client != conn && client.isOpen()) {
                    client.send(message);
                }
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("⚠️ ERROR on connection " + (conn != null ? conn.getRemoteSocketAddress() : "null"));
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server Started");
        System.out.println("Listening for WebSocket connections on port: " + getPort());
    }

    // Main method to run the server standalone
    public static void main(String[] args) {
        int port = 8888; // Standard local testing port
        CollabServer server = new CollabServer(port);
        server.start();
        // The server runs on its own thread, so it won't block the rest of your app
    }
}