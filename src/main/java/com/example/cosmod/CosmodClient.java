package com.example.cosmod;

import com.example.cosmod.client.CosmodClientNetwork;
import com.example.cosmod.client.CosmeticSlotHandler;
import com.example.cosmod.client.renderer.ShopNpcModel;
import com.example.cosmod.client.renderer.ShopNpcRenderer;
import com.example.cosmod.client.renderer.JobNpcRenderer;
import com.example.cosmod.client.renderer.JobNpcModel;
import com.example.cosmod.entity.CosmodEntities;
import com.example.cosmod.job.JobExpSyncPayload;
import com.example.cosmod.job.JobExpClientCache;
import com.example.cosmod.job.JobExpHud;
import com.example.cosmod.skill.SkillKey;
import com.example.cosmod.skill.SkillHud;
import com.example.cosmod.skill.SkillClientHandler;
import com.example.cosmod.skill.SkillUnlockPayload;
import com.example.cosmod.skill.SkillUnlockHud;
import com.example.cosmod.storage.OpenStoragePayload;
import com.example.cosmod.codex.CodexKey;
import com.example.cosmod.codex.CodexClientHandler;
import com.example.cosmod.codex.CodexSyncPayload;
import com.example.cosmod.codex.CodexClientCache;
import com.example.cosmod.client.screen.StorageScreen;
import com.example.cosmod.client.renderer.StorageNpcRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CosmodClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(ShopNpcRenderer.LAYER, ShopNpcModel::createBodyLayer);
        EntityRendererRegistry.register(CosmodEntities.SHOP_NPC, ShopNpcRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(JobNpcRenderer.LAYER, JobNpcModel::createBodyLayer);
        EntityRendererRegistry.register(CosmodEntities.JOB_NPC, JobNpcRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(StorageNpcRenderer.LAYER, com.example.cosmod.client.renderer.ShopNpcModel::createBodyLayer);
        EntityRendererRegistry.register(CosmodEntities.STORAGE_NPC, StorageNpcRenderer::new);

        CosmeticSlotHandler.register();
        CosmodClientNetwork.register();
        SkillKey.register();
        CodexKey.register();
        CodexClientHandler.register();
        SkillClientHandler.register();
        SkillHud.register();
        JobExpHud.register();
        SkillUnlockHud.register();

        ClientPlayNetworking.registerGlobalReceiver(JobExpSyncPayload.TYPE, (payload, ctx) -> {
            JobExpClientCache.set(payload.level(), payload.exp(), payload.expToNext(), payload.levelUp());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            JobExpClientCache.tick();
            SkillUnlockHud.tick();
        });
        ClientPlayNetworking.registerGlobalReceiver(SkillUnlockPayload.TYPE, (payload, ctx) -> {
            SkillUnlockHud.trigger(payload.skillName());
        });
        ClientPlayNetworking.registerGlobalReceiver(CodexSyncPayload.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> CodexClientCache.update(payload.farmerIds(), payload.minerIds()));
        });
        ClientPlayNetworking.registerGlobalReceiver(OpenStoragePayload.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> ctx.client().setScreen(new StorageScreen(payload.items(), payload.cost())));
        });
    }
}
