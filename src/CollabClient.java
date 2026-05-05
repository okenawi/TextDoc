import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public class    CollabClient extends WebSocketClient {

    private Consumer<String> onMessageCallback;

    public CollabClient(String serverUri, Consumer<String> onMessageCallback) throws URISyntaxException {
        super(new URI(serverUri));
        this.onMessageCallback = onMessageCallback;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        System.out.println("CLIENT: Successfully connected to the server!");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("CLIENT RECEIVED: " + message);

        if (onMessageCallback != null) {
            onMessageCallback.accept(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("CLIENT: Disconnected from server. Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("CLIENT ERROR!");
        ex.printStackTrace();
    }
}