import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public class CollabClient extends WebSocketClient {

    // This is the "wire" that connects the network back to your Main UI
    private Consumer<String> onMessageCallback;

    // We updated the constructor to accept the callback
    public CollabClient(String serverUri, Consumer<String> onMessageCallback) throws URISyntaxException {
        super(new URI(serverUri));
        this.onMessageCallback = onMessageCallback;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        System.out.println("🟢 CLIENT: Successfully connected to the server!");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("📩 CLIENT RECEIVED: " + message);

        // When a message arrives, send it through the wire to Main.java!
        if (onMessageCallback != null) {
            onMessageCallback.accept(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("🔴 CLIENT: Disconnected from server. Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("⚠️ CLIENT ERROR!");
        ex.printStackTrace();
    }
}