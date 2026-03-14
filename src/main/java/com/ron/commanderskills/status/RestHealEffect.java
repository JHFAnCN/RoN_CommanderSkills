package com.ron.commanderskills.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 休整技能治疗效果：持续时间内每秒回复生命。
 * amplifier 0/1/2 对应 2/6/10 点生命每秒。
 */
public class RestHealEffect extends MobEffect {

    private static final int[] HEAL_PER_SECOND = {2, 6, 10};

    public RestHealEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x88FF88);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            int idx = Math.min(amplifier, 2);
            float heal = HEAL_PER_SECOND[idx];
            entity.heal(heal);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每秒触发一次（20 tick）
        return duration > 0 && duration % 20 == 0;
    }
}
