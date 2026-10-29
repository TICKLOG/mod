package net.ticklog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

final class Api {
    static final String BASE = "https://ticklog.net/api";
    static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    /** One daemon thread for all network calls, so nothing ever blocks the render thread. */
    static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ticklog");
        t.setDaemon(true);
        return t;
    });

    private Api() {}

    /** Proves account ownership via Mojang (like joining a server) and returns a verified serverId. */
    static String challenge(Minecraft client, User user) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", user.getProfileId().toString());
        body.addProperty("name", user.getName());
        String serverId = post("/otp/challenge", body).get("serverId").getAsString();
        client.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);
        return serverId;
    }

    static String authenticate(Minecraft client, User user) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("serverId", challenge(client, user));
        return post("/mod/auth", body).get("token").getAsString();
    }

    static JsonObject post(String path, JsonObject body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("User-Agent", "ticklog-mod")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(path + " -> " + response.statusCode() + " " + response.body());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
