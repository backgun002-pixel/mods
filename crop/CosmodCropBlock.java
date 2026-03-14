package com.example.cosmod.crop;

import com.example.cosmod.block.FertileSoilBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.Supplier;

public class CosmodCropBlock extends CropBlock {

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE     = IntegerProperty.create("age", 0, MAX_AGE);
    public static final BooleanProperty WATERED = BooleanProperty.create("watered");

    private static final VoxelShape[] SHAPES = {
        Block.box(0, 0, 0, 16,  4, 16),
        Block.box(0, 0, 0, 16,  7, 16),
        Block.box(0, 0, 0, 16, 11, 16),
        Block.box(0, 0, 0, 16, 16, 16),
    };

    private final Supplier<Item> seedItem;
    private final Supplier<Item> harvestItem;
    private final int minHarvest;
    private final int maxHarvest;

    public CosmodCropBlock(Properties properties, Supplier<Item> seedItem,
                           Supplier<Item> harvestItem, int minHarvest, int maxHarvest) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(AGE, 0).setValue(WATERED, false));
        this.seedItem    = seedItem;
        this.harvestItem = harvestItem;
        this.minHarvest  = minHarvest;
        this.maxHarvest  = maxHarvest;
    }

    @Override public IntegerProperty getAgeProperty() { return AGE; }
    @Override public int getMaxAge()                  { return MAX_AGE; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world,
                               BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.getBlock() instanceof FertileSoilBlock;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world,
                           BlockPos pos, RandomSource random) {
        if (world.getRawBrightness(pos, 0) < 9) return;
        // 물을 받은 경우에만 성장
        if (!state.getValue(WATERED)) return;
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            if (random.nextInt(5) == 0) {
                // 성장 후 watered 리셋 (다시 물 줘야 함)
                world.setBlock(pos, state.setValue(AGE, age + 1).setValue(WATERED, false), 2);
            }
        }
    }

    // 물뿌리개로 물 주기
    public static BlockState water(BlockState state) {
        return state.setValue(WATERED, true);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) return List.of(new ItemStack(seedItem.get(), 1));
        int count = minHarvest + params.getLevel().random.nextInt(maxHarvest - minHarvest + 1);
        return List.of(
            new ItemStack(seedItem.get(),    1),
            new ItemStack(harvestItem.get(), count)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, WATERED);
    }
}
