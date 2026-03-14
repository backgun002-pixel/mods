package com.example.cosmod.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class DungeonTicketItem extends Item {

    public DungeonTicketItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        // 아이템 1개 소모
        ItemStack stack = player.getItemInHand(hand);
        stack.shrink(1);

        DungeonManager.teleportToDungeon(sp);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.literal("§7우클릭 시 지하 석굴 던전으로 텔레포트"));
        consumer.accept(Component.literal("§c사용 시 아이템이 소모됩니다"));
        consumer.accept(Component.literal("§e입장 레벨 제한 없음"));
    }
}
