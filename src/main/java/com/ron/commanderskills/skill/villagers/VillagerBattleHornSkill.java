package com.ron.commanderskills.skill.villagers;

import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.registrar.EffectRegistrar;
import com.ron.commanderskills.skill.CommanderSkill;
import com.solegendary.reignofnether.building.buildings.villagers.Barracks;
import net.minecraftforge.network.PacketDistributor;
import com.solegendary.reignofnether.building.buildings.villagers.Blacksmith;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class VillagerBattleHornSkill implements CommanderSkill {

    public static final String ID = "villager_battle_horn";

    // 持续时间（秒）：30 / 50 / 80
    private static final int[] DURATIONS = {30, 50, 80};
    // 伤害提升：20 / 30 / 40（百分比）
    private static final double[] DAMAGE = {0.20, 0.30, 0.40};
    // 移速提升：10 / 20 / 40（百分比）
    private static final double[] MOVE_SPEED = {0.10, 0.20, 0.40};

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
        return 450;
    }

    @Override
    public int getUnlockWood() {
        return 700;
    }

    @Override
    public int getUnlockOre() {
        return 450;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 550;
            case 3 -> 700;
            default -> 0;
        };
    }

    @Override
    public int getUpgradeWood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 850;
            case 3 -> 1000;
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
            case 1 -> Barracks.buildingName;
            case 2 -> Blacksmith.buildingName;
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
            case 1 -> 250;
            case 2 -> 350;
            case 3 -> 450;
            default -> 0;
        };
    }

    @Override
    public int getUseOre(int level) {
        return switch (level) {
            case 1 -> 250;
            case 2 -> 350;
            case 3 -> 450;
            default -> 0;
        };
    }

    @Override
    public int getCooldownTicks(int level) {
        // 2 / 3 / 4 分钟
        return switch (level) {
            case 1 -> 2 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 2 -> 3 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 3 -> 4 * 60 * ResourceCost.TICKS_PER_SECOND;
            default -> 0;
        };
    }

    @Override
    public int getRadius(int level) {
        // 直径：8 / 14 / 20 格 -> 半径 4 / 7 / 10
        return switch (level) {
            case 1 -> 4;   // 直径8格
            case 2 -> 7;   // 直径14格
            case 3 -> 10;  // 直径20格
            default -> 0;
        };
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        if (skillLevel <= 0 || skillLevel > getMaxLevel()) return;

        int radius = getRadius(skillLevel);
        double damage = DAMAGE[skillLevel - 1];
        double moveSpeed = MOVE_SPEED[skillLevel - 1];
        int durationTicks = DURATIONS[skillLevel - 1] * ResourceCost.TICKS_PER_SECOND;

        // 找到范围内、己方单位
        List<LivingEntity> units = UnitServerEvents.getAllUnits().stream()
            .filter(e -> e instanceof Unit u && u.getOwnerName().equals(player.getName().getString()))
            .filter(e -> e.level() == level)
            .filter(e -> e.blockPosition().closerThan(center, radius + 0.5))
            .toList();

        net.minecraft.world.effect.MobEffect effect = EffectRegistrar.BATTLE_HORN_BUFF.get();
        for (LivingEntity le : units) {
            le.addEffect(new MobEffectInstance(
                effect,
                durationTicks,
                skillLevel - 1,
                false,
                true
            ));
        }

        // 服务端播放给附近玩家；再发 S2C 让施放者客户端本地播放，指挥官视角（相机远离）也能听到
        net.minecraft.sounds.SoundEvent sound = ((net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent>) net.minecraft.sounds.SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0)).value();
        level.playSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 2.0F, 1.0F);
        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PlayCommanderSkillSoundS2CPacket(ID)
        );
    }
}

