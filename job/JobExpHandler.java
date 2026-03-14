package com.example.cosmod.job;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class JobExpHandler {

    public static void register() {

        // ── 전사/궁수: 몬스터 처치 시 EXP ────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // 죽은 엔티티가 몬스터인지 확인
            if (!(entity instanceof Monster)) return;

            // 킬러가 플레이어인지 확인
            if (!(damageSource.getEntity() instanceof ServerPlayer sp)) return;

            PlayerJobData data = PlayerJobManager.get(sp);
            JobClass combat = data.getCombatJob();
            if (combat == null) return;

            int expGain = getMonsterExp(entity);
            PlayerJobManager.giveExp(sp, combat, expGain);
        });

        // ── 광부: 광석/돌 채굴 ────────────────────────────────────
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer sp)) return;
            PlayerJobData data = PlayerJobManager.get(sp);
            if (data.getLifeJob() != JobClass.MINER) return;

            int exp = 0;
            if      (state.is(BlockTags.COAL_ORES))              exp = 3;
            else if (state.is(BlockTags.IRON_ORES))              exp = 5;
            else if (state.is(BlockTags.GOLD_ORES))              exp = 7;
            else if (state.is(BlockTags.DIAMOND_ORES))           exp = 15;
            else if (state.is(BlockTags.EMERALD_ORES))           exp = 20;
            else if (state.is(BlockTags.LAPIS_ORES))             exp = 8;
            else if (state.is(BlockTags.REDSTONE_ORES))          exp = 6;
            else if (state.is(BlockTags.COPPER_ORES))            exp = 4;
            else if (state.is(BlockTags.STONE_ORE_REPLACEABLES)) exp = 1;

            if (exp > 0) PlayerJobManager.giveExp(sp, JobClass.MINER, exp);
        });

        // ── 농사꾼: 작물 수확 ─────────────────────────────────────
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer sp)) return;
            PlayerJobData data = PlayerJobManager.get(sp);
            if (data.getLifeJob() != JobClass.FARMER) return;

            int exp = 0;
            Block block = state.getBlock();
            if (state.is(BlockTags.CROPS)) exp = 3;
            else if (block == Blocks.PUMPKIN || block == Blocks.MELON) exp = 5;

            if (exp > 0) PlayerJobManager.giveExp(sp, JobClass.FARMER, exp);
        });

        // ── 요리사: 음식 섭취 ─────────────────────────────────────
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

            PlayerJobData data = PlayerJobManager.get(sp);
            if (data.getLifeJob() != JobClass.COOK) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                PlayerJobManager.giveExp(sp, JobClass.COOK, 5);
            }
            return InteractionResult.PASS;
        });
    }

    /** 몬스터 종류별 EXP 반환 */
    private static int getMonsterExp(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (type == EntityType.WITHER)          return 80;
        if (type == EntityType.ENDER_DRAGON)    return 200;
        if (type == EntityType.WARDEN)          return 60;
        if (type == EntityType.ELDER_GUARDIAN)  return 40;
        if (type == EntityType.RAVAGER)         return 30;
        if (type == EntityType.EVOKER)          return 25;
        if (type == EntityType.VINDICATOR)      return 15;
        if (type == EntityType.BLAZE)           return 14;
        if (type == EntityType.WITHER_SKELETON) return 12;
        if (type == EntityType.GUARDIAN)        return 12;
        if (type == EntityType.PILLAGER)        return 12;
        if (type == EntityType.ENDERMAN)        return 10;
        if (type == EntityType.WITCH)           return 10;
        if (type == EntityType.CREEPER)         return 9;
        if (type == EntityType.CAVE_SPIDER)     return 7;
        if (type == EntityType.SPIDER)          return 6;
        if (type == EntityType.SKELETON)        return 6;
        if (type == EntityType.DROWNED)         return 6;
        if (type == EntityType.STRAY)           return 6;
        if (type == EntityType.ZOMBIE)          return 5;
        if (type == EntityType.HUSK)            return 5;
        if (type == EntityType.SLIME)           return 4;
        return 4;
    }
}
