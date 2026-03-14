package com.ron.commanderskills.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 客户端存储焰火箭雨效果圈：范围圈 + 轰炸信号提示 + 落下倒计时，所有人可见，显示至 delayTicks + durationTicks 后消失 */
public class FlameRocketRainZoneClientData {

    private static final List<ActiveZone> ZONES = new CopyOnWriteArrayList<>();
    private static final long MS_PER_TICK = 50L;

    public static void addZone(BlockPos center, int radius, int delayTicks, int durationTicks) {
        long startMs = System.currentTimeMillis();
        long endMs = startMs + (long) (delayTicks + durationTicks) * MS_PER_TICK;
        ZONES.add(new ActiveZone(center, radius, startMs, delayTicks, durationTicks, endMs));
    }

    public static List<ActiveZone> getActiveZones() {
        long now = System.currentTimeMillis();
        ZONES.removeIf(z -> now >= z.endTimeMs);
        return new ArrayList<>(ZONES);
    }

    public static void clear() {
        ZONES.clear();
    }

    /** 若处于延迟阶段（箭尚未落下），返回剩余秒数（向上取整）；否则返回 0 */
    public static int getRemainingSecondsUntilImpact(ActiveZone zone) {
        long now = System.currentTimeMillis();
        long impactMs = zone.startTimeMs + (long) zone.delayTicks * MS_PER_TICK;
        if (now >= impactMs) return 0;
        return (int) ((impactMs - now + 999) / 1000);
    }

    public record ActiveZone(BlockPos center, int radius, long startTimeMs, int delayTicks, int durationTicks, long endTimeMs) {}
}
