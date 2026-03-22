package com.example.cosmod.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

public class FertileSoilBlock extends Block {

    public FertileSoilBlock(Properties properties) {
        super(properties);
    }

    // Farmland 대신 단순 블록으로 - 클릭 감지 문제 방지
    public static Properties createProperties(net.minecraft.resources.ResourceKey<Block> key) {
        return Block.Properties.of()
            .strength(0.6f)
            .sound(SoundType.GRAVEL)
            .setId(key);
    }
}
