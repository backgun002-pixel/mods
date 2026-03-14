package com.example.cosmod.skill;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 서버 → 클라이언트: 화면 이펙트 (광폭화/샤프아이즈/종료) */
public record SkillScreenEffectPayload(int effect) implements CustomPacketPayload {

    public static final int EFFECT_NONE      = 0;
    public static final int EFFECT_BERSERK   = 1;
    public static final int EFFECT_SHARPEYES = 2;

    public static final Type<SkillScreenEffectPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "skill_screen_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillScreenEffectPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, SkillScreenEffectPayload::effect,
            SkillScreenEffectPayload::new
        );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
