package com.example.cosmod.combat;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.chat.Style;
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
                var inv = player.getInventory();

                int gearIdx  = payload.gearSlotIdx();
                int stoneIdx = payload.stoneSlotIdx();
                int stoneType = payload.stoneType();

                ItemStack gear  = inv.getItem(gearIdx);
                ItemStack stone = inv.getItem(stoneIdx);

                if (gear.isEmpty() || !(gear.getItem() instanceof GearItem)) {
                    msg(player, "§c강화 가능한 장비가 아닙니다.");
                    return;
                }
                if (stone.isEmpty()) {
                    msg(player, "§c강화석 또는 보석이 없습니다.");
                    return;
                }

                if (stoneType == 3) {
                    if (!(stone.getItem() instanceof GemItem gi)) {
                        msg(player, "§c보석 아이템이 아닙니다.");
                        return;
                    }
                    if (!GemItem.isIdentified(stone)) {
                        msg(player, "§c감정되지 않은 보석입니다. 먼저 우클릭으로 감정하세요.");
                        return;
                    }
                    var gearTag = GearItem.getGearTag(gear);
                    if (gearTag.contains("enhanced") && gearTag.getBoolean("enhanced").orElse(false)) {
                        msg(player, "§c이미 보석이 장착된 장비입니다.");
                        return;
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
                    stone.shrink(1);
                    inv.setItem(stoneIdx, stone);
                    inv.setItem(gearIdx, gear);
                    msg(player, "§b✦ 보석 장착 성공!");
                    return;
                }

                int beforeLevel = GearItem.getEnhanceLevel(gear);
                stone.shrink(1);
                inv.setItem(stoneIdx, stone);
                GearItem.EnhanceResult result = GearItem.enhance(gear, stoneType);
                inv.setItem(gearIdx, gear);

                int afterLevel = GearItem.getEnhanceLevel(gear);
                switch (result) {
                    case SUCCESS -> {
                        boolean special = afterLevel >= 7;
                        String msg = special
                            ? "★ 강화 성공! +" + afterLevel + " ★"
                            : "✦ 강화 성공!  +" + beforeLevel + " → +" + afterLevel;
                        int color = special ? 0xFFD700 : 0x55FF55;
                        ServerPlayNetworking.send(player, new EnhanceResultPayload(msg, color, special));
                    }
                    case FAIL -> {
                        String msg2 = (afterLevel < beforeLevel)
                            ? "✗ 강화 실패.  +" + beforeLevel + " → +" + afterLevel
                            : "✗ 강화 실패.  현재 +" + afterLevel;
                        ServerPlayNetworking.send(player, new EnhanceResultPayload(msg2, 0xFF5555, false));
                    }
                }
            }));
    }

    private static void msg(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), false);
    }
}
