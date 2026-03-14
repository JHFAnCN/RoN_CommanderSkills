package com.ron.commanderskills.skill;

import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高效传送门：全局己方生产建筑兵种生产速度提升，持续一段时间。
 * 生产速度加成 20%/40%/60% = 每 tick 多减 0.2/0.4/0.6 的 ticksLeft。
 */
public class EfficientPortalManager {

    private static final Map<String, Buff> BUFFS = new ConcurrentHashMap<>();

    public static void addBuff(String ownerName, long endTick, int level) {
        BUFFS.put(ownerName, new Buff(endTick, level));
    }

    /** 返回当前生效的额外减量（0.2/0.4/0.6），未生效返回 0 */
    public static float getProductionSpeedBonus(ServerLevel level, String ownerName) {
        Buff b = BUFFS.get(ownerName);
        if (b == null || level.getGameTime() >= b.endTick) {
            if (b != null) BUFFS.remove(ownerName);
            return 0f;
        }
        return switch (b.level) {
            case 1 -> 0.20f;
            case 2 -> 0.40f;
            case 3 -> 0.60f;
            default -> 0f;
        };
    }

    public static void clear() {
        BUFFS.clear();
    }

    private record Buff(long endTick, int level) {}
}
