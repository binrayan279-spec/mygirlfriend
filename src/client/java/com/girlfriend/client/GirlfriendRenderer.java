package com.girlfriend.client;

import com.girlfriend.GirlfriendConfig;
import com.girlfriend.GirlfriendEntity;
import com.girlfriend.GirlfriendMod;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

public class GirlfriendRenderer extends MobRenderer<GirlfriendEntity, AvatarRenderState, PlayerModel> {
    // Your skin: src/main/resources/assets/girlfriend/textures/entity/girlfriend.png (64x64)
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(GirlfriendMod.ID, "textures/entity/girlfriend.png");

    public GirlfriendRenderer(EntityRendererProvider.Context context) {
        super(context, makeModel(context), 0.5f);
    }

    private static PlayerModel makeModel(EntityRendererProvider.Context context) {
        boolean slim = GirlfriendConfig.get().slimArms;
        return new PlayerModel(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return TEXTURE;
    }
}
