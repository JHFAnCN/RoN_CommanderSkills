package com.ron.commanderskills.mixin;

import com.ron.commanderskills.client.BountyMarkClientData;
import com.ron.commanderskills.client.CommanderSkillsClientState;
import com.ron.commanderskills.client.CommanderSkillsClientEvents;
import com.ron.commanderskills.client.CauldronZoneClientData;
import com.ron.commanderskills.client.PhantomStrikeZoneClientData;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 主 mod 客户端执行 resetRTS（收到 /rts-reset 的广播）时，清空本 mod 的本地技能状态与选中状态，
 * 使 UI 立即显示为 0 级，避免残留上一局数据。
 */
@Mixin(PlayerClientEvents.class)
public abstract class PlayerClientEventsMixin {

    @Inject(method = "resetRTS", at = @At("HEAD"), remap = false)
    private static void ron_commanderskills$onResetRTS(boolean hardReset, CallbackInfo ci) {
        CommanderSkillsClientState.clear();
        CommanderSkillsClientEvents.clearSkillSelected();
        CauldronZoneClientData.clear();
        PhantomStrikeZoneClientData.clear();
        BountyMarkClientData.clear();
    }
}
