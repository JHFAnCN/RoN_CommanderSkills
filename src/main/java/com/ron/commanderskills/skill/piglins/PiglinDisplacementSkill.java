package com.ron.commanderskills.skill.piglins;

import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.skill.CommanderSkill;
import com.solegendary.reignofnether.building.buildings.piglins.Fortress;
import com.solegendary.reignofnether.building.buildings.piglins.PortalBasic;
import com.solegendary.reignofnether.building.buildings.piglins.WitherShrine;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

/**
 * 错位传送：左键选一个单位，再左键选另一个单位，两者位置互换。单体选择，无范围。
 */
public class PiglinDisplacementSkill implements CommanderSkill {

    public static final String ID = "piglin_displacement";

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
        return 260;
    }

    @Override
    public int getUnlockWood() {
        return 340;
    }

    @Override
    public int getUnlockOre() {
        return 440;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 340;
            case 3 -> 440;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeWood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 420;
            case 3 -> 540;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeOre(int toLevel) {
        return switch (toLevel) {
            case 2 -> 520;
            case 3 -> 660;
            default -> 0;
        };
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return switch (level) {
            case 1 -> PortalBasic.buildingName;
            case 2 -> WitherShrine.buildingName;
            case 3 -> Fortress.buildingName;
            default -> null;
        };
    }

    @Override
    public int getUseFood(int level) {
        return switch (level) {
            case 1 -> 80;
            case 2 -> 120;
            case 3 -> 160;
            default -> 0;
        };
    }

    @Override
    public int getUseWood(int level) {
        return switch (level) {
            case 1 -> 100;
            case 2 -> 150;
            case 3 -> 200;
            default -> 0;
        };
    }

    @Override
    public int getUseOre(int level) {
        return switch (level) {
            case 1 -> 160;
            case 2 -> 220;
            case 3 -> 280;
            default -> 0;
        };
    }

    @Override
    public int getCooldownTicks(int level) {
        return switch (level) {
            case 1 -> 3 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 2 -> 2 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 3 -> 1 * 60 * ResourceCost.TICKS_PER_SECOND;
            default -> 0;
        };
    }

    @Override
    public int getRadius(int level) {
        return 0;
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        // 实际交换在 DisplacementManager.tryTarget 中完成，此处仅音效（若服务端在 tryTarget 后调用 apply 则可用于统一音效）
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.0F);
        RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new PlayCommanderSkillSoundS2CPacket(ID));
    }
}
