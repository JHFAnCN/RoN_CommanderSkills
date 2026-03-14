package com.ron.commanderskills.skill;

import com.ron.commanderskills.config.SkillConfig;
import com.ron.commanderskills.data.CommanderSkillsSaveData;
import com.ron.commanderskills.network.DisplacementFirstTargetS2CPacket;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.network.SyncCommanderSkillsS2CPacket;
import com.ron.commanderskills.skill.DisplacementManager;
import com.ron.commanderskills.skill.monsters.MonsterTransmutationSkill;
import com.ron.commanderskills.skill.piglins.PiglinDisplacementSkill;
import com.ron.commanderskills.skill.piglins.PiglinHyperLeapSkill;
import com.ron.commanderskills.skill.villagers.VillagerFlameRocketRainSkill;
import com.ron.commanderskills.network.HyperLeapSourceZoneS2CPacket;
import com.ron.commanderskills.network.HyperLeapExecuteS2CPacket;
import com.ron.commanderskills.network.HyperLeapCooldownS2CPacket;
import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import net.minecraftforge.network.PacketDistributor;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class CommanderSkillManager {

    public static CommanderSkillState getState(ServerLevel level, ServerPlayer player, CommanderSkill skill) {
        if (level.getServer() == null) return null;
        CommanderSkillsSaveData data = CommanderSkillsSaveData.get(level.getServer().overworld());
        return data.getState(player.getName().getString(), skill.getId());
    }

    private static void setStateAndSync(ServerLevel level, ServerPlayer player, CommanderSkill skill, CommanderSkillState state) {
        if (level.getServer() == null) return;
        CommanderSkillsSaveData data = CommanderSkillsSaveData.get(level.getServer().overworld());
        data.setState(player.getName().getString(), skill.getId(), state);
        data.setDirty();

        Map<String, CommanderSkillState> single = new HashMap<>();
        single.put(skill.getId(), state);
        RoNCommanderSkillsNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new SyncCommanderSkillsS2CPacket(single)
        );
    }

    public static void unlockOrRankUp(ServerLevel level, ServerPlayer player, CommanderSkill skill) {
        if (skill == null) return;

        CommanderSkillState current = getState(level, player, skill);
        int newLevel = current == null ? 1 : current.level() + 1;
        if (newLevel > skill.getMaxLevel()) return;

        boolean sandbox = SandboxServer.isSandboxPlayer(player.getName().getString());
        if (!sandbox) {
            String requiredBuilding = skill.getRequiredBuildingNameForLevel(newLevel);
            if (requiredBuilding != null && !hasBuilding(level, player, requiredBuilding)) {
                return;
            }
            int food, wood, ore;
            if (newLevel == 1) {
                food = SkillConfig.getUnlockFood(skill);
                wood = SkillConfig.getUnlockWood(skill);
                ore = SkillConfig.getUnlockOre(skill);
            } else {
                food = SkillConfig.getUpgradeFood(skill, newLevel);
                wood = SkillConfig.getUpgradeWood(skill, newLevel);
                ore = SkillConfig.getUpgradeOre(skill, newLevel);
            }
            if (!canAfford(player.getName().getString(), food, wood, ore)) {
                return;
            }
            ResourcesServerEvents.addSubtractResources(new Resources(player.getName().getString(), -food, -wood, -ore));
        }
        float initialCooldown = 0;
        if (!sandbox && newLevel == 1 && (VillagerFlameRocketRainSkill.ID.equals(skill.getId()) || MonsterTransmutationSkill.ID.equals(skill.getId()) || PiglinHyperLeapSkill.ID.equals(skill.getId())))
            initialCooldown = SkillConfig.getCooldownTicks(skill, newLevel);
        setStateAndSync(level, player, skill, new CommanderSkillState(newLevel, initialCooldown));
        // 焰火箭雨/亡灵嬗变解锁：全局播报
        if (newLevel == 1 && level.getServer() != null) {
            if (VillagerFlameRocketRainSkill.ID.equals(skill.getId()))
                level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("[" + player.getName().getString() + "] 解锁了超级武器 焰火箭雨"), false);
            else if (MonsterTransmutationSkill.ID.equals(skill.getId()))
                level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("[" + player.getName().getString() + "] 解锁了超级武器 亡灵嬗变"), false);
            else if (PiglinHyperLeapSkill.ID.equals(skill.getId()))
                level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("[" + player.getName().getString() + "] 解锁了超级武器 超跃传送"), false);
        }
    }

    public static void useSkill(ServerLevel level, ServerPlayer player, CommanderSkill skill, BlockPos center) {
        if (skill == null) return;
        CommanderSkillState state = getState(level, player, skill);
        if (state == null || state.level() <= 0) return;

        if (state.cooldownTicks() > 0) return;

        boolean sandbox = SandboxServer.isSandboxPlayer(player.getName().getString());
        int radius = skill.getRadius(state.level());

        // 超跃传送：两阶段，第一步不扣资源
        if (PiglinHyperLeapSkill.ID.equals(skill.getId())) {
            int step = HyperLeapManager.tryStep(level, player, center);
            if (step == 0) return;
            if (step == 1) {
                RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new HyperLeapSourceZoneS2CPacket(center.getX(), center.getY(), center.getZ(), radius));
                RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new PlayCommanderSkillSoundS2CPacket("piglin_hyper_leap_source"));
                return;
            }
            // step == 2：扣资源、进冷却、广播执行与冷却
            if (!sandbox) {
                int useFood = SkillConfig.getUseFood(skill, state.level());
                int useWood = SkillConfig.getUseWood(skill, state.level());
                int useOre = SkillConfig.getUseOre(skill, state.level());
                if (!canAfford(player.getName().getString(), useFood, useWood, useOre)) return;
                ResourcesServerEvents.addSubtractResources(new Resources(
                    player.getName().getString(), -useFood, -useWood, -useOre));
            }
            float newCooldown = sandbox ? 0 : SkillConfig.getCooldownTicks(skill, state.level());
            setStateAndSync(level, player, skill, new CommanderSkillState(state.level(), newCooldown));
            RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new HyperLeapExecuteS2CPacket(center.getX(), center.getY(), center.getZ(), radius));
            RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new PlayCommanderSkillSoundS2CPacket("piglin_hyper_leap_execute"));
            RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new HyperLeapCooldownS2CPacket(player.getName().getString(), (int) newCooldown));
            return;
        }

        if (!sandbox) {
            int useFood = SkillConfig.getUseFood(skill, state.level());
            int useWood = SkillConfig.getUseWood(skill, state.level());
            int useOre = SkillConfig.getUseOre(skill, state.level());
            if (!canAfford(player.getName().getString(), useFood, useWood, useOre)) {
                return;
            }
            ResourcesServerEvents.addSubtractResources(new Resources(
                player.getName().getString(),
                -useFood,
                -useWood,
                -useOre
            ));
        }

        if (PiglinDisplacementSkill.ID.equals(skill.getId())) {
            int result = DisplacementManager.tryTarget(level, player, center);
            if (result == 0) return;
            if (result == 1) {
                Integer firstId = DisplacementManager.getFirstTargetId(player.getName().getString());
                RoNCommanderSkillsNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new DisplacementFirstTargetS2CPacket(firstId != null ? firstId : -1)
                );
                return;
            }
        }

        skill.apply(level, player, state.level(), center);
        // 沙盒模式无冷却；非沙盒为技能正常冷却
        float newCooldown = sandbox ? 0 : SkillConfig.getCooldownTicks(skill, state.level());
        setStateAndSync(level, player, skill, new CommanderSkillState(state.level(), newCooldown));
        // 焰火箭雨/亡灵嬗变：全局冷却倒计时广播，所有人可见
        if (VillagerFlameRocketRainSkill.ID.equals(skill.getId())) {
            RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new com.ron.commanderskills.network.FlameRocketRainCooldownS2CPacket(
                    player.getName().getString(), (int) newCooldown));
        } else if (MonsterTransmutationSkill.ID.equals(skill.getId())) {
            RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new com.ron.commanderskills.network.TransmutationCooldownS2CPacket(
                    player.getName().getString(), (int) newCooldown));
        }
    }

    private static boolean canAfford(String ownerName, int food, int wood, int ore) {
        for (Resources r : ResourcesServerEvents.resourcesList) {
            if (r.ownerName.equals(ownerName)) {
                return r.food >= food && r.wood >= wood && r.ore >= ore;
            }
        }
        return false;
    }

    private static boolean hasBuilding(ServerLevel level, ServerPlayer player, String buildingName) {
        if (buildingName == null) return true;
        String owner = player.getName().getString();
        return BuildingServerEvents.getBuildings().stream()
            .anyMatch(b -> b.ownerName.equals(owner) && b.isBuilt && buildingName.equals(b.getBuilding().name));
    }
}


