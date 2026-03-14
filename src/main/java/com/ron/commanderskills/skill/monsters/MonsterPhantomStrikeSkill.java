package com.ron.commanderskills.skill.monsters;

import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.skill.CommanderSkill;
import com.ron.commanderskills.skill.PhantomStrikeManager;
import com.ron.commanderskills.network.AddPhantomStrikeZoneS2CPacket;
import com.solegendary.reignofnether.building.buildings.monsters.DarkWatchtower;
import com.solegendary.reignofnether.building.buildings.monsters.Dungeon;
import com.solegendary.reignofnether.building.buildings.monsters.Stronghold;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

/**
 * 幻翼特攻队：选定范围显示轰炸信号圈，延迟 10/8/6 秒后在圈上方 60 格召唤 1/2/3 只苦力怕。
 */
public class MonsterPhantomStrikeSkill implements CommanderSkill {

    public static final String ID = "monster_phantom_strike";

    private static final int[] DELAY_SEC = {10, 8, 6};
    private static final int[] CREEPER_COUNT = {1, 2, 3};

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public com.solegendary.reignofnether.faction.Faction getFaction() {
        return com.solegendary.reignofnether.faction.Faction.MONSTERS;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getUnlockFood() {
        return 320;
    }

    @Override
    public int getUnlockWood() {
        return 600;
    }

    @Override
    public int getUnlockOre() {
        return 520;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 400;
            case 3 -> 520;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeWood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 550;
            case 3 -> 700;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeOre(int toLevel) {
        return switch (toLevel) {
            case 2 -> 680;
            case 3 -> 880;
            default -> 0;
        };
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return switch (level) {
            case 1 -> DarkWatchtower.buildingName;
            case 2 -> Dungeon.buildingName;
            case 3 -> Stronghold.buildingName;
            default -> null;
        };
    }

    @Override
    public int getUseFood(int level) {
        return switch (level) {
            case 1 -> 120;
            case 2 -> 180;
            case 3 -> 260;
            default -> 0;
        };
    }

    @Override
    public int getUseWood(int level) {
        return switch (level) {
            case 1 -> 320;
            case 2 -> 420;
            case 3 -> 540;
            default -> 0;
        };
    }

    @Override
    public int getUseOre(int level) {
        return switch (level) {
            case 1 -> 420;
            case 2 -> 560;
            case 3 -> 700;
            default -> 0;
        };
    }

    @Override
    public int getCooldownTicks(int level) {
        return switch (level) {
            case 1 -> 3 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 2, 3 -> 4 * 60 * ResourceCost.TICKS_PER_SECOND;
            default -> 0;
        };
    }

    @Override
    public int getRadius(int level) {
        // 直径 5/10/10 -> 半径 2/5/5
        return switch (level) {
            case 1 -> 2;
            case 2, 3 -> 5;
            default -> 0;
        };
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        if (skillLevel <= 0 || skillLevel > getMaxLevel()) return;

        int delayTicks = DELAY_SEC[skillLevel - 1] * ResourceCost.TICKS_PER_SECOND;
        int radius = getRadius(skillLevel);
        int count = CREEPER_COUNT[skillLevel - 1];

        PhantomStrikeManager.addStrike(level, center, radius, delayTicks, player.getName().getString(), count);

        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.ALL.noArg(),
            new AddPhantomStrikeZoneS2CPacket(center, radius, delayTicks)
        );

        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.7F);
        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PlayCommanderSkillSoundS2CPacket(ID)
        );
    }
}
