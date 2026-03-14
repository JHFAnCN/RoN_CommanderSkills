package com.ron.commanderskills.skill;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务端：焰火箭雨延迟箭矢生成。延迟 delayTicks 后，在 durationTicks 内每秒在区域内随机生成 1–3 支箭矢快速下落；
 * 箭矢碰到任意方块/生物/实体都会爆炸（1 TNT 范围伤害+烟花粒子），仅敌方/中立的建筑与兵种单位会受到爆炸伤害；下落过程有白色烟雾拖尾。
 * 箭雨落下时随机附带雷电劈下效果。
 */
public class FlameRocketRainManager {

    /** 每支箭矢落地时触发雷电的概率（0.0～1.0） */
    private static final double LIGHTNING_CHANCE = 0.35;

    /** 焰火箭雨生成的箭矢 UUID，命中时爆炸并移除此集合 */
    public static final Set<UUID> FLAME_ARROW_IDS = ConcurrentHashMap.newKeySet();

    private static final List<PendingRain> PENDING = new CopyOnWriteArrayList<>();

    public static void add(ServerLevel level, BlockPos center, int radius, int delayTicks, int durationTicks, String casterOwnerName) {
        long startTick = level.getGameTime();
        PENDING.add(new PendingRain(level, center, radius, delayTicks, durationTicks, startTick, casterOwnerName != null ? casterOwnerName : ""));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        // 焰火箭矢下落过程：白色烟雾粒子拖尾 + 着地检测（击中方块时若 ProjectileImpactEvent 未触发，用 onGround 兜底）
        AABB worldBox = new AABB(-3e7, level.getMinBuildHeight(), -3e7, 3e7, level.getMaxBuildHeight(), 3e7);
        for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, worldBox)) {
            if (!FLAME_ARROW_IDS.contains(arrow.getUUID()) || !arrow.isAlive()) continue;
            if (arrow.onGround()) {
                // 箭已着地（击中方块），若 ProjectileImpactEvent 未触发则在此补爆，仍用 arrow 为爆炸源以便 Detonate 过滤己方
                FLAME_ARROW_IDS.remove(arrow.getUUID());
                Vec3 pos = arrow.position();
                level.explode(arrow, pos.x, pos.y, pos.z, EXPLOSION_POWER_TNT, Level.ExplosionInteraction.TNT);
                com.solegendary.reignofnether.util.MiscUtil.doRandomFireworkExplosion(level, pos);
                level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0.5);
                trySpawnLightning(level, pos);
                arrow.discard();
                continue;
            }
            Vec3 p = arrow.position();
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.02);
        }
        PENDING.removeIf(pr -> {
            if (pr.level != level) return false;
            long age = now - pr.startTick;
            if (age < pr.delayTicks) return false;
            if (age >= pr.delayTicks + pr.durationTicks) return true;

            // 箭雨阶段：每秒生成 1–3 支箭
            int secondIndex = (int) ((age - pr.delayTicks) / 20);
            if (secondIndex > pr.lastSpawnedSecond) {
                pr.lastSpawnedSecond = secondIndex;
                int count = 1 + level.getRandom().nextInt(3);
                for (int i = 0; i < count; i++) {
                    spawnFlameArrow(level, pr.center, pr.radius, pr.casterOwnerName);
                }
            }
            return false;
        });
    }

    private static void spawnFlameArrow(ServerLevel level, BlockPos center, int radius, String casterOwnerName) {
        int dx = radius == 0 ? 0 : level.getRandom().nextInt(radius * 2 + 1) - radius;
        int dz = radius == 0 ? 0 : level.getRandom().nextInt(radius * 2 + 1) - radius;
        double x = center.getX() + 0.5 + dx;
        double z = center.getZ() + 0.5 + dz;
        double y = center.getY() + 40;

        Arrow arrow = new Arrow(level, x, y, z);
        arrow.setDeltaMovement(0, -3.5, 0);
        arrow.setNoGravity(false);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setOwner(null);
        arrow.getPersistentData().putString("RonCS_FlameRocketRain", "1");
        if (casterOwnerName != null && !casterOwnerName.isEmpty())
            arrow.getPersistentData().putString("RonCS_FlameRocketRainCaster", casterOwnerName);
        level.addFreshEntity(arrow);
        FLAME_ARROW_IDS.add(arrow.getUUID());
    }

    /** 箭矢落地时随机在落点召唤雷电 */
    private static void trySpawnLightning(ServerLevel level, Vec3 pos) {
        if (level.getRandom().nextDouble() >= LIGHTNING_CHANCE) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            level.addFreshEntity(bolt);
        }
    }

    /** 爆炸威力为 1 个 TNT（范围伤害） */
    private static final float EXPLOSION_POWER_TNT = 4.0f;

    /** 由 RoNCommanderSkillsServerEvents 在 ProjectileImpactEvent 中调用：焰火箭矢碰到任意目标都会爆炸并移除，伤害过滤在 ExplosionEvent.Detonate 中处理 */
    public static boolean onArrowImpact(AbstractArrow arrow, HitResult hitResult) {
        if (!FLAME_ARROW_IDS.remove(arrow.getUUID())) return false;
        Level level = arrow.level();
        if (level.isClientSide()) return true;
        Vec3 pos = arrow.position();
        if (level instanceof ServerLevel sl) {
            level.explode(arrow, pos.x, pos.y, pos.z, EXPLOSION_POWER_TNT, Level.ExplosionInteraction.TNT);
            com.solegendary.reignofnether.util.MiscUtil.doRandomFireworkExplosion(level, pos);
            sl.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0.5);
            trySpawnLightning(sl, pos);
        }
        arrow.discard();
        return true;
    }

    private static class PendingRain {
        final ServerLevel level;
        final BlockPos center;
        final int radius;
        final int delayTicks;
        final int durationTicks;
        final long startTick;
        final String casterOwnerName;
        int lastSpawnedSecond = -1;

        PendingRain(ServerLevel level, BlockPos center, int radius, int delayTicks, int durationTicks, long startTick, String casterOwnerName) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.delayTicks = delayTicks;
            this.durationTicks = durationTicks;
            this.startTick = startTick;
            this.casterOwnerName = casterOwnerName;
        }
    }
}
