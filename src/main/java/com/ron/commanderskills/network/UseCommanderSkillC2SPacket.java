package com.ron.commanderskills.network;

import com.ron.commanderskills.skill.CommanderSkillManager;
import com.ron.commanderskills.skill.CommanderSkills;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UseCommanderSkillC2SPacket {

    private final String skillId;
    private final BlockPos targetPos;

    public UseCommanderSkillC2SPacket(String skillId, BlockPos targetPos) {
        this.skillId = skillId;
        this.targetPos = targetPos;
    }

    public static void encode(UseCommanderSkillC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.skillId);
        buf.writeBlockPos(msg.targetPos);
    }

    public static UseCommanderSkillC2SPacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        BlockPos pos = buf.readBlockPos();
        return new UseCommanderSkillC2SPacket(id, pos);
    }

    public static void handle(UseCommanderSkillC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            var server = player.getServer();
            if (server == null) return;
            ServerLevel overworld = server.overworld();
            if (overworld == null) return;
            CommanderSkillManager.useSkill(overworld, player, CommanderSkills.get(msg.skillId), msg.targetPos);
        });
        context.setPacketHandled(true);
    }
}

