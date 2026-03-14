package com.example.cosmod.codex;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class CodexMenuType {

    public static MenuType<CodexMenu> FARMER_TYPE;
    public static MenuType<CodexMenu> MINER_TYPE;

    public static void register() {
        FARMER_TYPE = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath("cosmod", "codex_farmer"),
            new MenuType<>((syncId, inv) -> new CodexMenu(syncId, inv, CodexData.Tab.FARMER),
                FeatureFlags.VANILLA_SET)
        );
        MINER_TYPE = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath("cosmod", "codex_miner"),
            new MenuType<>((syncId, inv) -> new CodexMenu(syncId, inv, CodexData.Tab.MINER),
                FeatureFlags.VANILLA_SET)
        );
    }

    public static void openFor(ServerPlayer player, CodexData.Tab tab) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.literal(tab == CodexData.Tab.FARMER ? "농부 도감" : "광부 도감");
            }
            @Override public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new CodexMenu(syncId, inv, tab);
            }
        });
    }
}
