package com.example.cosmod.client.renderer;

import com.example.cosmod.CosmodMod;
import com.example.cosmod.storage.StorageNpcEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class StorageNpcRenderer extends EntityRenderer<StorageNpcEntity, EntityRenderState> {

    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "storage_npc"), "main");

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/alex.png");

    private final ShopNpcModel model;

    public StorageNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new ShopNpcModel(ctx.bakeLayer(ShopNpcRenderer.LAYER));
    }

    @Override
    public EntityRenderState createRenderState() { return new EntityRenderState(); }

    public Identifier getTextureLocation(EntityRenderState state) { return TEXTURE; }
}
