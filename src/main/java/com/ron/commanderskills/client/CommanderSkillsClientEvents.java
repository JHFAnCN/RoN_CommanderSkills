package com.ron.commanderskills.client;

import com.ron.commanderskills.network.CancelDisplacementC2SPacket;
import com.ron.commanderskills.network.DisplacementClientBridge;
import com.ron.commanderskills.network.RoNCommanderSkillsNetwork;
import com.ron.commanderskills.network.UnlockCommanderSkillC2SPacket;
import com.ron.commanderskills.network.UseCommanderSkillC2SPacket;
import com.ron.commanderskills.config.SkillConfig;
import com.ron.commanderskills.registrar.EffectRegistrar;
import com.ron.commanderskills.skill.CommanderSkill;
import com.ron.commanderskills.skill.CommanderSkillState;
import com.ron.commanderskills.skill.CommanderSkills;
import com.ron.commanderskills.skill.monsters.MonsterPhantomStrikeSkill;
import com.ron.commanderskills.skill.monsters.MonsterSculkWardSkill;
import com.ron.commanderskills.skill.monsters.MonsterTransmutationSkill;
import com.ron.commanderskills.skill.monsters.MonsterUndyingSkill;
import com.ron.commanderskills.skill.piglins.PiglinBountySkill;
import com.ron.commanderskills.skill.piglins.PiglinDisplacementSkill;
import com.ron.commanderskills.skill.piglins.PiglinEfficientPortalSkill;
import com.ron.commanderskills.skill.piglins.PiglinHyperLeapSkill;
import com.ron.commanderskills.skill.villagers.VillagerBattleHornSkill;
import com.ron.commanderskills.skill.villagers.VillagerCauldronRevelrySkill;
import com.ron.commanderskills.skill.villagers.VillagerFlameRocketRainSkill;
import com.ron.commanderskills.skill.villagers.VillagerRestSkill;
import com.ron.commanderskills.client.TransmutationZoneClientData;
import com.ron.commanderskills.client.TransmutationCooldownClientData;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CommanderSkillsClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static Button skillButton;
    private static Button unlockButton;
    private static Button restSkillButton;
    private static Button restUnlockButton;
    private static Button cauldronSkillButton;
    private static Button cauldronUnlockButton;
    private static Button undyingSkillButton;
    private static Button undyingUnlockButton;
    private static Button sculkSkillButton;
    private static Button sculkUnlockButton;
    private static Button phantomSkillButton;
    private static Button phantomUnlockButton;
    private static Button efficientPortalSkillButton;
    private static Button efficientPortalUnlockButton;
    private static Button bountySkillButton;
    private static Button bountyUnlockButton;
    private static Button displacementSkillButton;
    private static Button displacementUnlockButton;
    private static Button hyperLeapSkillButton;
    private static Button hyperLeapUnlockButton;
    private static Button flameRocketRainSkillButton;
    private static Button flameRocketRainUnlockButton;
    private static Button transmutationSkillButton;
    private static Button transmutationUnlockButton;
    /** 当前选中的技能 ID，用于施放；null 表示未选中 */
    private static String selectedSkillId = null;
    /** 错位传送：已选中第一个单位，等待点击第二个单位时为 true，显示“单位互换 1/2” */
    private static boolean displacementWaitingSecond = false;
    /** 错位传送：第一个目标实体 ID，用于紫色粒子包裹；-1 表示无效 */
    private static int displacementFirstTargetEntityId = -1;

    public static void registerClientEvents() {
        // no-op, annotation-based registration
    }

    static {
        DisplacementClientBridge.onFirstTargetReceived = CommanderSkillsClientEvents::onDisplacementFirstTargetReceived;
    }

    /** 供 mixin 在游戏重置（/rts-reset）时调用，取消技能选中状态 */
    public static void clearSkillSelected() {
        selectedSkillId = null;
        displacementWaitingSecond = false;
        displacementFirstTargetEntityId = -1;
        HyperLeapClientData.clearSourceZone();
    }

    /** 技能面板使用的阵营：沙盒模式下随沙盒阵营切换（NONE 时按村民显示）；非沙盒为当前 RTS 阵营 */
    private static Faction getSkillPanelFaction() {
        if (SandboxClientEvents.isSandboxPlayer()) {
            Faction f = SandboxClientEvents.getFaction();
            return f == Faction.NONE ? Faction.VILLAGERS : f;
        }
        return PlayerClientEvents.getFaction();
    }

    /** 错位传送：收到服务端“已选中第一个单位”时由 DisplacementFirstTargetS2CPacket 调用，entityId 为第一个目标实体 ID */
    public static void onDisplacementFirstTargetReceived(Integer entityId) {
        displacementWaitingSecond = true;
        displacementFirstTargetEntityId = entityId != null ? entityId : -1;
    }

    /** 加入游戏时先清空本地状态，等服务器同步后再显示，避免多人/服务器下显示错误等级或 UI 错乱 */
    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn evt) {
        CommanderSkillsClientState.clear();
        selectedSkillId = null;
        displacementWaitingSecond = false;
        displacementFirstTargetEntityId = -1;
        CauldronZoneClientData.clear();
        PhantomStrikeZoneClientData.clear();
        FlameRocketRainZoneClientData.clear();
        FlameRocketRainCooldownClientData.clear();
        TransmutationZoneClientData.clear();
        TransmutationCooldownClientData.clear();
        HyperLeapClientData.clear();
        HyperLeapCooldownClientData.clear();
        BountyMarkClientData.clear();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        CommanderSkillsClientState.clear();
        selectedSkillId = null;
        displacementWaitingSecond = false;
        displacementFirstTargetEntityId = -1;
        CauldronZoneClientData.clear();
        PhantomStrikeZoneClientData.clear();
        FlameRocketRainZoneClientData.clear();
        FlameRocketRainCooldownClientData.clear();
        TransmutationZoneClientData.clear();
        TransmutationCooldownClientData.clear();
        HyperLeapClientData.clear();
        HyperLeapCooldownClientData.clear();
        BountyMarkClientData.clear();
    }

    // 技能面板：左列升级、右列技能，纵向排列；外框罩住全部
    private static final int PANEL_PADDING = 4;
    private static final int COL_GAP = 6;
    private static final int ROW_GAP = 4;
    private static final int PANEL_BG_COLOR = 0xE0_000000; // 半透明黑

    private static final int SKILL_ROWS = 4;

    /** 计算面板与按钮位置：左列=升级，右列=技能，从上到下依次为技能1～4（村民含焰火箭雨） */
    private static void updateButtonPositionsForInput() {
        int screenHeight = MC.getWindow().getGuiScaledHeight();
        int iconFrameSize = Button.DEFAULT_ICON_FRAME_SIZE;
        int panelW = PANEL_PADDING + iconFrameSize + COL_GAP + iconFrameSize + PANEL_PADDING;
        int panelH = PANEL_PADDING + SKILL_ROWS * iconFrameSize + (SKILL_ROWS - 1) * ROW_GAP + PANEL_PADDING;
        int panelX = 4;
        int panelY = screenHeight / 2 - panelH / 2;

        int leftX = panelX + PANEL_PADDING;
        int rightX = panelX + PANEL_PADDING + iconFrameSize + COL_GAP;
        int row0Y = panelY + PANEL_PADDING;
        int row1Y = panelY + PANEL_PADDING + iconFrameSize + ROW_GAP;
        int row2Y = panelY + PANEL_PADDING + 2 * (iconFrameSize + ROW_GAP);
        int row3Y = panelY + PANEL_PADDING + 3 * (iconFrameSize + ROW_GAP);

        if (unlockButton != null) { unlockButton.x = leftX; unlockButton.y = row0Y; }
        if (skillButton != null) { skillButton.x = rightX; skillButton.y = row0Y; }
        if (restUnlockButton != null) { restUnlockButton.x = leftX; restUnlockButton.y = row1Y; }
        if (restSkillButton != null) { restSkillButton.x = rightX; restSkillButton.y = row1Y; }
        if (cauldronUnlockButton != null) { cauldronUnlockButton.x = leftX; cauldronUnlockButton.y = row2Y; }
        if (cauldronSkillButton != null) { cauldronSkillButton.x = rightX; cauldronSkillButton.y = row2Y; }
        if (flameRocketRainUnlockButton != null) { flameRocketRainUnlockButton.x = leftX; flameRocketRainUnlockButton.y = row3Y; }
        if (flameRocketRainSkillButton != null) { flameRocketRainSkillButton.x = rightX; flameRocketRainSkillButton.y = row3Y; }
        if (undyingUnlockButton != null) { undyingUnlockButton.x = leftX; undyingUnlockButton.y = row0Y; }
        if (undyingSkillButton != null) { undyingSkillButton.x = rightX; undyingSkillButton.y = row0Y; }
        if (sculkUnlockButton != null) { sculkUnlockButton.x = leftX; sculkUnlockButton.y = row1Y; }
        if (sculkSkillButton != null) { sculkSkillButton.x = rightX; sculkSkillButton.y = row1Y; }
        if (phantomUnlockButton != null) { phantomUnlockButton.x = leftX; phantomUnlockButton.y = row2Y; }
        if (phantomSkillButton != null) { phantomSkillButton.x = rightX; phantomSkillButton.y = row2Y; }
        if (transmutationUnlockButton != null) { transmutationUnlockButton.x = leftX; transmutationUnlockButton.y = row3Y; }
        if (transmutationSkillButton != null) { transmutationSkillButton.x = rightX; transmutationSkillButton.y = row3Y; }
        if (efficientPortalUnlockButton != null) { efficientPortalUnlockButton.x = leftX; efficientPortalUnlockButton.y = row0Y; }
        if (efficientPortalSkillButton != null) { efficientPortalSkillButton.x = rightX; efficientPortalSkillButton.y = row0Y; }
        if (bountyUnlockButton != null) { bountyUnlockButton.x = leftX; bountyUnlockButton.y = row1Y; }
        if (bountySkillButton != null) { bountySkillButton.x = rightX; bountySkillButton.y = row1Y; }
        if (displacementUnlockButton != null) { displacementUnlockButton.x = leftX; displacementUnlockButton.y = row2Y; }
        if (displacementSkillButton != null) { displacementSkillButton.x = rightX; displacementSkillButton.y = row2Y; }
        if (hyperLeapUnlockButton != null) { hyperLeapUnlockButton.x = leftX; hyperLeapUnlockButton.y = row3Y; }
        if (hyperLeapSkillButton != null) { hyperLeapSkillButton.x = rightX; hyperLeapSkillButton.y = row3Y; }
    }

    /** HIGHEST 优先：在其他 HUD 消费点击前先判定是否点在我们的技能/升级按钮上 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre evt) {
        if (!OrthoviewClientEvents.isEnabled() || !(evt.getScreen() instanceof TopdownGui)) return;
        if (MC.player == null) return;
        if (!PlayerClientEvents.isRTSPlayer()) return;
        Faction faction = getSkillPanelFaction();
        if (faction != Faction.VILLAGERS && faction != Faction.MONSTERS && faction != Faction.PIGLINS) return;

        ensureButtons();
        updateButtonPositionsForInput();

        int mouseX = (int) evt.getMouseX();
        int mouseY = (int) evt.getMouseY();

        if (evt.getButton() == 0) { // 左键
            if (faction == Faction.VILLAGERS) {
            CommanderSkillState stateBH = CommanderSkillsClientState.get(VillagerBattleHornSkill.ID);
            int levelBH = stateBH == null ? 0 : stateBH.level();
            CommanderSkill skillBH = CommanderSkills.get(VillagerBattleHornSkill.ID);
            boolean upgradeBH = levelBH < (skillBH != null ? skillBH.getMaxLevel() : 3)
                && clientHasBuildingForNextLevel(skillBH, levelBH)
                && clientCanAffordUpgrade(skillBH, levelBH);
            CommanderSkillState stateRest = CommanderSkillsClientState.get(VillagerRestSkill.ID);
            int levelRest = stateRest == null ? 0 : stateRest.level();
            CommanderSkill skillRest = CommanderSkills.get(VillagerRestSkill.ID);
            boolean upgradeRest = levelRest < (skillRest != null ? skillRest.getMaxLevel() : 3)
                && clientHasBuildingForNextLevel(skillRest, levelRest)
                && clientCanAffordUpgrade(skillRest, levelRest);

            if (!skillButton.isHidden.get() && skillButton.isMouseOver(mouseX, mouseY)) {
                if (levelBH > 0) {
                    selectedSkillId = VillagerBattleHornSkill.ID.equals(selectedSkillId) ? null : VillagerBattleHornSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!unlockButton.isHidden.get() && unlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeBH || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(
                        new UnlockCommanderSkillC2SPacket(VillagerBattleHornSkill.ID)
                    );
                }
                return;
            }
            if (!restSkillButton.isHidden.get() && restSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelRest > 0) {
                    selectedSkillId = VillagerRestSkill.ID.equals(selectedSkillId) ? null : VillagerRestSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!restUnlockButton.isHidden.get() && restUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeRest || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(
                        new UnlockCommanderSkillC2SPacket(VillagerRestSkill.ID)
                    );
                }
                return;
            }
            CommanderSkillState stateCauldron = CommanderSkillsClientState.get(VillagerCauldronRevelrySkill.ID);
            int levelCauldron = stateCauldron == null ? 0 : stateCauldron.level();
            CommanderSkill skillCauldron = CommanderSkills.get(VillagerCauldronRevelrySkill.ID);
            boolean upgradeCauldron = levelCauldron < (skillCauldron != null ? skillCauldron.getMaxLevel() : 3)
                && clientHasBuildingForNextLevel(skillCauldron, levelCauldron)
                && clientCanAffordUpgrade(skillCauldron, levelCauldron);
            if (!cauldronSkillButton.isHidden.get() && cauldronSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelCauldron > 0) {
                    selectedSkillId = VillagerCauldronRevelrySkill.ID.equals(selectedSkillId) ? null : VillagerCauldronRevelrySkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!cauldronUnlockButton.isHidden.get() && cauldronUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeCauldron || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(
                        new UnlockCommanderSkillC2SPacket(VillagerCauldronRevelrySkill.ID)
                    );
                }
                return;
            }
            CommanderSkillState stateFlameRocketRain = CommanderSkillsClientState.get(VillagerFlameRocketRainSkill.ID);
            int levelFlameRocketRain = stateFlameRocketRain == null ? 0 : stateFlameRocketRain.level();
            CommanderSkill skillFlameRocketRain = CommanderSkills.get(VillagerFlameRocketRainSkill.ID);
            boolean upgradeFlameRocketRain = levelFlameRocketRain < (skillFlameRocketRain != null ? skillFlameRocketRain.getMaxLevel() : 1)
                && clientHasBuildingForNextLevel(skillFlameRocketRain, levelFlameRocketRain)
                && clientCanAffordUpgrade(skillFlameRocketRain, levelFlameRocketRain);
            if (!flameRocketRainSkillButton.isHidden.get() && flameRocketRainSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelFlameRocketRain > 0) {
                    selectedSkillId = VillagerFlameRocketRainSkill.ID.equals(selectedSkillId) ? null : VillagerFlameRocketRainSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!flameRocketRainUnlockButton.isHidden.get() && flameRocketRainUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeFlameRocketRain || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(
                        new UnlockCommanderSkillC2SPacket(VillagerFlameRocketRainSkill.ID)
                    );
                }
                return;
            }
            }
        }
        if (faction == Faction.MONSTERS) {
            CommanderSkillState stateUndying = CommanderSkillsClientState.get(MonsterUndyingSkill.ID);
            int levelUndying = stateUndying == null ? 0 : stateUndying.level();
            CommanderSkill skillUndying = CommanderSkills.get(MonsterUndyingSkill.ID);
            boolean upgradeUndying = levelUndying < (skillUndying != null ? skillUndying.getMaxLevel() : 3)
                && clientHasBuildingForNextLevel(skillUndying, levelUndying)
                && clientCanAffordUpgrade(skillUndying, levelUndying);
            if (!undyingSkillButton.isHidden.get() && undyingSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelUndying > 0) {
                    selectedSkillId = MonsterUndyingSkill.ID.equals(selectedSkillId) ? null : MonsterUndyingSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!undyingUnlockButton.isHidden.get() && undyingUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeUndying || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UnlockCommanderSkillC2SPacket(MonsterUndyingSkill.ID));
                }
                return;
            }
            CommanderSkillState stateSculk = CommanderSkillsClientState.get(MonsterSculkWardSkill.ID);
            int levelSculk = stateSculk == null ? 0 : stateSculk.level();
            CommanderSkill skillSculk = CommanderSkills.get(MonsterSculkWardSkill.ID);
            boolean upgradeSculk = levelSculk < (skillSculk != null ? skillSculk.getMaxLevel() : 3)
                && clientHasBuildingForNextLevel(skillSculk, levelSculk)
                && clientCanAffordUpgrade(skillSculk, levelSculk);
            if (!sculkSkillButton.isHidden.get() && sculkSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelSculk > 0) {
                    selectedSkillId = MonsterSculkWardSkill.ID.equals(selectedSkillId) ? null : MonsterSculkWardSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!sculkUnlockButton.isHidden.get() && sculkUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeSculk || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UnlockCommanderSkillC2SPacket(MonsterSculkWardSkill.ID));
                }
                return;
            }
            CommanderSkillState statePhantom = CommanderSkillsClientState.get(MonsterPhantomStrikeSkill.ID);
            int levelPhantom = statePhantom == null ? 0 : statePhantom.level();
            CommanderSkill skillPhantom = CommanderSkills.get(MonsterPhantomStrikeSkill.ID);
            boolean upgradePhantom = levelPhantom < (skillPhantom != null ? skillPhantom.getMaxLevel() : 3)
                && clientHasBuildingForNextLevel(skillPhantom, levelPhantom)
                && clientCanAffordUpgrade(skillPhantom, levelPhantom);
            if (!phantomSkillButton.isHidden.get() && phantomSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelPhantom > 0) {
                    selectedSkillId = MonsterPhantomStrikeSkill.ID.equals(selectedSkillId) ? null : MonsterPhantomStrikeSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!phantomUnlockButton.isHidden.get() && phantomUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradePhantom || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UnlockCommanderSkillC2SPacket(MonsterPhantomStrikeSkill.ID));
                }
                return;
            }
            CommanderSkillState stateTransmutation = CommanderSkillsClientState.get(MonsterTransmutationSkill.ID);
            int levelTransmutation = stateTransmutation == null ? 0 : stateTransmutation.level();
            CommanderSkill skillTransmutation = CommanderSkills.get(MonsterTransmutationSkill.ID);
            boolean upgradeTransmutation = levelTransmutation < (skillTransmutation != null ? skillTransmutation.getMaxLevel() : 1)
                && clientHasBuildingForNextLevel(skillTransmutation, levelTransmutation)
                && clientCanAffordUpgrade(skillTransmutation, levelTransmutation);
            if (!transmutationSkillButton.isHidden.get() && transmutationSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelTransmutation > 0) {
                    selectedSkillId = MonsterTransmutationSkill.ID.equals(selectedSkillId) ? null : MonsterTransmutationSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!transmutationUnlockButton.isHidden.get() && transmutationUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeTransmutation || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UnlockCommanderSkillC2SPacket(MonsterTransmutationSkill.ID));
                }
                return;
            }
        }
        if (faction == Faction.PIGLINS) {
            CommanderSkillState stateEP = CommanderSkillsClientState.get(PiglinEfficientPortalSkill.ID);
            int levelEP = stateEP == null ? 0 : stateEP.level();
            CommanderSkill skillEP = CommanderSkills.get(PiglinEfficientPortalSkill.ID);
            boolean upgradeEP = levelEP < (skillEP != null ? skillEP.getMaxLevel() : 3)
                && clientHasBuildingForNextLevel(skillEP, levelEP)
                && clientCanAffordUpgrade(skillEP, levelEP);
            if (!efficientPortalSkillButton.isHidden.get() && efficientPortalSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelEP > 0) {
                    selectedSkillId = PiglinEfficientPortalSkill.ID.equals(selectedSkillId) ? null : PiglinEfficientPortalSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!efficientPortalUnlockButton.isHidden.get() && efficientPortalUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeEP || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UnlockCommanderSkillC2SPacket(PiglinEfficientPortalSkill.ID));
                }
                return;
            }
            CommanderSkillState stateBounty = CommanderSkillsClientState.get(PiglinBountySkill.ID);
            int levelBounty = stateBounty == null ? 0 : stateBounty.level();
            CommanderSkill skillBounty = CommanderSkills.get(PiglinBountySkill.ID);
            boolean upgradeBounty = levelBounty < (skillBounty != null ? skillBounty.getMaxLevel() : 3)
                && clientHasBuildingForNextLevel(skillBounty, levelBounty)
                && clientCanAffordUpgrade(skillBounty, levelBounty);
            if (!bountySkillButton.isHidden.get() && bountySkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelBounty > 0) {
                    selectedSkillId = PiglinBountySkill.ID.equals(selectedSkillId) ? null : PiglinBountySkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!bountyUnlockButton.isHidden.get() && bountyUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeBounty || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UnlockCommanderSkillC2SPacket(PiglinBountySkill.ID));
                }
                return;
            }
            CommanderSkillState stateDisp = CommanderSkillsClientState.get(PiglinDisplacementSkill.ID);
            int levelDisp = stateDisp == null ? 0 : stateDisp.level();
            CommanderSkill skillDisp = CommanderSkills.get(PiglinDisplacementSkill.ID);
            boolean upgradeDisp = levelDisp < (skillDisp != null ? skillDisp.getMaxLevel() : 3)
                && clientHasBuildingForNextLevel(skillDisp, levelDisp)
                && clientCanAffordUpgrade(skillDisp, levelDisp);
            if (!displacementSkillButton.isHidden.get() && displacementSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelDisp > 0) {
                    selectedSkillId = PiglinDisplacementSkill.ID.equals(selectedSkillId) ? null : PiglinDisplacementSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!displacementUnlockButton.isHidden.get() && displacementUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeDisp || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UnlockCommanderSkillC2SPacket(PiglinDisplacementSkill.ID));
                }
                return;
            }
            CommanderSkillState stateHyperLeap = CommanderSkillsClientState.get(PiglinHyperLeapSkill.ID);
            int levelHyperLeap = stateHyperLeap == null ? 0 : stateHyperLeap.level();
            CommanderSkill skillHyperLeap = CommanderSkills.get(PiglinHyperLeapSkill.ID);
            boolean upgradeHyperLeap = levelHyperLeap < (skillHyperLeap != null ? skillHyperLeap.getMaxLevel() : 1)
                && clientHasBuildingForNextLevel(skillHyperLeap, levelHyperLeap)
                && clientCanAffordUpgrade(skillHyperLeap, levelHyperLeap);
            if (!hyperLeapSkillButton.isHidden.get() && hyperLeapSkillButton.isMouseOver(mouseX, mouseY)) {
                if (levelHyperLeap > 0) {
                    selectedSkillId = PiglinHyperLeapSkill.ID.equals(selectedSkillId) ? null : PiglinHyperLeapSkill.ID;
                    evt.setCanceled(true);
                }
                return;
            }
            if (!hyperLeapUnlockButton.isHidden.get() && hyperLeapUnlockButton.isMouseOver(mouseX, mouseY)) {
                if (upgradeHyperLeap || SandboxClientEvents.isSandboxPlayer()) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UnlockCommanderSkillC2SPacket(PiglinHyperLeapSkill.ID));
                }
                return;
            }
        }
        if (evt.getButton() == 1) {
            if (selectedSkillId != null) {
                if (PiglinDisplacementSkill.ID.equals(selectedSkillId)) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new CancelDisplacementC2SPacket());
                    displacementWaitingSecond = false;
                    displacementFirstTargetEntityId = -1;
                } else if (PiglinHyperLeapSkill.ID.equals(selectedSkillId)) {
                    RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new com.ron.commanderskills.network.CancelHyperLeapC2SPacket());
                    HyperLeapClientData.clearSourceZone();
                }
                selectedSkillId = null;
                evt.setCanceled(true);
            }
        }
    }

    /** 左键在地图上施放在 Post 中处理，避免 Pre 时光标位置未更新 */
    @SubscribeEvent
    public static void onScreenMouseClickPost(ScreenEvent.MouseButtonPressed.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || !(evt.getScreen() instanceof TopdownGui)) return;
        if (MC.player == null) return;
        if (!PlayerClientEvents.isRTSPlayer()) return;
        Faction faction = getSkillPanelFaction();
        if (faction != Faction.VILLAGERS && faction != Faction.MONSTERS && faction != Faction.PIGLINS) return;
        if (evt.getButton() != 0) return;

        if (selectedSkillId == null) return;
        CommanderSkill skill = CommanderSkills.get(selectedSkillId);
        CommanderSkillState state = CommanderSkillsClientState.get(selectedSkillId);
        int level = state == null ? 0 : state.level();
        if (skill == null || level <= 0) return;

        int mouseX = (int) evt.getMouseX();
        int mouseY = (int) evt.getMouseY();
        updateButtonPositionsForInput();
        if (isOverOurButtons(mouseX, mouseY)) return;

        BlockPos bp = CursorClientEvents.getPreselectedBlockPos();
        boolean canSendNoTarget = selectedSkillId != null && (MonsterUndyingSkill.ID.equals(selectedSkillId) || PiglinEfficientPortalSkill.ID.equals(selectedSkillId) || PiglinBountySkill.ID.equals(selectedSkillId));
        boolean canSendWithTarget = bp != null && selectedSkillId != null;
        if (canSendWithTarget || canSendNoTarget) {
            BlockPos usePos = bp != null ? bp : (MC.player != null ? MC.player.blockPosition() : BlockPos.ZERO);
            if (PiglinDisplacementSkill.ID.equals(selectedSkillId)) {
                RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UseCommanderSkillC2SPacket(selectedSkillId, usePos));
                if (displacementWaitingSecond) {
                    selectedSkillId = null;
                    displacementWaitingSecond = false;
                    displacementFirstTargetEntityId = -1;
                }
            } else if (PiglinHyperLeapSkill.ID.equals(selectedSkillId)) {
                RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UseCommanderSkillC2SPacket(selectedSkillId, usePos));
                if (HyperLeapClientData.isWaitingDest()) {
                    selectedSkillId = null;
                    HyperLeapClientData.clearSourceZone();
                }
            } else {
                RoNCommanderSkillsNetwork.CHANNEL.sendToServer(new UseCommanderSkillC2SPacket(selectedSkillId, usePos));
                selectedSkillId = null;
            }
        }
        // Post 事件不可取消，不能调用 setCanceled，否则会 UnsupportedOperationException 闪退
    }

    private static boolean isOverOurButtons(int mouseX, int mouseY) {
        ensureButtons();
        Faction faction = getSkillPanelFaction();
        if (faction == Faction.VILLAGERS) {
            return (skillButton != null && skillButton.isMouseOver(mouseX, mouseY))
                || (unlockButton != null && unlockButton.isMouseOver(mouseX, mouseY))
                || (restSkillButton != null && restSkillButton.isMouseOver(mouseX, mouseY))
                || (restUnlockButton != null && restUnlockButton.isMouseOver(mouseX, mouseY))
                || (cauldronSkillButton != null && cauldronSkillButton.isMouseOver(mouseX, mouseY))
                || (flameRocketRainSkillButton != null && flameRocketRainSkillButton.isMouseOver(mouseX, mouseY))
                || (flameRocketRainUnlockButton != null && flameRocketRainUnlockButton.isMouseOver(mouseX, mouseY))
                || (cauldronUnlockButton != null && cauldronUnlockButton.isMouseOver(mouseX, mouseY));
        }
        if (faction == Faction.MONSTERS) {
            return (undyingSkillButton != null && undyingSkillButton.isMouseOver(mouseX, mouseY))
                || (undyingUnlockButton != null && undyingUnlockButton.isMouseOver(mouseX, mouseY))
                || (sculkSkillButton != null && sculkSkillButton.isMouseOver(mouseX, mouseY))
                || (sculkUnlockButton != null && sculkUnlockButton.isMouseOver(mouseX, mouseY))
                || (phantomSkillButton != null && phantomSkillButton.isMouseOver(mouseX, mouseY))
                || (phantomUnlockButton != null && phantomUnlockButton.isMouseOver(mouseX, mouseY))
                || (transmutationSkillButton != null && transmutationSkillButton.isMouseOver(mouseX, mouseY))
                || (transmutationUnlockButton != null && transmutationUnlockButton.isMouseOver(mouseX, mouseY));
        }
        if (faction == Faction.PIGLINS) {
            return (efficientPortalSkillButton != null && efficientPortalSkillButton.isMouseOver(mouseX, mouseY))
                || (efficientPortalUnlockButton != null && efficientPortalUnlockButton.isMouseOver(mouseX, mouseY))
                || (bountySkillButton != null && bountySkillButton.isMouseOver(mouseX, mouseY))
                || (bountyUnlockButton != null && bountyUnlockButton.isMouseOver(mouseX, mouseY))
                || (displacementSkillButton != null && displacementSkillButton.isMouseOver(mouseX, mouseY))
                || (displacementUnlockButton != null && displacementUnlockButton.isMouseOver(mouseX, mouseY))
                || (hyperLeapSkillButton != null && hyperLeapSkillButton.isMouseOver(mouseX, mouseY))
                || (hyperLeapUnlockButton != null && hyperLeapUnlockButton.isMouseOver(mouseX, mouseY));
        }
        return false;
    }

    /** 与主项目一致：在 ScreenEvent.Render.Post 中绘制；LOWEST 使本 mod 最后画，tooltip 能盖在最上层 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || !(evt.getScreen() instanceof TopdownGui)) return;
        if (MC.player == null || MC.level == null) return;
        if (!PlayerClientEvents.isRTSPlayer()) return;
        Faction faction = getSkillPanelFaction();
        if (faction != Faction.VILLAGERS && faction != Faction.MONSTERS && faction != Faction.PIGLINS) return;

        ensureButtons();
        updateButtonPositionsForInput();

        int screenHeight = MC.getWindow().getGuiScaledHeight();
        int iconFrameSize = Button.DEFAULT_ICON_FRAME_SIZE;
        int panelW = PANEL_PADDING + iconFrameSize + COL_GAP + iconFrameSize + PANEL_PADDING;
        int panelH = PANEL_PADDING + SKILL_ROWS * iconFrameSize + (SKILL_ROWS - 1) * ROW_GAP + PANEL_PADDING;
        int panelX = 4;
        int panelY = screenHeight / 2 - panelH / 2;

        GuiGraphics guiGraphics = evt.getGuiGraphics();
        MyRenderer.renderFrameWithBg(guiGraphics, panelX, panelY, panelW, panelH, PANEL_BG_COLOR);

        int leftX = panelX + PANEL_PADDING;
        int rightX = panelX + PANEL_PADDING + iconFrameSize + COL_GAP;
        int row0Y = panelY + PANEL_PADDING;
        int row1Y = panelY + PANEL_PADDING + iconFrameSize + ROW_GAP;
        int row2Y = panelY + PANEL_PADDING + 2 * (iconFrameSize + ROW_GAP);

        int mouseX = (int) evt.getMouseX();
        int mouseY = (int) evt.getMouseY();

        // 战斗号角
        CommanderSkill skillBH = CommanderSkills.get(VillagerBattleHornSkill.ID);
        CommanderSkillState stateBH = CommanderSkillsClientState.get(VillagerBattleHornSkill.ID);
        int levelBH = stateBH == null ? 0 : stateBH.level();
        float cooldownBH = stateBH == null ? 0 : stateBH.cooldownTicks();
        boolean skillCanUseBH = levelBH > 0 && cooldownBH <= 0 && clientCanAffordUse(skillBH, levelBH);
        boolean upgradeCanDoBH = levelBH < (skillBH != null ? skillBH.getMaxLevel() : 3)
            && clientHasBuildingForNextLevel(skillBH, levelBH)
            && clientCanAffordUpgrade(skillBH, levelBH);
        float cooldownGreyBH = 0f;
        if (levelBH > 0 && skillBH != null) {
            cooldownGreyBH = cooldownBH <= 0 ? 1f : (1f - Math.min(1f, cooldownBH / (float) SkillConfig.getCooldownTicks(skillBH, levelBH)));
        }
        skillButton.isHidden = () -> faction != Faction.VILLAGERS;
        skillButton.isSelected = () -> VillagerBattleHornSkill.ID.equals(selectedSkillId);
        skillButton.isEnabled = () -> levelBH > 0;
        skillButton.greyPercent = cooldownGreyBH;
        skillButton.tooltipLines = buildSkillTooltip(skillBH, levelBH, cooldownBH, skillCanUseBH);
        unlockButton.isHidden = () -> faction != Faction.VILLAGERS || levelBH >= (skillBH != null ? skillBH.getMaxLevel() : 3);
        unlockButton.isEnabled = () -> true;
        unlockButton.greyPercent = 0f;
        unlockButton.tooltipLines = buildUnlockTooltip(skillBH, levelBH, upgradeCanDoBH);

        // 休整
        CommanderSkill skillRest = CommanderSkills.get(VillagerRestSkill.ID);
        CommanderSkillState stateRest = CommanderSkillsClientState.get(VillagerRestSkill.ID);
        int levelRest = stateRest == null ? 0 : stateRest.level();
        float cooldownRest = stateRest == null ? 0 : stateRest.cooldownTicks();
        boolean skillCanUseRest = levelRest > 0 && cooldownRest <= 0 && clientCanAffordUse(skillRest, levelRest);
        boolean upgradeCanDoRest = levelRest < (skillRest != null ? skillRest.getMaxLevel() : 3)
            && clientHasBuildingForNextLevel(skillRest, levelRest)
            && clientCanAffordUpgrade(skillRest, levelRest);
        float cooldownGreyRest = 0f;
        if (levelRest > 0 && skillRest != null) {
            cooldownGreyRest = cooldownRest <= 0 ? 1f : (1f - Math.min(1f, cooldownRest / (float) SkillConfig.getCooldownTicks(skillRest, levelRest)));
        }
        restSkillButton.isHidden = () -> faction != Faction.VILLAGERS;
        restSkillButton.isSelected = () -> VillagerRestSkill.ID.equals(selectedSkillId);
        restSkillButton.isEnabled = () -> levelRest > 0;
        restSkillButton.greyPercent = cooldownGreyRest;
        restSkillButton.tooltipLines = buildSkillTooltip(skillRest, levelRest, cooldownRest, skillCanUseRest);
        restUnlockButton.isHidden = () -> faction != Faction.VILLAGERS || levelRest >= (skillRest != null ? skillRest.getMaxLevel() : 3);
        restUnlockButton.isEnabled = () -> true;
        restUnlockButton.greyPercent = 0f;
        restUnlockButton.tooltipLines = buildUnlockTooltip(skillRest, levelRest, upgradeCanDoRest);

        // 坩埚狂欢
        CommanderSkill skillCauldron = CommanderSkills.get(VillagerCauldronRevelrySkill.ID);
        CommanderSkillState stateCauldron = CommanderSkillsClientState.get(VillagerCauldronRevelrySkill.ID);
        int levelCauldron = stateCauldron == null ? 0 : stateCauldron.level();
        float cooldownCauldron = stateCauldron == null ? 0 : stateCauldron.cooldownTicks();
        boolean skillCanUseCauldron = levelCauldron > 0 && cooldownCauldron <= 0 && clientCanAffordUse(skillCauldron, levelCauldron);
        boolean upgradeCanDoCauldron = levelCauldron < (skillCauldron != null ? skillCauldron.getMaxLevel() : 3)
            && clientHasBuildingForNextLevel(skillCauldron, levelCauldron)
            && clientCanAffordUpgrade(skillCauldron, levelCauldron);
        float cooldownGreyCauldron = 0f;
        if (levelCauldron > 0 && skillCauldron != null) {
            cooldownGreyCauldron = cooldownCauldron <= 0 ? 1f : (1f - Math.min(1f, cooldownCauldron / (float) SkillConfig.getCooldownTicks(skillCauldron, levelCauldron)));
        }
        cauldronSkillButton.isHidden = () -> faction != Faction.VILLAGERS;
        cauldronSkillButton.isSelected = () -> VillagerCauldronRevelrySkill.ID.equals(selectedSkillId);
        cauldronSkillButton.isEnabled = () -> levelCauldron > 0;
        cauldronSkillButton.greyPercent = cooldownGreyCauldron;
        cauldronSkillButton.tooltipLines = buildSkillTooltip(skillCauldron, levelCauldron, cooldownCauldron, skillCanUseCauldron);
        cauldronUnlockButton.isHidden = () -> faction != Faction.VILLAGERS || levelCauldron >= (skillCauldron != null ? skillCauldron.getMaxLevel() : 3);
        cauldronUnlockButton.isEnabled = () -> true;
        cauldronUnlockButton.greyPercent = 0f;
        cauldronUnlockButton.tooltipLines = buildUnlockTooltip(skillCauldron, levelCauldron, upgradeCanDoCauldron);

        CommanderSkill skillFlameRocketRain = CommanderSkills.get(VillagerFlameRocketRainSkill.ID);
        CommanderSkillState stateFlameRocketRain = CommanderSkillsClientState.get(VillagerFlameRocketRainSkill.ID);
        int levelFlameRocketRain = stateFlameRocketRain == null ? 0 : stateFlameRocketRain.level();
        float cooldownFlameRocketRain = stateFlameRocketRain == null ? 0 : stateFlameRocketRain.cooldownTicks();
        boolean skillCanUseFlameRocketRain = levelFlameRocketRain > 0 && cooldownFlameRocketRain <= 0 && clientCanAffordUse(skillFlameRocketRain, levelFlameRocketRain);
        boolean upgradeCanDoFlameRocketRain = levelFlameRocketRain < (skillFlameRocketRain != null ? skillFlameRocketRain.getMaxLevel() : 1)
            && clientHasBuildingForNextLevel(skillFlameRocketRain, levelFlameRocketRain)
            && clientCanAffordUpgrade(skillFlameRocketRain, levelFlameRocketRain);
        float cooldownGreyFlameRocketRain = levelFlameRocketRain > 0 && skillFlameRocketRain != null
            ? (cooldownFlameRocketRain <= 0 ? 1f : (1f - Math.min(1f, cooldownFlameRocketRain / (float) SkillConfig.getCooldownTicks(skillFlameRocketRain, levelFlameRocketRain))))
            : 0f;
        flameRocketRainSkillButton.isHidden = () -> faction != Faction.VILLAGERS;
        flameRocketRainSkillButton.isSelected = () -> VillagerFlameRocketRainSkill.ID.equals(selectedSkillId);
        flameRocketRainSkillButton.isEnabled = () -> levelFlameRocketRain > 0;
        flameRocketRainSkillButton.greyPercent = cooldownGreyFlameRocketRain;
        flameRocketRainSkillButton.tooltipLines = buildSkillTooltip(skillFlameRocketRain, levelFlameRocketRain, cooldownFlameRocketRain, skillCanUseFlameRocketRain);
        flameRocketRainUnlockButton.isHidden = () -> faction != Faction.VILLAGERS || levelFlameRocketRain >= (skillFlameRocketRain != null ? skillFlameRocketRain.getMaxLevel() : 1);
        flameRocketRainUnlockButton.isEnabled = () -> true;
        flameRocketRainUnlockButton.greyPercent = 0f;
        flameRocketRainUnlockButton.tooltipLines = buildUnlockTooltip(skillFlameRocketRain, levelFlameRocketRain, upgradeCanDoFlameRocketRain);

        CommanderSkill skillUndying = CommanderSkills.get(MonsterUndyingSkill.ID);
        CommanderSkillState stateUndying = CommanderSkillsClientState.get(MonsterUndyingSkill.ID);
        int levelUndying = stateUndying == null ? 0 : stateUndying.level();
        float cooldownUndying = stateUndying == null ? 0 : stateUndying.cooldownTicks();
        boolean skillCanUseUndying = levelUndying > 0 && cooldownUndying <= 0 && clientCanAffordUse(skillUndying, levelUndying);
        boolean upgradeCanDoUndying = levelUndying < (skillUndying != null ? skillUndying.getMaxLevel() : 3)
            && clientHasBuildingForNextLevel(skillUndying, levelUndying)
            && clientCanAffordUpgrade(skillUndying, levelUndying);
        float cooldownGreyUndying = levelUndying > 0 && skillUndying != null
            ? (cooldownUndying <= 0 ? 1f : (1f - Math.min(1f, cooldownUndying / (float) SkillConfig.getCooldownTicks(skillUndying, levelUndying))))
            : 0f;
        undyingSkillButton.isHidden = () -> faction != Faction.MONSTERS;
        undyingSkillButton.isSelected = () -> MonsterUndyingSkill.ID.equals(selectedSkillId);
        undyingSkillButton.isEnabled = () -> levelUndying > 0;
        undyingSkillButton.greyPercent = cooldownGreyUndying;
        undyingSkillButton.tooltipLines = buildSkillTooltip(skillUndying, levelUndying, cooldownUndying, skillCanUseUndying);
        undyingUnlockButton.isHidden = () -> faction != Faction.MONSTERS || levelUndying >= (skillUndying != null ? skillUndying.getMaxLevel() : 3);
        undyingUnlockButton.tooltipLines = buildUnlockTooltip(skillUndying, levelUndying, upgradeCanDoUndying);

        CommanderSkill skillSculk = CommanderSkills.get(MonsterSculkWardSkill.ID);
        CommanderSkillState stateSculk = CommanderSkillsClientState.get(MonsterSculkWardSkill.ID);
        int levelSculk = stateSculk == null ? 0 : stateSculk.level();
        float cooldownSculk = stateSculk == null ? 0 : stateSculk.cooldownTicks();
        boolean skillCanUseSculk = levelSculk > 0 && cooldownSculk <= 0 && clientCanAffordUse(skillSculk, levelSculk);
        boolean upgradeCanDoSculk = levelSculk < (skillSculk != null ? skillSculk.getMaxLevel() : 3)
            && clientHasBuildingForNextLevel(skillSculk, levelSculk)
            && clientCanAffordUpgrade(skillSculk, levelSculk);
        float cooldownGreySculk = levelSculk > 0 && skillSculk != null
            ? (cooldownSculk <= 0 ? 1f : (1f - Math.min(1f, cooldownSculk / (float) SkillConfig.getCooldownTicks(skillSculk, levelSculk))))
            : 0f;
        sculkSkillButton.isHidden = () -> faction != Faction.MONSTERS;
        sculkSkillButton.isSelected = () -> MonsterSculkWardSkill.ID.equals(selectedSkillId);
        sculkSkillButton.isEnabled = () -> levelSculk > 0;
        sculkSkillButton.greyPercent = cooldownGreySculk;
        sculkSkillButton.tooltipLines = buildSkillTooltip(skillSculk, levelSculk, cooldownSculk, skillCanUseSculk);
        sculkUnlockButton.isHidden = () -> faction != Faction.MONSTERS || levelSculk >= (skillSculk != null ? skillSculk.getMaxLevel() : 3);
        sculkUnlockButton.tooltipLines = buildUnlockTooltip(skillSculk, levelSculk, upgradeCanDoSculk);

        CommanderSkill skillPhantom = CommanderSkills.get(MonsterPhantomStrikeSkill.ID);
        CommanderSkillState statePhantom = CommanderSkillsClientState.get(MonsterPhantomStrikeSkill.ID);
        int levelPhantom = statePhantom == null ? 0 : statePhantom.level();
        float cooldownPhantom = statePhantom == null ? 0 : statePhantom.cooldownTicks();
        boolean skillCanUsePhantom = levelPhantom > 0 && cooldownPhantom <= 0 && clientCanAffordUse(skillPhantom, levelPhantom);
        boolean upgradeCanDoPhantom = levelPhantom < (skillPhantom != null ? skillPhantom.getMaxLevel() : 3)
            && clientHasBuildingForNextLevel(skillPhantom, levelPhantom)
            && clientCanAffordUpgrade(skillPhantom, levelPhantom);
        float cooldownGreyPhantom = levelPhantom > 0 && skillPhantom != null
            ? (cooldownPhantom <= 0 ? 1f : (1f - Math.min(1f, cooldownPhantom / (float) SkillConfig.getCooldownTicks(skillPhantom, levelPhantom))))
            : 0f;
        phantomSkillButton.isHidden = () -> faction != Faction.MONSTERS;
        phantomSkillButton.isSelected = () -> MonsterPhantomStrikeSkill.ID.equals(selectedSkillId);
        phantomSkillButton.isEnabled = () -> levelPhantom > 0;
        phantomSkillButton.greyPercent = cooldownGreyPhantom;
        phantomSkillButton.tooltipLines = buildSkillTooltip(skillPhantom, levelPhantom, cooldownPhantom, skillCanUsePhantom);
        phantomUnlockButton.isHidden = () -> faction != Faction.MONSTERS || levelPhantom >= (skillPhantom != null ? skillPhantom.getMaxLevel() : 3);
        phantomUnlockButton.tooltipLines = buildUnlockTooltip(skillPhantom, levelPhantom, upgradeCanDoPhantom);

        CommanderSkill skillTransmutation = CommanderSkills.get(MonsterTransmutationSkill.ID);
        CommanderSkillState stateTransmutation = CommanderSkillsClientState.get(MonsterTransmutationSkill.ID);
        int levelTransmutation = stateTransmutation == null ? 0 : stateTransmutation.level();
        float cooldownTransmutation = stateTransmutation == null ? 0 : stateTransmutation.cooldownTicks();
        boolean skillCanUseTransmutation = levelTransmutation > 0 && cooldownTransmutation <= 0 && clientCanAffordUse(skillTransmutation, levelTransmutation);
        boolean upgradeCanDoTransmutation = levelTransmutation < (skillTransmutation != null ? skillTransmutation.getMaxLevel() : 1)
            && clientHasBuildingForNextLevel(skillTransmutation, levelTransmutation)
            && clientCanAffordUpgrade(skillTransmutation, levelTransmutation);
        float cooldownGreyTransmutation = levelTransmutation > 0 && skillTransmutation != null
            ? (cooldownTransmutation <= 0 ? 1f : (1f - Math.min(1f, cooldownTransmutation / (float) SkillConfig.getCooldownTicks(skillTransmutation, levelTransmutation))))
            : 0f;
        transmutationSkillButton.isHidden = () -> faction != Faction.MONSTERS;
        transmutationSkillButton.isSelected = () -> MonsterTransmutationSkill.ID.equals(selectedSkillId);
        transmutationSkillButton.isEnabled = () -> levelTransmutation > 0;
        transmutationSkillButton.greyPercent = cooldownGreyTransmutation;
        transmutationSkillButton.tooltipLines = buildSkillTooltip(skillTransmutation, levelTransmutation, cooldownTransmutation, skillCanUseTransmutation);
        transmutationUnlockButton.isHidden = () -> faction != Faction.MONSTERS || levelTransmutation >= (skillTransmutation != null ? skillTransmutation.getMaxLevel() : 1);
        transmutationUnlockButton.isEnabled = () -> true;
        transmutationUnlockButton.greyPercent = 0f;
        transmutationUnlockButton.tooltipLines = buildUnlockTooltip(skillTransmutation, levelTransmutation, upgradeCanDoTransmutation);

        CommanderSkill skillEP = CommanderSkills.get(PiglinEfficientPortalSkill.ID);
        CommanderSkillState stateEP = CommanderSkillsClientState.get(PiglinEfficientPortalSkill.ID);
        int levelEP = stateEP == null ? 0 : stateEP.level();
        float cooldownEP = stateEP == null ? 0 : stateEP.cooldownTicks();
        boolean skillCanUseEP = levelEP > 0 && cooldownEP <= 0 && clientCanAffordUse(skillEP, levelEP);
        boolean upgradeCanDoEP = levelEP < (skillEP != null ? skillEP.getMaxLevel() : 3)
            && clientHasBuildingForNextLevel(skillEP, levelEP)
            && clientCanAffordUpgrade(skillEP, levelEP);
        float cooldownGreyEP = levelEP > 0 && skillEP != null ? (cooldownEP <= 0 ? 1f : (1f - Math.min(1f, cooldownEP / (float) SkillConfig.getCooldownTicks(skillEP, levelEP)))) : 0f;
        efficientPortalSkillButton.isHidden = () -> faction != Faction.PIGLINS;
        efficientPortalSkillButton.isSelected = () -> PiglinEfficientPortalSkill.ID.equals(selectedSkillId);
        efficientPortalSkillButton.isEnabled = () -> levelEP > 0;
        efficientPortalSkillButton.greyPercent = cooldownGreyEP;
        efficientPortalSkillButton.tooltipLines = buildSkillTooltip(skillEP, levelEP, cooldownEP, skillCanUseEP);
        efficientPortalUnlockButton.isHidden = () -> faction != Faction.PIGLINS || levelEP >= (skillEP != null ? skillEP.getMaxLevel() : 3);
        efficientPortalUnlockButton.tooltipLines = buildUnlockTooltip(skillEP, levelEP, upgradeCanDoEP);

        CommanderSkill skillBounty = CommanderSkills.get(PiglinBountySkill.ID);
        CommanderSkillState stateBounty = CommanderSkillsClientState.get(PiglinBountySkill.ID);
        int levelBounty = stateBounty == null ? 0 : stateBounty.level();
        float cooldownBounty = stateBounty == null ? 0 : stateBounty.cooldownTicks();
        boolean skillCanUseBounty = levelBounty > 0 && cooldownBounty <= 0 && clientCanAffordUse(skillBounty, levelBounty);
        boolean upgradeCanDoBounty = levelBounty < (skillBounty != null ? skillBounty.getMaxLevel() : 3)
            && clientHasBuildingForNextLevel(skillBounty, levelBounty)
            && clientCanAffordUpgrade(skillBounty, levelBounty);
        float cooldownGreyBounty = levelBounty > 0 && skillBounty != null ? (cooldownBounty <= 0 ? 1f : (1f - Math.min(1f, cooldownBounty / (float) SkillConfig.getCooldownTicks(skillBounty, levelBounty)))) : 0f;
        bountySkillButton.isHidden = () -> faction != Faction.PIGLINS;
        bountySkillButton.isSelected = () -> PiglinBountySkill.ID.equals(selectedSkillId);
        bountySkillButton.isEnabled = () -> levelBounty > 0;
        bountySkillButton.greyPercent = cooldownGreyBounty;
        bountySkillButton.tooltipLines = buildSkillTooltip(skillBounty, levelBounty, cooldownBounty, skillCanUseBounty);
        bountyUnlockButton.isHidden = () -> faction != Faction.PIGLINS || levelBounty >= (skillBounty != null ? skillBounty.getMaxLevel() : 3);
        bountyUnlockButton.tooltipLines = buildUnlockTooltip(skillBounty, levelBounty, upgradeCanDoBounty);

        CommanderSkill skillDisp = CommanderSkills.get(PiglinDisplacementSkill.ID);
        CommanderSkillState stateDisp = CommanderSkillsClientState.get(PiglinDisplacementSkill.ID);
        int levelDisp = stateDisp == null ? 0 : stateDisp.level();
        float cooldownDisp = stateDisp == null ? 0 : stateDisp.cooldownTicks();
        boolean skillCanUseDisp = levelDisp > 0 && cooldownDisp <= 0 && clientCanAffordUse(skillDisp, levelDisp);
        boolean upgradeCanDoDisp = levelDisp < (skillDisp != null ? skillDisp.getMaxLevel() : 3)
            && clientHasBuildingForNextLevel(skillDisp, levelDisp)
            && clientCanAffordUpgrade(skillDisp, levelDisp);
        float cooldownGreyDisp = levelDisp > 0 && skillDisp != null ? (cooldownDisp <= 0 ? 1f : (1f - Math.min(1f, cooldownDisp / (float) SkillConfig.getCooldownTicks(skillDisp, levelDisp)))) : 0f;
        displacementSkillButton.isHidden = () -> faction != Faction.PIGLINS;
        displacementSkillButton.isSelected = () -> PiglinDisplacementSkill.ID.equals(selectedSkillId);
        displacementSkillButton.isEnabled = () -> levelDisp > 0;
        displacementSkillButton.greyPercent = cooldownGreyDisp;
        displacementSkillButton.tooltipLines = buildSkillTooltip(skillDisp, levelDisp, cooldownDisp, skillCanUseDisp);
        displacementUnlockButton.isHidden = () -> faction != Faction.PIGLINS || levelDisp >= (skillDisp != null ? skillDisp.getMaxLevel() : 3);
        displacementUnlockButton.tooltipLines = buildUnlockTooltip(skillDisp, levelDisp, upgradeCanDoDisp);

        CommanderSkill skillHyperLeap = CommanderSkills.get(PiglinHyperLeapSkill.ID);
        CommanderSkillState stateHyperLeap = CommanderSkillsClientState.get(PiglinHyperLeapSkill.ID);
        int levelHyperLeap = stateHyperLeap == null ? 0 : stateHyperLeap.level();
        float cooldownHyperLeap = stateHyperLeap == null ? 0 : stateHyperLeap.cooldownTicks();
        boolean skillCanUseHyperLeap = levelHyperLeap > 0 && cooldownHyperLeap <= 0 && clientCanAffordUse(skillHyperLeap, levelHyperLeap);
        boolean upgradeCanDoHyperLeap = levelHyperLeap < (skillHyperLeap != null ? skillHyperLeap.getMaxLevel() : 1)
            && clientHasBuildingForNextLevel(skillHyperLeap, levelHyperLeap)
            && clientCanAffordUpgrade(skillHyperLeap, levelHyperLeap);
        float cooldownGreyHyperLeap = levelHyperLeap > 0 && skillHyperLeap != null ? (cooldownHyperLeap <= 0 ? 1f : (1f - Math.min(1f, cooldownHyperLeap / (float) SkillConfig.getCooldownTicks(skillHyperLeap, levelHyperLeap)))) : 0f;
        hyperLeapSkillButton.isHidden = () -> faction != Faction.PIGLINS;
        hyperLeapSkillButton.isSelected = () -> PiglinHyperLeapSkill.ID.equals(selectedSkillId);
        hyperLeapSkillButton.isEnabled = () -> levelHyperLeap > 0;
        hyperLeapSkillButton.greyPercent = cooldownGreyHyperLeap;
        hyperLeapSkillButton.tooltipLines = buildSkillTooltip(skillHyperLeap, levelHyperLeap, cooldownHyperLeap, skillCanUseHyperLeap);
        hyperLeapUnlockButton.isHidden = () -> faction != Faction.PIGLINS || levelHyperLeap >= (skillHyperLeap != null ? skillHyperLeap.getMaxLevel() : 1);
        hyperLeapUnlockButton.isEnabled = () -> true;
        hyperLeapUnlockButton.greyPercent = 0f;
        hyperLeapUnlockButton.tooltipLines = buildUnlockTooltip(skillHyperLeap, levelHyperLeap, upgradeCanDoHyperLeap);

        if (faction == Faction.VILLAGERS) {
            unlockButton.render(guiGraphics, unlockButton.x, unlockButton.y, mouseX, mouseY);
            skillButton.render(guiGraphics, skillButton.x, skillButton.y, mouseX, mouseY);
            restUnlockButton.render(guiGraphics, restUnlockButton.x, restUnlockButton.y, mouseX, mouseY);
            restSkillButton.render(guiGraphics, restSkillButton.x, restSkillButton.y, mouseX, mouseY);
            cauldronUnlockButton.render(guiGraphics, cauldronUnlockButton.x, cauldronUnlockButton.y, mouseX, mouseY);
            cauldronSkillButton.render(guiGraphics, cauldronSkillButton.x, cauldronSkillButton.y, mouseX, mouseY);
            flameRocketRainUnlockButton.render(guiGraphics, flameRocketRainUnlockButton.x, flameRocketRainUnlockButton.y, mouseX, mouseY);
            flameRocketRainSkillButton.render(guiGraphics, flameRocketRainSkillButton.x, flameRocketRainSkillButton.y, mouseX, mouseY);
        } else if (faction == Faction.MONSTERS) {
            undyingUnlockButton.render(guiGraphics, undyingUnlockButton.x, undyingUnlockButton.y, mouseX, mouseY);
            undyingSkillButton.render(guiGraphics, undyingSkillButton.x, undyingSkillButton.y, mouseX, mouseY);
            sculkUnlockButton.render(guiGraphics, sculkUnlockButton.x, sculkUnlockButton.y, mouseX, mouseY);
            sculkSkillButton.render(guiGraphics, sculkSkillButton.x, sculkSkillButton.y, mouseX, mouseY);
            phantomUnlockButton.render(guiGraphics, phantomUnlockButton.x, phantomUnlockButton.y, mouseX, mouseY);
            phantomSkillButton.render(guiGraphics, phantomSkillButton.x, phantomSkillButton.y, mouseX, mouseY);
            transmutationUnlockButton.render(guiGraphics, transmutationUnlockButton.x, transmutationUnlockButton.y, mouseX, mouseY);
            transmutationSkillButton.render(guiGraphics, transmutationSkillButton.x, transmutationSkillButton.y, mouseX, mouseY);
        } else if (faction == Faction.PIGLINS) {
            efficientPortalUnlockButton.render(guiGraphics, efficientPortalUnlockButton.x, efficientPortalUnlockButton.y, mouseX, mouseY);
            efficientPortalSkillButton.render(guiGraphics, efficientPortalSkillButton.x, efficientPortalSkillButton.y, mouseX, mouseY);
            bountyUnlockButton.render(guiGraphics, bountyUnlockButton.x, bountyUnlockButton.y, mouseX, mouseY);
            bountySkillButton.render(guiGraphics, bountySkillButton.x, bountySkillButton.y, mouseX, mouseY);
            displacementUnlockButton.render(guiGraphics, displacementUnlockButton.x, displacementUnlockButton.y, mouseX, mouseY);
            displacementSkillButton.render(guiGraphics, displacementSkillButton.x, displacementSkillButton.y, mouseX, mouseY);
            hyperLeapUnlockButton.render(guiGraphics, hyperLeapUnlockButton.x, hyperLeapUnlockButton.y, mouseX, mouseY);
            hyperLeapSkillButton.render(guiGraphics, hyperLeapSkillButton.x, hyperLeapSkillButton.y, mouseX, mouseY);
        }

        if (faction == Faction.VILLAGERS) {
            if (skillButton.isMouseOver(mouseX, mouseY) && skillButton.tooltipLines != null && !skillButton.tooltipLines.isEmpty())
                skillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!unlockButton.isHidden.get() && unlockButton.isMouseOver(mouseX, mouseY) && unlockButton.tooltipLines != null && !unlockButton.tooltipLines.isEmpty())
                unlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (restSkillButton.isMouseOver(mouseX, mouseY) && restSkillButton.tooltipLines != null && !restSkillButton.tooltipLines.isEmpty())
                restSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!restUnlockButton.isHidden.get() && restUnlockButton.isMouseOver(mouseX, mouseY) && restUnlockButton.tooltipLines != null && !restUnlockButton.tooltipLines.isEmpty())
                restUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (cauldronSkillButton.isMouseOver(mouseX, mouseY) && cauldronSkillButton.tooltipLines != null && !cauldronSkillButton.tooltipLines.isEmpty())
                cauldronSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!cauldronUnlockButton.isHidden.get() && cauldronUnlockButton.isMouseOver(mouseX, mouseY) && cauldronUnlockButton.tooltipLines != null && !cauldronUnlockButton.tooltipLines.isEmpty())
                cauldronUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (flameRocketRainSkillButton.isMouseOver(mouseX, mouseY) && flameRocketRainSkillButton.tooltipLines != null && !flameRocketRainSkillButton.tooltipLines.isEmpty())
                flameRocketRainSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!flameRocketRainUnlockButton.isHidden.get() && flameRocketRainUnlockButton.isMouseOver(mouseX, mouseY) && flameRocketRainUnlockButton.tooltipLines != null && !flameRocketRainUnlockButton.tooltipLines.isEmpty())
                flameRocketRainUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
        } else if (faction == Faction.MONSTERS) {
            if (undyingSkillButton.isMouseOver(mouseX, mouseY) && undyingSkillButton.tooltipLines != null && !undyingSkillButton.tooltipLines.isEmpty())
                undyingSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!undyingUnlockButton.isHidden.get() && undyingUnlockButton.isMouseOver(mouseX, mouseY) && undyingUnlockButton.tooltipLines != null && !undyingUnlockButton.tooltipLines.isEmpty())
                undyingUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (sculkSkillButton.isMouseOver(mouseX, mouseY) && sculkSkillButton.tooltipLines != null && !sculkSkillButton.tooltipLines.isEmpty())
                sculkSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!sculkUnlockButton.isHidden.get() && sculkUnlockButton.isMouseOver(mouseX, mouseY) && sculkUnlockButton.tooltipLines != null && !sculkUnlockButton.tooltipLines.isEmpty())
                sculkUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (phantomSkillButton.isMouseOver(mouseX, mouseY) && phantomSkillButton.tooltipLines != null && !phantomSkillButton.tooltipLines.isEmpty())
                phantomSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!phantomUnlockButton.isHidden.get() && phantomUnlockButton.isMouseOver(mouseX, mouseY) && phantomUnlockButton.tooltipLines != null && !phantomUnlockButton.tooltipLines.isEmpty())
                phantomUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (transmutationSkillButton.isMouseOver(mouseX, mouseY) && transmutationSkillButton.tooltipLines != null && !transmutationSkillButton.tooltipLines.isEmpty())
                transmutationSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!transmutationUnlockButton.isHidden.get() && transmutationUnlockButton.isMouseOver(mouseX, mouseY) && transmutationUnlockButton.tooltipLines != null && !transmutationUnlockButton.tooltipLines.isEmpty())
                transmutationUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
        } else if (faction == Faction.PIGLINS) {
            if (efficientPortalSkillButton.isMouseOver(mouseX, mouseY) && efficientPortalSkillButton.tooltipLines != null && !efficientPortalSkillButton.tooltipLines.isEmpty())
                efficientPortalSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!efficientPortalUnlockButton.isHidden.get() && efficientPortalUnlockButton.isMouseOver(mouseX, mouseY) && efficientPortalUnlockButton.tooltipLines != null && !efficientPortalUnlockButton.tooltipLines.isEmpty())
                efficientPortalUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (bountySkillButton.isMouseOver(mouseX, mouseY) && bountySkillButton.tooltipLines != null && !bountySkillButton.tooltipLines.isEmpty())
                bountySkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!bountyUnlockButton.isHidden.get() && bountyUnlockButton.isMouseOver(mouseX, mouseY) && bountyUnlockButton.tooltipLines != null && !bountyUnlockButton.tooltipLines.isEmpty())
                bountyUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (displacementSkillButton.isMouseOver(mouseX, mouseY) && displacementSkillButton.tooltipLines != null && !displacementSkillButton.tooltipLines.isEmpty())
                displacementSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!displacementUnlockButton.isHidden.get() && displacementUnlockButton.isMouseOver(mouseX, mouseY) && displacementUnlockButton.tooltipLines != null && !displacementUnlockButton.tooltipLines.isEmpty())
                displacementUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (hyperLeapSkillButton.isMouseOver(mouseX, mouseY) && hyperLeapSkillButton.tooltipLines != null && !hyperLeapSkillButton.tooltipLines.isEmpty())
                hyperLeapSkillButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
            else if (!hyperLeapUnlockButton.isHidden.get() && hyperLeapUnlockButton.isMouseOver(mouseX, mouseY) && hyperLeapUnlockButton.tooltipLines != null && !hyperLeapUnlockButton.tooltipLines.isEmpty())
                hyperLeapUnlockButton.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);
        }

        if (faction == Faction.PIGLINS && displacementWaitingSecond) {
            String hint = I18n.get("ron_commanderskills.piglin_displacement.hint_second");
            int sw = MC.getWindow().getGuiScaledWidth();
            int sh = MC.getWindow().getGuiScaledHeight();
            int w = MC.font.width(hint);
            guiGraphics.drawString(MC.font, hint, (sw - w) / 2, sh - 42, 0xFFFF00, false);
        }
        if (faction == Faction.PIGLINS && PiglinHyperLeapSkill.ID.equals(selectedSkillId)) {
            String hint = HyperLeapClientData.isWaitingDest() ? I18n.get("ron_commanderskills.piglin_hyper_leap.hint_dest") : I18n.get("ron_commanderskills.piglin_hyper_leap.hint_source");
            int sw = MC.getWindow().getGuiScaledWidth();
            int sh = MC.getWindow().getGuiScaledHeight();
            int w = MC.font.width(hint);
            guiGraphics.drawString(MC.font, hint, (sw - w) / 2, sh - 42, 0x88CCFF, false);
        }
        // 焰火箭雨：全局冷却倒计时（屏幕底端中间靠右）
        if (FlameRocketRainCooldownClientData.isOnCooldown()) {
            int sw = MC.getWindow().getGuiScaledWidth();
            int sh = MC.getWindow().getGuiScaledHeight();
            String label = I18n.get("ron_commanderskills.villager_flame_rocket_rain");
            int sec = FlameRocketRainCooldownClientData.getRemainingSeconds();
            String text = label + " " + (sec / 60) + ":" + String.format("%02d", sec % 60);
            int x = sw / 2 + 60;
            int y = sh - 22;
            guiGraphics.drawString(MC.font, text, x, y, 0xFFCC44, false);
        }
        // 亡灵嬗变：全局冷却倒计时（屏幕底端中间靠右，略在焰火箭雨右侧）
        if (TransmutationCooldownClientData.isOnCooldown()) {
            int sw = MC.getWindow().getGuiScaledWidth();
            int sh = MC.getWindow().getGuiScaledHeight();
            String label = I18n.get("ron_commanderskills.monster_transmutation");
            int sec = TransmutationCooldownClientData.getRemainingSeconds();
            String text = label + " " + (sec / 60) + ":" + String.format("%02d", sec % 60);
            int x = sw / 2 + 180;
            int y = sh - 22;
            guiGraphics.drawString(MC.font, text, x, y, 0xCC88FF, false);
        }
        // 超跃传送：全局冷却倒计时（屏幕底端中间靠右）
        if (HyperLeapCooldownClientData.isOnCooldown()) {
            int sw = MC.getWindow().getGuiScaledWidth();
            int sh = MC.getWindow().getGuiScaledHeight();
            String label = I18n.get("ron_commanderskills.piglin_hyper_leap");
            int sec = HyperLeapCooldownClientData.getRemainingSeconds();
            String text = label + " " + (sec / 60) + ":" + String.format("%02d", sec % 60);
            int x = sw / 2 + 300;
            int y = sh - 22;
            guiGraphics.drawString(MC.font, text, x, y, 0x4488FF, false);
        }
        // 焰火箭雨：落下前倒计时全局显示（取当前所有区域中最近一次落下的秒数）
        int impactSec = 0;
        for (FlameRocketRainZoneClientData.ActiveZone zone : FlameRocketRainZoneClientData.getActiveZones()) {
            int r = FlameRocketRainZoneClientData.getRemainingSecondsUntilImpact(zone);
            if (r > impactSec) impactSec = r;
        }
        if (impactSec > 0) {
            int sw = MC.getWindow().getGuiScaledWidth();
            int sh = MC.getWindow().getGuiScaledHeight();
            String text = I18n.get("ron_commanderskills.villager_flame_rocket_rain") + " " + impactSec + I18n.get("ron_commanderskills.flame_rocket_rain.seconds_until_impact");
            int w = MC.font.width(text);
            guiGraphics.drawString(MC.font, text, (sw - w) / 2, sh - 42, 0xFFAA22, false);
        }
    }

    private static boolean clientCanAffordUse(CommanderSkill skill, int level) {
        if (skill == null) return false;
        Resources r = ResourcesClientEvents.getResources(MC.player != null ? MC.player.getName().getString() : "");
        if (r == null) return false;
        return r.food >= SkillConfig.getUseFood(skill, level) && r.wood >= SkillConfig.getUseWood(skill, level) && r.ore >= SkillConfig.getUseOre(skill, level);
    }

    private static boolean clientCanAffordUpgrade(CommanderSkill skill, int level) {
        if (skill == null || level >= skill.getMaxLevel()) return false;
        int next = level + 1;
        int food = next == 1 ? SkillConfig.getUnlockFood(skill) : SkillConfig.getUpgradeFood(skill, next);
        int wood = next == 1 ? SkillConfig.getUnlockWood(skill) : SkillConfig.getUpgradeWood(skill, next);
        int ore = next == 1 ? SkillConfig.getUnlockOre(skill) : SkillConfig.getUpgradeOre(skill, next);
        Resources r = ResourcesClientEvents.getResources(MC.player != null ? MC.player.getName().getString() : "");
        if (r == null) return false;
        return r.food >= food && r.wood >= wood && r.ore >= ore;
    }

    private static boolean clientHasBuildingForNextLevel(CommanderSkill skill, int level) {
        if (skill == null || level >= skill.getMaxLevel()) return true;
        String buildingName = skill.getRequiredBuildingNameForLevel(level + 1);
        if (buildingName == null) return true;
        String owner = MC.player != null ? MC.player.getName().getString() : "";
        return BuildingClientEvents.getBuildings().stream()
            .anyMatch(b -> owner.equals(b.ownerName) && b.isBuilt && buildingName.equals(b.getBuilding().name));
    }

    private static List<FormattedCharSequence> buildSkillTooltip(CommanderSkill skill, int level, float cooldownTicks, boolean canUse) {
        if (skill == null) return List.of(fcs("Skill", true));
        String id = skill.getId();
        List<FormattedCharSequence> lines = new java.util.ArrayList<>();
        lines.add(fcs(I18n.get("ron_commanderskills." + id), true));
        lines.add(fcs(I18n.get("ron_commanderskills.tooltip.current_level", level), false));
        lines.add(fcs(I18n.get("ron_commanderskills." + id + ".desc"), false));
        if (level > 0) {
            int useFood = SkillConfig.getUseFood(skill, level);
            int useWood = SkillConfig.getUseWood(skill, level);
            int useOre = SkillConfig.getUseOre(skill, level);
            lines.add(fcs(I18n.get("ron_commanderskills.tooltip.use_cost", useFood, useWood, useOre), false));
            if (cooldownTicks > 0) {
                int sec = (int) (cooldownTicks / 20f);
                lines.add(fcs(I18n.get("ron_commanderskills.tooltip.cooldown", sec), false));
            }
            lines.add(fcs(I18n.get("ron_commanderskills." + id + ".use_hint"), false));
        } else {
            lines.add(fcs(I18n.get("ron_commanderskills." + id + ".unlock_requires"), false));
        }
        return lines;
    }

    private static List<FormattedCharSequence> buildUnlockTooltip(CommanderSkill skill, int level, boolean canUpgrade) {
        if (skill == null || level >= skill.getMaxLevel()) return List.of();
        if (SandboxClientEvents.isSandboxPlayer()) canUpgrade = true;
        String id = skill.getId();
        int next = level + 1;
        int food = next == 1 ? SkillConfig.getUnlockFood(skill) : SkillConfig.getUpgradeFood(skill, next);
        int wood = next == 1 ? SkillConfig.getUnlockWood(skill) : SkillConfig.getUpgradeWood(skill, next);
        int ore = next == 1 ? SkillConfig.getUnlockOre(skill) : SkillConfig.getUpgradeOre(skill, next);
        String buildingRaw = skill.getRequiredBuildingNameForLevel(next);
        String buildingKey = buildingRaw != null ? "ron_commanderskills.building." + buildingRaw.toLowerCase() : "";
        String buildingName = buildingRaw != null ? I18n.get(buildingKey) : "";
        if (buildingRaw != null && buildingName.equals(buildingKey)) buildingName = buildingRaw;

        List<FormattedCharSequence> lines = new java.util.ArrayList<>();
        lines.add(fcs(level == 0 ? I18n.get("ron_commanderskills." + id + ".unlock") : I18n.get("ron_commanderskills." + id + ".upgrade"), true));
        lines.add(fcs(I18n.get("ron_commanderskills.tooltip.current_level", level), false));
        lines.add(fcs(I18n.get("ron_commanderskills.tooltip.requires_building", buildingName), false));
        lines.add(fcs(I18n.get("ron_commanderskills." + id + ".upgrade_requires", food, wood, ore, buildingName), false));
        if (!canUpgrade) {
            lines.add(fcs(I18n.get("ron_commanderskills.tooltip.condition_not_met"), false));
        }
        return lines;
    }

    /** 在世界渲染阶段绘制技能范围预览、增益单位描边、坩埚/幻翼效果圈 */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (MC.player == null || MC.level == null) return;
        if (!OrthoviewClientEvents.isEnabled() || !(MC.screen instanceof TopdownGui)) return;
        if (!PlayerClientEvents.isRTSPlayer()) return;
        Faction faction = getSkillPanelFaction();
        if (faction != Faction.VILLAGERS && faction != Faction.MONSTERS && faction != Faction.PIGLINS) return;

        net.minecraft.world.effect.MobEffect battleHornBuff = EffectRegistrar.BATTLE_HORN_BUFF.get();
        net.minecraft.world.effect.MobEffect restHeal = EffectRegistrar.REST_HEAL.get();
        net.minecraft.world.effect.MobEffect undeadHeal = EffectRegistrar.UNDEAD_HEAL.get();
        net.minecraft.world.effect.MobEffect sculkWard = EffectRegistrar.SCULK_WARD.get();

        if (selectedSkillId != null) {
            CommanderSkill skill = CommanderSkills.get(selectedSkillId);
            CommanderSkillState state = CommanderSkillsClientState.get(selectedSkillId);
            int level = state == null ? 0 : state.level();
            if (skill != null && level > 0) {
                BlockPos center = CursorClientEvents.getPreselectedBlockPos();
                if (center != null) {
                    int radius = skill.getRadius(level);
                    if (radius > 0) {
                        Set<BlockPos> bps = getSkillRangeBlocks(center, radius, MC.level);
                        boolean isRest = VillagerRestSkill.ID.equals(selectedSkillId);
                        boolean isCauldron = VillagerCauldronRevelrySkill.ID.equals(selectedSkillId);
                        boolean isFlameRocketRain = VillagerFlameRocketRainSkill.ID.equals(selectedSkillId);
                        boolean isSculk = MonsterSculkWardSkill.ID.equals(selectedSkillId);
                        boolean isPhantom = MonsterPhantomStrikeSkill.ID.equals(selectedSkillId);
                        boolean isTransmutation = MonsterTransmutationSkill.ID.equals(selectedSkillId);
                        boolean isBounty = PiglinBountySkill.ID.equals(selectedSkillId);
                        boolean isHyperLeap = PiglinHyperLeapSkill.ID.equals(selectedSkillId);
                        float r = isFlameRocketRain ? 1f : (isHyperLeap ? 0.2f : (isTransmutation ? 0.5f : (isCauldron ? 0.6f : (isRest ? 0.2f : (isSculk ? 0.2f : (isPhantom ? 1f : (isBounty ? 1f : 0f)))))));
                        float g = isFlameRocketRain ? 0.4f : (isHyperLeap ? 0.4f : (isTransmutation ? 0f : (isCauldron ? 0.2f : (isRest ? 0.8f : (isSculk ? 0.8f : (isPhantom ? 0.4f : (isBounty ? 0.85f : 0.4f)))))));
                        float b = isFlameRocketRain ? 0.1f : (isHyperLeap ? 1f : (isTransmutation ? 1f : (isCauldron ? 0.6f : (isRest ? 0.2f : (isSculk ? 0.6f : (isPhantom ? 0f : (isBounty ? 0f : 1f)))))));
                        for (BlockPos bp : bps) {
                            AABB aabb = new AABB(bp);
                            MyRenderer.drawLineBox(evt.getPoseStack(), aabb, r, g, b, 0.3f);
                        }
                    }
                }
            }
        }

        // 战斗号角/休整（村民）或 不死者/幽匿护佑（怪物）：获得效果的单位绘制醒目提示
        Vec3 cam = MC.gameRenderer.getMainCamera().getPosition();
        AABB searchBox = new AABB(cam.x - 64, cam.y - 64, cam.z - 64, cam.x + 64, cam.y + 64, cam.z + 64);
        for (LivingEntity le : MC.level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (!FogOfWarClientEvents.isInBrightChunk(le)) continue;
            if (faction == Faction.VILLAGERS) {
                if (le.hasEffect(battleHornBuff)) {
                    AABB aabb = le.getBoundingBox();
                    AABB outer = aabb.inflate(0.15);
                    MyRenderer.drawLineBox(evt.getPoseStack(), outer, 1.0f, 1.0f, 0f, 0.6f);
                    MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 1.0f, 0f, 1.0f);
                } else if (le.hasEffect(restHeal)) {
                    AABB aabb = le.getBoundingBox();
                    AABB outer = aabb.inflate(0.15);
                    MyRenderer.drawLineBox(evt.getPoseStack(), outer, 0.2f, 1.0f, 0.2f, 0.6f);
                    MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 0.2f, 1.0f, 0.2f, 1.0f);
                }
            } else if (faction == Faction.MONSTERS) {
                if (le.hasEffect(undeadHeal)) {
                    AABB aabb = le.getBoundingBox();
                    AABB outer = aabb.inflate(0.15);
                    MyRenderer.drawLineBox(evt.getPoseStack(), outer, 0.6f, 0.4f, 1.0f, 0.6f);
                    MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 0.6f, 0.4f, 1.0f, 1.0f);
                } else if (le.hasEffect(sculkWard)) {
                    AABB aabb = le.getBoundingBox();
                    AABB outer = aabb.inflate(0.15);
                    MyRenderer.drawLineBox(evt.getPoseStack(), outer, 0.2f, 0.8f, 0.7f, 0.6f);
                    MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 0.2f, 0.8f, 0.7f, 1.0f);
                }
            }
            if (BountyMarkClientData.isMarked(le)) {
                AABB aabb = le.getBoundingBox();
                AABB outer = aabb.inflate(0.15);
                MyRenderer.drawLineBox(evt.getPoseStack(), outer, 1.0f, 0.85f, 0f, 0.6f);
                MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 0.85f, 0f, 1.0f);
            }
        }

        // 错位传送：被选中的生物用紫色粒子包裹环绕（多圈环绕全身）
        if (displacementWaitingSecond && displacementFirstTargetEntityId >= 0 && MC.level != null) {
            var entity = MC.level.getEntity(displacementFirstTargetEntityId);
            if (entity != null && entity.isAlive() && FogOfWarClientEvents.isInBrightChunk(entity.blockPosition())) {
                AABB aabb = entity.getBoundingBox();
                MyRenderer.drawLineBox(evt.getPoseStack(), aabb.inflate(0.2), 0.6f, 0.2f, 1.0f, 0.8f);
                net.minecraft.core.particles.DustParticleOptions purple = new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.6f, 0.2f, 1.0f), 0.6f);
                double t = System.currentTimeMillis() / 80.0;
                double ex = entity.getX();
                double ez = entity.getZ();
                double h = entity.getBbHeight();
                double eyBase = entity.getY();
                // 上中下三层环绕 + 更多粒子形成包裹感
                for (int layer = 0; layer < 3; layer++) {
                    double yOff = eyBase + h * (0.25 + layer * 0.35);
                    double r = 0.4 + layer * 0.15;
                    int n = 12;
                    for (int i = 0; i < n; i++) {
                        double a = t + i * 2 * Math.PI / n;
                        double x = ex + Math.cos(a) * r;
                        double z = ez + Math.sin(a) * r;
                        MC.level.addParticle(purple, x, yOff, z, 0, 0.02, 0);
                    }
                }
            }
        }

        // 坩埚狂欢效果圈：范围提示 + 药水掉落视觉（持续到效果圈结束）
        for (CauldronZoneClientData.ActiveZone zone : CauldronZoneClientData.getActiveZones()) {
            if (!FogOfWarClientEvents.isInBrightChunk(zone.center())) continue;
            Set<BlockPos> bps = getSkillRangeBlocks(zone.center(), zone.radius(), MC.level);
            for (BlockPos bp : bps) {
                AABB aabb = new AABB(bp);
                MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 0.6f, 0.2f, 0.6f, 0.35f);
            }
            // 效果圈存在期间持续播放药水瓶下落粒子（每帧多处随机位置）
            if (MC.level != null) {
                int cx = zone.center().getX();
                int cy = zone.center().getY();
                int cz = zone.center().getZ();
                var rand = MC.level.getRandom();
                for (int i = 0; i < 3; i++) {
                    double px = cx + (rand.nextDouble() - 0.5) * zone.radius() * 2;
                    double pz = cz + (rand.nextDouble() - 0.5) * zone.radius() * 2;
                    MC.level.addParticle(net.minecraft.core.particles.ParticleTypes.SPLASH,
                        px + 0.5, cy + 2.2 + rand.nextDouble() * 0.6, pz + 0.5, 0, -0.25, 0);
                }
            }
        }

        // 幻翼特攻队：范围圈显示效果范围 + 轰炸信号醒目提示（中心上升粒子柱）
        for (PhantomStrikeZoneClientData.ActiveZone zone : PhantomStrikeZoneClientData.getActiveZones()) {
            if (!FogOfWarClientEvents.isInBrightChunk(zone.center())) continue;
            Set<BlockPos> bps = getSkillRangeBlocks(zone.center(), zone.radius(), MC.level);
            for (BlockPos bp : bps) {
                AABB aabb = new AABB(bp);
                MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 0.35f, 0f, 0.85f);
            }
            // 轰炸信号：范围中心向上方喷出醒目粒子（火焰/烟雾），延迟结束后信号消失
            if (MC.level != null) {
                double cx = zone.center().getX() + 0.5;
                double cy = zone.center().getY() + 0.5;
                double cz = zone.center().getZ() + 0.5;
                long t = System.currentTimeMillis() / 100L;
                for (int h = 0; h < 8; h++) {
                    double y = cy + h * 0.6 + (t % 3) * 0.1;
                    MC.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, cx, y, cz, 0.02, 0.15, 0.02);
                    MC.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, cx, y, cz, 0, 0.12, 0);
                }
            }
        }

        // 焰火箭雨：范围圈 + 轰炸信号（醒目红黄焰色）
        for (FlameRocketRainZoneClientData.ActiveZone zone : FlameRocketRainZoneClientData.getActiveZones()) {
            if (!FogOfWarClientEvents.isInBrightChunk(zone.center())) continue;
            Set<BlockPos> bps = getSkillRangeBlocks(zone.center(), zone.radius(), MC.level);
            for (BlockPos bp : bps) {
                AABB aabb = new AABB(bp);
                MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 0.6f, 0.1f, 0.9f);
            }
        }

        // 亡灵嬗变：范围圈 + 转换法阵（紫色） + 目标头顶粒子柱（可转变紫/不可转变红）
        for (TransmutationZoneClientData.ActiveZone zone : TransmutationZoneClientData.getActiveZones()) {
            if (!FogOfWarClientEvents.isInBrightChunk(zone.center())) continue;
            Set<BlockPos> bps = getSkillRangeBlocks(zone.center(), zone.radius(), MC.level);
            for (BlockPos bp : bps) {
                AABB aabb = new AABB(bp);
                MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 0.55f, 0f, 0.9f, 0.85f);
            }
            // 法阵：中心区域额外粒子
            net.minecraft.core.particles.ParticleOptions particleType = net.minecraft.core.particles.ParticleTypes.WITCH;
            Vec3 centerVec = Vec3.atCenterOf(zone.center());
            if (MC.level != null && MC.level.getRandom().nextInt(4) == 0) {
                double dx = (MC.level.getRandom().nextDouble() - 0.5) * zone.radius();
                double dz = (MC.level.getRandom().nextDouble() - 0.5) * zone.radius();
                MC.level.addParticle(particleType, centerVec.x + dx, centerVec.y + 0.5, centerVec.z + dz, 0.0, 0.08, 0.0);
            }
            // 目标实体头顶细小粒子柱（可转变=紫色 WITCH 细柱，不可转变=红石 DUST 细柱）
            for (Integer eid : zone.convertibleEntityIds()) {
                var e = MC.level.getEntity(eid);
                if (e instanceof LivingEntity le && FogOfWarClientEvents.isInBrightChunk(le)) {
                    double x = le.getX(); double y = le.getY() + le.getBbHeight(); double z = le.getZ();
                    for (int i = 0; i < 5; i++)
                        MC.level.addParticle(net.minecraft.core.particles.ParticleTypes.WITCH, x, y + i * 0.2, z, 0.0, 0.01, 0.0);
                }
            }
            net.minecraft.core.particles.DustParticleOptions redDust = new net.minecraft.core.particles.DustParticleOptions(
                new org.joml.Vector3f(1.0f, 0.0f, 0.0f), 0.35f);
            for (Integer eid : zone.nonConvertibleEntityIds()) {
                var e = MC.level.getEntity(eid);
                if (e instanceof LivingEntity le && FogOfWarClientEvents.isInBrightChunk(le)) {
                    double x = le.getX(); double y = le.getY() + le.getBbHeight(); double z = le.getZ();
                    for (int i = 0; i < 5; i++)
                        MC.level.addParticle(redDust, x, y + i * 0.2, z, 0.0, 0.01, 0.0);
                }
            }
        }

        // 超跃传送：蓝圈（源区）+ 蓝色粒子绕圈心螺旋上升，高度 8 格；红圈（目标预览）；传送后紫粒子四散
        BlockPos srcCenter = HyperLeapClientData.getSourceCenter();
        int srcRad = HyperLeapClientData.getSourceRadius();
        if (srcCenter != null && srcRad > 0 && FogOfWarClientEvents.isInBrightChunk(srcCenter)) {
            Set<BlockPos> bps = getSkillRangeBlocks(srcCenter, srcRad, MC.level);
            for (BlockPos bp : bps) {
                AABB aabb = new AABB(bp);
                MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 0.2f, 0.4f, 1.0f, 0.9f);
            }
            Vec3 srcVec = Vec3.atCenterOf(srcCenter);
            if (MC.level != null) {
                net.minecraft.core.particles.DustParticleOptions blueDust = new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.2f, 0.45f, 1.0f), 0.5f);
                // 绕圈子中心螺旋上升，总高度 8 格（约 50 个粒子，随高度绕中心旋转约 2 圈）
                double time = System.currentTimeMillis() / 300.0;
                int steps = 50;
                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps;
                    double y = srcVec.y + 0.2 + t * 8.0;
                    double angle = time + t * 4.0 * Math.PI;
                    double rx = Math.cos(angle) * srcRad;
                    double rz = Math.sin(angle) * srcRad;
                    MC.level.addParticle(blueDust, srcVec.x + rx, y, srcVec.z + rz, 0.0, 0.03, 0.0);
                }
            }
        }
        if (HyperLeapClientData.isWaitingDest()) {
            BlockPos destPreview = CursorClientEvents.getPreselectedBlockPos();
            if (destPreview != null && FogOfWarClientEvents.isInBrightChunk(destPreview)) {
                Set<BlockPos> bps = getSkillRangeBlocks(destPreview, 10, MC.level);
                for (BlockPos bp : bps) {
                    AABB aabb = new AABB(bp);
                    MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 0.2f, 0.2f, 0.9f);
                }
            }
        }
        for (HyperLeapClientData.ExecuteZone zone : HyperLeapClientData.getExecuteZones()) {
            if (!FogOfWarClientEvents.isInBrightChunk(zone.center())) continue;
            Vec3 c = Vec3.atCenterOf(zone.center());
            if (MC.level != null) {
                var rand = MC.level.getRandom();
                for (int i = 0; i < 12; i++) {
                    double dx = (rand.nextDouble() - 0.5) * zone.radius() * 2;
                    double dz = (rand.nextDouble() - 0.5) * zone.radius() * 2;
                    MC.level.addParticle(net.minecraft.core.particles.ParticleTypes.WITCH, c.x + dx, c.y + 0.5, c.z + dz, (rand.nextDouble() - 0.5) * 0.1, 0.15, (rand.nextDouble() - 0.5) * 0.1);
                }
            }
        }
    }

    /** 本地计算技能范围圆形格子（不依赖主 mod MiscUtil），仅用于预览 */
    private static Set<BlockPos> getSkillRangeBlocks(BlockPos centrePos, int radius, net.minecraft.world.level.Level level) {
        if (radius <= 0) return Set.of();
        Set<BlockPos> out = new HashSet<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    int gx = centrePos.getX() + x;
                    int gz = centrePos.getZ() + z;
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz) - 1;
                    out.add(new BlockPos(gx, y, gz));
                }
            }
        }
        return out;
    }

    private static void ensureButtons() {
        if (skillButton == null) {
            skillButton = new Button(
                "Battle Horn",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/goat_horn.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (unlockButton == null) {
            unlockButton = new Button(
                "Unlock Battle Horn",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (restSkillButton == null) {
            restSkillButton = new Button(
                "Rest",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/golden_apple.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (restUnlockButton == null) {
            restUnlockButton = new Button(
                "Unlock Rest",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (cauldronSkillButton == null) {
            cauldronSkillButton = new Button(
                "Cauldron Revelry",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/splash_potion.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (cauldronUnlockButton == null) {
            cauldronUnlockButton = new Button(
                "Unlock Cauldron Revelry",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (flameRocketRainSkillButton == null) {
            flameRocketRainSkillButton = new Button(
                "Flame Rocket Rain",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/firework_rocket.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (flameRocketRainUnlockButton == null) {
            flameRocketRainUnlockButton = new Button(
                "Unlock Flame Rocket Rain",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/crossbow_arrow.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (undyingSkillButton == null) {
            undyingSkillButton = new Button(
                "Undying",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/totem_of_undying.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (undyingUnlockButton == null) {
            undyingUnlockButton = new Button(
                "Unlock Undying",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (sculkSkillButton == null) {
            sculkSkillButton = new Button(
                "Sculk Ward",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/sculk_catalyst_top.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (sculkUnlockButton == null) {
            sculkUnlockButton = new Button(
                "Unlock Sculk Ward",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (phantomSkillButton == null) {
            phantomSkillButton = new Button(
                "Phantom Strike",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/phantom_membrane.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (phantomUnlockButton == null) {
            phantomUnlockButton = new Button(
                "Unlock Phantom Strike",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (transmutationSkillButton == null) {
            transmutationSkillButton = new Button(
                "Transmutation",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/bone.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (transmutationUnlockButton == null) {
            transmutationUnlockButton = new Button(
                "Unlock Transmutation",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/splash_potion.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (efficientPortalSkillButton == null) {
            efficientPortalSkillButton = new Button(
                "Efficient Portal",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/end_portal_frame_top.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (efficientPortalUnlockButton == null) {
            efficientPortalUnlockButton = new Button(
                "Unlock Efficient Portal",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (bountySkillButton == null) {
            bountySkillButton = new Button(
                "Bounty",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/gold_ingot.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (bountyUnlockButton == null) {
            bountyUnlockButton = new Button(
                "Unlock Bounty",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (displacementSkillButton == null) {
            displacementSkillButton = new Button(
                "Displacement",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/ender_pearl.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (displacementUnlockButton == null) {
            displacementUnlockButton = new Button(
                "Unlock Displacement",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (hyperLeapSkillButton == null) {
            hyperLeapSkillButton = new Button(
                "Hyper Leap",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/ender_eye.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
        if (hyperLeapUnlockButton == null) {
            hyperLeapUnlockButton = new Button(
                "Unlock Hyper Leap",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/chorus_fruit.png"),
                (com.solegendary.reignofnether.keybinds.Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                (List<FormattedCharSequence>) null
            );
        }
    }
}
