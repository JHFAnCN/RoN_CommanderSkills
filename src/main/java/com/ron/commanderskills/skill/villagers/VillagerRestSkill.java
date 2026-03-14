package com.ron.commanderskills.skill.villagers;

import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.registrar.EffectRegistrar;
import com.ron.commanderskills.skill.CommanderSkill;
import com.solegendary.reignofnether.building.buildings.villagers.ArcaneTower;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.building.buildings.villagers.VillagerHouse;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * 休整：技能使用后出现治疗区域，持续 10/15/20 秒，区域内己方单位每秒治疗 2/6/10 点血，并获得额外 5/10/15 点血量持续 2 分钟。
 */
public class VillagerRestSkill implements CommanderSkill {

    public static final String ID = "villager_rest";

    // 治疗区域持续（秒）：10 / 15 / 20
    private static final int[] DURATIONS = {10, 15, 20};
    // 每秒治疗量：2 / 6 / 10
    private static final int[] HEAL_PER_SECOND = {2, 6, 10};
    // 额外血量持续 2 分钟（tick）
    private static final int EXTRA_HEALTH_DURATION_TICKS = 2 * 60 * ResourceCost.TICKS_PER_SECOND;

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
        return 400;
    }

    @Override
    public int getUnlockWood() {
        return 450;
    }

    @Override
    public int getUnlockOre() {
        return 450;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 500;
            case 3 -> 650;
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
            case 2 -> 550;
            case 3 -> 700;
            default -> 0;
        };
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return switch (level) {
            case 1 -> VillagerHouse.buildingName;
            case 2 -> ArcaneTower.buildingName;
            case 3 -> Castle.buildingName;
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
            case 1 -> 180;
            case 2 -> 260;
            case 3 -> 320;
            default -> 0;
        };
    }

    @Override
    public int getUseOre(int level) {
        return switch (level) {
            case 1 -> 180;
            case 2 -> 260;
            case 3 -> 320;
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
        // 直径 5/8/12 格 -> 半径 2/4/6
        return switch (level) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            default -> 0;
        };
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        if (skillLevel <= 0 || skillLevel > getMaxLevel()) return;

        int radius = getRadius(skillLevel);
        int durationTicks = DURATIONS[skillLevel - 1] * ResourceCost.TICKS_PER_SECOND;

        List<LivingEntity> units = UnitServerEvents.getAllUnits().stream()
            .filter(e -> e instanceof Unit u && u.getOwnerName().equals(player.getName().getString()))
            .filter(e -> e.level() == level)
            .filter(e -> e.blockPosition().closerThan(center, radius + 0.5))
            .toList();

        var healEffect = EffectRegistrar.REST_HEAL.get();
        var extraHealthEffect = EffectRegistrar.REST_EXTRA_HEALTH.get();
        int amp = skillLevel - 1;
        for (LivingEntity le : units) {
            le.addEffect(new MobEffectInstance(healEffect, durationTicks, amp, false, true));
            le.addEffect(new MobEffectInstance(extraHealthEffect, EXTRA_HEALTH_DURATION_TICKS, amp, false, true));
        }

        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.0F);
        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PlayCommanderSkillSoundS2CPacket(ID)
        );
    }
}
