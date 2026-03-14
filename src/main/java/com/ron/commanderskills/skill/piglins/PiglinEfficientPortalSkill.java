package com.ron.commanderskills.skill.piglins;

import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.skill.CommanderSkill;
import com.ron.commanderskills.skill.EfficientPortalManager;
import com.solegendary.reignofnether.building.buildings.piglins.Bastion;
import com.solegendary.reignofnether.building.buildings.piglins.Fortress;
import com.solegendary.reignofnether.building.buildings.piglins.PortalBasic;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

/**
 * 高效传送门：己方兵种单位生产速度提升 20%/40%/60%，持续 30/40/50 秒。
 */
public class PiglinEfficientPortalSkill implements CommanderSkill {

    public static final String ID = "piglin_efficient_portal";

    private static final int[] DURATIONS_SEC = {30, 40, 50};

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
        return 3;
    }

    @Override
    public int getUnlockFood() {
        return 280;
    }

    @Override
    public int getUnlockWood() {
        return 380;
    }

    @Override
    public int getUnlockOre() {
        return 480;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 360;
            case 3 -> 460;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeWood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 480;
            case 3 -> 600;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeOre(int toLevel) {
        return switch (toLevel) {
            case 2 -> 560;
            case 3 -> 700;
            default -> 0;
        };
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return switch (level) {
            case 1 -> PortalBasic.buildingName;
            case 2 -> Bastion.buildingName;
            case 3 -> Fortress.buildingName;
            default -> null;
        };
    }

    @Override
    public int getUseFood(int level) {
        return switch (level) {
            case 1 -> 70;
            case 2 -> 100;
            case 3 -> 130;
            default -> 0;
        };
    }

    @Override
    public int getUseWood(int level) {
        return switch (level) {
            case 1 -> 90;
            case 2 -> 130;
            case 3 -> 170;
            default -> 0;
        };
    }

    @Override
    public int getUseOre(int level) {
        return switch (level) {
            case 1 -> 130;
            case 2 -> 180;
            case 3 -> 230;
            default -> 0;
        };
    }

    @Override
    public int getCooldownTicks(int level) {
        return switch (level) {
            case 1 -> 2 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 2 -> 3 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 3 -> 4 * 60 * ResourceCost.TICKS_PER_SECOND;
            default -> 0;
        };
    }

    @Override
    public int getRadius(int level) {
        return 0; // 全局
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        if (skillLevel <= 0 || skillLevel > getMaxLevel()) return;
        long endTick = level.getGameTime() + DURATIONS_SEC[skillLevel - 1] * ResourceCost.TICKS_PER_SECOND;
        EfficientPortalManager.addBuff(player.getName().getString(), endTick, skillLevel);

        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.6F, 0.9F);
        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PlayCommanderSkillSoundS2CPacket(ID)
        );
    }
}
