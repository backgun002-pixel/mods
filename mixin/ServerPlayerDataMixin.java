package com.example.cosmod.mixin;

// Data Attachment API를 사용하므로 이 Mixin은 비워둠
// 실제 저장은 CosmodAttachments를 통해 처리
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
public class ServerPlayerDataMixin {
    // placeholder - 실제 저장은 Fabric Data Attachment API로 처리
}
