package dev.goowac.client.music;
public record SpotifyTrack(String title, String artist, String album, int durationSeconds, int positionSeconds, boolean playing) {
    public static SpotifyTrack empty() { return new SpotifyTrack("Nothing playing", "", "", 0, 0, false); }
}
