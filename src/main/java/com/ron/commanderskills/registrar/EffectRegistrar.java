package com.ron.commanderskills.registrar;

import com.ron.commanderskills.RoNCommanderSkillsMod;
import com.ron.commanderskills.status.BattleHornBuffEffect;
import com.ron.commanderskills.status.RestHealEffect;
import com.ron.commanderskills.status.RestExtraHealthEffect;
import com.ron.commanderskills.status.SculkWardEffect;
import com.ron.commanderskills.status.UndeadHealEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EffectRegistrar {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RoNCommanderSkillsMod.MOD_ID);

    public static final RegistryObject<MobEffect> BATTLE_HORN_BUFF =
        MOB_EFFECTS.register("battle_horn_buff", BattleHornBuffEffect::new);

    public static final RegistryObject<MobEffect> REST_HEAL =
        MOB_EFFECTS.register("rest_heal", RestHealEffect::new);

    public static final RegistryObject<MobEffect> REST_EXTRA_HEALTH =
        MOB_EFFECTS.register("rest_extra_health", RestExtraHealthEffect::new);

    public static final RegistryObject<MobEffect> UNDEAD_HEAL =
        MOB_EFFECTS.register("undead_heal", UndeadHealEffect::new);

    public static final RegistryObject<MobEffect> SCULK_WARD =
        MOB_EFFECTS.register("sculk_ward", SculkWardEffect::new);
}
