package com.girlfriend.client;

import com.girlfriend.GirlfriendMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class GirlfriendClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRenderers.register(GirlfriendMod.GIRLFRIEND, GirlfriendRenderer::new);
    }
}
