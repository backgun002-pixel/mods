package com.example.cosmod.skill;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 클라이언트 → 서버: 스킬 사용 요청 */
public record SkillPayload(int skillSlot) implements CustomPacketPayload {
    public static final Type<SkillPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "use_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, SkillPayload::skillSlot,
            SkillPayload::new
        );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
