package com.ron.commanderskills.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 休整技能额外血量：持续 2 分钟内增加 5/10/15 点最大生命。amplifier 0/1/2 对应技能 1/2/3 级。
 */
public class RestExtraHealthEffect extends MobEffect {

    private static final UUID MAX_HEALTH_UUID = UUID.fromString("a1b2c3d4-5e6f-7890-abcd-ef1234567890");
    private static final double[] EXTRA_HEALTH = {5, 10, 15};

    public RestExtraHealthEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x88FF88);
        this.addAttributeModifier(Attributes.MAX_HEALTH, MAX_HEALTH_UUID.toString(), 0, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        int idx = Math.min(amplifier, 2);
        if (modifier.getId().equals(MAX_HEALTH_UUID)) return EXTRA_HEALTH[idx];
        return 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {}

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
