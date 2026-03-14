package com.ron.commanderskills.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 客户端存储幻翼特攻队轰炸信号圈，所有人可见，直到 endTimeMs 后消失 */
public class PhantomStrikeZoneClientData {

    private static final List<ActiveZone> ZONES = new CopyOnWriteArrayList<>();
    private static final long MS_PER_TICK = 50L;

    public static void addZone(BlockPos center, int radius, int delayTicks) {
        long endMs = System.currentTimeMillis() + (long) delayTicks * MS_PER_TICK;
        ZONES.add(new ActiveZone(center, radius, endMs));
    }

    public static List<ActiveZone> getActiveZones() {
        long now = System.currentTimeMillis();
        ZONES.removeIf(z -> now >= z.endTimeMs);
        return new ArrayList<>(ZONES);
    }

    public static void clear() {
        ZONES.clear();
    }

    public record ActiveZone(BlockPos center, int radius, long endTimeMs) {}
}
