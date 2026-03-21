package com.example.cosmod;

import com.example.cosmod.combat.CombatItems;
import com.example.cosmod.combat.SetBonusHandler;
import com.example.cosmod.stat.StatSyncHandler;
import com.example.cosmod.job.JobNpcNetwork;
import com.example.cosmod.job.JobExpHandler;
import com.example.cosmod.job.PlayerJobManager;
import com.example.cosmod.dungeon.DungeonItems;
import com.example.cosmod.dungeon.DungeonGimmickHandler;
import com.example.cosmod.skill.SkillHandler;
import com.example.cosmod.job.JobCheatCommand;
import com.example.cosmod.job.CosmodAttachments;
import com.example.cosmod.skill.SkillJobSyncPayload;
import com.example.cosmod.job.JobExpSyncPayload;
import com.example.cosmod.skill.SkillUnlockPayload;
import com.example.cosmod.storage.StorageNpcNetwork;
import com.example.cosmod.storage.DeathEventHandler;
import com.example.cosmod.codex.CodexNetwork;
import com.example.cosmod.weapon.CosmodWeapons;
import com.example.cosmod.weapon.WeaponSkillItem;
import com.example.cosmod.weapon.WeaponSkillManager;
import com.example.cosmod.weapon.WeaponAttackHandler;
import com.example.cosmod.weapon.WeaponCooldownPayload;
import com.example.cosmod.weapon.impl.FlameKingSwordManager;
import com.example.cosmod.codex.CodexRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import com.example.cosmod.combat.EnhanceTableNetwork;
import com.example.cosmod.combat.GemRerollNetwork;
import com.example.cosmod.combat.GearItem;
import com.example.cosmod.crop.CosmodCrops;
import com.example.cosmod.network.CosmeticSyncNetwork;
import com.example.cosmod.economy.CosmodEconomyItems;
import com.example.cosmod.economy.ShopRegistry;
import com.example.cosmod.economy.ShopServerNetwork;
import com.example.cosmod.entity.CosmodEntities;
import com.example.cosmod.item.CosmodItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CosmodMod implements ModInitializer {
    public static final String MOD_ID = "cosmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CosmodItems.register();
        CosmodEconomyItems.register();
        CombatItems.register();
        SetBonusHandler.register();
        StatSyncHandler.register();
        JobNpcNetwork.register();
        JobExpHandler.register();
        var _att = CosmodAttachments.JOB_DATA;
        PlayerJobManager.registerEvents();
        DungeonItems.register();
        DungeonGimmickHandler.register();
        PayloadTypeRegistry.playS2C().register(SkillJobSyncPayload.TYPE, SkillJobSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JobExpSyncPayload.TYPE, JobExpSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SkillUnlockPayload.TYPE, SkillUnlockPayload.CODEC);
        StorageNpcNetwork.register();
        SkillHandler.register();
        JobCheatCommand.register();
        CosmodCrops.register();
        CosmodEntities.register();
        DeathEventHandler.register();
        CosmodWeapons.register();
        WeaponCooldownPayload.register();
        WeaponSkillManager.register();
        WeaponAttackHandler.register();
        FlameKingSwordManager.register();
        CodexNetwork.register();
        CodexRegistry.initCustomItems();
        CosmeticSyncNetwork.registerServer();
        ShopRegistry.init();
        ShopServerNetwork.register();
        EnhanceTableNetwork.registerServer();
        GemRerollNetwork.registerServer();
        registerCreativeTab();
        LOGGER.info("[Cosmod] 초기화 완료!");
    }

    private static void registerCreativeTab() {
        ResourceKey<CreativeModeTab> key = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MOD_ID, "cosmod_tab")
        );
        CreativeModeTab tab = FabricItemGroup.builder()
            .title(Component.literal("Cosmod"))
            .icon(() -> new ItemStack(CosmodEconomyItems.COSMO_COIN))
            .displayItems((params, output) -> {
                output.accept(CosmodItems.COSME_HAT);
                output.accept(CosmodItems.COSME_SHIRT);
                output.accept(CosmodItems.COSME_PANTS);
                output.accept(CosmodItems.COSME_SHOES);
                output.accept(CosmodEconomyItems.COSMO_COIN);
                output.accept(CosmodCrops.TOMATO);
                output.accept(CosmodCrops.PEPPER);
                output.accept(CosmodCrops.CORN);
                output.accept(CosmodCrops.TOMATO_SEEDS);
                output.accept(CosmodCrops.PEPPER_SEEDS);
                output.accept(CosmodCrops.CORN_SEEDS);
                output.accept(new ItemStack(CosmodCrops.FERTILE_SOIL));
                output.accept(new ItemStack(CosmodCrops.SPRINKLER));
                output.accept(CosmodCrops.WATERING_CAN);
                output.accept(GearItem.createWithOptions(CombatItems.DAGGER));
                output.accept(GearItem.createWithOptions(CombatItems.SWORD));
                output.accept(GearItem.createWithOptions(CombatItems.GREATSWORD));
                output.accept(GearItem.createWithOptions(CombatItems.SHORTBOW));
                output.accept(GearItem.createWithOptions(CombatItems.LONGBOW));
                output.accept(new net.minecraft.world.item.ItemStack(CosmodWeapons.FROST_BLADE));
                output.accept(new net.minecraft.world.item.ItemStack(CosmodWeapons.FLAME_SWORD));
                output.accept(new net.minecraft.world.item.ItemStack(CosmodWeapons.THUNDER_BLADE));
                output.accept(new net.minecraft.world.item.ItemStack(CosmodWeapons.LAVA_MAUL));
                output.accept(new net.minecraft.world.item.ItemStack(CosmodWeapons.WIND_REAPER));
                output.accept(GearItem.createWithOptions(CombatItems.JSH_HELMET));
                output.accept(GearItem.createWithOptions(CombatItems.JSH_CHESTPLATE));
                output.accept(GearItem.createWithOptions(CombatItems.JSH_LEGGINGS));
                output.accept(GearItem.createWithOptions(CombatItems.JSH_BOOTS));
                output.accept(GearItem.createWithOptions(CombatItems.PJH_HELMET));
                output.accept(GearItem.createWithOptions(CombatItems.PJH_CHESTPLATE));
                output.accept(GearItem.createWithOptions(CombatItems.PJH_LEGGINGS));
                output.accept(GearItem.createWithOptions(CombatItems.PJH_BOOTS));
                output.accept(GearItem.createWithOptions(CombatItems.YYJ_HELMET));
                output.accept(GearItem.createWithOptions(CombatItems.YYJ_CHESTPLATE));
                output.accept(GearItem.createWithOptions(CombatItems.YYJ_LEGGINGS));
                output.accept(GearItem.createWithOptions(CombatItems.YYJ_BOOTS));
                output.accept(CombatItems.WEAPON_BOX);
                output.accept(CombatItems.ARMOR_BOX);
                output.accept(new ItemStack(CombatItems.ENHANCE_TABLE));
                output.accept(CombatItems.ENHANCE_STONE_BASIC);
                output.accept(CombatItems.ENHANCE_STONE_MID);
                output.accept(CombatItems.ENHANCE_STONE_HIGH);
                output.accept(CombatItems.GEM_BASIC);
                output.accept(CombatItems.GEM_MID);
                output.accept(CombatItems.GEM_HIGH);
                output.accept(CosmodItems.RUBY);
                output.accept(CosmodItems.SAPPHIRE);
                output.accept(CosmodItems.RED_DIAMOND);
                output.accept(DungeonItems.DUNGEON_TICKET);
            })
            .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key, tab);
    }
}
