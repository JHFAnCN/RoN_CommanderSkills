package com.ron.commanderskills.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 客户端存储当前存在的坩埚狂欢效果圈，用于绘制范围与药水下落视觉效果 */
public class CauldronZoneClientData {

    private static final List<ActiveZone> ZONES = new CopyOnWriteArrayList<>();

    /** 约 50ms 每 tick */
    private static final long MS_PER_TICK = 50L;

    public static void addZone(BlockPos center, int radius, int durationTicks) {
        long endMs = System.currentTimeMillis() + (long) durationTicks * MS_PER_TICK;
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
