package com.ron.commanderskills.network;

import com.ron.commanderskills.client.CommanderSkillsClientState;
import com.ron.commanderskills.skill.CommanderSkillState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncCommanderSkillsS2CPacket {

    private final Map<String, CommanderSkillState> states;

    public SyncCommanderSkillsS2CPacket(Map<String, CommanderSkillState> states) {
        this.states = states;
    }

    public static void encode(SyncCommanderSkillsS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.states.size());
        msg.states.forEach((id, state) -> {
            buf.writeUtf(id);
            buf.writeVarInt(state.level());
            buf.writeFloat(state.cooldownTicks());
        });
    }

    public static SyncCommanderSkillsS2CPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, CommanderSkillState> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            int level = buf.readVarInt();
            float cd = buf.readFloat();
            map.put(id, new CommanderSkillState(level, cd));
        }
        return new SyncCommanderSkillsS2CPacket(map);
    }

    public static void handle(SyncCommanderSkillsS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> CommanderSkillsClientState.updateFromServer(msg.states));
        context.setPacketHandled(true);
    }
}

