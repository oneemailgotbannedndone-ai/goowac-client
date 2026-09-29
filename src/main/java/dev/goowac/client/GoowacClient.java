package dev.goowac.client;

import dev.goowac.client.config.ConfigStore;
import dev.goowac.client.detection.DetectionTestEngine;
import dev.goowac.client.detection.DetectionTestResult;
import dev.goowac.client.module.Module;
import dev.goowac.client.module.ModuleManager;
import dev.goowac.client.music.SpotifyService;
import dev.goowac.client.music.SpotifyTrack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public final class GoowacClient implements ClientModInitializer {
    public static final ModuleManager MODULES = new ModuleManager();
    public static final SpotifyService SPOTIFY = new SpotifyService();
    public static final DetectionTestEngine DETECTION_TEST = new DetectionTestEngine(MODULES);
    public static final ConfigStore CONFIG = new ConfigStore(MODULES);

    private static KeyBinding menu, hud, sprint, coords, detection;

    @Override
    public void onInitializeClient() {
        set("HUD", true);
        set("FPS", true);
        set("Coordinates", true);
        set("Module List", true);
        set("Spotify Now Playing", true);

        menu = key("open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        hud = key("toggle_hud", GLFW.GLFW_KEY_H);
        sprint = key("toggle_sprint", GLFW.GLFW_KEY_G);
        coords = key("copy_coords", GLFW.GLFW_KEY_C);
        detection = key("detection_test", GLFW.GLFW_KEY_D);

        CONFIG.load();
        set("Detection Test", false);
        SPOTIFY.start();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menu.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClientMenuScreen());
                }
            }
            while (hud.wasPressed()) toggle("HUD", client);
            while (sprint.wasPressed()) toggle("Sprint", client);
            while (coords.wasPressed()) copyCoords(client);
            while (detection.wasPressed()) toggleDetectionTest(client);

            if (client.player != null && enabled("Sprint") && client.player.forwardSpeed > 0) {
                client.player.setSprinting(true);
            }

            DETECTION_TEST.tick(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderHud(drawContext));
    }

    private static KeyBinding key(String id, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.goowacclient." + id,
            InputUtil.Type.KEYSYM,
            code,
            KeyBinding.Category.MISC
        ));
    }

    private static void renderHud(net.minecraft.client.gui.DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.options.hudHidden || !enabled("HUD")) {
            return;
        }

        int x = 8;
        int y = 8;

        drawContext.drawTextWithShadow(client.textRenderer, "GOOWAC CLIENT", x, y, 0xFFE0B6FF);
        y += 12;

        if (enabled("FPS")) {
            drawContext.drawTextWithShadow(client.textRenderer,
                "FPS: " + client.getCurrentFps(), x, y, 0xFFFFFFFF);
            y += 12;
        }

        if (enabled("Coordinates")) {
            drawContext.drawTextWithShadow(
                client.textRenderer,
                String.format("XYZ: %.1f / %.1f / %.1f",
                    client.player.getX(), client.player.getY(), client.player.getZ()),
                x, y, 0xFFD8D8E8
            );
            y += 12;
        }

        if (enabled("Module List")) {
            for (Module module : MODULES.all()) {
                if (!module.enabled()
                    || module.name().equals("HUD")
                    || module.name().equals("Module List")) {
                    continue;
                }

                drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "• " + module.name(),
                    x, y, 0xFFB58CFF
                );

                y += 11;
                if (y > 120) break;
            }
        }

        if (enabled("Spotify Now Playing")) {
            SpotifyTrack track = SPOTIFY.current();
            if (track.playing()) {
                drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "♫ " + trim(track.title() + " — " + track.artist(), 72),
                    8, client.getWindow().getScaledHeight() - 34, 0xFFE5C8FF
                );
            }
        }

        if (enabled("Spotify Lyrics") && !SPOTIFY.lyrics().isBlank()) {
            String[] lines = SPOTIFY.lyrics().replace("\r", "").split("\n");
            int sy = client.getWindow().getScaledHeight() - 86;
            int count = 0;

            for (int i = Math.max(0, lines.length - 4);
                 i < lines.length && count < 4;
                 i++) {

                String line = lines[i].replaceFirst(
                    "^\\[[0-9]{1,2}:[0-9]{2}(?:\\.[0-9]{1,3})?]\\s*",
                    ""
                );

                if (!line.isBlank()) {
                    drawContext.drawTextWithShadow(
                        client.textRenderer,
                        trim(line, 88),
                        8, sy + count++ * 12, 0xFFF2EDF7
                    );
                }
            }
        }

        if (enabled("Detection Test") && DETECTION_TEST.active()) {
            int sy = 132;
            drawContext.drawTextWithShadow(
                client.textRenderer,
                "DETECTION TEST · LOCAL ONLY",
                8, sy, 0xFFFFD38C
            );
            sy += 12;

            drawContext.drawTextWithShadow(
                client.textRenderer,
                "Triggered: " + DETECTION_TEST.triggeredCount() +
                    "/" + DETECTION_TEST.results().size(),
                8, sy, 0xFFFFFFFF
            );
            sy += 12;

            int shown = 0;
            for (DetectionTestResult result : DETECTION_TEST.results()) {
                if (shown++ >= 4) break;

                int color = result.triggered() ? 0xFFFF8B8B : 0xFF9FE3A6;
                drawContext.drawTextWithShadow(
                    client.textRenderer,
                    (result.triggered() ? "!" : "✓") + " " + result.check(),
                    8, sy, color
                );
                sy += 11;
            }
        }
    }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private static void copyCoords(MinecraftClient client) {
        if (client.player == null) return;

        String coords = String.format("%.1f %.1f %.1f",
            client.player.getX(), client.player.getY(), client.player.getZ());

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(coords), null);
            notify(client, "Copied coordinates: " + coords);
        } catch (Exception e) {
            notify(client, "Could not access the system clipboard.");
        }
    }

    private static void toggleDetectionTest(MinecraftClient client) {
        if (DETECTION_TEST.active()) {
            DETECTION_TEST.stop();
            notify(client, "Detection Test stopped.");
        } else {
            DETECTION_TEST.start(client);
            notify(client, "Detection Test started: synthetic local checks only.");
        }

        Module module = MODULES.find("Detection Test");
        if (module != null) {
            module.setEnabled(DETECTION_TEST.active());
        }

        CONFIG.save();
    }

    public static boolean enabled(String name) {
        Module module = MODULES.find(name);
        return module != null && module.enabled();
    }

    private static void set(String name, boolean value) {
        Module module = MODULES.find(name);
        if (module != null) module.setEnabled(value);
    }

    private static void toggle(String name, MinecraftClient client) {
        Module module = MODULES.find(name);
        if (module == null) return;

        module.toggle();
        CONFIG.save();
        notify(client, module.name() + (module.enabled() ? " enabled" : " disabled"));
    }

    private static void notify(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§d[Goowac] §f" + message), true);
        }
    }
}
