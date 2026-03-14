package com.ron.commanderskills.mixin;

import com.ron.commanderskills.data.CommanderSkillsSaveData;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.network.SyncCommanderSkillsS2CPacket;
import com.ron.commanderskills.skill.BountyMarkManager;
import com.ron.commanderskills.skill.CauldronZoneManager;
import com.ron.commanderskills.skill.DisplacementManager;
import com.ron.commanderskills.skill.EfficientPortalManager;
import com.ron.commanderskills.skill.PhantomStrikeManager;
import com.ron.commanderskills.skill.CommanderSkill;
import com.ron.commanderskills.skill.CommanderSkillState;
import com.ron.commanderskills.skill.CommanderSkills;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 主 mod 执行 /rts-reset 或 /rts-hard-reset 时，同时清空本 mod 的技能存档并向所有玩家同步 0 级，
 * 避免新一局仍显示上一局的等级导致 UI 错乱。
 */
@Mixin(PlayerServerEvents.class)
public abstract class PlayerServerEventsMixin {

    @Shadow(remap = false)
    public static ServerLevel serverLevel;

    @Inject(method = "resetRTS", at = @At("TAIL"), remap = false)
    private static void ron_commanderskills$onResetRTS(boolean hardReset, CallbackInfo ci) {
        if (serverLevel == null) return;
        CommanderSkillsSaveData data = CommanderSkillsSaveData.get(serverLevel);
        data.clearAll();
        CauldronZoneManager.clear();
        PhantomStrikeManager.clear();
        EfficientPortalManager.clear();
        BountyMarkManager.clear();
        DisplacementManager.clear();

        Map<String, CommanderSkillState> toSync = new HashMap<>();
        for (CommanderSkill skill : CommanderSkills.getAll().values()) {
            toSync.put(skill.getId(), new CommanderSkillState(0, 0));
        }
        SyncCommanderSkillsS2CPacket packet = new SyncCommanderSkillsS2CPacket(toSync);
        for (ServerPlayer player : serverLevel.players()) {
            RoNCommanderSkillsNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                packet
            );
        }
    }
}
