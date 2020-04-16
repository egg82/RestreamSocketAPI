package ninja.egg82.restream;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.*;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import ninja.egg82.core.RestreamApi20;
import ninja.egg82.core.RestreamSocket;
import ninja.egg82.utils.FileUtil;

public class RestreamSocketClient {
    private final OAuth20Service service;
    private final File cacheFile;

    private String refreshToken = null;
    private long refreshTokenExpires = -1L;
    private String accessToken = null;
    private long accessTokenExpires = -1L;
    private RestreamSocket client = null;

    private BiConsumer<Integer, String> onConnect = null;
    private BiConsumer<Integer, String> onDisconnect = null;
    private Consumer<Exception> onException = null;
    private Consumer<String> onEvent = null;

    public RestreamSocketClient(String clientID, String clientSecret, String callbackURL, File cacheFile) throws IOException, ExecutionException, InterruptedException {
        this.cacheFile = FileUtil.getOrCreateFile(cacheFile);
        this.service = new ServiceBuilder(clientID)
                .responseType("code")
                .apiSecret(clientSecret)
                .callback(callbackURL)
                .defaultScope("profile.default.read,channels.default.read,chat.default.read,stream.default.read")
                .userAgent("egg82/RestreamSocketAPI")
                .build(RestreamApi20.instance());
        tryParseCache();
        if (refreshToken != null && isAccessTokenExpired()) {
            tryGetAccessToken();
        }
    }

    public boolean isAuthorized() { return refreshToken != null && !isRefreshTokenExpired(); }

    public boolean isConnected() { return client != null && !client.isClosed(); }

    public String getAuthURL() { return service.getAuthorizationUrl(); }

    public void authorize(String authCode) throws IOException, ExecutionException, InterruptedException {
        OAuth2AccessToken token = service.getAccessToken(authCode);
        refreshToken = token.getRefreshToken();
        refreshTokenExpires = System.currentTimeMillis() + 31536000000L; // 1 year
        accessToken = token.getAccessToken();
        accessTokenExpires = System.currentTimeMillis() + (token.getExpiresIn() * 1000L);

        writeCache();
        if (client != null && !client.isClosed()) {
            client.closeBlocking();
            tryGetSocket();
            client.connect();
        }
    }

    public boolean refreshAccessToken() throws IOException, ExecutionException, InterruptedException {
        if (refreshToken != null && !isRefreshTokenExpired()) {
            tryGetAccessToken();
            if (client != null && !client.isClosed()) {
                client.closeBlocking();
                tryGetSocket();
                client.connect();
            }
        }
        return accessToken != null && !isAccessTokenExpired();
    }

    public boolean reconnectSocket() throws IOException, InterruptedException {
        if (accessToken != null && !isAccessTokenExpired()) {
            if (client != null && !client.isClosed()) {
                client.closeBlocking();
            }
            tryGetSocket();
            client.connect();
            return true;
        }
        return false;
    }

    public boolean isRefreshTokenExpired() { return System.currentTimeMillis() >= refreshTokenExpires; }

    public boolean isAccessTokenExpired() { return System.currentTimeMillis() >= accessTokenExpires; }

    public void close() throws InterruptedException {
        if (client != null && !client.isClosed()) {
            client.closeBlocking();
        }
    }

    public void connect() throws IOException, ExecutionException, InterruptedException {
        if (client != null && !client.isClosed()) {
            client.closeBlocking();
        }
        if (refreshToken != null && isAccessTokenExpired()) {
            tryGetAccessToken();
        }
        tryGetSocket();
        client.connectBlocking();
    }

    public void onConnect(BiConsumer<Integer, String> consumer) throws IOException, ExecutionException, InterruptedException {
        this.onConnect = consumer;
        if (client != null && !client.isClosed()) {
            client.closeBlocking();

            if (refreshToken != null && isAccessTokenExpired()) {
                tryGetAccessToken();
            }
            if (accessToken != null && !isAccessTokenExpired()) {
                tryGetSocket();
            }
        }
    }

    public void onDisconnect(BiConsumer<Integer, String> consumer) throws IOException, ExecutionException, InterruptedException {
        this.onDisconnect = consumer;
        if (client != null && !client.isClosed()) {
            client.closeBlocking();

            if (refreshToken != null && isAccessTokenExpired()) {
                tryGetAccessToken();
            }
            if (accessToken != null && !isAccessTokenExpired()) {
                tryGetSocket();
            }
        }
    }

    public void onException(Consumer<Exception> consumer) throws IOException, ExecutionException, InterruptedException {
        this.onException = consumer;
        if (client != null && !client.isClosed()) {
            client.closeBlocking();

            if (refreshToken != null && isAccessTokenExpired()) {
                tryGetAccessToken();
            }
            if (accessToken != null && !isAccessTokenExpired()) {
                tryGetSocket();
            }
        }
    }

    public void onEvent(Consumer<String> consumer) throws IOException, ExecutionException, InterruptedException {
        this.onEvent = consumer;
        if (client != null && !client.isClosed()) {
            client.closeBlocking();

            if (refreshToken != null && isAccessTokenExpired()) {
                tryGetAccessToken();
            }
            if (accessToken != null && !isAccessTokenExpired()) {
                tryGetSocket();
            }
        }
    }

    private void tryParseCache() throws IOException {
        try (
                FileReader reader = new FileReader(cacheFile);
                BufferedReader in = new BufferedReader(reader);
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    if (refreshToken == null) {
                        refreshToken = line;
                    } else if (refreshTokenExpires == -1L) {
                        refreshTokenExpires = Long.parseLong(line);
                    } else if (accessToken == null) {
                        accessToken = line;
                    } else if (accessTokenExpires == -1L) {
                        accessTokenExpires = Long.parseLong(line);
                        return;
                    }
                }
            }
        }
    }

    private void writeCache() throws IOException {
        try (
                FileWriter writer = new FileWriter(cacheFile);
                BufferedWriter out = new BufferedWriter(writer)
        ) {
            out.write(refreshToken + System.lineSeparator());
            out.write(refreshTokenExpires + System.lineSeparator());
            out.write(accessToken + System.lineSeparator());
            out.write(accessTokenExpires + System.lineSeparator());
        }
    }

    private void tryGetAccessToken() throws IOException, ExecutionException, InterruptedException {
        OAuth2AccessToken token = service.refreshAccessToken(refreshToken);
        refreshToken = token.getRefreshToken();
        refreshTokenExpires = System.currentTimeMillis() + 31536000000L; // 1 year
        accessToken = token.getAccessToken();
        accessTokenExpires = System.currentTimeMillis() + (token.getExpiresIn() * 1000L);
        writeCache();
    }

    private void tryGetSocket() throws IOException {
        try {
            client = new RestreamSocket(accessToken, onConnect, onDisconnect, onException, onEvent);
        } catch (URISyntaxException ex) {
            // Should never happen
            throw new IOException("Illegal WebSocket URL identified.", ex);
        }
    }
}
