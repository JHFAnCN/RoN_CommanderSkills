package com.ron.commanderskills.skill;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错位传送：第一次点击选中单位 A，第二次点击选中单位 B，交换 A 与 B 的位置。
 * 目标可为己方/盟友/敌方单位（Unit），或中立单位（Unit 且 Faction.NONE），或非玩家的生物（MobCategory.CREATURE）。
 */
public class DisplacementManager {

    private static final Map<String, Integer> PENDING_FIRST_ID = new ConcurrentHashMap<>();

    /** 0=未选中到单位或无效，1=已选中第一个单位（需发 S2C 提示客户端），2=完成互换（应扣资源、进冷却） */
    public static int tryTarget(ServerLevel level, ServerPlayer player, BlockPos blockPos) {
        String ownerName = player.getName().getString();
        AABB aabb = new AABB(blockPos.getX() - 0.6, blockPos.getY() - 0.1, blockPos.getZ() - 0.6,
            blockPos.getX() + 1.6, blockPos.getY() + 2.9, blockPos.getZ() + 1.6);
        var list = level.getEntitiesOfClass(LivingEntity.class, aabb).stream()
            .filter(e -> !(e instanceof Player))
            .filter(e -> e instanceof Unit || e.getType().getCategory() == MobCategory.CREATURE)
            .filter(e -> e.blockPosition().closerThan(blockPos, 1.5))
            .toList();
        if (list.size() != 1) {
            PENDING_FIRST_ID.remove(ownerName);
            return 0;
        }
        LivingEntity second = list.get(0);
        Integer firstId = PENDING_FIRST_ID.get(ownerName);
        if (firstId == null) {
            PENDING_FIRST_ID.put(ownerName, second.getId());
            return 1;
        }
        Entity firstEntity = level.getEntity(firstId);
        PENDING_FIRST_ID.remove(ownerName);
        if (firstEntity == null || !firstEntity.isAlive() || firstEntity.equals(second)) return 0;

        Vec3 p1 = firstEntity.position();
        Vec3 p2 = second.position();
        firstEntity.teleportTo(p2.x, p2.y, p2.z);
        second.teleportTo(p1.x, p1.y, p1.z);
        return 2;
    }

    public static void clear() {
        PENDING_FIRST_ID.clear();
    }

    /** 取消该指挥官的错位传送“第一个目标”状态，用于右键取消 */
    public static void clearForPlayer(String ownerName) {
        PENDING_FIRST_ID.remove(ownerName);
    }

    /** 获取当前等待第二目标的指挥官的已选第一个单位实体 ID，用于客户端显示粒子 */
    public static Integer getFirstTargetId(String ownerName) {
        return PENDING_FIRST_ID.get(ownerName);
    }
}
