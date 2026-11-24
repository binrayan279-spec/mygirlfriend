package com.girlfriend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Stored in .minecraft/config/girlfriend.json */
public class GirlfriendConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("girlfriend.json");
    private static GirlfriendConfig instance;

    public String apiKey = "";
    public String model = "claude-haiku-5-5";
    public int maxTokens = 150;
    public String defaultName = "Luna";
    public boolean slimArms = true;   // true = Alex-style arms, false = Steve-style (restart needed)
    public int talkRange = 24;        // she hears your chat within this many blocks
    public String personality = "You are {name}, the player's warm, caring and playful girlfriend, "
            + "living with them in their Minecraft world. Talk casually and affectionately, like a real person texting. "
            + "Keep replies short (1-3 sentences) and use the occasional emoji. React to what is going on in the game when it fits. "
            + "Keep everything sweet and wholesome: hugs, cuddles and kisses on the cheek are fine, but never anything sexually explicit; "
            + "if asked, gently steer the conversation elsewhere. If the player sincerely asks whether you are real, "
            + "be honest that you are an AI character. If they seem sad or stressed, be supportive.";

    public static synchronized GirlfriendConfig get() {
        if (instance == null) instance = reload();
        return instance;
    }

    public static synchronized GirlfriendConfig reload() {
        GirlfriendConfig c = null;
        try {
            if (Files.exists(FILE)) c = GSON.fromJson(Files.readString(FILE), GirlfriendConfig.class);
        } catch (Exception e) {
            GirlfriendMod.LOGGER.warn("Bad girlfriend.json, using defaults: {}", e.toString());
        }
        if (c == null) {
            c = new GirlfriendConfig();
            try { Files.writeString(FILE, GSON.toJson(c)); } catch (Exception ignored) { }
        }
        instance = c;
        return c;
    }

    public String key() {
        if (apiKey != null && !apiKey.isBlank()) return apiKey.trim();
        String env = System.getenv("ANTHROPIC_API_KEY");
        return env == null ? "" : env.trim();
    }
}
