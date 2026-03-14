package com.ron.commanderskills.network;

import com.ron.commanderskills.client.TransmutationCooldownClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 亡灵嬗变：有人施放时广播给所有客户端，用于 HUD 显示全局冷却倒计时（屏幕底端中间靠右） */
public class TransmutationCooldownS2CPacket {

    private final String casterName;
    private final int cooldownTicksRemaining;

    public TransmutationCooldownS2CPacket(String casterName, int cooldownTicksRemaining) {
        this.casterName = casterName != null ? casterName : "";
        this.cooldownTicksRemaining = cooldownTicksRemaining;
    }

    public static void encode(TransmutationCooldownS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.casterName);
        buf.writeVarInt(msg.cooldownTicksRemaining);
    }

    public static TransmutationCooldownS2CPacket decode(FriendlyByteBuf buf) {
        return new TransmutationCooldownS2CPacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(TransmutationCooldownS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            TransmutationCooldownClientData.setCooldown(msg.casterName, msg.cooldownTicksRemaining)
        );
        ctx.get().setPacketHandled(true);
    }
}
