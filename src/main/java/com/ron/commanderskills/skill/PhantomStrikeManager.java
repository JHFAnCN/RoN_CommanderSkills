package com.ron.commanderskills.skill;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务端：幻翼特攻队延迟召唤闪电苦力怕。到点后在目标上方 60 格生成若干 CreeperUnit（charged）。
 */
public class PhantomStrikeManager {

    private static final List<PendingStrike> STRIKES = new CopyOnWriteArrayList<>();

    public static void addStrike(ServerLevel level, BlockPos center, int radius, int delayTicks, String ownerName, int creeperCount) {
        long spawnAtTick = level.getGameTime() + delayTicks;
        STRIKES.add(new PendingStrike(level, center, radius, spawnAtTick, ownerName, creeperCount));
    }

    public static void clear() {
        STRIKES.clear();
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        STRIKES.removeIf(s -> {
            if (s.level != level || now < s.spawnAtTick) return false;
            // 到点：在 center 上方 60 格、半径内随机位置生成 creeperCount 只闪电苦力怕
            EntityType<?> creeperType = EntityRegistrar.CREEPER_UNIT.get();
            for (int i = 0; i < s.creeperCount; i++) {
                int dx = (level.getRandom().nextInt(Math.max(1, s.radius * 2 + 1))) - s.radius;
                int dz = (level.getRandom().nextInt(Math.max(1, s.radius * 2 + 1))) - s.radius;
                BlockPos spawnPos = s.center.offset(dx, 60, dz);
                // 使用 NBT 数据设置闪电状态
                CompoundTag nbt = new CompoundTag();
                nbt.putBoolean("powered", true);
                var entity = creeperType.spawn(level, nbt, null, spawnPos, MobSpawnType.MOB_SUMMONED, true, false);
                if (entity instanceof Unit unit) {
                    unit.setOwnerName(s.ownerName);
                    unit.setupEquipmentAndUpgradesServer();
                }
            }
            return true;
        });
    }

    private static class PendingStrike {
        final ServerLevel level;
        final BlockPos center;
        final int radius;
        final long spawnAtTick;
        final String ownerName;
        final int creeperCount;

        PendingStrike(ServerLevel level, BlockPos center, int radius, long spawnAtTick, String ownerName, int creeperCount) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.spawnAtTick = spawnAtTick;
            this.ownerName = ownerName;
            this.creeperCount = creeperCount;
        }
    }
}
