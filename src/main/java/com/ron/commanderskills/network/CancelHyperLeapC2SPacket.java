package com.ron.commanderskills.network;

import com.ron.commanderskills.skill.HyperLeapManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端右键取消超跃传送时发送，服务端清除该玩家的源圈状态 */
public class CancelHyperLeapC2SPacket {

    public static void encode(CancelHyperLeapC2SPacket msg, FriendlyByteBuf buf) {}

    public static CancelHyperLeapC2SPacket decode(FriendlyByteBuf buf) {
        return new CancelHyperLeapC2SPacket();
    }

    public static void handle(CancelHyperLeapC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) HyperLeapManager.clearForPlayer(player.getName().getString());
        });
        ctx.get().setPacketHandled(true);
    }
}
