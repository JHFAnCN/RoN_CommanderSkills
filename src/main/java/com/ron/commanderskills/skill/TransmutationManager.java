package com.ron.commanderskills.skill;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 亡灵嬗变：延迟 5 秒后将范围内敌方/中立单位转变为己方亡灵，不可转变者扣 40% 血；被嬗变单位每秒扣 1 血直至死亡。
 */
public class TransmutationManager {

    private static final List<PendingTransmutation> PENDING = new CopyOnWriteArrayList<>();
    private static final double DAMAGE_PERCENT_NON_CONVERTIBLE = 0.40;
    /** 被嬗变产生的单位 ID，按维度存储，每秒扣 1 血直至死亡 */
    private static final java.util.Map<ResourceKey<Level>, Set<Integer>> CONVERTED_DOT_ENTITY_IDS = new ConcurrentHashMap<>();

    public static void add(ServerLevel level, BlockPos center, int radius, int delayTicks, String casterOwnerName) {
        long startTick = level.getGameTime();
        PENDING.add(new PendingTransmutation(level, center, radius, startTick, delayTicks, casterOwnerName != null ? casterOwnerName : ""));
    }

    /** 收集范围内敌方/中立单位，并分为可转变 / 不可转变，供发包与执行用 */
    public static class TargetsInRange {
        public final int[] convertibleEntityIds;
        public final int[] nonConvertibleEntityIds;

        public TargetsInRange(int[] convertibleEntityIds, int[] nonConvertibleEntityIds) {
            this.convertibleEntityIds = convertibleEntityIds;
            this.nonConvertibleEntityIds = nonConvertibleEntityIds;
        }
    }

    public static TargetsInRange getTargetsInRange(ServerLevel level, BlockPos center, int radius, String casterOwnerName) {
        Vec3 pos = Vec3.atCenterOf(center);
        AABB aabb = new AABB(center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius, center.getY() + radius, center.getZ() + radius);
        List<LivingEntity> inRange = level.getEntitiesOfClass(LivingEntity.class, aabb,
            e -> e.distanceToSqr(pos) <= (double) (radius * radius));
        List<Integer> convertible = new ArrayList<>();
        List<Integer> nonConvertible = new ArrayList<>();
        for (LivingEntity le : inRange) {
            if (!(le instanceof Unit unit)) continue;
            if (isFriendlyToCaster(unit, casterOwnerName)) continue;
            if (getConversionTargetType(le) != null) convertible.add(le.getId());
            else nonConvertible.add(le.getId());
        }
        return new TargetsInRange(convertible.stream().mapToInt(i -> i).toArray(), nonConvertible.stream().mapToInt(i -> i).toArray());
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        PENDING.removeIf(pr -> {
            if (pr.level != level) return false;
            long age = now - pr.startTick;
            if (age < pr.delayTicks) return false;
            runConversion(pr);
            return true;
        });
        // 被嬗变单位每秒扣 1 血直至死亡
        if (now % 20 == 0) {
            Set<Integer> ids = CONVERTED_DOT_ENTITY_IDS.get(level.dimension());
            if (ids != null && !ids.isEmpty()) {
                DamageSources damageSources = level.damageSources();
                for (Integer id : new ArrayList<>(ids)) {
                    Entity entity = level.getEntity(id);
                    if (entity instanceof LivingEntity le) {
                        if (le.isAlive())
                            le.hurt(damageSources.magic(), 1.0F);
                        else
                            ids.remove(id);
                    } else {
                        ids.remove(id);
                    }
                }
            }
        }
    }

    private static boolean isFriendlyToCaster(Unit unit, String caster) {
        if (caster == null || caster.isEmpty()) return false;
        return caster.equals(unit.getOwnerName()) || AlliancesServerEvents.isAllied(caster, unit.getOwnerName());
    }

    /** 可转变则返回目标 EntityType，否则返回 null */
    private static EntityType<?> getConversionTargetType(LivingEntity entity) {
        if (entity instanceof VillagerUnit) return EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get();
        if (entity instanceof PillagerUnit || entity instanceof VindicatorUnit || entity instanceof EvokerUnit || entity instanceof WitchUnit) {
            int r = entity.getRandom().nextInt(3);
            return switch (r) {
                case 0 -> EntityRegistrar.ZOMBIE_UNIT.get();
                case 1 -> EntityRegistrar.DROWNED_UNIT.get();
                default -> EntityRegistrar.HUSK_UNIT.get();
            };
        }
        if (entity instanceof GruntUnit || entity instanceof BruteUnit || entity instanceof HeadhunterUnit)
            return EntityRegistrar.ZOMBIE_PIGLIN_UNIT.get();
        if (entity instanceof HoglinUnit) return EntityRegistrar.ZOGLIN_UNIT.get();
        if (entity instanceof WitherSkeletonUnit)
            return entity.getRandom().nextBoolean() ? EntityRegistrar.SKELETON_UNIT.get() : EntityRegistrar.STRAY_UNIT.get();
        return null;
    }

    private static void runConversion(PendingTransmutation pr) {
        ServerLevel level = pr.level;
        String caster = pr.casterOwnerName;
        Vec3 pos = Vec3.atCenterOf(pr.center);
        AABB aabb = new AABB(pr.center.getX() - pr.radius, pr.center.getY() - pr.radius, pr.center.getZ() - pr.radius,
            pr.center.getX() + pr.radius, pr.center.getY() + pr.radius, pr.center.getZ() + pr.radius);
        List<LivingEntity> inRange = level.getEntitiesOfClass(LivingEntity.class, aabb,
            e -> e.distanceToSqr(pos) <= (double) (pr.radius * pr.radius));
        DamageSources damageSources = level.damageSources();
        for (LivingEntity le : inRange) {
            if (!(le instanceof Unit unit)) continue;
            if (isFriendlyToCaster(unit, caster)) continue;
            EntityType<?> targetType = getConversionTargetType(le);
            if (targetType != null) {
                BlockPos at = le.blockPosition();
                var newEntity = targetType.spawn(level, (net.minecraft.nbt.CompoundTag) null, null, at, MobSpawnType.MOB_SUMMONED, true, false);
                if (newEntity instanceof Unit newUnit) {
                    newUnit.setOwnerName(caster);
                    newUnit.setupEquipmentAndUpgradesServer();
                }
                if (newEntity != null)
                    CONVERTED_DOT_ENTITY_IDS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(newEntity.getId());
                le.discard();
            } else {
                float maxHp = le.getMaxHealth();
                le.hurt(damageSources.magic(), maxHp * (float) DAMAGE_PERCENT_NON_CONVERTIBLE);
            }
        }
    }

    private static class PendingTransmutation {
        final ServerLevel level;
        final BlockPos center;
        final int radius;
        final long startTick;
        final int delayTicks;
        final String casterOwnerName;

        PendingTransmutation(ServerLevel level, BlockPos center, int radius, long startTick, int delayTicks, String casterOwnerName) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.startTick = startTick;
            this.delayTicks = delayTicks;
            this.casterOwnerName = casterOwnerName;
        }
    }
}
