package com.ron.commanderskills.skill;

import com.ron.commanderskills.skill.monsters.MonsterPhantomStrikeSkill;
import com.ron.commanderskills.skill.monsters.MonsterSculkWardSkill;
import com.ron.commanderskills.skill.monsters.MonsterTransmutationSkill;
import com.ron.commanderskills.skill.monsters.MonsterUndyingSkill;
import com.ron.commanderskills.skill.piglins.PiglinBountySkill;
import com.ron.commanderskills.skill.piglins.PiglinDisplacementSkill;
import com.ron.commanderskills.skill.piglins.PiglinHyperLeapSkill;
import com.ron.commanderskills.skill.piglins.PiglinEfficientPortalSkill;
import com.ron.commanderskills.skill.villagers.VillagerBattleHornSkill;
import com.ron.commanderskills.skill.villagers.VillagerCauldronRevelrySkill;
import com.ron.commanderskills.skill.villagers.VillagerFlameRocketRainSkill;
import com.ron.commanderskills.skill.villagers.VillagerRestSkill;

import java.util.HashMap;
import java.util.Map;

public class CommanderSkills {

    private static final Map<String, CommanderSkill> REGISTRY = new HashMap<>();

    static {
        register(new VillagerBattleHornSkill());
        register(new VillagerRestSkill());
        register(new VillagerCauldronRevelrySkill());
        register(new VillagerFlameRocketRainSkill());
        register(new MonsterUndyingSkill());
        register(new MonsterSculkWardSkill());
        register(new MonsterPhantomStrikeSkill());
        register(new MonsterTransmutationSkill());
        register(new PiglinEfficientPortalSkill());
        register(new PiglinBountySkill());
        register(new PiglinDisplacementSkill());
        register(new PiglinHyperLeapSkill());
    }

    private static void register(CommanderSkill skill) {
        REGISTRY.put(skill.getId(), skill);
    }

    public static CommanderSkill get(String id) {
        return REGISTRY.get(id);
    }

    public static Map<String, CommanderSkill> getAll() {
        return REGISTRY;
    }
}

