# Goowac Client

Fabric client project for Minecraft Java 1.21.11.

## Included
- Goowac Client GUI opened with Right Shift.
- Searchable categorized module browser.
- Publicly advertised Krypton-style module catalogue represented as original Goowac entries.
- FPS, coordinates, module list, Spotify now-playing, and Spotify lyrics HUD.
- Local **Detection Test** mode.
- Detection Test simulates common movement, rotation, attack-timing, and chat-cadence checks and reports which enabled Goowac modules would be flagged by the synthetic model.
- The harness intentionally does not inspect, modify, spoof, evade, or bypass real server anti-cheat traffic.
- GitHub Actions builds the Fabric JAR and publishes it as a release asset after a successful build.

## Controls
- Right Shift: open Goowac menu
- H: toggle HUD
- G: toggle Sprint
- C: copy coordinates
- D: toggle the local Detection Test

Use Java 21.

The 82-name catalogue is a feature catalogue and UI surface; server-impacting automation is disabled in this build.
