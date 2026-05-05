import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;


public class CollabServer extends WebSocketServer {

    private final Set<WebSocket> activeConnections;
    private static final java.util.Map<String, String[]> ROOM_CODES = new java.util.HashMap<>();


    public CollabServer(int port) {
        super(new InetSocketAddress(port));
        this.activeConnections = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        activeConnections.add(conn);
        System.out.println("NEW CONNECTION: " + conn.getRemoteSocketAddress());
        System.out.println("Total active users: " + activeConnections.size());

        try {
            String documentId = "default";
            List<String> history = OperationRepository.getOperations(documentId);

            System.out.println(" Sending " + history.size() + " operations to new client...");

            for (String op : history) {
                conn.send(op);
            }

            System.out.println(" History sent to: " + conn.getRemoteSocketAddress());

        } catch (Exception e) {
            System.err.println(" Failed to send history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        activeConnections.remove(conn);
        System.out.println("DISCONNECTED: " + conn.getRemoteSocketAddress());
        System.out.println("Total active users: " + activeConnections.size());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("RECEIVED from " + conn.getRemoteSocketAddress() + ": " + message);

        try {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(message, JsonObject.class);

            String type = json.get("type").getAsString();

            if (type.equals("JOIN")) {
                String code = json.get("code").getAsString();
                if (!ROOM_CODES.containsKey(code)) {
                    conn.send("{\"type\":\"JOIN_REJECTED\",\"reason\":\"Invalid room code\"}");
                    conn.close();
                    return;
                }
                String[] roomInfo = ROOM_CODES.get(code);
                String role = roomInfo[1];
                conn.setAttachment(role);
                conn.send("{\"type\":\"JOIN_ACCEPTED\",\"role\":\"" + role + "\"}");

                conn.send("{\"type\":\"HISTORY_START\"}");
                List<String> history = OperationRepository.getOperations(roomInfo[0]);
                for (String op : history) conn.send(op);
                conn.send("{\"type\":\"HISTORY_END\"}");
                return;
            }

            String siteId = json.get("siteId").getAsString();
            int    clock  = json.get("clock").getAsInt();

            String documentId = "default";

            String value = (json.has("value") && !json.get("value").isJsonNull())
                    ? json.get("value").getAsString() : null;

            String afterSiteId = (json.has("afterSiteId") && !json.get("afterSiteId").isJsonNull())
                    ? json.get("afterSiteId").getAsString() : null;

            int afterClock = json.has("afterClock") ? json.get("afterClock").getAsInt() : 0;

            boolean isBold   = json.has("isBold")   && json.get("isBold").getAsBoolean();
            boolean isItalic = json.has("isItalic") && json.get("isItalic").getAsBoolean();

            OperationRepository.saveOperation(
                    documentId, type, siteId, clock,
                    value, afterSiteId, afterClock,
                    isBold, isItalic
            );

            System.out.println(" Saved to MongoDB: " + type + " by " + siteId);

        } catch (Exception e) {
            System.err.println(" Failed to save to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }

        String senderRole = conn.getAttachment();
        if ("viewer".equals(senderRole)) {
            conn.send("{\"type\":\"PERMISSION_DENIED\",\"reason\":\"Viewers cannot edit\"}");
            return;
        }

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
        System.err.println("ERROR on connection " + (conn != null ? conn.getRemoteSocketAddress() : "null"));
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server Started");
        System.out.println("Listening for connections on port: " + getPort());
        Database.initialize();

        String editorCode = generateCode();
        String viewerCode = generateCode();
        ROOM_CODES.put(editorCode, new String[]{"default", "editor"});
        ROOM_CODES.put(viewerCode, new String[]{"default", "viewer"});

        System.out.println("=================================");
        System.out.println(" EDITOR CODE : " + editorCode);
        System.out.println("  VIEWER CODE : " + viewerCode);
        System.out.println("=================================");
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        java.util.Random rng = new java.util.Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }

    public static void main(String[] args) {
        int port = 8888;
        CollabServer server = new CollabServer(port);
        server.start();
    }
}



