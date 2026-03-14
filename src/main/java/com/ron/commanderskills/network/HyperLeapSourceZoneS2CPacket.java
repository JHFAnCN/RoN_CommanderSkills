package com.ron.commanderskills.network;

import com.ron.commanderskills.client.HyperLeapClientData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 超跃传送：服务端在玩家设置源圈后发送，客户端显示蓝圈并播放地狱传送门进入音效 */
public class HyperLeapSourceZoneS2CPacket {

    private final int centerX, centerY, centerZ;
    private final int radius;

    public HyperLeapSourceZoneS2CPacket(int centerX, int centerY, int centerZ, int radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    public static void encode(HyperLeapSourceZoneS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.centerX);
        buf.writeVarInt(msg.centerY);
        buf.writeVarInt(msg.centerZ);
        buf.writeVarInt(msg.radius);
    }

    public static HyperLeapSourceZoneS2CPacket decode(FriendlyByteBuf buf) {
        return new HyperLeapSourceZoneS2CPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(HyperLeapSourceZoneS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            HyperLeapClientData.setSourceZone(new BlockPos(msg.centerX, msg.centerY, msg.centerZ), msg.radius);
            if (HyperLeapClientData.onSourceZoneReceived != null) HyperLeapClientData.onSourceZoneReceived.run();
        });
        ctx.get().setPacketHandled(true);
    }
}
