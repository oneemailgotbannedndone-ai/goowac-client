package dev.goowac.client.music;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SpotifyService implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "goowac-spotify");
        t.setDaemon(true);
        return t;
    });

    private final HttpClient http = HttpClient.newHttpClient();
    private volatile SpotifyTrack current = SpotifyTrack.empty();
    private volatile String lyrics = "";

    public void start() {
        executor.scheduleAtFixedRate(this::poll, 0, 2, TimeUnit.SECONDS);
    }

    private void poll() {
        SpotifyTrack next = readMacSpotify();
        if (!next.title().equals(current.title()) || !next.artist().equals(current.artist())) {
            lyrics = "";
        }
        current = next;

        if (next.playing() && !next.title().isBlank() && lyrics.isEmpty()) {
            fetchLyrics(next);
        }
    }

    private SpotifyTrack readMacSpotify() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            return SpotifyTrack.empty();
        }

        String script =
            "tell application \"Spotify\" to if player state is playing then " +
            "return (name of current track) & \"|||\" & " +
            "(artist of current track) & \"|||\" & " +
            "(album of current track)";

        try {
            Process process = new ProcessBuilder("osascript", "-e", script)
                .redirectErrorStream(true)
                .start();

            if (!process.waitFor(1500, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return SpotifyTrack.empty();
            }

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String output = reader.readLine();
                if (output == null || output.isBlank()) {
                    return SpotifyTrack.empty();
                }

                String[] parts = output.split("\\|\\|\\|", -1);
                if (parts.length < 3) {
                    return SpotifyTrack.empty();
                }

                return new SpotifyTrack(parts[0], parts[1], parts[2], 0, 0, true);
            }
        } catch (Exception ignored) {
            return SpotifyTrack.empty();
        }
    }

    private void fetchLyrics(SpotifyTrack track) {
        try {
            String url = "https://lrclib.net/api/get?track_name=" + enc(track.title())
                + "&artist_name=" + enc(track.artist())
                + "&album_name=" + enc(track.album());

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("User-Agent", "GoowacClient/0.2")
                .build();

            HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                return;
            }

            JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
            String synced = object.has("syncedLyrics") && !object.get("syncedLyrics").isJsonNull()
                ? object.get("syncedLyrics").getAsString()
                : "";
            String plain = object.has("plainLyrics") && !object.get("plainLyrics").isJsonNull()
                ? object.get("plainLyrics").getAsString()
                : "";

            lyrics = synced.isBlank() ? plain : synced;
        } catch (Exception ignored) {
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public SpotifyTrack current() {
        return current;
    }

    public String lyrics() {
        return lyrics;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
