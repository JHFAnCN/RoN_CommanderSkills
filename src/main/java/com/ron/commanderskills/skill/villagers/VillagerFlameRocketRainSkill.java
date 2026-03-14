package com.ron.commanderskills.skill.villagers;

import com.ron.commanderskills.network.FlameRocketRainEffectS2CPacket;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.skill.CommanderSkill;
import com.ron.commanderskills.skill.FlameRocketRainManager;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * 焰火箭雨：超级武器大招。解锁需求=城堡；解锁后直接进入10分钟冷却；范围直径40格；延迟10秒后箭雨20秒（1–3支/秒），触地/触实体苦力怕级爆炸+烟花粒子；音效全局可闻。
 */
public class VillagerFlameRocketRainSkill implements CommanderSkill {

    public static final String ID = "villager_flame_rocket_rain";

    private static final int COOLDOWN_TICKS = 10 * 60 * ResourceCost.TICKS_PER_SECOND; // 10分钟
    private static final int RADIUS = 20; // 直径40格
    private static final int DELAY_TICKS = 10 * ResourceCost.TICKS_PER_SECOND; // 10秒
    private static final int DURATION_TICKS = 20 * ResourceCost.TICKS_PER_SECOND; // 20秒

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getUnlockFood() {
        return 500;
    }

    @Override
    public int getUnlockWood() {
        return 1000;
    }

    @Override
    public int getUnlockOre() {
        return 1000;
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

    /** 解锁需求：城堡 */
    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return level == 1 ? Castle.buildingName : null;
    }

    @Override
    public int getUseFood(int level) {
        return 200;
    }

    @Override
    public int getUseWood(int level) {
        return 400;
    }

    @Override
    public int getUseOre(int level) {
        return 400;
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
        if (skillLevel <= 0) return;
        String ownerName = player.getName().getString();
        // 全局音效：敲钟 + 火焰弹发射 + 烟花发射（所有玩家可闻）
        RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new PlayCommanderSkillSoundS2CPacket(ID));
        // 向所有客户端发送效果包：范围圈与轰炸信号提示
        RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
            new FlameRocketRainEffectS2CPacket(center.getX(), center.getY(), center.getZ(), RADIUS, DELAY_TICKS, DURATION_TICKS));
        // 服务端调度箭雨：延迟 10 秒后 20 秒内每秒 1–3 支箭，仅击中建筑/敌方或中立单位时爆炸
        FlameRocketRainManager.add(level, center, RADIUS, DELAY_TICKS, DURATION_TICKS, ownerName);
    }
}
