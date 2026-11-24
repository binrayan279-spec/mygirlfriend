package com.girlfriend;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Sends your chat to Claude and prints her reply in the in-game chat. */
public final class GirlfriendAI {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Map<UUID, List<String[]>> HISTORY = new ConcurrentHashMap<>();
    private static final String[] OFFLINE = {
            "Hehe, I love spending time with you \u2665", "Mhm? Tell me more!", "You're the best, you know that?",
            "I'm so happy we're here together.", "Aww, you're so sweet!"};
    private static boolean hintShown = false;
    private static final Random RANDOM = new Random();

    private GirlfriendAI() {}

    public static void talk(GirlfriendEntity gf, ServerPlayer player, String userText) {
        GirlfriendConfig cfg = GirlfriendConfig.reload();
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        if (server == null) return;
        String name = gf.displayName();

        if (cfg.key().isBlank()) {
            if (!hintShown) {
                hintShown = true;
                player.displayClientMessage(Component.literal("[Girlfriend] No API key set. Put your Anthropic key in config/girlfriend.json (\"apiKey\") to enable real AI chat.")
                        .withStyle(ChatFormatting.GRAY), false);
            }
            deliver(player, name, OFFLINE[RANDOM.nextInt(OFFLINE.length)]);
            return;
        }

        List<String[]> hist = HISTORY.computeIfAbsent(gf.getUUID(), k -> new ArrayList<>());
        List<String[]> snapshot;
        synchronized (hist) {
            hist.add(new String[]{"user", userText});
            while (hist.size() > 20) hist.remove(0);
            while (!hist.isEmpty() && !hist.get(0)[0].equals("user")) hist.remove(0);
            snapshot = new ArrayList<>(hist);
        }

        String system = cfg.personality.replace("{name}", name)
                + "\n\nCurrent situation: the player is called " + player.getName().getString()
                + (gf.level().isRaining() ? " and it is raining in the game" : "")
                + ". The player's health is " + (int) player.getHealth() + "/20."
                + " Reply in plain text only, no markdown, no stage directions longer than a few words.";

        CompletableFuture.runAsync(() -> {
            String reply;
            try {
                reply = callApi(cfg, system, snapshot);
                synchronized (hist) { hist.add(new String[]{"assistant", reply}); }
            } catch (Exception e) {
                GirlfriendMod.LOGGER.warn("Girlfriend AI request failed: {}", e.toString());
                synchronized (hist) { if (!hist.isEmpty()) hist.remove(hist.size() - 1); }
                server.execute(() -> player.displayClientMessage(Component.literal("[Girlfriend] " + name
                        + " couldn't answer. Check your API key / model in config/girlfriend.json and the game log.")
                        .withStyle(ChatFormatting.RED), false));
                return;
            }
            final String r = reply;
            server.execute(() -> deliver(player, name, r));
        });
    }

    private static void deliver(ServerPlayer player, String name, String text) {
        player.displayClientMessage(Component.literal("<" + name + "> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE)), false);
    }

    private static String callApi(GirlfriendConfig cfg, String system, List<String[]> msgs) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", cfg.model);
        body.addProperty("max_tokens", cfg.maxTokens);
        body.addProperty("system", system);
        JsonArray arr = new JsonArray();
        for (String[] m : msgs) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m[0]);
            o.addProperty("content", m[1]);
            arr.add(o);
        }
        body.add("messages", arr);

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                .timeout(Duration.ofSeconds(30))
                .header("content-type", "application/json")
                .header("x-api-key", cfg.key())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode() + ": " + res.body());

        StringBuilder sb = new StringBuilder();
        for (JsonElement e : JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonArray("content")) {
            JsonObject o = e.getAsJsonObject();
            if ("text".equals(o.get("type").getAsString())) sb.append(o.get("text").getAsString());
        }
        String out = sb.toString().trim();
        if (out.isEmpty()) throw new RuntimeException("empty reply");
        return out;
    }
}
