package ninja.egg82.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class RestreamSocket extends WebSocketClient {
    private BiConsumer<Integer, String> onConnect;
    private BiConsumer<Integer, String> onDisconnect;
    private Consumer<Exception> onException;
    private Consumer<String> onEvent;

    public RestreamSocket(String accessToken, BiConsumer<Integer, String> onConnect, BiConsumer<Integer, String> onDisconnect, Consumer<Exception> onException, Consumer<String> onEvent) throws URISyntaxException {
        super(new URI("wss://streaming.api.restream.io/ws?accessToken=" + accessToken));

        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
        this.onException = onException;
        this.onEvent = onEvent;
    }

    public void onOpen(ServerHandshake handshakedata) {
        if (onConnect != null) {
            onConnect.accept((int) handshakedata.getHttpStatus(), handshakedata.getHttpStatusMessage());
        }
    }

    public void onMessage(String message) {
        if (onEvent != null) {
            onEvent.accept(message);
        }
    }

    public void onClose(int code, String reason, boolean remote) {
        if (remote && onDisconnect != null) {
            onDisconnect.accept(code, reason);
        }
    }

    public void onError(Exception ex) {
        if (onException != null) {
            onException.accept(ex);
        }
    }
}
