package com.example.cosmod.combat;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class EnhanceTableNetwork {

    public static void sendOpenGui(ServerPlayer player) {
        ServerPlayNetworking.send(player, new OpenEnhancePayload());
    }

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(OpenEnhancePayload.TYPE, OpenEnhancePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnhanceResultPayload.TYPE, EnhanceResultPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EnhanceRequestPayload.TYPE, EnhanceRequestPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(EnhanceRequestPayload.TYPE,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();

                // 클라이언트에서 아이템 자체를 전송받음 (슬롯 인덱스 방식 X)
                ItemStack gear  = payload.gearItem().copy();
                ItemStack stone = payload.stoneItem().copy();
                int stoneType   = payload.stoneType();

                if (gear.isEmpty() || !(gear.getItem() instanceof GearItem)) {
                    msg(player, "§c강화 가능한 장비가 아닙니다."); return;
                }
                if (stone.isEmpty()) {
                    msg(player, "§c강화석 또는 보석이 없습니다."); return;
                }

                // ── 보석 장착 (stoneType == 3) ───────────────────
                if (stoneType == 3) {
                    if (!(stone.getItem() instanceof GemItem gi)) {
                        msg(player, "§c보석 아이템이 아닙니다."); return;
                    }
                    if (!GemItem.isIdentified(stone)) {
                        msg(player, "§c감정되지 않은 보석입니다. 먼저 우클릭으로 감정하세요."); return;
                    }
                    var gearTag = GearItem.getGearTag(gear);
                    if (gearTag.contains("enhanced") && gearTag.getBoolean("enhanced").orElse(false)) {
                        msg(player, "§c이미 보석이 장착된 장비입니다."); return;
                    }
                    net.minecraft.nbt.CompoundTag gemTag = GemItem.getGemTag(stone);
                    int lines = gi.getTier().optionLines;
                    for (int ii = 0; ii < lines; ii++) {
                        if (!gemTag.contains("gem_type_" + ii)) continue;
                        int optType = gemTag.getInt("gem_type_" + ii).orElse(0);
                        int val     = gemTag.getInt("gem_val_"  + ii).orElse(0);
                        gearTag.putInt("gem_opt_type_" + ii, optType);
                        gearTag.putInt("gem_opt_val_"  + ii, val);
                    }
                    gearTag.putInt("gem_lines", lines);
                    gearTag.putBoolean("enhanced", true);
                    GearItem.setGearTag(gear, gearTag);

                    // 결과 아이템만 클라이언트로 전송 (인벤토리에 직접 안 넣음)
                    ServerPlayNetworking.send(player,
                        new EnhanceResultPayload("§b✦ 보석 장착 성공!", 0x55CCFF, false, gear));
                    return;
                }

                // ── 강화 처리 ─────────────────────────────────────
                int beforeLevel = GearItem.getEnhanceLevel(gear);
                GearItem.EnhanceResult result = GearItem.enhance(gear, stoneType);
                int afterLevel = GearItem.getEnhanceLevel(gear);

                // 결과 아이템만 클라이언트로 전송
                switch (result) {
                    case SUCCESS -> {
                        boolean special = afterLevel >= 7;
                        String msg = special
                            ? "§e§l★ 강화에 성공했습니다! §f현재 §e§l+" + afterLevel + " ★"
                            : "§a✦ 강화에 성공했습니다! §f현재 §e+" + afterLevel;
                        int color = special ? 0xFFD700 : 0x55FF55;
                        ServerPlayNetworking.send(player,
                            new EnhanceResultPayload(msg, color, special, gear));
                    }
                    case FAIL -> {
                        String msg2 = (afterLevel < beforeLevel)
                            ? "§c✗ 강화에 실패했습니다. §f현재 §c+" + afterLevel + " §8(하락)"
                            : "§c✗ 강화에 실패했습니다. §f현재 §7+" + afterLevel;
                        ServerPlayNetworking.send(player,
                            new EnhanceResultPayload(msg2, 0xFF5555, false, gear));
                    }
                }
            }));
    }

    private static void msg(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), false);
    }
}
