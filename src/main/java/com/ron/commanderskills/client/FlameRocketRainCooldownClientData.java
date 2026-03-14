package com.ron.commanderskills.client;

/**
 * 客户端存储焰火箭雨全局冷却显示：有人施放时服务端广播，此处记录冷却结束时间，HUD 在屏幕底端中右绘制倒计时。
 */
public class FlameRocketRainCooldownClientData {

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

    /** 剩余秒数（向上取整），未在冷却则返回 0 */
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
