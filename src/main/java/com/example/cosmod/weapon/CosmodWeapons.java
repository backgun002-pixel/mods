package com.example.cosmod.weapon;

import com.example.cosmod.CosmodMod;
import com.example.cosmod.weapon.impl.FrostBlade;
import com.example.cosmod.weapon.impl.FlameKingSword;
import com.example.cosmod.weapon.impl.ThunderBlade;
import com.example.cosmod.weapon.impl.LavaMaul;
import com.example.cosmod.weapon.impl.WindReaper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class CosmodWeapons {

    public static WeaponSkillItem FROST_BLADE;
    public static WeaponSkillItem FLAME_SWORD;
    public static WeaponSkillItem THUNDER_BLADE;
    public static WeaponSkillItem LAVA_MAUL;
    public static WeaponSkillItem WIND_REAPER;

    public static void register() {
        FROST_BLADE   = reg("frost_blade",   new FrostBlade(props("frost_blade")));
        FLAME_SWORD   = reg("flame_sword",   new FlameKingSword(props("flame_sword")));
        THUNDER_BLADE = reg("thunder_blade", new ThunderBlade(props("thunder_blade")));
        LAVA_MAUL     = reg("lava_maul",     new LavaMaul(props("lava_maul")));
        WIND_REAPER   = reg("windreaper",     new WindReaper(props("windreaper")));
    }

    // 1.21.11: Item.Properties에 setId() 필수
    private static Item.Properties props(String id) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, id));
        return new Item.Properties()
            .setId(key)
            .stacksTo(1)
            .durability(1000);
    }

    private static <T extends WeaponSkillItem> T reg(String id, T item) {
        Registry.register(BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, id), item);
        return item;
    }
}
