package com.ron.commanderskills.network;

import com.ron.commanderskills.client.FlameRocketRainZoneClientData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 焰火箭雨：广播给所有客户端，显示范围圈与轰炸信号提示；箭雨与爆炸由服务端 FlameRocketRainManager 调度。 */
public class FlameRocketRainEffectS2CPacket {

    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int radius;
    private final int delayTicks;
    private final int durationTicks;

    public FlameRocketRainEffectS2CPacket(int centerX, int centerY, int centerZ, int radius, int delayTicks, int durationTicks) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.delayTicks = delayTicks;
        this.durationTicks = durationTicks;
    }

    public static void encode(FlameRocketRainEffectS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.centerX);
        buf.writeVarInt(msg.centerY);
        buf.writeVarInt(msg.centerZ);
        buf.writeVarInt(msg.radius);
        buf.writeVarInt(msg.delayTicks);
        buf.writeVarInt(msg.durationTicks);
    }

    public static FlameRocketRainEffectS2CPacket decode(FriendlyByteBuf buf) {
        return new FlameRocketRainEffectS2CPacket(
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt()
        );
    }

    public static void handle(FlameRocketRainEffectS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            FlameRocketRainZoneClientData.addZone(
                new BlockPos(msg.centerX, msg.centerY, msg.centerZ),
                msg.radius, msg.delayTicks, msg.durationTicks
            )
        );
        ctx.get().setPacketHandled(true);
    }
}
