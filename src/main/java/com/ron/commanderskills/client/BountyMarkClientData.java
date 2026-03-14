package com.ron.commanderskills.client;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端存储悬赏标记的实体 ID，用于绘制金色描边 */
public class BountyMarkClientData {

    private static final long MS_PER_TICK = 50L;
    private static final Map<Integer, Long> MARKED_UNTIL_MS = new ConcurrentHashMap<>();

    public static void addMarks(Iterable<Integer> entityIds, int durationTicks) {
        long endMs = System.currentTimeMillis() + (long) durationTicks * MS_PER_TICK;
        for (int id : entityIds) {
            MARKED_UNTIL_MS.put(id, endMs);
        }
    }

    public static void removeMark(int entityId) {
        MARKED_UNTIL_MS.remove(entityId);
    }

    /** 返回当前仍应显示描边的实体 ID（未过期） */
    public static Set<Integer> getMarkedEntityIds() {
        long now = System.currentTimeMillis();
        MARKED_UNTIL_MS.entrySet().removeIf(e -> now >= e.getValue());
        return MARKED_UNTIL_MS.keySet();
    }

    public static boolean isMarked(Entity entity) {
        return MARKED_UNTIL_MS.containsKey(entity.getId());
    }

    public static void clear() {
        MARKED_UNTIL_MS.clear();
    }
}
