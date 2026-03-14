package com.ron.commanderskills.network;

import com.ron.commanderskills.skill.CommanderSkillManager;
import com.ron.commanderskills.skill.CommanderSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UnlockCommanderSkillC2SPacket {

    private final String skillId;

    public UnlockCommanderSkillC2SPacket(String skillId) {
        this.skillId = skillId;
    }

    public static void encode(UnlockCommanderSkillC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.skillId);
    }

    public static UnlockCommanderSkillC2SPacket decode(FriendlyByteBuf buf) {
        return new UnlockCommanderSkillC2SPacket(buf.readUtf());
    }

    public static void handle(UnlockCommanderSkillC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            var server = player.getServer();
            if (server == null) return;
            ServerLevel overworld = server.overworld();
            if (overworld == null) return;
            CommanderSkillManager.unlockOrRankUp(overworld, player, CommanderSkills.get(msg.skillId));
        });
        context.setPacketHandled(true);
    }
}

