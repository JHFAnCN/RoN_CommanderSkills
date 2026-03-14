package com.ron.commanderskills;

import com.ron.commanderskills.building.CommanderSkillsBuildings;
import com.ron.commanderskills.client.CommanderSkillsClientEvents;
import com.ron.commanderskills.config.SkillConfig;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.registrar.EffectRegistrar;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RoNCommanderSkillsMod.MOD_ID)
public class RoNCommanderSkillsMod {

    public static final String MOD_ID = "ron_commanderskills";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @SuppressWarnings("removal")
    public RoNCommanderSkillsMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EffectRegistrar.MOB_EFFECTS.register(modBus);
        modBus.addListener(this::onCommonSetup);

        // Register COMMON config (skill costs and cooldowns)
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SkillConfig.SPEC, "ron_commanderskills-common.toml");
        // Register client-only listeners
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> CommanderSkillsClientEvents::registerClientEvents);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(RoNCommanderSkillsNetwork::init);
        // 注册附属建筑到主模组，并加入村民建造列表（无快捷键则传 null）
        event.enqueueWork(CommanderSkillsBuildings::registerBuildings);
    }
}

