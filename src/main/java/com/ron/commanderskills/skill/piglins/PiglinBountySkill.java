package com.ron.commanderskills.skill.piglins;

import com.ron.commanderskills.network.AddBountyMarksS2CPacket;
import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.skill.BountyMarkManager;
import com.ron.commanderskills.skill.CommanderSkill;
import com.solegendary.reignofnether.building.buildings.piglins.FlameSanctuary;
import com.solegendary.reignofnether.building.buildings.piglins.Fortress;
import com.solegendary.reignofnether.building.buildings.piglins.HoglinStables;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 悬赏：技能使用后，随机标记全局的敌方/中立兵种单位（醒目标记，金色发光描边）2/4/6 个。
 * 每击杀一个被标记单位，返还一次该技能的资源代价。持续 2/3/4 分钟。
 */
public class PiglinBountySkill implements CommanderSkill {

    public static final String ID = "piglin_bounty";

    private static final int[] DURATIONS_SEC = {120, 180, 240}; // 2/3/4 分钟
    /** 1/2/3 级标记数量 */
    private static final int[] MARK_COUNTS = {2, 4, 6};

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
        return 220;
    }

    @Override
    public int getUnlockWood() {
        return 320;
    }

    @Override
    public int getUnlockOre() {
        return 380;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return switch (toLevel) {
            case 2 -> 300;
            case 3 -> 400;
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
            case 2 -> 480;
            case 3 -> 620;
            default -> 0;
        };
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return switch (level) {
            case 1 -> HoglinStables.buildingName;
            case 2 -> FlameSanctuary.buildingName;
            case 3 -> Fortress.buildingName;
            default -> null;
        };
    }

    @Override
    public int getUseFood(int level) {
        return switch (level) {
            case 1 -> 50;
            case 2 -> 85;
            case 3 -> 120;
            default -> 0;
        };
    }

    @Override
    public int getUseWood(int level) {
        return switch (level) {
            case 1 -> 70;
            case 2 -> 100;
            case 3 -> 130;
            default -> 0;
        };
    }

    @Override
    public int getUseOre(int level) {
        return switch (level) {
            case 1 -> 100;
            case 2 -> 150;
            case 3 -> 200;
            default -> 0;
        };
    }

    @Override
    public int getCooldownTicks(int level) {
        return switch (level) {
            case 1 -> 1 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 2 -> 2 * 60 * ResourceCost.TICKS_PER_SECOND;
            case 3 -> 3 * 60 * ResourceCost.TICKS_PER_SECOND;
            default -> 0;
        };
    }

    @Override
    public int getRadius(int level) {
        return 0; // 全局标记，无范围限制
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        if (skillLevel <= 0 || skillLevel > getMaxLevel()) return;
        String casterName = player.getName().getString();
        int markCount = MARK_COUNTS[skillLevel - 1];
        long endTick = level.getGameTime() + DURATIONS_SEC[skillLevel - 1] * ResourceCost.TICKS_PER_SECOND;

        // 全局查找所有敌方/中立的兵种单位（收集为可变列表以便 shuffle）
        List<LivingEntity> candidates = UnitServerEvents.getAllUnits().stream()
            .filter(e -> e.level() == level)
            .filter(e -> e instanceof Unit u && !u.getOwnerName().equals(casterName))
            .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(candidates);
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < Math.min(markCount, candidates.size()); i++) {
            LivingEntity e = candidates.get(i);
            BountyMarkManager.mark(e, casterName, endTick, skillLevel);
            ids.add(e.getId());
        }

        RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
            new AddBountyMarksS2CPacket(ids, (int) (DURATIONS_SEC[skillLevel - 1] * ResourceCost.TICKS_PER_SECOND)));
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
        RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new PlayCommanderSkillSoundS2CPacket(ID));
    }
}
