package com.example.cosmod.client.renderer;

import com.example.cosmod.CosmodMod;
import com.example.cosmod.dungeon.entity.EliteStoneGuardEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class EliteStoneGuardRenderer
        extends LivingEntityRenderer<EliteStoneGuardEntity, LivingEntityRenderState, EliteStoneGuardModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "elite_stone_guard"), "main");

    private static final Identifier TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");

    public EliteStoneGuardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EliteStoneGuardModel(ctx.bakeLayer(LAYER)), 0.7f);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
