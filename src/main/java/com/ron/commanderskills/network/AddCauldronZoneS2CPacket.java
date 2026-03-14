package com.ron.commanderskills.network;

import com.ron.commanderskills.client.CauldronZoneClientData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端创建坩埚狂欢效果圈时广播给所有客户端，用于显示范围与药水视觉效果 */
public class AddCauldronZoneS2CPacket {

    private final BlockPos center;
    private final int radius;
    private final int durationTicks;

    public AddCauldronZoneS2CPacket(BlockPos center, int radius, int durationTicks) {
        this.center = center;
        this.radius = radius;
        this.durationTicks = durationTicks;
    }

    public static void encode(AddCauldronZoneS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.center);
        buf.writeVarInt(msg.radius);
        buf.writeVarInt(msg.durationTicks);
    }

    public static AddCauldronZoneS2CPacket decode(FriendlyByteBuf buf) {
        return new AddCauldronZoneS2CPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(AddCauldronZoneS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CauldronZoneClientData.addZone(msg.center, msg.radius, msg.durationTicks));
        ctx.get().setPacketHandled(true);
    }
}
