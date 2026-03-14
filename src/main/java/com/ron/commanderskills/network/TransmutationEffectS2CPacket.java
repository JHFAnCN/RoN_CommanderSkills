package com.ron.commanderskills.network;

import com.ron.commanderskills.client.TransmutationZoneClientData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 亡灵嬗变：广播给所有客户端，显示范围圈、法阵与目标实体粒子（可转变紫色/不可转变红色） */
public class TransmutationEffectS2CPacket {

    private final int centerX, centerY, centerZ;
    private final int radius;
    private final int delayTicks;
    private final int[] convertibleEntityIds;
    private final int[] nonConvertibleEntityIds;

    public TransmutationEffectS2CPacket(int centerX, int centerY, int centerZ, int radius, int delayTicks,
                                        int[] convertibleEntityIds, int[] nonConvertibleEntityIds) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.delayTicks = delayTicks;
        this.convertibleEntityIds = convertibleEntityIds != null ? convertibleEntityIds : new int[0];
        this.nonConvertibleEntityIds = nonConvertibleEntityIds != null ? nonConvertibleEntityIds : new int[0];
    }

    public static void encode(TransmutationEffectS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.centerX);
        buf.writeVarInt(msg.centerY);
        buf.writeVarInt(msg.centerZ);
        buf.writeVarInt(msg.radius);
        buf.writeVarInt(msg.delayTicks);
        buf.writeVarInt(msg.convertibleEntityIds.length);
        for (int id : msg.convertibleEntityIds) buf.writeVarInt(id);
        buf.writeVarInt(msg.nonConvertibleEntityIds.length);
        for (int id : msg.nonConvertibleEntityIds) buf.writeVarInt(id);
    }

    public static TransmutationEffectS2CPacket decode(FriendlyByteBuf buf) {
        int cx = buf.readVarInt(), cy = buf.readVarInt(), cz = buf.readVarInt();
        int r = buf.readVarInt(), delay = buf.readVarInt();
        int n1 = buf.readVarInt();
        int[] conv = new int[n1];
        for (int i = 0; i < n1; i++) conv[i] = buf.readVarInt();
        int n2 = buf.readVarInt();
        int[] non = new int[n2];
        for (int i = 0; i < n2; i++) non[i] = buf.readVarInt();
        return new TransmutationEffectS2CPacket(cx, cy, cz, r, delay, conv, non);
    }

    public static void handle(TransmutationEffectS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            TransmutationZoneClientData.addZone(
                new BlockPos(msg.centerX, msg.centerY, msg.centerZ),
                msg.radius, msg.delayTicks, msg.convertibleEntityIds, msg.nonConvertibleEntityIds
            )
        );
        ctx.get().setPacketHandled(true);
    }
}
