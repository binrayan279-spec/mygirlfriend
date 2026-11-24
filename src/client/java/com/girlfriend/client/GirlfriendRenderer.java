package com.girlfriend.client;

import com.girlfriend.GirlfriendConfig;
import com.girlfriend.GirlfriendEntity;
import com.girlfriend.GirlfriendMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class GirlfriendRenderer extends MobRenderer<GirlfriendEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(GirlfriendMod.ID, "textures/entity/girlfriend.png");

    public GirlfriendRenderer(EntityRendererProvider.Context context) {
        super(context, makeModel(context), 0.5f);
    }

    private static HumanoidModel<HumanoidRenderState> makeModel(EntityRendererProvider.Context context) {
        boolean slim = GirlfriendConfig.get().slimArms;
        ModelPart root = context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER);
        for (String name : new String[]{"jacket", "left_sleeve", "right_sleeve", "left_pants", "right_pants", "ear", "cloak"}) {
            try { root.getChild(name).visible = false; } catch (Exception ignored) { }
        }
        return new HumanoidModel<>(root);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
