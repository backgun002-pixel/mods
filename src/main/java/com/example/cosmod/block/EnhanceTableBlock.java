package com.example.cosmod.block;

import com.example.cosmod.combat.EnhanceTableNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EnhanceTableBlock extends Block {

    public EnhanceTableBlock(Properties props) {
        super(props);
    }

    public static Properties createProperties(net.minecraft.resources.ResourceKey<Block> key) {
        return Block.Properties.of()
            .strength(3.0f)
            .sound(SoundType.ANVIL)
            .setId(key);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level,
                                            BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            EnhanceTableNetwork.sendOpenGui(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
