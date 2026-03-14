package com.example.cosmod.crop;

import com.example.cosmod.block.FertileSoilBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CropSeedItem extends Item {

    private final CosmodCropBlock cropBlock;

    public CropSeedItem(Properties properties, CosmodCropBlock cropBlock) {
        super(properties);
        this.cropBlock = cropBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        BlockState soil = level.getBlockState(pos);

        // 클릭한 면이 UP이 아니면
        if (face != Direction.UP) {
            if (!level.isClientSide() && ctx.getPlayer() != null)
                ctx.getPlayer().displayClientMessage(
                    Component.literal("§c윗면을 클릭하세요!"), true);
            return InteractionResult.PASS;
        }

        // 비옥한 땅이 아니면
        if (!(soil.getBlock() instanceof FertileSoilBlock)) {
            if (!level.isClientSide() && ctx.getPlayer() != null)
                ctx.getPlayer().displayClientMessage(
                    Component.literal("§c비옥한 땅 위에만 심을 수 있어요!"), true);
            return InteractionResult.PASS;
        }

        BlockPos plantPos = pos.above();

        // 위 블록이 비어있지 않으면
        if (!level.isEmptyBlock(plantPos)) {
            if (!level.isClientSide() && ctx.getPlayer() != null)
                ctx.getPlayer().displayClientMessage(
                    Component.literal("§c위에 공간이 없어요!"), true);
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            level.setBlock(plantPos, cropBlock.defaultBlockState(), 3);
            ctx.getItemInHand().shrink(1);
            ctx.getPlayer().displayClientMessage(
                Component.literal("§a씨앗을 심었어요! 🌱"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
