package com.ron.commanderskills.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 幽匿护佑：白天不燃烧、免疫白天负面效果由同时施加的防火实现；
 * 本效果仅用于标记“在圈内受护佑”，复活概率由 amplifier 在死亡事件中读取。
 * amplifier 0/1/2 -> 10%/30%/50% 亡灵复活概率。
 */
public class SculkWardEffect extends MobEffect {

    public SculkWardEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x22_CC_AA);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
