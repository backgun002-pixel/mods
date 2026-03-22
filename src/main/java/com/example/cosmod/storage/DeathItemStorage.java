package com.example.cosmod.storage;

import com.mojang.serialization.DynamicOps;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DeathItemStorage {

    public static final AttachmentType<CompoundTag> STORED_ITEMS =
        AttachmentRegistry.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC)
            .buildAndRegister(Identifier.fromNamespaceAndPath("cosmod", "death_items"));

    public static void saveItems(ServerPlayer player, List<ItemStack> items) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();

        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                var result = ItemStack.CODEC.encodeStart(ops, stack);
                if (result.isSuccess()) {
                    list.add(result.getOrThrow());
                }
            }
        }

        tag.put("items", list);
        tag.putLong("time", System.currentTimeMillis());
        tag.putInt("cost", DeathItemCostCalculator.calculateCost(items));
        player.setAttached(STORED_ITEMS, tag);


    }

    public static List<ItemStack> getItems(ServerPlayer player) {
        CompoundTag tag = player.getAttachedOrElse(STORED_ITEMS, null);
        if (tag == null || !tag.contains("items")) return List.of();

        List<ItemStack> result = new ArrayList<>();
        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag rawList = tag.get("items");
        if (!(rawList instanceof ListTag list)) return List.of();

        for (Tag entry : list) {
            ItemStack.CODEC.parse(ops, entry)
                .resultOrPartial(err -> {})
                .ifPresent(result::add);
        }
        return result;
    }

    public static int getCost(ServerPlayer player) {
        CompoundTag tag = player.getAttachedOrElse(STORED_ITEMS, null);
        if (tag == null) return 0;
        return tag.getInt("cost").orElse(0);
    }

    public static boolean hasItems(ServerPlayer player) {
        CompoundTag tag = player.getAttachedOrElse(STORED_ITEMS, null);
        if (tag == null || !tag.contains("items")) return false;
        Tag rawList = tag.get("items");
        return rawList instanceof ListTag lt && !lt.isEmpty();
    }

    public static void clearItems(ServerPlayer player) {
        player.setAttached(STORED_ITEMS, new CompoundTag());
    }
}
