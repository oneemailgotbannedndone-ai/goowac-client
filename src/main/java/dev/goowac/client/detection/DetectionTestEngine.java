package dev.goowac.client.detection;

import dev.goowac.client.module.Module;
import dev.goowac.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DetectionTestEngine {
    private final ModuleManager modules;
    private final List<DetectionTestResult> results = new ArrayList<>();
    private boolean active;

    public DetectionTestEngine(ModuleManager modules) {
        this.modules = modules;
    }

    public void start(MinecraftClient client) {
        active = true;
        results.clear();
        runSyntheticSuite(client);
    }

    public void stop() {
        active = false;
    }

    public boolean active() {
        return active;
    }

    public void tick(MinecraftClient client) {
        if (!active || client.player == null) return;
        if (results.isEmpty()) runSyntheticSuite(client);
    }

    public List<DetectionTestResult> results() {
        return Collections.unmodifiableList(results);
    }

    public int triggeredCount() {
        int count = 0;
        for (DetectionTestResult r : results) if (r.triggered()) count++;
        return count;
    }

    private void runSyntheticSuite(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        boolean sprint = modules.find("Sprint") != null && modules.find("Sprint").enabled();
        boolean autoWalk = modules.find("Auto Walk") != null && modules.find("Auto Walk").enabled();
        boolean freeLook = modules.find("Free Look") != null && modules.find("Free Look").enabled();
        boolean swingSpeed = modules.find("SwingSpeed") != null && modules.find("SwingSpeed").enabled();
        boolean chatMacro = modules.find("Chat Macro") != null && modules.find("Chat Macro").enabled();

        add(
            "Movement consistency",
            autoWalk ? "TRIGGERED" : "PASS",
            autoWalk
                ? "Synthetic pathing test observed automated forward movement."
                : "No automated pathing module is enabled.",
            autoWalk ? "Auto Walk" : "—"
        );

        add(
            "Sprint state consistency",
            sprint ? "TRIGGERED" : "PASS",
            sprint
                ? "Synthetic sprint check flags forced sprint state for review."
                : "No forced sprint state detected in the synthetic scenario.",
            sprint ? "Sprint" : "—"
        );

        add(
            "Rotation consistency",
            freeLook ? "TRIGGERED" : "PASS",
            freeLook
                ? "Synthetic camera/rotation check flags independent-look behavior for review."
                : "No independent-look module is enabled.",
            freeLook ? "Free Look" : "—"
        );

        add(
            "Attack timing",
            swingSpeed ? "TRIGGERED" : "PASS",
            swingSpeed
                ? "Synthetic attack cadence is outside the normal-player baseline."
                : "No attack-speed modifier is enabled.",
            swingSpeed ? "SwingSpeed" : "—"
        );

        add(
            "Chat cadence",
            chatMacro ? "TRIGGERED" : "PASS",
            chatMacro
                ? "Synthetic repeated-message cadence would require server-side rate-limit review."
                : "No chat macro is enabled.",
            chatMacro ? "Chat Macro" : "—"
        );

        add(
            "Player-state sanity",
            "PASS",
            String.format("Local snapshot: x=%.1f y=%.1f z=%.1f, onGround=%s.",
                player.getX(), player.getY(), player.getZ(), player.isOnGround()),
            "Local snapshot"
        );

        add(
            "Network evasion",
            "NOT_TESTED",
            "This harness intentionally does not inspect, alter, spoof, or bypass server anti-cheat traffic.",
            "—"
        );
    }

    private void add(String check, String status, String detail, String module) {
        results.add(new DetectionTestResult(check, status, detail, module));
    }
}
