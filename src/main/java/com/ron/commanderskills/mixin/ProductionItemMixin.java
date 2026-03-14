package com.ron.commanderskills.mixin;

import com.ron.commanderskills.skill.EfficientPortalManager;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 高效传送门：己方生产建筑 tick 时若玩家有该 buff，额外减少 ticksLeft（生产加速）。
 */
@Mixin(ProductionItem.class)
public class ProductionItemMixin {

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void ron_commanderskills$efficientPortalBonus(ProductionPlacement placement, ActiveProduction active, CallbackInfoReturnable<Boolean> cir) {
        Level level = placement.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (active.ticksLeft <= 0) return;
        float bonus = EfficientPortalManager.getProductionSpeedBonus(serverLevel, placement.ownerName);
        if (bonus > 0f) {
            active.ticksLeft -= bonus;
        }
    }
}
