package com.ron.commanderskills.network;

import com.ron.commanderskills.skill.DisplacementManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端右键取消错位传送时发送，服务端清除该玩家的“第一个目标”状态 */
public class CancelDisplacementC2SPacket {

    public static void encode(CancelDisplacementC2SPacket msg, FriendlyByteBuf buf) {}

    public static CancelDisplacementC2SPacket decode(FriendlyByteBuf buf) {
        return new CancelDisplacementC2SPacket();
    }

    public static void handle(CancelDisplacementC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DisplacementManager.clearForPlayer(player.getName().getString());
            }
        });
        context.setPacketHandled(true);
    }
}
