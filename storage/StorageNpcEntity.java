package com.example.cosmod.storage;

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

public class StorageNpcEntity extends PathfinderMob {

    public StorageNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("§b[보관소] §f아이템 관리인"));
        this.setCustomNameVisible(true);
        this.setNoAi(true);
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
            if (DeathItemStorage.hasItems(sp)) {
                StorageNpcNetwork.sendOpenStorageGui(sp);
            } else {
                sp.displayClientMessage(
                    Component.literal("§b아이템 관리인: §f현재 보관 중인 아이템이 없습니다."), false);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    public boolean isInvulnerableTo(DamageSource source) { return true; }

    @Override
    protected void registerGoals() {}
}
