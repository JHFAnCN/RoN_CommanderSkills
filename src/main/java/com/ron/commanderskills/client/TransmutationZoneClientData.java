package com.ron.commanderskills.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/** 客户端存储亡灵嬗变效果圈：范围圈 + 法阵提示 + 可转变/不可转变实体 ID，用于绘制范围与头顶粒子柱 */
public class TransmutationZoneClientData {

    private static final List<ActiveZone> ZONES = new CopyOnWriteArrayList<>();
    private static final long MS_PER_TICK = 50L;

    public static void addZone(BlockPos center, int radius, int delayTicks,
                              int[] convertibleEntityIds, int[] nonConvertibleEntityIds) {
        long startMs = System.currentTimeMillis();
        long endMs = startMs + (long) delayTicks * MS_PER_TICK;
        Set<Integer> conv = convertibleEntityIds == null ? Set.of() :
            Arrays.stream(convertibleEntityIds).boxed().collect(Collectors.toSet());
        Set<Integer> non = nonConvertibleEntityIds == null ? Set.of() :
            Arrays.stream(nonConvertibleEntityIds).boxed().collect(Collectors.toSet());
        ZONES.add(new ActiveZone(center, radius, startMs, delayTicks, endMs, conv, non));
    }

    public static List<ActiveZone> getActiveZones() {
        long now = System.currentTimeMillis();
        ZONES.removeIf(z -> now >= z.endTimeMs);
        return new ArrayList<>(ZONES);
    }

    public static void clear() {
        ZONES.clear();
    }

    public record ActiveZone(BlockPos center, int radius, long startTimeMs, int delayTicks, long endTimeMs,
                             Set<Integer> convertibleEntityIds, Set<Integer> nonConvertibleEntityIds) {}
}
