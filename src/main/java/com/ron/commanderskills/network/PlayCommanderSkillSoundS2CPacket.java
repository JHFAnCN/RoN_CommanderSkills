package com.ron.commanderskills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端施放技能后发给施放者，客户端在本地播放音效。
 * 实际播放逻辑在 PlayCommanderSkillSoundS2CPacketClientHandler 中，通过反射调用，避免服务端加载 SoundInstance 等 client-only 类导致 NoClassDefFoundError。
 */
public class PlayCommanderSkillSoundS2CPacket {

    private static final String CLIENT_HANDLER_CLASS = "com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacketClientHandler";

    private final String skillId;

    public PlayCommanderSkillSoundS2CPacket(String skillId) {
        this.skillId = skillId;
    }

    public String getSkillId() {
        return skillId;
    }

    public static void encode(PlayCommanderSkillSoundS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.skillId);
    }

    public static PlayCommanderSkillSoundS2CPacket decode(FriendlyByteBuf buf) {
        return new PlayCommanderSkillSoundS2CPacket(buf.readUtf());
    }

    public static void handle(PlayCommanderSkillSoundS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Class.forName(CLIENT_HANDLER_CLASS)
                        .getMethod("handleClient", String.class)
                        .invoke(null, msg.getSkillId());
                } catch (Exception ignored) {
                    // 仅客户端执行，服务端不加载 ClientHandler
                }
            })
        );
        context.setPacketHandled(true);
    }
}
