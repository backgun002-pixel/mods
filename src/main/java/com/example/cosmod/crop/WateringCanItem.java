package com.example.cosmod.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WateringCanItem extends Item {

    public WateringCanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof CosmodCropBlock) {
            if (!state.getValue(CosmodCropBlock.WATERED)) {
                if (!level.isClientSide()) {
                    level.setBlock(pos, CosmodCropBlock.water(state), 3);
                    if (ctx.getPlayer() != null) {
                        ctx.getPlayer().displayClientMessage(
                            Component.literal("§b물을 줬어요! 💧"), true);
                    }
                }
                return InteractionResult.SUCCESS;
            } else {
                if (!level.isClientSide() && ctx.getPlayer() != null) {
                    ctx.getPlayer().displayClientMessage(
                        Component.literal("§7이미 물을 받았어요."), true);
                }
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.PASS;
    }
}
