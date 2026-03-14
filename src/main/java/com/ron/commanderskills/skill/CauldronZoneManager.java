package com.ron.commanderskills.skill;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务端：管理坩埚狂欢效果圈。圈内敌方/中立单位随机获得 缓慢/虚弱/凋零，等级 1/2/3；己方/盟友获得 力量/速度/生命恢复 1 级，出圈失效。
 */
public class CauldronZoneManager {

    private static final List<CauldronZone> ZONES = new CopyOnWriteArrayList<>();

    private static final net.minecraft.world.effect.MobEffect[] DEBUFFS = {
        MobEffects.MOVEMENT_SLOWDOWN,   // 缓慢
        MobEffects.WEAKNESS,            // 虚弱
        MobEffects.WITHER               // 凋零
    };

    private static final net.minecraft.world.effect.MobEffect[] ALLY_BUFFS = {
        MobEffects.DAMAGE_BOOST,        // 力量
        MobEffects.MOVEMENT_SPEED,      // 速度
        MobEffects.REGENERATION         // 生命恢复
    };

    private static final int REFRESH_TICKS = 20; // 每秒刷新一次
    private static final int BUFF_DURATION_TICKS = 40; // 2 秒，出圈后自动消失

    public static void addZone(ServerLevel level, BlockPos center, int radius, int durationTicks, String casterName, int skillLevel) {
        long endTick = level.getGameTime() + durationTicks;
        ZONES.add(new CauldronZone(level, center, radius, endTick, casterName, skillLevel));
    }

    /** 重置游戏时清空所有效果圈 */
    public static void clear() {
        ZONES.clear();
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        ZONES.removeIf(z -> now >= z.endTick);

        for (CauldronZone zone : ZONES) {
            if (zone.level != level) continue;
            if ((now - zone.createdAt) % REFRESH_TICKS != 0) continue;

            Vec3 centerVec = Vec3.atCenterOf(zone.center);
            double r = zone.radius + 0.5;
            AABB aabb = new AABB(
                zone.center.getX() - r, zone.center.getY() - r, zone.center.getZ() - r,
                zone.center.getX() + r, zone.center.getY() + r, zone.center.getZ() + r
            );
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);

            int debuffAmplifier = Math.max(0, zone.skillLevel - 1); // 1/2/3 级 -> 0/1/2
            for (LivingEntity le : entities) {
                if (!le.blockPosition().closerThan(zone.center, zone.radius + 0.5)) continue;
                if (le.isInvulnerable() || !le.isAlive()) continue;
                boolean isAlly = le instanceof Unit u && (u.getOwnerName().equals(zone.casterName) || com.solegendary.reignofnether.alliance.AlliancesServerEvents.isAllied(zone.casterName, u.getOwnerName()));
                if (isAlly) {
                    // 己方/盟友：随机一种增益 力量/速度/生命恢复 1 级
                    int bufIdx = Math.abs((le.getId() + zone.center.hashCode()) % 3);
                    le.addEffect(new MobEffectInstance(ALLY_BUFFS[bufIdx], BUFF_DURATION_TICKS, 0, false, true));
                } else {
                    // 敌方或中立：随机一种 debuff 缓慢/虚弱/凋零，等级 1/2/3
                    int idx = Math.abs((le.getId() + zone.center.hashCode()) % 3);
                    le.addEffect(new MobEffectInstance(DEBUFFS[idx], BUFF_DURATION_TICKS, debuffAmplifier, false, true));
                }
            }
        }
    }

    private static class CauldronZone {
        final ServerLevel level;
        final BlockPos center;
        final int radius;
        final long endTick;
        final String casterName;
        final int skillLevel;
        final long createdAt;

        CauldronZone(ServerLevel level, BlockPos center, int radius, long endTick, String casterName, int skillLevel) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.endTick = endTick;
            this.casterName = casterName;
            this.skillLevel = skillLevel;
            this.createdAt = level.getGameTime();
        }
    }
}
