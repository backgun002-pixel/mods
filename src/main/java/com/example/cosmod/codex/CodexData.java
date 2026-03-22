package com.example.cosmod.codex;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/** 플레이어 도감 등록 데이터 (서버 저장) */
public class CodexData {

    public static final AttachmentType<CompoundTag> CODEX =
        AttachmentRegistry.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC)
            .buildAndRegister(Identifier.fromNamespaceAndPath("cosmod", "codex"));

    public enum Tab { FARMER, MINER }

    // ── 서버: 등록 ────────────────────────────────────────────────
    public static void register(ServerPlayer player, Tab tab, String id) {
        CompoundTag tag = player.getAttachedOrElse(CODEX, new CompoundTag());
        String key = tab.name() + ":" + id;
        tag.putBoolean(key, true);
        player.setAttached(CODEX, tag);
    }

    public static boolean isRegistered(ServerPlayer player, Tab tab, String id) {
        CompoundTag tag = player.getAttachedOrElse(CODEX, null);
        if (tag == null) return false;
        return tag.getBoolean(tab.name() + ":" + id).orElse(false);
    }

    public static Set<String> getRegistered(ServerPlayer player, Tab tab) {
        CompoundTag tag = player.getAttachedOrElse(CODEX, null);
        Set<String> result = new HashSet<>();
        if (tag == null) return result;
        String prefix = tab.name() + ":";
        for (String key : tag.keySet()) {
            if (key.startsWith(prefix) && tag.getBoolean(key).orElse(false)) {
                result.add(key.substring(prefix.length()));
            }
        }
        return result;
    }
}
