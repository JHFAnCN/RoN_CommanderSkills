package com.ron.commanderskills.data;

import com.ron.commanderskills.skill.CommanderSkillState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class CommanderSkillsSaveData extends SavedData {

    // ownerName -> (skillId -> state)
    private final Map<String, Map<String, CommanderSkillState>> data = new HashMap<>();

    private static CommanderSkillsSaveData create() {
        return new CommanderSkillsSaveData();
    }

    @Nonnull
    public static CommanderSkillsSaveData get(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        var overworld = server.overworld();
        // 加载世界时 overworld 可能尚未就绪，避免 NPE 导致闪退
        if (overworld == null) {
            return create();
        }
        return overworld
            .getDataStorage()
            .computeIfAbsent(CommanderSkillsSaveData::load, CommanderSkillsSaveData::create, "ron_commander_skills");
    }

    public static CommanderSkillsSaveData load(CompoundTag tag) {
        CommanderSkillsSaveData data = create();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag eTag = (CompoundTag) t;
            String owner = eTag.getString("owner");
            String skillId = eTag.getString("skill");
            int level = eTag.getInt("level");
            float cd = eTag.getFloat("cooldown");
            data.setState(owner, skillId, new CommanderSkillState(level, cd));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        data.forEach((owner, skills) -> skills.forEach((skillId, state) -> {
            CompoundTag eTag = new CompoundTag();
            eTag.putString("owner", owner);
            eTag.putString("skill", skillId);
            eTag.putInt("level", state.level());
            eTag.putFloat("cooldown", state.cooldownTicks());
            list.add(eTag);
        }));
        tag.put("entries", list);
        return tag;
    }

    public CommanderSkillState getState(String owner, String skillId) {
        Map<String, CommanderSkillState> map = data.get(owner);
        if (map == null) return null;
        return map.get(skillId);
    }

    public void setState(String owner, String skillId, CommanderSkillState state) {
        data.computeIfAbsent(owner, k -> new HashMap<>()).put(skillId, state);
        setDirty();
    }

    /** 游戏重置（如 /rts-reset）时清空所有玩家技能数据，与主 mod 重置保持一致 */
    public void clearAll() {
        data.clear();
        setDirty();
    }
}

