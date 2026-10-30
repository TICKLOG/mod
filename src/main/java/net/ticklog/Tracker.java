package net.ticklog;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends a heartbeat to ticklog.net every 30 seconds while the game runs, tagged with
 * where the player is: launcher (menus), singleplayer, or a server (with address), and
 * with the stats counted since the last one. The server credits time from the gaps
 * between heartbeats, nothing is stored locally.
 */
final class Tracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("ticklog");
    private static final long INTERVAL_MS = 30_000;
    private static final long RETRY_MS = 30_000;

    private record State(String kind, String server) {}

    private String token;
    private long lastBeat;
    private long nextAuthTry;
    private State current = new State("launcher", "");
    private final AtomicBoolean busy = new AtomicBoolean();

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private static State stateOf(Minecraft client) {
        if (client.level == null || client.player == null) return new State("launcher", "");
        if (client.isLocalServer()) return new State("singleplayer", "");
        ServerData server = client.getCurrentServer();
        if (server == null) return new State("server", "");
        if (server.isRealm()) return new State("server", "realms");
        return new State("server", server.ip == null ? "" : server.ip);
    }

    private void tick(Minecraft client) {
        Stats.INSTANCE.tick(client);
        long now = System.currentTimeMillis();
        State state = stateOf(client);

        if (token == null) {
            current = state;
            if (now >= nextAuthTry) {
                nextAuthTry = now + RETRY_MS;
                authenticate(client);
            }
            return;
        }

        if (!state.equals(current)) {
            beat(current, now); // close the previous stretch with its own kind
            current = state;
            return;
        }
        if (now - lastBeat >= INTERVAL_MS) beat(current, now);
    }

    private void authenticate(Minecraft client) {
        if (!busy.compareAndSet(false, true)) return;
        User user = client.getUser();
        CompletableFuture.runAsync(() -> {
            try {
                token = Api.authenticate(client, user);
                lastBeat = 0;
                LOGGER.info("ticklog: tracking as {}", user.getName());
            } catch (Exception e) {
                LOGGER.warn("ticklog: auth failed: {}", e.getMessage());
            } finally {
                busy.set(false);
            }
        }, Api.EXEC);
    }

    private void beat(State state, long now) {
        if (token == null) return;
        lastBeat = now;
        String t = token;
        JsonObject body = new JsonObject();
        body.addProperty("kind", state.kind());
        body.addProperty("server", state.server());
        JsonObject stats = Stats.INSTANCE.take();
        body.add("stats", stats);
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(Api.BASE + "/heartbeat"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + t)
                        .header("User-Agent", "ticklog-mod")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();
                HttpResponse<String> r = Api.HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 401) {
                    token = null; // replaced by a newer game start, re-authenticate
                } else if (r.statusCode() != 200) {
                    LOGGER.warn("ticklog: heartbeat {} {}", r.statusCode(), r.body());
                }
            } catch (Exception e) {
                LOGGER.warn("ticklog: heartbeat failed: {}", e.getMessage());
                Minecraft.getInstance().execute(() -> Stats.INSTANCE.restore(stats));
            }
        }, Api.EXEC);
    }
}
