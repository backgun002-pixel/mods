package com.example.cosmod.entity;

import com.example.cosmod.CosmodMod;
import com.example.cosmod.dungeon.entity.DungeonGuardianEntity;
import com.example.cosmod.dungeon.entity.EliteStoneGuardEntity;
import com.example.cosmod.dungeon.entity.StoneGolemEntity;
import com.example.cosmod.dungeon.entity.StoneGuardEntity;
import com.example.cosmod.job.JobNpcEntity;
import com.example.cosmod.storage.StorageNpcEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class CosmodEntities {

    public static EntityType<ShopNpcEntity> SHOP_NPC;
    public static EntityType<JobNpcEntity> JOB_NPC;
    public static EntityType<StorageNpcEntity> STORAGE_NPC;

    // 던전 엔티티
    public static EntityType<StoneGuardEntity>      STONE_GUARD;
    public static EntityType<StoneGolemEntity>      STONE_GOLEM;
    public static EntityType<EliteStoneGuardEntity> ELITE_STONE_GUARD;
    public static EntityType<DungeonGuardianEntity> DUNGEON_GUARDIAN;

    public static void register() {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
            BuiltInRegistries.ENTITY_TYPE.key(),
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "shop_npc")
        );

        SHOP_NPC = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            key,
            EntityType.Builder.<ShopNpcEntity>of(ShopNpcEntity::new, MobCategory.CREATURE)
                .sized(0.6f, 1.95f)
                .build(key)
        );

        // 필수: 엔티티 속성 등록 (없으면 소환 불가)
        FabricDefaultAttributeRegistry.register(SHOP_NPC, ShopNpcEntity.createAttributes());

        // 직업 NPC
        ResourceKey<EntityType<?>> jobKey = ResourceKey.create(
            BuiltInRegistries.ENTITY_TYPE.key(),
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "job_npc")
        );
        JOB_NPC = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, jobKey,
            EntityType.Builder.<JobNpcEntity>of(JobNpcEntity::new, MobCategory.CREATURE)
                .sized(0.6f, 1.95f).build(jobKey)
        );
        FabricDefaultAttributeRegistry.register(JOB_NPC, JobNpcEntity.createAttributes());

        // 아이템 보관소 NPC
        ResourceKey<EntityType<?>> storageKey = ResourceKey.create(
            BuiltInRegistries.ENTITY_TYPE.key(),
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "storage_npc")
        );
        STORAGE_NPC = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, storageKey,
            EntityType.Builder.<StorageNpcEntity>of(StorageNpcEntity::new, MobCategory.CREATURE)
                .sized(0.6f, 1.95f).build(storageKey)
        );
        FabricDefaultAttributeRegistry.register(STORAGE_NPC, StorageNpcEntity.createAttributes());

        // 던전 몬스터 등록
        ResourceKey<EntityType<?>> sgKey = ResourceKey.create(
            BuiltInRegistries.ENTITY_TYPE.key(),
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "stone_guard"));
        STONE_GUARD = Registry.register(BuiltInRegistries.ENTITY_TYPE, sgKey,
            EntityType.Builder.<StoneGuardEntity>of(StoneGuardEntity::new, MobCategory.MONSTER)
                .sized(0.6f, 1.95f).build(sgKey));
        FabricDefaultAttributeRegistry.register(STONE_GUARD, StoneGuardEntity.createAttributes());

        ResourceKey<EntityType<?>> sgoKey = ResourceKey.create(
            BuiltInRegistries.ENTITY_TYPE.key(),
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "stone_golem"));
        STONE_GOLEM = Registry.register(BuiltInRegistries.ENTITY_TYPE, sgoKey,
            EntityType.Builder.<StoneGolemEntity>of(StoneGolemEntity::new, MobCategory.MONSTER)
                .sized(1.0f, 2.0f).build(sgoKey));
        FabricDefaultAttributeRegistry.register(STONE_GOLEM, StoneGolemEntity.createAttributes());

        ResourceKey<EntityType<?>> esgKey = ResourceKey.create(
            BuiltInRegistries.ENTITY_TYPE.key(),
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "elite_stone_guard"));
        ELITE_STONE_GUARD = Registry.register(BuiltInRegistries.ENTITY_TYPE, esgKey,
            EntityType.Builder.<EliteStoneGuardEntity>of(EliteStoneGuardEntity::new, MobCategory.MONSTER)
                .sized(0.6f, 1.95f).build(esgKey));
        FabricDefaultAttributeRegistry.register(ELITE_STONE_GUARD, EliteStoneGuardEntity.createAttributes());

        ResourceKey<EntityType<?>> dgKey = ResourceKey.create(
            BuiltInRegistries.ENTITY_TYPE.key(),
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "dungeon_guardian"));
        DUNGEON_GUARDIAN = Registry.register(BuiltInRegistries.ENTITY_TYPE, dgKey,
            EntityType.Builder.<DungeonGuardianEntity>of(DungeonGuardianEntity::new, MobCategory.MONSTER)
                .sized(1.2f, 2.5f).build(dgKey));
        FabricDefaultAttributeRegistry.register(DUNGEON_GUARDIAN, DungeonGuardianEntity.createAttributes());

        CosmodMod.LOGGER.info("[Cosmod] NPC 엔티티 등록 완료");
    }
}
