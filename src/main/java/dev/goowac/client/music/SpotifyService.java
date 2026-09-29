package dev.goowac.client.music;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.*;
import java.util.concurrent.*;
public final class SpotifyService implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "goowac-spotify"); t.setDaemon(true); return t; });
    private final HttpClient http = HttpClient.newHttpClient();
    private volatile SpotifyTrack current = SpotifyTrack.empty();
    private volatile String lyrics = "";
    public void start() { executor.scheduleAtFixedRate(this::poll, 0, 2, TimeUnit.SECONDS); }
    private void poll() {
        SpotifyTrack next = readMacSpotify();
        if (!next.title().equals(current.title()) || !next.artist().equals(current.artist())) lyrics = "";
        current = next;
        if (next.playing() && !next.title().isBlank() && lyrics.isEmpty()) fetchLyrics(next);
    }
    private SpotifyTrack readMacSpotify() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) return SpotifyTrack.empty();
        String script = "tell application \"Spotify\" to if player state is playing then return (name of current track) & \"|||\\" & (artist of current track) & \"|||\\" & (album of current track)";
        try {
            Process p = new ProcessBuilder("osascript", "-e", script).redirectErrorStream(true).start();
            if (!p.waitFor(1500, TimeUnit.MILLISECONDS)) { p.destroyForcibly(); return SpotifyTrack.empty(); }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String out = r.readLine(); if (out == null || out.isBlank()) return SpotifyTrack.empty();
                String[] a = out.split("\\|\\|\\|", -1); if (a.length < 3) return SpotifyTrack.empty();
                return new SpotifyTrack(a[0], a[1], a[2], 0, 0, true);
            }
        } catch (Exception ignored) { return SpotifyTrack.empty(); }
    }
    private void fetchLyrics(SpotifyTrack t) {
        try {
            String url = "https://lrclib.net/api/get?track_name=" + enc(t.title()) + "&artist_name=" + enc(t.artist()) + "&album_name=" + enc(t.album());
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().header("User-Agent","GoowacClient/0.2").build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode()/100 != 2) return;
            JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
            String synced = obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull() ? obj.get("syncedLyrics").getAsString() : "";
            String plain = obj.has("plainLyrics") && !obj.get("plainLyrics").isJsonNull() ? obj.get("plainLyrics").getAsString() : "";
            lyrics = synced.isBlank() ? plain : synced;
        } catch (Exception ignored) {}
    }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    public SpotifyTrack current() { return current; }
    public String lyrics() { return lyrics; }
    @Override public void close() { executor.shutdownNow(); }
}
