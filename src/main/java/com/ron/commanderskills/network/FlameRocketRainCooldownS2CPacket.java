package com.ron.commanderskills.network;

import com.ron.commanderskills.client.FlameRocketRainCooldownClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 焰火箭雨：有人施放时广播给所有客户端，用于在 HUD 显示全局冷却倒计时 */
public class FlameRocketRainCooldownS2CPacket {

    private final String casterName;
    private final int cooldownTicksRemaining;

    public FlameRocketRainCooldownS2CPacket(String casterName, int cooldownTicksRemaining) {
        this.casterName = casterName != null ? casterName : "";
        this.cooldownTicksRemaining = cooldownTicksRemaining;
    }

    public static void encode(FlameRocketRainCooldownS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.casterName);
        buf.writeVarInt(msg.cooldownTicksRemaining);
    }

    public static FlameRocketRainCooldownS2CPacket decode(FriendlyByteBuf buf) {
        return new FlameRocketRainCooldownS2CPacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(FlameRocketRainCooldownS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            FlameRocketRainCooldownClientData.setCooldown(msg.casterName, msg.cooldownTicksRemaining)
        );
        ctx.get().setPacketHandled(true);
    }
}
