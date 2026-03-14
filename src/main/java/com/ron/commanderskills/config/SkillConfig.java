package com.ron.commanderskills.config;

import com.ron.commanderskills.skill.CommanderSkill;
import com.ron.commanderskills.skill.CommanderSkills;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能解锁/升级/使用代价与冷却时间配置。配置文件为 ron_commanderskills-common.toml，按技能 ID 分节。
 */
public class SkillConfig {

    private static final Map<String, ForgeConfigSpec.IntValue> UNLOCK_FOOD = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> UNLOCK_WOOD = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> UNLOCK_ORE = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> UPGRADE_FOOD_2 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> UPGRADE_WOOD_2 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> UPGRADE_ORE_2 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> UPGRADE_FOOD_3 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> UPGRADE_WOOD_3 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> UPGRADE_ORE_3 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> USE_FOOD_1 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> USE_WOOD_1 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> USE_ORE_1 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> USE_FOOD_2 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> USE_WOOD_2 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> USE_ORE_2 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> USE_FOOD_3 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> USE_WOOD_3 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> USE_ORE_3 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> COOLDOWN_TICKS_1 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> COOLDOWN_TICKS_2 = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> COOLDOWN_TICKS_3 = new HashMap<>();

    public static final ForgeConfigSpec SPEC;
    private static final int MAX_COST = 999999;
    private static final int MAX_COOLDOWN = 72000; // 1 hour in ticks

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Skill costs (food, wood, ore) and cooldown in ticks (20 ticks = 1 second). Edit values and restart game to apply.")
            .push("skills");
        for (Map.Entry<String, CommanderSkill> e : CommanderSkills.getAll().entrySet()) {
            String id = e.getKey();
            CommanderSkill s = e.getValue();
            builder.comment("Skill: " + id).push(id);
            UNLOCK_FOOD.put(id, builder.defineInRange("unlock_food", s.getUnlockFood(), 0, MAX_COST));
            UNLOCK_WOOD.put(id, builder.defineInRange("unlock_wood", s.getUnlockWood(), 0, MAX_COST));
            UNLOCK_ORE.put(id, builder.defineInRange("unlock_ore", s.getUnlockOre(), 0, MAX_COST));
            UPGRADE_FOOD_2.put(id, builder.defineInRange("upgrade_food_2", s.getUpgradeFood(2), 0, MAX_COST));
            UPGRADE_WOOD_2.put(id, builder.defineInRange("upgrade_wood_2", s.getUpgradeWood(2), 0, MAX_COST));
            UPGRADE_ORE_2.put(id, builder.defineInRange("upgrade_ore_2", s.getUpgradeOre(2), 0, MAX_COST));
            UPGRADE_FOOD_3.put(id, builder.defineInRange("upgrade_food_3", s.getUpgradeFood(3), 0, MAX_COST));
            UPGRADE_WOOD_3.put(id, builder.defineInRange("upgrade_wood_3", s.getUpgradeWood(3), 0, MAX_COST));
            UPGRADE_ORE_3.put(id, builder.defineInRange("upgrade_ore_3", s.getUpgradeOre(3), 0, MAX_COST));
            USE_FOOD_1.put(id, builder.defineInRange("use_food_1", s.getUseFood(1), 0, MAX_COST));
            USE_WOOD_1.put(id, builder.defineInRange("use_wood_1", s.getUseWood(1), 0, MAX_COST));
            USE_ORE_1.put(id, builder.defineInRange("use_ore_1", s.getUseOre(1), 0, MAX_COST));
            USE_FOOD_2.put(id, builder.defineInRange("use_food_2", s.getUseFood(2), 0, MAX_COST));
            USE_WOOD_2.put(id, builder.defineInRange("use_wood_2", s.getUseWood(2), 0, MAX_COST));
            USE_ORE_2.put(id, builder.defineInRange("use_ore_2", s.getUseOre(2), 0, MAX_COST));
            USE_FOOD_3.put(id, builder.defineInRange("use_food_3", s.getUseFood(3), 0, MAX_COST));
            USE_WOOD_3.put(id, builder.defineInRange("use_wood_3", s.getUseWood(3), 0, MAX_COST));
            USE_ORE_3.put(id, builder.defineInRange("use_ore_3", s.getUseOre(3), 0, MAX_COST));
            COOLDOWN_TICKS_1.put(id, builder.defineInRange("cooldown_ticks_1", s.getCooldownTicks(1), 0, MAX_COOLDOWN));
            COOLDOWN_TICKS_2.put(id, builder.defineInRange("cooldown_ticks_2", s.getCooldownTicks(2), 0, MAX_COOLDOWN));
            COOLDOWN_TICKS_3.put(id, builder.defineInRange("cooldown_ticks_3", s.getCooldownTicks(3), 0, MAX_COOLDOWN));
            builder.pop();
        }
        builder.pop();
        SPEC = builder.build();
    }

    public static int getUnlockFood(CommanderSkill skill) {
        ForgeConfigSpec.IntValue v = UNLOCK_FOOD.get(skill.getId());
        return v != null ? v.get() : skill.getUnlockFood();
    }

    public static int getUnlockWood(CommanderSkill skill) {
        ForgeConfigSpec.IntValue v = UNLOCK_WOOD.get(skill.getId());
        return v != null ? v.get() : skill.getUnlockWood();
    }

    public static int getUnlockOre(CommanderSkill skill) {
        ForgeConfigSpec.IntValue v = UNLOCK_ORE.get(skill.getId());
        return v != null ? v.get() : skill.getUnlockOre();
    }

    public static int getUpgradeFood(CommanderSkill skill, int toLevel) {
        Map<String, ForgeConfigSpec.IntValue> m = toLevel == 2 ? UPGRADE_FOOD_2 : UPGRADE_FOOD_3;
        ForgeConfigSpec.IntValue v = m.get(skill.getId());
        return v != null ? v.get() : skill.getUpgradeFood(toLevel);
    }

    public static int getUpgradeWood(CommanderSkill skill, int toLevel) {
        Map<String, ForgeConfigSpec.IntValue> m = toLevel == 2 ? UPGRADE_WOOD_2 : UPGRADE_WOOD_3;
        ForgeConfigSpec.IntValue v = m.get(skill.getId());
        return v != null ? v.get() : skill.getUpgradeWood(toLevel);
    }

    public static int getUpgradeOre(CommanderSkill skill, int toLevel) {
        Map<String, ForgeConfigSpec.IntValue> m = toLevel == 2 ? UPGRADE_ORE_2 : UPGRADE_ORE_3;
        ForgeConfigSpec.IntValue v = m.get(skill.getId());
        return v != null ? v.get() : skill.getUpgradeOre(toLevel);
    }

    public static int getUseFood(CommanderSkill skill, int level) {
        Map<String, ForgeConfigSpec.IntValue> m = level == 1 ? USE_FOOD_1 : (level == 2 ? USE_FOOD_2 : USE_FOOD_3);
        ForgeConfigSpec.IntValue v = m.get(skill.getId());
        return v != null ? v.get() : skill.getUseFood(level);
    }

    public static int getUseWood(CommanderSkill skill, int level) {
        Map<String, ForgeConfigSpec.IntValue> m = level == 1 ? USE_WOOD_1 : (level == 2 ? USE_WOOD_2 : USE_WOOD_3);
        ForgeConfigSpec.IntValue v = m.get(skill.getId());
        return v != null ? v.get() : skill.getUseWood(level);
    }

    public static int getUseOre(CommanderSkill skill, int level) {
        Map<String, ForgeConfigSpec.IntValue> m = level == 1 ? USE_ORE_1 : (level == 2 ? USE_ORE_2 : USE_ORE_3);
        ForgeConfigSpec.IntValue v = m.get(skill.getId());
        return v != null ? v.get() : skill.getUseOre(level);
    }

    public static int getCooldownTicks(CommanderSkill skill, int level) {
        Map<String, ForgeConfigSpec.IntValue> m = level == 1 ? COOLDOWN_TICKS_1 : (level == 2 ? COOLDOWN_TICKS_2 : COOLDOWN_TICKS_3);
        ForgeConfigSpec.IntValue v = m.get(skill.getId());
        return v != null ? v.get() : skill.getCooldownTicks(level);
    }
}
