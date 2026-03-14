package com.ron.commanderskills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端在错位传送已选中第一个单位后发送，客户端显示“单位互换 1/2”并用紫色粒子包裹该单位 */
public class DisplacementFirstTargetS2CPacket {

    private final int firstTargetEntityId;

    public DisplacementFirstTargetS2CPacket(int firstTargetEntityId) {
        this.firstTargetEntityId = firstTargetEntityId;
    }

    public static void encode(DisplacementFirstTargetS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.firstTargetEntityId);
    }

    public static DisplacementFirstTargetS2CPacket decode(FriendlyByteBuf buf) {
        return new DisplacementFirstTargetS2CPacket(buf.readVarInt());
    }

    public static void handle(DisplacementFirstTargetS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (DisplacementClientBridge.onFirstTargetReceived != null) {
                DisplacementClientBridge.onFirstTargetReceived.accept(msg.firstTargetEntityId);
            }
        });
        context.setPacketHandled(true);
    }
}
