package com.ron.commanderskills.network;

import com.ron.commanderskills.client.HyperLeapCooldownClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 超跃传送：施放后广播，所有人可见冷却倒计时（屏幕底端中间靠右） */
public class HyperLeapCooldownS2CPacket {

    private final String casterName;
    private final int cooldownTicksRemaining;

    public HyperLeapCooldownS2CPacket(String casterName, int cooldownTicksRemaining) {
        this.casterName = casterName != null ? casterName : "";
        this.cooldownTicksRemaining = cooldownTicksRemaining;
    }

    public static void encode(HyperLeapCooldownS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.casterName);
        buf.writeVarInt(msg.cooldownTicksRemaining);
    }

    public static HyperLeapCooldownS2CPacket decode(FriendlyByteBuf buf) {
        return new HyperLeapCooldownS2CPacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(HyperLeapCooldownS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            HyperLeapCooldownClientData.setCooldown(msg.casterName, msg.cooldownTicksRemaining)
        );
        ctx.get().setPacketHandled(true);
    }
}
