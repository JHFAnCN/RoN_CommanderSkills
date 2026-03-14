package com.ron.commanderskills;

import com.ron.commanderskills.data.CommanderSkillsSaveData;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.network.SyncCommanderSkillsS2CPacket;
import com.ron.commanderskills.skill.BountyMarkManager;
import com.ron.commanderskills.skill.CauldronZoneManager;
import com.ron.commanderskills.config.SkillConfig;
import com.ron.commanderskills.skill.CommanderSkill;
import com.ron.commanderskills.skill.FlameRocketRainManager;
import com.ron.commanderskills.skill.PhantomStrikeManager;
import com.ron.commanderskills.skill.TransmutationManager;
import com.ron.commanderskills.skill.CommanderSkillState;
import com.ron.commanderskills.registrar.EffectRegistrar;
import com.ron.commanderskills.skill.CommanderSkills;
import com.ron.commanderskills.skill.piglins.PiglinBountySkill;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.building.buildings.placements.BridgePlacement;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Explosion;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = RoNCommanderSkillsMod.MOD_ID)
public class RoNCommanderSkillsServerEvents {

    /** 玩家进入世界时同步全部技能状态到客户端（无存档视为0级）。延迟 1 tick 确保客户端已就绪（专用服务器上立即发可能丢失）。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent evt) {
        if (!(evt.getEntity() instanceof ServerPlayer player)) return;
        var server = player.getServer();
        if (server == null) return;
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;
        server.execute(() -> {
            CommanderSkillsSaveData data = CommanderSkillsSaveData.get(overworld);
            String name = player.getName().getString();
            Map<String, CommanderSkillState> toSync = new HashMap<>();
            for (CommanderSkill skill : CommanderSkills.getAll().values()) {
                CommanderSkillState state = data.getState(name, skill.getId());
                int lv = state == null ? 0 : state.level();
                float cd = state == null ? 0 : state.cooldownTicks();
                toSync.put(skill.getId(), new CommanderSkillState(lv, cd));
            }
            RoNCommanderSkillsNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncCommanderSkillsS2CPacket(toSync)
            );
        });
    }

    /** 幽匿护佑：亡灵单位死亡时若带有幽匿护佑效果，按概率复活；悬赏：被标记单位死亡时给施放者发放造价倍数资源 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent evt) {
        LivingEntity entity = evt.getEntity();
        if (entity.level().isClientSide()) return;

        BountyMarkManager.Mark bounty = BountyMarkManager.getAndRemoveOnDeath(entity.getId());
        if (bounty != null) {
            // 每击杀一个被标记单位，返还一次该技能的资源代价
            CommanderSkill skill = CommanderSkills.get(PiglinBountySkill.ID);
            int level = bounty.skillLevel();
            int useFood = skill != null ? SkillConfig.getUseFood(skill, level) : 50;
            int useWood = skill != null ? SkillConfig.getUseWood(skill, level) : 70;
            int useOre = skill != null ? SkillConfig.getUseOre(skill, level) : 100;
            BountyMarkManager.grantBountyReward(bounty.casterOwnerName(), useFood, useWood, useOre);
        }

        if (!(entity instanceof Unit)) return;
        MobEffectInstance ward = entity.getEffect(EffectRegistrar.SCULK_WARD.get());
        boolean isUndead = entity.getMobType() == MobType.UNDEAD || entity.getType() == EntityType.WARDEN
            || entity.getType() == EntityRegistrar.WARDEN_UNIT.get();
        if (ward == null || !isUndead) return;
        int amp = ward.getAmplifier();
        double chance = amp == 0 ? 0.10 : (amp == 1 ? 0.30 : 0.50);
        if (entity.getRandom().nextDouble() >= chance) return;

        BlockPos pos = entity.blockPosition();
        EntityType<?> type = entity.getType();
        String ownerName = ((Unit) entity).getOwnerName();
        ServerLevel level = (ServerLevel) entity.level();
        var server = level.getServer();
        if (server == null) return;
        server.execute(() -> {
            var newEntity = type.spawn(level, (net.minecraft.nbt.CompoundTag) null, null, pos, MobSpawnType.MOB_SUMMONED, true, false);
            if (newEntity instanceof Unit unit) {
                unit.setOwnerName(ownerName);
                unit.setupEquipmentAndUpgradesServer();
            }
        });
    }

    /** 每 tick 减少技能冷却，并同步到客户端 */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD) {
            return;
        }
        ServerLevel level = (ServerLevel) evt.level;
        CauldronZoneManager.tick(level);
        PhantomStrikeManager.tick(level);
        FlameRocketRainManager.tick(level);
        TransmutationManager.tick(level);
        CommanderSkillsSaveData data = CommanderSkillsSaveData.get(level);
        for (ServerPlayer player : level.players()) {
            String name = player.getName().getString();
            Map<String, CommanderSkillState> toSync = new HashMap<>();
            for (CommanderSkill skill : CommanderSkills.getAll().values()) {
                CommanderSkillState state = data.getState(name, skill.getId());
                if (state == null || state.cooldownTicks() <= 0) continue;
                float newCd = state.cooldownTicks() - 1f;
                if (newCd < 0) newCd = 0;
                CommanderSkillState newState = new CommanderSkillState(state.level(), newCd);
                data.setState(name, skill.getId(), newState);
                data.setDirty();
                toSync.put(skill.getId(), newState);
            }
            if (!toSync.isEmpty()) {
                RoNCommanderSkillsNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncCommanderSkillsS2CPacket(toSync)
                );
            }
        }
    }

    /** 焰火箭雨箭矢命中时：碰到任意目标都爆炸，优先处理以保证击中方块时也能触发 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileImpact(ProjectileImpactEvent evt) {
        if (evt.getProjectile() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow
            && FlameRocketRainManager.onArrowImpact(arrow, evt.getRayTraceResult())) {
            if (evt.getRayTraceResult().getType() == HitResult.Type.ENTITY) {
                evt.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            }
        }
    }

    /** 焰火箭雨爆炸：仅对敌方/中立的建筑与实体造成伤害；箭矢已对任意目标爆炸，此处过滤受击者 */
    private static final int FLAME_ROCKET_RAIN_BUILDING_DAMAGE = 20;

    /** 判断实体是否为施法者己方（不应对其造成爆炸伤害） */
    private static boolean isFriendlyToCaster(net.minecraft.world.entity.Entity entity, String caster) {
        if (caster == null || caster.isEmpty()) return false;
        if (entity instanceof Player p) return caster.equals(p.getName().getString()) || AlliancesServerEvents.isAllied(caster, p.getName().getString());
        if (entity instanceof Unit u) return caster.equals(u.getOwnerName()) || AlliancesServerEvents.isAllied(caster, u.getOwnerName());
        return false;
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate evt) {
        Explosion exp = evt.getExplosion();
        if (!(exp.getExploder() instanceof AbstractArrow arrow)) return;
        if (!"1".equals(arrow.getPersistentData().getString("RonCS_FlameRocketRain"))) return;
        if (evt.getLevel().isClientSide()) return;
        String caster = arrow.getPersistentData().getString("RonCS_FlameRocketRainCaster");

        // 实体：仅敌方/中立受到爆炸伤害，从受影响列表移除己方
        evt.getAffectedEntities().removeIf(e -> isFriendlyToCaster(e, caster));

        // 建筑：仅敌方/中立建筑受到伤害
        Set<BuildingPlacement> affectedBuildings = new HashSet<>();
        for (BlockPos bp : evt.getAffectedBlocks()) {
            BuildingPlacement building = BuildingUtils.findBuilding(false, bp);
            if (building != null) affectedBuildings.add(building);
        }
        for (BuildingPlacement building : affectedBuildings) {
            if (caster != null && !caster.isEmpty() && (caster.equals(building.ownerName) || AlliancesServerEvents.isAllied(caster, building.ownerName)))
                continue;
            int dmg = FLAME_ROCKET_RAIN_BUILDING_DAMAGE;
            if (building instanceof BridgePlacement) dmg /= 2;
            if (building instanceof GarrisonableBuilding garr && garr.getCapacity() > 0) {
                for (LivingEntity le : garr.getOccupants())
                    le.hurt(exp.getDamageSource(), evt.getLevel().getRandom().nextFloat() * (dmg + 1) / 2f);
            }
            building.destroyRandomBlocks(dmg);
        }
    }
}
