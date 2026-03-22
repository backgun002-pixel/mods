package com.example.cosmod.combat;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class EnhanceMenuType {

    public static MenuType<EnhanceMenu> TYPE;

    public static void register() {
        TYPE = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath("cosmod", "enhance"),
            new MenuType<>((syncId, inv) -> new EnhanceMenu(syncId, inv),
                FeatureFlags.VANILLA_SET)
        );
    }
}
