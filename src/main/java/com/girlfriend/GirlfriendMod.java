package com.girlfriend;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public class GirlfriendMod implements ModInitializer {
    public static final String ID = "girlfriend";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    private static final ResourceKey<EntityType<?>> KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "girlfriend"));
    public static final EntityType<GirlfriendEntity> GIRLFRIEND = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, KEY,
            EntityType.Builder.of(GirlfriendEntity::new, MobCategory.CREATURE).sized(0.6f, 1.8f).build(KEY));

    @Override
    public void onInitialize() {
        GirlfriendConfig.get();
        FabricDefaultAttributeRegistry.register(GIRLFRIEND, GirlfriendEntity.createAttributes());

        // Any chat message you send near her goes to her; she answers in chat.
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            GirlfriendEntity gf = findNearest(sender, GirlfriendConfig.get().talkRange);
            if (gf != null) GirlfriendAI.talk(gf, sender, message.signedContent());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                Commands.literal("gf")
                        .then(Commands.literal("spawn").executes(c -> spawn(c.getSource())))
                        .then(Commands.literal("name").then(
                                Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(c -> rename(c.getSource(), StringArgumentType.getString(c, "name")))))
                        .then(Commands.literal("stay").executes(c -> stay(c.getSource(), true)))
                        .then(Commands.literal("follow").executes(c -> stay(c.getSource(), false)))
                        .then(Commands.literal("kiss").executes(c -> kiss(c.getSource())))));
    }

    static GirlfriendEntity findNearest(ServerPlayer p, double range) {
        ServerLevel w = (ServerLevel) p.level();
        List<GirlfriendEntity> list = w.getEntitiesOfClass(GirlfriendEntity.class,
                p.getBoundingBox().inflate(range), e -> p.getUUID().equals(e.getOwnerUuid()));
        return list.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
    }

    private static int spawn(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        GirlfriendEntity gf = findNearest(p, 128);
        if (gf != null) {
            gf.setPos(p.getX(), p.getY(), p.getZ());
            gf.getNavigation().stop();
            gf.setStaying(false);
            src.sendSuccess(() -> Component.literal(gf.displayName() + " came over to you."), false);
            return 1;
        }
        ServerLevel w = (ServerLevel) p.level();
        GirlfriendEntity e = new GirlfriendEntity(GIRLFRIEND, w);
        e.setPos(p.getX(), p.getY(), p.getZ());
        e.setYRot(p.getYRot() + 180f);
        e.setOwner(p.getUUID());
        e.setCustomName(Component.literal(GirlfriendConfig.get().defaultName));
        e.setCustomNameVisible(true);
        w.addFreshEntity(e);
        src.sendSuccess(() -> Component.literal("She's here! Use /gf name <name> to rename her. Sneak + right-click to kiss her."), false);
        return 1;
    }

    private static int rename(CommandSourceStack src, String name) throws CommandSyntaxException {
        GirlfriendEntity gf = findNearest(src.getPlayerOrException(), 128);
        if (gf == null) { src.sendFailure(Component.literal("No girlfriend nearby. Use /gf spawn first.")); return 0; }
        gf.setCustomName(Component.literal(name));
        gf.setCustomNameVisible(true);
        src.sendSuccess(() -> Component.literal("Her name is now " + name + " \u2665"), false);
        return 1;
    }

    private static int stay(CommandSourceStack src, boolean stay) throws CommandSyntaxException {
        GirlfriendEntity gf = findNearest(src.getPlayerOrException(), 128);
        if (gf == null) { src.sendFailure(Component.literal("No girlfriend nearby. Use /gf spawn first.")); return 0; }
        gf.setStaying(stay);
        src.sendSuccess(() -> Component.literal(gf.displayName() + (stay ? " will stay here." : " will follow you.")), false);
        return 1;
    }

    private static int kiss(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        GirlfriendEntity gf = findNearest(p, 4);
        if (gf == null) { src.sendFailure(Component.literal("You need to be right next to her.")); return 0; }
        gf.kiss(p);
        return 1;
    }
}
