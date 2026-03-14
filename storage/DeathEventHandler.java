package com.example.cosmod.storage;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DeathEventHandler {

    public static void register() {

        // AFTER_DEATH: 플레이어가 실제로 죽은 직후 (아이템 드롭 전)
        // 하지만 AFTER_DEATH는 드롭을 막지 못함
        // → keepInventory처럼 gamerule을 끄고 아이템을 직접 가로챔
        // → ServerPlayerEvents.COPY_FROM (리스폰 시)이 아닌
        //    ALLOW_DEATH에서 인벤토리 수집 후 바닐라 드롭을 막으려면
        //    keepInventory를 잠깐 켜는 방식 사용

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayer player)) return true;
            if (player.level().isClientSide()) return true;

            // 이미 보관 중인 아이템이 있으면 스킵 (중복 저장 방지)
            if (DeathItemStorage.hasItems(player)) return true;

            List<ItemStack> toSave = new ArrayList<>();

            // 메인 인벤토리 수집 후 비우기
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    toSave.add(stack.copy());
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }

            // 방어구 슬롯
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    toSave.add(stack.copy());
                    player.setItemSlot(slot, ItemStack.EMPTY);
                }
            }

            // 오프핸드
            ItemStack offhand = player.getItemBySlot(EquipmentSlot.OFFHAND);
            if (!offhand.isEmpty()) {
                toSave.add(offhand.copy());
                player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }

            if (!toSave.isEmpty()) {
                DeathItemStorage.saveItems(player, toSave);
                int cost = DeathItemStorage.getCost(player);
                // sendSystemMessage: 사망 후에도 클라이언트에 전달됨
                player.sendSystemMessage(Component.literal(
                    "§c☠ 사망! §f아이템 §e" + toSave.size() + "§f종이 보관소에 저장됐습니다. " +
                    "§b[아이템 관리인 NPC]§f에서 §e" + cost + " 코인§f으로 회수하세요."));
            }

            return true; // 실제로 죽게 허용 (인벤토리는 이미 비었으므로 드롭 없음)
        });
    }
}
