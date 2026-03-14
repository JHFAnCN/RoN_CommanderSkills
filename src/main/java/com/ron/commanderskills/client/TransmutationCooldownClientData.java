package com.ron.commanderskills.client;

/**
 * 客户端存储亡灵嬗变全局冷却：有人施放时服务端广播，HUD 在屏幕底端中间靠右绘制倒计时。
 */
public class TransmutationCooldownClientData {

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

    public static String getCasterName() {
        return lastCasterName;
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
