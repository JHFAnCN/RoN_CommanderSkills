package com.ron.commanderskills.network;

import com.ron.commanderskills.client.PhantomStrikeZoneClientData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 幻翼特攻队：广播给所有客户端，显示轰炸信号圈直到 delayTicks 后消失（苦力怕落地） */
public class AddPhantomStrikeZoneS2CPacket {

    private final BlockPos center;
    private final int radius;
    private final int delayTicks;

    public AddPhantomStrikeZoneS2CPacket(BlockPos center, int radius, int delayTicks) {
        this.center = center;
        this.radius = radius;
        this.delayTicks = delayTicks;
    }

    public static void encode(AddPhantomStrikeZoneS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.center);
        buf.writeVarInt(msg.radius);
        buf.writeVarInt(msg.delayTicks);
    }

    public static AddPhantomStrikeZoneS2CPacket decode(FriendlyByteBuf buf) {
        return new AddPhantomStrikeZoneS2CPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(AddPhantomStrikeZoneS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PhantomStrikeZoneClientData.addZone(msg.center, msg.radius, msg.delayTicks));
        ctx.get().setPacketHandled(true);
    }
}
