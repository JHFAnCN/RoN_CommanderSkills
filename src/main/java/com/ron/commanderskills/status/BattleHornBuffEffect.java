package com.ron.commanderskills.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 战斗号角增益：按等级 20%/30%/40% 伤害，10%/20%/40% 移速（无攻速）。
 * amplifier 0/1/2 对应技能 1/2/3 级。
 */
public class BattleHornBuffEffect extends MobEffect {

    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("e91f1e74-2c8c-4e19-8a17-7b7f3a1c3b01");
    private static final UUID DAMAGE_UUID = UUID.fromString("4cf0ef36-9cb9-4f7e-90a2-0e2f45a3a9e2");
    private static final UUID MOVE_SPEED_UUID = UUID.fromString("bb8c79a0-29b3-4bd1-bf48-3e3b8bb4b1aa");

    private static final double[] ATTACK_SPEED = {0, 0, 0};
    private static final double[] DAMAGE = {0.20, 0.30, 0.40};
    private static final double[] MOVE_SPEED = {0.10, 0.20, 0.40};

    public BattleHornBuffEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFAA33);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID.toString(), 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, DAMAGE_UUID.toString(), 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVE_SPEED_UUID.toString(), 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        int idx = Math.min(amplifier, 2);
        UUID id = modifier.getId();
        if (id.equals(ATTACK_SPEED_UUID)) return ATTACK_SPEED[idx];
        if (id.equals(DAMAGE_UUID)) return DAMAGE[idx];
        if (id.equals(MOVE_SPEED_UUID)) return MOVE_SPEED[idx];
        return 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {}

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}

