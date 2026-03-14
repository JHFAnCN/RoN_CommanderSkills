package com.ron.commanderskills.client;

/** 超跃传送全局冷却，HUD 在屏幕底端中间靠右显示 */
public class HyperLeapCooldownClientData {

    private static String lastCasterName = "";
    private static long cooldownEndMs = 0L;

    public static void setCooldown(String casterName, int cooldownTicksRemaining) {
        if (cooldownTicksRemaining <= 0) return;
        lastCasterName = casterName != null ? casterName : "";
        cooldownEndMs = System.currentTimeMillis() + (long) cooldownTicksRemaining * 50L;
    }

    public static boolean isOnCooldown() {
        return System.currentTimeMillis() < cooldownEndMs;
    }

    public static int getRemainingSeconds() {
        long remaining = cooldownEndMs - System.currentTimeMillis();
        if (remaining <= 0) return 0;
        return (int) ((remaining + 999) / 1000);
    }

    public static void clear() {
        lastCasterName = "";
        cooldownEndMs = 0L;
    }
}
