package com.ron.commanderskills.skill;

import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface CommanderSkill {

    String getId();

    /** 技能所属阵营，用于 UI 只显示当前阵营技能。默认村民。 */
    default Faction getFaction() {
        return Faction.VILLAGERS;
    }

    int getMaxLevel();

    // 解锁第1级消耗
    int getUnlockFood();
    int getUnlockWood();
    int getUnlockOre();

    // 升级到 toLevel 的消耗（toLevel = 2 或 3）
    int getUpgradeFood(int toLevel);
    int getUpgradeWood(int toLevel);
    int getUpgradeOre(int toLevel);

    /** 解锁/升级到该等级需要的前置建筑名，与主 mod Building.buildingName 一致，如 Barracks.buildingName */
    String getRequiredBuildingNameForLevel(int level);

    // 使用消耗（按等级）
    int getUseFood(int level);
    int getUseWood(int level);
    int getUseOre(int level);

    // 冷却（tick，按等级）
    int getCooldownTicks(int level);

    // 技能范围（半径方块，按等级）
    int getRadius(int level);

    // 服务器端实际生效逻辑
    void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center);
}

