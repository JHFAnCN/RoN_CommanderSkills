package com.ron.commanderskills.skill.monsters;

import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.registrar.EffectRegistrar;
import com.ron.commanderskills.skill.CommanderSkill;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.buildings.monsters.Graveyard;
import com.solegendary.reignofnether.building.buildings.monsters.Laboratory;
import com.solegendary.reignofnether.building.buildings.monsters.Stronghold;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.PacketDistributor;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;

import java.util.List;

/**
 * 不死者：对全局己方/盟友亡灵生物（含监守者）每秒治疗 1/2/3 点血，持续 20 秒。
 */
public class MonsterUndyingSkill implements CommanderSkill {

    public static final String ID = "monster_undying";

    private static final int DURATION_SEC = 20;
    private static final int[] HEAL_PER_SECOND = {1, 2, 3};

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
        return 350;
    }

    @Override
    public int getUnlockWood() {
        return 450;
    }

    @Override
    public int getUnlockOre() {
        return 350;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 420;
            case 3 -> 520;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeWood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 380;
            case 3 -> 550;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeOre(int toLevel) {
        return switch (toLevel) {
            case 2 -> 280;
            case 3 -> 420;
            default -> 0;
        };
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return switch (level) {
            case 1 -> Graveyard.buildingName;
            case 2 -> Laboratory.buildingName;
            case 3 -> Stronghold.buildingName;
            default -> null;
        };
    }

    @Override
    public int getUseFood(int level) {
        return switch (level) {
            case 1 -> 80;
            case 2 -> 140;
            case 3 -> 200;
            default -> 0;
        };
    }

    @Override
    public int getUseWood(int level) {
        return switch (level) {
            case 1 -> 150;
            case 2 -> 220;
            case 3 -> 280;
            default -> 0;
        };
    }

    @Override
    public int getUseOre(int level) {
        return switch (level) {
            case 1 -> 150;
            case 2 -> 220;
            case 3 -> 280;
            default -> 0;
        };
    }

    @Override
    public int getCooldownTicks(int level) {
        return switch (level) {
            case 1 -> 3 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 2 -> 4 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 3 -> 5 * 60 * ResourceCost.TICKS_PER_SECOND;
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

        String ownerName = player.getName().getString();
        int durationTicks = DURATION_SEC * ResourceCost.TICKS_PER_SECOND;
        int amplifier = skillLevel - 1;

        List<LivingEntity> units = UnitServerEvents.getAllUnits().stream()
            .filter(e -> e.level() == level)
            .filter(e -> e instanceof Unit u && (u.getOwnerName().equals(ownerName) || AlliancesServerEvents.isAllied(ownerName, u.getOwnerName())))
            .filter(e -> e.getMobType() == MobType.UNDEAD || e.getType() == EntityRegistrar.WARDEN_UNIT.get())
            .toList();

        var effect = EffectRegistrar.UNDEAD_HEAL.get();
        for (LivingEntity le : units) {
            le.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
        }

        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.7F, 0.9F);
        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PlayCommanderSkillSoundS2CPacket(ID)
        );
    }
}
