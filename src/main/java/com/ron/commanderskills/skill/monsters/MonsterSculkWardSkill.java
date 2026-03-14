package com.ron.commanderskills.skill.monsters;

import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.registrar.EffectRegistrar;
import com.ron.commanderskills.skill.CommanderSkill;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.buildings.monsters.HauntedHouse;
import com.solegendary.reignofnether.building.buildings.monsters.Laboratory;
import com.solegendary.reignofnether.building.buildings.monsters.Stronghold;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;

import java.util.List;

/**
 * 幽匿护佑：范围内己方/盟友单位白天不燃烧、免疫白天负面效果；亡灵 10%/30%/50% 概率复活，持续 40/60/80 秒。
 */
public class MonsterSculkWardSkill implements CommanderSkill {

    public static final String ID = "monster_sculk_ward";

    private static final int[] DURATIONS_SEC = {40, 60, 80};

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
        return 220;
    }

    @Override
    public int getUnlockWood() {
        return 520;
    }

    @Override
    public int getUnlockOre() {
        return 420;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 320;
            case 3 -> 450;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeWood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 650;
            case 3 -> 780;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeOre(int toLevel) {
        return switch (toLevel) {
            case 2 -> 550;
            case 3 -> 720;
            default -> 0;
        };
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return switch (level) {
            case 1 -> HauntedHouse.buildingName;
            case 2 -> Laboratory.buildingName;
            case 3 -> Stronghold.buildingName;
            default -> null;
        };
    }

    @Override
    public int getUseFood(int level) {
        return switch (level) {
            case 1 -> 60;
            case 2 -> 180;
            case 3 -> 320;
            default -> 0;
        };
    }

    @Override
    public int getUseWood(int level) {
        return switch (level) {
            case 1 -> 80;
            case 2 -> 260;
            case 3 -> 420;
            default -> 0;
        };
    }

    @Override
    public int getUseOre(int level) {
        return switch (level) {
            case 1 -> 260;
            case 2 -> 360;
            case 3 -> 560;
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
        // 直径 8/12/16 -> 半径 4/6/8
        return switch (level) {
            case 1 -> 4;
            case 2 -> 6;
            case 3 -> 8;
            default -> 0;
        };
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        if (skillLevel <= 0 || skillLevel > getMaxLevel()) return;

        String ownerName = player.getName().getString();
        int radius = getRadius(skillLevel);
        int durationTicks = DURATIONS_SEC[skillLevel - 1] * ResourceCost.TICKS_PER_SECOND;
        int amplifier = skillLevel - 1;

        List<LivingEntity> units = UnitServerEvents.getAllUnits().stream()
            .filter(e -> e.level() == level)
            .filter(e -> e.blockPosition().closerThan(center, radius + 0.5))
            .filter(e -> e instanceof Unit u && (u.getOwnerName().equals(ownerName) || AlliancesServerEvents.isAllied(ownerName, u.getOwnerName())))
            .toList();

        var wardEffect = EffectRegistrar.SCULK_WARD.get();
        var fireResist = MobEffects.FIRE_RESISTANCE;
        for (LivingEntity le : units) {
            le.addEffect(new MobEffectInstance(wardEffect, durationTicks, amplifier, false, true));
            le.addEffect(new MobEffectInstance(fireResist, durationTicks, 0, false, true));
        }

        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 0.7F, 1.0F);
        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PlayCommanderSkillSoundS2CPacket(ID)
        );
    }
}
