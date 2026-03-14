package com.ron.commanderskills.network;

import com.ron.commanderskills.client.BountyMarkClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 悬赏：同步被标记的实体 ID 列表及持续时间，客户端绘制金色描边 */
public class AddBountyMarksS2CPacket {

    private final List<Integer> entityIds;
    private final int durationTicks;

    public AddBountyMarksS2CPacket(List<Integer> entityIds, int durationTicks) {
        this.entityIds = entityIds;
        this.durationTicks = durationTicks;
    }

    public static void encode(AddBountyMarksS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityIds.size());
        for (int id : msg.entityIds) buf.writeVarInt(id);
        buf.writeVarInt(msg.durationTicks);
    }

    public static AddBountyMarksS2CPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Integer> ids = new ArrayList<>(n);
        for (int i = 0; i < n; i++) ids.add(buf.readVarInt());
        int duration = buf.readVarInt();
        return new AddBountyMarksS2CPacket(ids, duration);
    }

    public static void handle(AddBountyMarksS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> BountyMarkClientData.addMarks(msg.entityIds, msg.durationTicks));
        ctx.get().setPacketHandled(true);
    }
}
