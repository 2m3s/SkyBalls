package com.epic60869.skyballs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Proves to the SBC chat server which Minecraft account this connection is, like a Minecraft server login: the mod
 * gets a random serverId from /mod-api/auth/challenge, "joins" it through Mojang's session server, and sends it as
 * casinoAuth; the server asks Mojang whether this player joined it. Needed for things that change your data on the
 * server (the casino, /sb nick). A login lasts for one connection; a reconnect logs in again.
 */
public final class SkyBallsLogin {
    private static final String CHALLENGE_URL = "https://tastyfish.org/mod-api/auth/challenge";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    private static Object loggedInConnection;
    private static boolean loggingIn;
    private static String problem = "";
    private static final List<Runnable> WAITING = new ArrayList<>();

    private SkyBallsLogin() {}

    public static boolean loggedIn() {
        Object connection = SkyBallsGlobalChat.currentConnection();
        return connection != null && connection == loggedInConnection;
    }

    /** Why the last login failed, or "". */
    public static String problem() {
        return problem;
    }

    /** The server forgot the login (it restarted, or answered "notLoggedIn"): log in again next time. */
    public static void forget() {
        loggedInConnection = null;
    }

    /** Runs {@code then} on the game thread once logged in (now, if already). Logs in if needed. */
    public static void whenLoggedIn(Runnable then) {
        if (loggedIn()) {
            then.run();
            return;
        }
        if (then != null) WAITING.add(then);
        Object connection = SkyBallsGlobalChat.currentConnection();
        if (connection == null) {
            SkyBallsGlobalChat.ensureConnected();
            return;
        }
        if (loggingIn) return;
        loggingIn = true;
        problem = "";
        Minecraft mc = Minecraft.getInstance();
        HttpRequest request = HttpRequest.newBuilder(URI.create(CHALLENGE_URL)).timeout(Duration.ofSeconds(15)).GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) throw new IllegalStateException("challenge HTTP " + response.statusCode());
            String serverId = JsonParser.parseString(response.body()).getAsJsonObject().get("serverId").getAsString();
            try {
                mc.services().sessionService().joinServer(mc.getUser().getProfileId(), mc.getUser().getAccessToken(), serverId);
            } catch (Exception e) {
                throw new IllegalStateException("Mojang login failed (this needs a real Minecraft account)");
            }
            return serverId;
        }).whenComplete((serverId, e) -> mc.execute(() -> {
            loggingIn = false;
            if (e != null) {
                Throwable cause = e;
                while (cause.getCause() != null) cause = cause.getCause();
                problem = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
                WAITING.clear();
                return;
            }
            JsonObject auth = new JsonObject();
            auth.addProperty("type", "casinoAuth");
            auth.addProperty("serverId", serverId);
            auth.addProperty("username", mc.getUser().getName());
            if (!SkyBallsGlobalChat.send(auth)) {
                WAITING.clear();
                return;
            }
            loggedInConnection = connection;
            List<Runnable> ready = new ArrayList<>(WAITING);
            WAITING.clear();
            ready.forEach(Runnable::run);
        }));
    }
}
