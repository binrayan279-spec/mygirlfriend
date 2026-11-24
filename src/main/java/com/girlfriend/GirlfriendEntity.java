package com.girlfriend;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;
import java.util.UUID;

public class GirlfriendEntity extends PathfinderMob {
    private UUID ownerUuid;
    private boolean staying;

    public GirlfriendEntity(EntityType<? extends GirlfriendEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);       // can't be hurt by normal damage
        this.setPersistenceRequired();    // never despawns
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FollowOwnerGoal(this));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return !isStaying() && super.canUse(); }
        });
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || staying;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwner(UUID uuid) { this.ownerUuid = uuid; }
    public boolean isStaying() { return staying; }
    public void setStaying(boolean s) { this.staying = s; if (s) getNavigation().stop(); }

    public Player getOwner() {
        return ownerUuid == null ? null : level().getPlayerByUUID(ownerUuid);
    }

    public String displayName() {
        return getName().getString();
    }

    /** Right-click = stay/follow. Sneak + right-click = kiss on the cheek. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        if (ownerUuid == null) {
            ownerUuid = player.getUUID();
            sp.displayClientMessage(Component.literal(displayName() + " is now yours \u2665"), true);
            return InteractionResult.SUCCESS;
        }
        if (!player.getUUID().equals(ownerUuid)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            kiss(sp);
        } else {
            setStaying(!staying);
            sp.displayClientMessage(Component.literal(displayName() + (staying ? " will stay here." : " will follow you.")), true);
        }
        return InteractionResult.SUCCESS;
    }

    public void kiss(ServerPlayer player) {
        if (!(level() instanceof ServerLevel sl)) return;
        getLookControl().setLookAt(player, 30f, 30f);
        sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.9, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        playSound(SoundEvents.VILLAGER_CELEBRATE, 1.0f, 1.4f);
        player.displayClientMessage(Component.literal(displayName() + " blushes and kisses you on the cheek \u2665"), true);
        GirlfriendAI.talk(this, player, "*gives you a sweet kiss on the cheek*");
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (ownerUuid != null) output.putString("GfOwner", ownerUuid.toString());
        output.putBoolean("GfStaying", staying);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String s = input.getStringOr("GfOwner", "");
        if (!s.isEmpty()) {
            try { ownerUuid = UUID.fromString(s); } catch (IllegalArgumentException ignored) { }
        }
        staying = input.getBooleanOr("GfStaying", false);
    }

    /** Walks toward her owner and teleports if she gets left far behind. */
    static class FollowOwnerGoal extends Goal {
        private final GirlfriendEntity gf;
        private int cooldown;

        FollowOwnerGoal(GirlfriendEntity gf) {
            this.gf = gf;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override public boolean canUse() {
            Player o = gf.getOwner();
            return o != null && !gf.isStaying() && gf.distanceToSqr(o) > 36;
        }

        @Override public boolean canContinueToUse() {
            Player o = gf.getOwner();
            return o != null && !gf.isStaying() && gf.distanceToSqr(o) > 9;
        }

        @Override public void tick() {
            Player o = gf.getOwner();
            if (o == null || --cooldown > 0) return;
            cooldown = 10;
            if (gf.distanceToSqr(o) > 400) {
                gf.setPos(o.getX(), o.getY(), o.getZ());
                gf.getNavigation().stop();
            } else {
                gf.getNavigation().moveTo(o, 1.2);
            }
        }

        @Override public void stop() { gf.getNavigation().stop(); }
    }
}
