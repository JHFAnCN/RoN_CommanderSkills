package com.ron.commanderskills.skill.monsters;

import com.ron.commanderskills.network.PlayCommanderSkillSoundS2CPacket;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.network.TransmutationEffectS2CPacket;
import com.ron.commanderskills.skill.CommanderSkill;
import com.ron.commanderskills.skill.TransmutationManager;
import com.solegendary.reignofnether.building.buildings.monsters.Stronghold;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * 亡灵嬗变：超级武器大招。幽匿要塞解锁；解锁后直接进入10分钟冷却；范围直径30格；延迟5秒后将范围内敌方/中立单位转变为己方亡灵，不可转变者扣40%血。
 */
public class MonsterTransmutationSkill implements CommanderSkill {

    public static final String ID = "monster_transmutation";

    private static final int COOLDOWN_TICKS = 10 * 60 * ResourceCost.TICKS_PER_SECOND; // 10分钟
    private static final int DELAY_TICKS = 5 * ResourceCost.TICKS_PER_SECOND; // 5秒

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
        return 1;
    }

    @Override
    public int getUnlockFood() {
        return 600;
    }

    @Override
    public int getUnlockWood() {
        return 1200;
    }

    @Override
    public int getUnlockOre() {
        return 1200;
    }

    @Override
    public int getUpgradeFood(int toLevel) {
        return 0;
    }

    @Override
    public int getUpgradeWood(int toLevel) {
        return 0;
    }

    @Override
    public int getUpgradeOre(int toLevel) {
        return 0;
    }

    @Override
    public String getRequiredBuildingNameForLevel(int level) {
        return level == 1 ? Stronghold.buildingName : null;
    }

    @Override
    public int getUseFood(int level) {
        return 300;
    }

    @Override
    public int getUseWood(int level) {
        return 500;
    }

    @Override
    public int getUseOre(int level) {
        return 500;
    }

    @Override
    public int getCooldownTicks(int level) {
        return COOLDOWN_TICKS;
    }

    private static final int RADIUS = 15; // 直径30格，仅1级

    @Override
    public int getRadius(int level) {
        return RADIUS;
    }

    @Override
    public void apply(ServerLevel level, ServerPlayer player, int skillLevel, BlockPos center) {
        if (skillLevel <= 0) return;
        String ownerName = player.getName().getString();
        RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new PlayCommanderSkillSoundS2CPacket(ID));
        TransmutationManager.add(level, center, RADIUS, DELAY_TICKS, ownerName);
        TransmutationManager.TargetsInRange targets = TransmutationManager.getTargetsInRange(level, center, RADIUS, ownerName);
        RoNCommanderSkillsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
            new TransmutationEffectS2CPacket(center.getX(), center.getY(), center.getZ(), RADIUS, DELAY_TICKS,
                targets.convertibleEntityIds, targets.nonConvertibleEntityIds));
    }
}
