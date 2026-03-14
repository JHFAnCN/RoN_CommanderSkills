package com.ron.commanderskills.skill.villagers;

import com.ron.commanderskills.network.AddCauldronZoneS2CPacket;
import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.skill.CauldronZoneManager;
import com.ron.commanderskills.skill.CommanderSkill;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.building.buildings.villagers.WheatFarm;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

/**
 * 坩埚狂欢：在目标位置生成效果圈，圈内敌方/中立单位随机获得 缓慢/虚弱/凋零/中毒（1级），出圈失效。持续 30/40/50 秒。
 */
public class VillagerCauldronRevelrySkill implements CommanderSkill {

    public static final String ID = "villager_cauldron_revelry";

    private static final int[] DURATIONS = {30, 40, 50}; // 秒

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getUnlockFood() {
        return 250;
    }

    @Override
    public int getUnlockWood() {
        return 650;
    }

    @Override
    public int getUnlockOre() {
        return 650;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 350;
            case 3 -> 450;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeWood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 800;
            case 3 -> 950;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeOre(int toLevel) {
        return switch (toLevel) {
            case 2 -> 800;
            case 3 -> 950;
            default -> 0;
        };
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return switch (level) {
            case 1 -> WheatFarm.buildingName;
            case 2 -> Library.buildingName;
            case 3 -> Castle.buildingName;
            default -> null;
        };
    }

    @Override
    public int getUseFood(int level) {
        return switch (level) {
            case 1 -> 50;
            case 2 -> 75;
            case 3 -> 100;
            default -> 0;
        };
    }

    @Override
    public int getUseWood(int level) {
        return switch (level) {
            case 1 -> 250;
            case 2 -> 320;
            case 3 -> 400;
            default -> 0;
        };
    }

    @Override
    public int getUseOre(int level) {
        return switch (level) {
            case 1 -> 350;
            case 2 -> 450;
            case 3 -> 550;
            default -> 0;
        };
    }

    @Override
    public int getCooldownTicks(int level) {
        return switch (level) {
            case 1 -> 3 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 2 -> 4 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 3 -> 4 * 60 * ResourceCost.TICKS_PER_SECOND;
            default -> 0;
        };
    }

    @Override
    public int getRadius(int level) {
        return switch (level) {
            case 1 -> 4;  // 直径 8
            case 2, 3 -> 5; // 直径 10
            default -> 0;
        };
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        if (skillLevel <= 0 || skillLevel > getMaxLevel()) return;

        int radius = getRadius(skillLevel);
        int durationTicks = DURATIONS[skillLevel - 1] * ResourceCost.TICKS_PER_SECOND;

        CauldronZoneManager.addZone(level, center, radius, durationTicks, player.getName().getString(), skillLevel);

        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WITCH_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.0F);
        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PlayCommanderSkillSoundS2CPacket(ID)
        );
        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.ALL.noArg(),
            new AddCauldronZoneS2CPacket(center, radius, durationTicks)
        );
    }
}
