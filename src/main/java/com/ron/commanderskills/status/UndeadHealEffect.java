package com.ron.commanderskills.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 不死者技能：仅对亡灵生物生效，持续时间内每秒回复生命。
 * amplifier 0/1/2 对应 1/2/3 点生命每秒。
 */
public class UndeadHealEffect extends MobEffect {

    private static final int[] HEAL_PER_SECOND = {1, 2, 3};

    public UndeadHealEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x88_88_FF);
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
        return duration > 0 && duration % 20 == 0;
    }
}
