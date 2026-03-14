package com.ron.commanderskills.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * 仅客户端加载：播放技能音效。由 PlayCommanderSkillSoundS2CPacket 通过反射调用，避免服务端加载 SoundInstance 等 client-only 类。
 */
public final class PlayCommanderSkillSoundS2CPacketClientHandler {

    private static SoundEvent sound(Holder<SoundEvent> holder) {
        return holder.value();
    }

    private static SoundEvent sound(SoundEvent event) {
        return event;
    }

    public static void handleClient(String skillId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Vec3 pos = mc.gameRenderer.getMainCamera().getPosition();
        if ("villager_battle_horn".equals(skillId)) {
            SoundEvent event = sound(SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0));
            mc.getSoundManager().play(new SimpleSoundInstance(
                event, SoundSource.PLAYERS, 2.0F, 1.0F,
                RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("villager_rest".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(
                sound(SoundEvents.BEACON_ACTIVATE), SoundSource.PLAYERS, 0.8F, 1.0F,
                RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("villager_cauldron_revelry".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(
                sound(SoundEvents.WITCH_AMBIENT), SoundSource.PLAYERS, 0.8F, 1.0F,
                RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("villager_flame_rocket_rain".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.BELL_BLOCK), SoundSource.PLAYERS, 1.2F, 0.9F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.GHAST_SHOOT), SoundSource.PLAYERS, 0.8F, 1.0F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.FIREWORK_ROCKET_LAUNCH), SoundSource.PLAYERS, 1.0F, 1.0F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.FIREWORK_ROCKET_BLAST), SoundSource.PLAYERS, 0.9F, 0.85F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("monster_transmutation".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.ZOMBIE_VILLAGER_CURE), SoundSource.PLAYERS, 1.0F, 0.9F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.ZOMBIE_AMBIENT), SoundSource.PLAYERS, 0.9F, 0.85F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.SCULK_CATALYST_BLOOM), SoundSource.PLAYERS, 0.7F, 0.8F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("monster_undying".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.ZOMBIE_VILLAGER_CURE), SoundSource.PLAYERS, 0.7F, 0.9F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("monster_sculk_ward".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.SCULK_SHRIEKER_SHRIEK), SoundSource.PLAYERS, 0.7F, 1.0F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("monster_phantom_strike".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.PHANTOM_AMBIENT), SoundSource.PLAYERS, 0.9F, 0.9F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("piglin_efficient_portal".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.PORTAL_TRIGGER), SoundSource.PLAYERS, 0.6F, 0.9F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("piglin_bounty".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.EXPERIENCE_ORB_PICKUP), SoundSource.PLAYERS, 1.0F, 1.0F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.NOTE_BLOCK_PLING), SoundSource.PLAYERS, 0.8F, 1.2F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("piglin_hyper_leap_source".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.PORTAL_TRIGGER), SoundSource.PLAYERS, 0.8F, 1.0F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
        } else if ("piglin_hyper_leap_execute".equals(skillId)) {
            mc.getSoundManager().play(new SimpleSoundInstance(sound(SoundEvents.PORTAL_TRAVEL), SoundSource.PLAYERS, 0.6F, 0.9F, RandomSource.create(), pos.x(), pos.y(), pos.z()));
        }
    }
}
