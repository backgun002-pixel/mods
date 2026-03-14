package com.example.cosmod.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SprinklerBlock extends Block {

    public SprinklerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world,
                           BlockPos pos, RandomSource random) {
        // 5x5 범위 내 작물에 자동으로 물 주기
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos target = pos.offset(dx, dy, dz);
                    BlockState targetState = world.getBlockState(target);
                    if (targetState.getBlock() instanceof CosmodCropBlock
                            && !targetState.getValue(CosmodCropBlock.WATERED)) {
                        world.setBlock(target, CosmodCropBlock.water(targetState), 3);
                    }
                }
            }
        }
    }
}
