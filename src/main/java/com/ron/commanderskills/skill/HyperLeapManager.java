package com.ron.commanderskills.skill;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 超跃传送：第一次点击设源圈（蓝），第二次点击设目标圈（红），将蓝圈内所有己方/中立/敌方单位传送到红圈内。
 */
public class HyperLeapManager {

    private static final Map<String, BlockPos> PENDING_SOURCE = new ConcurrentHashMap<>();

    /** 0=无效，1=已设源圈（等待目标），2=已执行传送 */
    public static int tryStep(ServerLevel level, ServerPlayer player, BlockPos center) {
        String ownerName = player.getName().getString();
        BlockPos existing = PENDING_SOURCE.get(ownerName);
        int radius = 10;
        if (existing == null) {
            PENDING_SOURCE.put(ownerName, center.immutable());
            return 1;
        }
        BlockPos source = existing;
        BlockPos dest = center.immutable();
        PENDING_SOURCE.remove(ownerName);

        Vec3 srcVec = Vec3.atCenterOf(source);
        Vec3 destVec = Vec3.atCenterOf(dest);
        AABB srcAabb = new AABB(source.getX() - radius, source.getY() - radius, source.getZ() - radius,
            source.getX() + radius, source.getY() + radius, source.getZ() + radius);
        var list = level.getEntitiesOfClass(LivingEntity.class, srcAabb, e -> e instanceof Unit);

        for (LivingEntity le : list) {
            if (!(le instanceof Unit)) continue;
            if (le.distanceToSqr(srcVec) > (double) (radius * radius)) continue;
            double dx = (level.getRandom().nextDouble() - 0.5) * radius * 2;
            double dz = (level.getRandom().nextDouble() - 0.5) * radius * 2;
            double ty = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, dest.getX() + (int) dx, dest.getZ() + (int) dz);
            le.teleportTo(destVec.x + dx, ty, destVec.z + dz);
        }
        return 2;
    }

    public static void clearForPlayer(String ownerName) {
        PENDING_SOURCE.remove(ownerName);
    }
}
