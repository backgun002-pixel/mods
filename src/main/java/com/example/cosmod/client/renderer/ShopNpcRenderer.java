package com.example.cosmod.client.renderer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import com.example.cosmod.entity.ShopNpcEntity;
import com.example.cosmod.CosmodMod;

public class ShopNpcRenderer extends EntityRenderer<ShopNpcEntity, ShopNpcRenderState> {

    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "shop_npc"), "main");

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    private final ShopNpcModel model;

    public ShopNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new ShopNpcModel(ctx.bakeLayer(LAYER));
    }

    @Override
    public ShopNpcRenderState createRenderState() {
        return new ShopNpcRenderState();
    }

    public Identifier getTextureLocation(ShopNpcRenderState state) {
        return TEXTURE;
    }
}
