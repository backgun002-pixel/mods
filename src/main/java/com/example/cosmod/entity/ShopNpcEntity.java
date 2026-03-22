package com.example.cosmod.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ShopNpcEntity extends PathfinderMob {

    public ShopNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("§6[상점] §f잡화상인"));
        this.setCustomNameVisible(true);
        this.setNoAi(true);
        this.setInvulnerable(true);
    }

    // 1.21.x 필수: 기본 속성 정의
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level().isClientSide()) {
            player.displayClientMessage(
                Component.literal("§e잡화상인: §f어서오세요!"), false);
            if (player instanceof ServerPlayer serverPlayer) {
                ShopNpcNetwork.sendOpenShopGui(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public boolean isInvulnerableTo(DamageSource source) { return true; }

    @Override
    protected void registerGoals() {}
}
