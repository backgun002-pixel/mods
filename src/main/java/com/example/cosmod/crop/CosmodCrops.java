package com.example.cosmod.crop;

import com.example.cosmod.CosmodMod;
import com.example.cosmod.block.FertileSoilBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class CosmodCrops {

    // 씨앗 (CropSeedItem)
    public static CropSeedItem TOMATO_SEEDS;
    public static CropSeedItem PEPPER_SEEDS;
    public static CropSeedItem CORN_SEEDS;

    // 수확물
    public static Item TOMATO;
    public static Item PEPPER;
    public static Item CORN;

    // 작물 블록
    public static CosmodCropBlock TOMATO_CROP;
    public static CosmodCropBlock PEPPER_CROP;
    public static CosmodCropBlock CORN_CROP;

    // 특수 블록
    public static FertileSoilBlock FERTILE_SOIL;
    public static SprinklerBlock   SPRINKLER;

    // 도구
    public static WateringCanItem  WATERING_CAN;

    public static void register() {
        // 수확물
        TOMATO = registerItem("tomato");
        PEPPER = registerItem("pepper");
        CORN   = registerItem("corn");

        // 작물 블록 (씨앗 참조는 나중에 연결)
        TOMATO_CROP = registerCropBlock("tomato_crop", () -> TOMATO_SEEDS, () -> TOMATO, 1, 3);
        PEPPER_CROP = registerCropBlock("pepper_crop", () -> PEPPER_SEEDS, () -> PEPPER, 1, 3);
        CORN_CROP   = registerCropBlock("corn_crop",   () -> CORN_SEEDS,   () -> CORN,   1, 2);

        // 씨앗 (CropSeedItem - 비옥한 땅 위에 심는 특수 아이템)
        TOMATO_SEEDS = registerSeed("tomato_seeds", TOMATO_CROP);
        PEPPER_SEEDS = registerSeed("pepper_seeds", PEPPER_CROP);
        CORN_SEEDS   = registerSeed("corn_seeds",   CORN_CROP);

        // 비옥한 땅
        ResourceKey<Block> soilBlockKey = blockKey("fertile_soil");
        ResourceKey<Item>  soilItemKey  = itemKey("fertile_soil");
        FERTILE_SOIL = new FertileSoilBlock(
            FertileSoilBlock.createProperties(soilBlockKey)
        );
        Registry.register(BuiltInRegistries.BLOCK, soilBlockKey, FERTILE_SOIL);
        Registry.register(BuiltInRegistries.ITEM, soilItemKey,
            new BlockItem(FERTILE_SOIL, new Item.Properties().setId(soilItemKey)));

        // 스프링클러
        ResourceKey<Block> sprinklerBlockKey = blockKey("sprinkler");
        ResourceKey<Item>  sprinklerItemKey  = itemKey("sprinkler");
        SPRINKLER = new SprinklerBlock(
            Block.Properties.of().strength(1.5f)
                .sound(SoundType.METAL).setId(sprinklerBlockKey)
        );
        Registry.register(BuiltInRegistries.BLOCK, sprinklerBlockKey, SPRINKLER);
        Registry.register(BuiltInRegistries.ITEM, sprinklerItemKey,
            new BlockItem(SPRINKLER, new Item.Properties().setId(sprinklerItemKey)));

        // 물뿌리개
        ResourceKey<Item> canKey = itemKey("watering_can");
        WATERING_CAN = new WateringCanItem(
            new Item.Properties().stacksTo(1).durability(64).setId(canKey)
        );
        Registry.register(BuiltInRegistries.ITEM, canKey, WATERING_CAN);

        CosmodMod.LOGGER.info("[Cosmod] 작물 시스템 등록 완료");
    }

    private static CosmodCropBlock registerCropBlock(String id,
            java.util.function.Supplier<Item> seed,
            java.util.function.Supplier<Item> harvest,
            int minH, int maxH) {
        ResourceKey<Block> key = blockKey(id);
        Block.Properties props = Block.Properties.of()
            .noCollision().randomTicks().instabreak().noOcclusion().setId(key);
        CosmodCropBlock block = new CosmodCropBlock(props, seed, harvest, minH, maxH);
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        return block;
    }

    private static Item registerItem(String id) {
        ResourceKey<Item> key = itemKey(id);
        Item item = new Item(new Item.Properties().stacksTo(64).setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    private static CropSeedItem registerSeed(String id, CosmodCropBlock crop) {
        ResourceKey<Item> key = itemKey(id);
        CropSeedItem item = new CropSeedItem(new Item.Properties().stacksTo(64).setId(key), crop);
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, path));
    }
    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, path));
    }
}
