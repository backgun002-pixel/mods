package com.example.cosmod.dungeon;

import com.example.cosmod.CosmodMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class DungeonItems {

    public static DungeonTicketItem DUNGEON_TICKET;

    public static void register() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "dungeon_ticket"));
        DUNGEON_TICKET = new DungeonTicketItem(
            new Item.Properties().setId(key).stacksTo(16));
        Registry.register(BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "dungeon_ticket"),
            DUNGEON_TICKET);
    }
}
