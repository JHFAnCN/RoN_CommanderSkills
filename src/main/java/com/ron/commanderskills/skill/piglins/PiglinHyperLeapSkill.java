package com.ron.commanderskills.skill.piglins;

import com.ron.commanderskills.skill.CommanderSkill;
import com.solegendary.reignofnether.building.buildings.piglins.Fortress;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * 超跃传送：超级武器。下界要塞解锁；两阶段——先设蓝色源圈，再设红色目标圈，将蓝圈内己方/盟友单位传送到红圈。直径20格，冷却10分钟，解锁即进冷却。
 */
public class PiglinHyperLeapSkill implements CommanderSkill {

    public static final String ID = "piglin_hyper_leap";

    private static final int COOLDOWN_TICKS = 10 * 60 * ResourceCost.TICKS_PER_SECOND;
    private static final int RADIUS = 10; // 直径20格

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public com.solegendary.reignofnether.faction.Faction getFaction() {
        return com.solegendary.reignofnether.faction.Faction.PIGLINS;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getUnlockFood() {
        return 550;
    }

    @Override
    public int getUnlockWood() {
        return 1100;
    }

    @Override
    public int getUnlockOre() {
        return 1100;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return 0;
    }

    @Override
    public int getUpgradeWood(int toLevel) {
        return 0;
    }

    @Override
    public int getUpgradeOre(int toLevel) {
        return 0;
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return level == 1 ? Fortress.buildingName : null;
    }

    @Override
    public int getUseFood(int level) {
        return 350;
    }

    @Override
    public int getUseWood(int level) {
        return 600;
    }

    @Override
    public int getUseOre(int level) {
        return 600;
    }

    @Override
    public int getCooldownTicks(int level) {
        return COOLDOWN_TICKS;
    }

    @Override
    public int getRadius(int level) {
        return RADIUS;
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        // 实际传送在 HyperLeapManager.tryStep 中完成；此处仅用于 step==2 时的音效/广播由 Manager 与 CommanderSkillManager 处理
    }
}
