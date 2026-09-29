package dev.goowac.client.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.goowac.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigStore {
    private final ModuleManager modules;

    public ConfigStore(ModuleManager modules) {
        this.modules = modules;
    }

    private Path path() {
        return MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config")
            .resolve("goowac.json");
    }

    public void load() {
        Path file = path();
        if (!Files.exists(file)) return;
        try {
            JsonObject root = new GsonBuilder().create().fromJson(
                Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("enabled")) return;

            JsonArray array = root.getAsJsonArray("enabled");
            java.util.List<String> names = new java.util.ArrayList<>();
            array.forEach(element -> names.add(element.getAsString()));
            names.remove("Detection Test");
            modules.restoreEnabledNames(names);
        } catch (Exception ignored) {
            // Keep defaults when a config is malformed.
        }
    }

    public void save() {
        try {
            Path file = path();
            Files.createDirectories(file.getParent());

            JsonObject root = new JsonObject();
            JsonArray enabled = new JsonArray();
            modules.enabledNames().stream().filter(name -> !name.equals("Detection Test")).forEach(enabled::add);
            root.add("enabled", enabled);
            root.addProperty("version", 1);

            Files.writeString(
                file,
                new GsonBuilder().setPrettyPrinting().create().toJson(root),
                StandardCharsets.UTF_8
            );
        } catch (IOException ignored) {
            // Config persistence is optional and must never break the client.
        }
    }
}
