package com.ron.commanderskills.network;

import com.ron.commanderskills.client.HyperLeapClientData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 超跃传送：传送执行后发送，客户端在目标点播放紫色粒子并播放传送音效 */
public class HyperLeapExecuteS2CPacket {

    private final int destX, destY, destZ;
    private final int radius;

    public HyperLeapExecuteS2CPacket(int destX, int destY, int destZ, int radius) {
        this.destX = destX;
        this.destY = destY;
        this.destZ = destZ;
        this.radius = radius;
    }

    public static void encode(HyperLeapExecuteS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.destX);
        buf.writeVarInt(msg.destY);
        buf.writeVarInt(msg.destZ);
        buf.writeVarInt(msg.radius);
    }

    public static HyperLeapExecuteS2CPacket decode(FriendlyByteBuf buf) {
        return new HyperLeapExecuteS2CPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(HyperLeapExecuteS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            HyperLeapClientData.addExecuteZone(new BlockPos(msg.destX, msg.destY, msg.destZ), msg.radius);
            HyperLeapClientData.clearSourceZone();
            if (HyperLeapClientData.onExecuteReceived != null) HyperLeapClientData.onExecuteReceived.run();
        });
        ctx.get().setPacketHandled(true);
    }
}
