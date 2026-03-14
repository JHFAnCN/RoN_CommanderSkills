package com.ron.commanderskills.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 超跃传送客户端：源圈（蓝）、等待目标状态、执行圈（紫粒子） */
public class HyperLeapClientData {

    private static BlockPos sourceCenter = null;
    private static int sourceRadius = 0;
    private static boolean waitingDest = false;

    /** 收到源圈包时回调（用于播放音效） */
    public static Runnable onSourceZoneReceived = null;
    /** 收到执行包时回调（用于播放传送音效） */
    public static Runnable onExecuteReceived = null;

    public static void setSourceZone(BlockPos center, int radius) {
        sourceCenter = center != null ? center.immutable() : null;
        sourceRadius = radius;
        waitingDest = center != null;
    }

    public static void clearSourceZone() {
        sourceCenter = null;
        sourceRadius = 0;
        waitingDest = false;
    }

    public static BlockPos getSourceCenter() { return sourceCenter; }
    public static int getSourceRadius() { return sourceRadius; }
    public static boolean isWaitingDest() { return waitingDest; }

    private static final List<ExecuteZone> EXECUTE_ZONES = new CopyOnWriteArrayList<>();
    private static final long DURATION_MS = 3000;

    public static void addExecuteZone(BlockPos center, int radius) {
        EXECUTE_ZONES.add(new ExecuteZone(center.immutable(), radius, System.currentTimeMillis() + DURATION_MS));
    }

    public static List<ExecuteZone> getExecuteZones() {
        long now = System.currentTimeMillis();
        EXECUTE_ZONES.removeIf(z -> now >= z.endTimeMs);
        return new ArrayList<>(EXECUTE_ZONES);
    }

    public static void clear() {
        sourceCenter = null;
        sourceRadius = 0;
        waitingDest = false;
        EXECUTE_ZONES.clear();
    }

    public record ExecuteZone(BlockPos center, int radius, long endTimeMs) {}
}
