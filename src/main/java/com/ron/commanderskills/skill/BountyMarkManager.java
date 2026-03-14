package com.ron.commanderskills.skill;

import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 悬赏：每击杀一个被标记单位，施放者获得一次该技能的资源代价返还。
 */
public class BountyMarkManager {

    private static final Map<Integer, Mark> MARKS = new ConcurrentHashMap<>();

    /** @param skillLevel 技能等级 1/2/3，用于结算时取技能使用代价 */
    public static void mark(LivingEntity entity, String casterOwnerName, long endTick, int skillLevel) {
        MARKS.put(entity.getId(), new Mark(casterOwnerName, endTick, skillLevel));
    }

    /** 死亡时调用：若该实体曾被悬赏则返回 Mark 并移除，用于发放奖励 */
    public static Mark getAndRemoveOnDeath(int entityId) {
        return MARKS.remove(entityId);
    }

    public static void onEntityRemoved(int entityId) {
        MARKS.remove(entityId);
    }

    /** 返还资源并同步到客户端（与扣资源使用同一套 addSubtractResources，客户端会收到更新） */
    public static void grantBountyReward(String casterOwnerName, int food, int wood, int ore) {
        ResourcesServerEvents.addSubtractResources(new Resources(casterOwnerName, food, wood, ore));
    }

    public static void clear() {
        MARKS.clear();
    }

    /** @param skillLevel 技能等级 1/2/3，奖励 = 悬赏技能该等级使用代价（1倍返还） */
    public record Mark(String casterOwnerName, long endTick, int skillLevel) {}

    private static final Map<EntityType<?>, ResourceCost> UNIT_COST_MAP = new HashMap<>();
    static {
        try {
            put(EntityRegistrar.CREEPER_UNIT.get(), ResourceCosts.CREEPER);
            put(EntityRegistrar.ZOMBIE_UNIT.get(), ResourceCosts.ZOMBIE);
            put(EntityRegistrar.SKELETON_UNIT.get(), ResourceCosts.SKELETON);
            put(EntityRegistrar.STRAY_UNIT.get(), ResourceCosts.STRAY);
            put(EntityRegistrar.HUSK_UNIT.get(), ResourceCosts.HUSK);
            put(EntityRegistrar.DROWNED_UNIT.get(), ResourceCosts.DROWNED);
            put(EntityRegistrar.SPIDER_UNIT.get(), ResourceCosts.SPIDER);
            put(EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get(), ResourceCosts.ZOMBIE_VILLAGER);
            put(EntityRegistrar.VILLAGER_UNIT.get(), ResourceCosts.VILLAGER);
            put(EntityRegistrar.PILLAGER_UNIT.get(), ResourceCosts.PILLAGER);
            put(EntityRegistrar.IRON_GOLEM_UNIT.get(), ResourceCosts.IRON_GOLEM);
            put(EntityRegistrar.WITCH_UNIT.get(), ResourceCosts.WITCH);
            put(EntityRegistrar.EVOKER_UNIT.get(), ResourceCosts.EVOKER);
            put(EntityRegistrar.RAVAGER_UNIT.get(), ResourceCosts.RAVAGER);
            put(EntityRegistrar.SLIME_UNIT.get(), ResourceCosts.SLIME);
            put(EntityRegistrar.WARDEN_UNIT.get(), ResourceCosts.WARDEN);
            put(EntityRegistrar.GRUNT_UNIT.get(), ResourceCosts.GRUNT);
            put(EntityRegistrar.BRUTE_UNIT.get(), ResourceCosts.BRUTE);
            put(EntityRegistrar.HEADHUNTER_UNIT.get(), ResourceCosts.HEADHUNTER);
            put(EntityRegistrar.HOGLIN_UNIT.get(), ResourceCosts.HOGLIN);
            put(EntityRegistrar.BLAZE_UNIT.get(), ResourceCosts.BLAZE);
            put(EntityRegistrar.WITHER_SKELETON_UNIT.get(), ResourceCosts.WITHER_SKELETON);
            put(EntityRegistrar.MAGMA_CUBE_UNIT.get(), ResourceCosts.MAGMA_CUBE);
            put(EntityRegistrar.GHAST_UNIT.get(), ResourceCosts.GHAST);
            put(EntityRegistrar.NECROMANCER_UNIT.get(), ResourceCosts.NECROMANCER);
            put(EntityRegistrar.ZOMBIE_PIGLIN_UNIT.get(), ResourceCosts.ZOMBIE_PIGLIN);
            put(EntityRegistrar.ZOGLIN_UNIT.get(), ResourceCosts.ZOGLIN);
            put(EntityRegistrar.ENDERMAN_UNIT.get(), ResourceCosts.ENDERMAN);
            put(EntityRegistrar.VINDICATOR_UNIT.get(), ResourceCosts.VINDICATOR);
            put(EntityRegistrar.ROYAL_GUARD_UNIT.get(), ResourceCosts.ROYAL_GUARD);
            put(EntityRegistrar.ENCHANTER_UNIT.get(), ResourceCosts.ENCHANTER);
            put(EntityRegistrar.WRETCHED_WRAITH_UNIT.get(), ResourceCosts.WRETCHED_WRAITH);
            put(EntityRegistrar.PIGLIN_MERCHANT_UNIT.get(), ResourceCosts.PIGLIN_MERCHANT);
            put(EntityRegistrar.WILDFIRE_UNIT.get(), ResourceCosts.WILDFIRE);
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    private static void put(EntityType<?> type, ResourceCost cost) {
        UNIT_COST_MAP.put(type, cost);
    }
}
