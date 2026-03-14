package com.ron.commanderskills.network;

import com.ron.commanderskills.RoNCommanderSkillsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class RoNCommanderSkillsNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel CHANNEL;

    public static void init() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(RoNCommanderSkillsMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        int id = 0;
        CHANNEL.messageBuilder(UseCommanderSkillC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(UseCommanderSkillC2SPacket::encode)
            .decoder(UseCommanderSkillC2SPacket::decode)
            .consumerMainThread(UseCommanderSkillC2SPacket::handle)
            .add();

        CHANNEL.messageBuilder(UnlockCommanderSkillC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(UnlockCommanderSkillC2SPacket::encode)
            .decoder(UnlockCommanderSkillC2SPacket::decode)
            .consumerMainThread(UnlockCommanderSkillC2SPacket::handle)
            .add();

        CHANNEL.messageBuilder(SyncCommanderSkillsS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SyncCommanderSkillsS2CPacket::encode)
            .decoder(SyncCommanderSkillsS2CPacket::decode)
            .consumerMainThread(SyncCommanderSkillsS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(PlayCommanderSkillSoundS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(PlayCommanderSkillSoundS2CPacket::encode)
            .decoder(PlayCommanderSkillSoundS2CPacket::decode)
            .consumerMainThread(PlayCommanderSkillSoundS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(AddCauldronZoneS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(AddCauldronZoneS2CPacket::encode)
            .decoder(AddCauldronZoneS2CPacket::decode)
            .consumerMainThread(AddCauldronZoneS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(AddPhantomStrikeZoneS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(AddPhantomStrikeZoneS2CPacket::encode)
            .decoder(AddPhantomStrikeZoneS2CPacket::decode)
            .consumerMainThread(AddPhantomStrikeZoneS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(AddBountyMarksS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(AddBountyMarksS2CPacket::encode)
            .decoder(AddBountyMarksS2CPacket::decode)
            .consumerMainThread(AddBountyMarksS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(DisplacementFirstTargetS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(DisplacementFirstTargetS2CPacket::encode)
            .decoder(DisplacementFirstTargetS2CPacket::decode)
            .consumerMainThread(DisplacementFirstTargetS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(CancelDisplacementC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(CancelDisplacementC2SPacket::encode)
            .decoder(CancelDisplacementC2SPacket::decode)
            .consumerMainThread(CancelDisplacementC2SPacket::handle)
            .add();

        CHANNEL.messageBuilder(FlameRocketRainEffectS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlameRocketRainEffectS2CPacket::encode)
            .decoder(FlameRocketRainEffectS2CPacket::decode)
            .consumerMainThread(FlameRocketRainEffectS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(FlameRocketRainCooldownS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlameRocketRainCooldownS2CPacket::encode)
            .decoder(FlameRocketRainCooldownS2CPacket::decode)
            .consumerMainThread(FlameRocketRainCooldownS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(TransmutationEffectS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(TransmutationEffectS2CPacket::encode)
            .decoder(TransmutationEffectS2CPacket::decode)
            .consumerMainThread(TransmutationEffectS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(TransmutationCooldownS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(TransmutationCooldownS2CPacket::encode)
            .decoder(TransmutationCooldownS2CPacket::decode)
            .consumerMainThread(TransmutationCooldownS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(HyperLeapSourceZoneS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(HyperLeapSourceZoneS2CPacket::encode)
            .decoder(HyperLeapSourceZoneS2CPacket::decode)
            .consumerMainThread(HyperLeapSourceZoneS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(HyperLeapExecuteS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(HyperLeapExecuteS2CPacket::encode)
            .decoder(HyperLeapExecuteS2CPacket::decode)
            .consumerMainThread(HyperLeapExecuteS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(HyperLeapCooldownS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(HyperLeapCooldownS2CPacket::encode)
            .decoder(HyperLeapCooldownS2CPacket::decode)
            .consumerMainThread(HyperLeapCooldownS2CPacket::handle)
            .add();

        CHANNEL.messageBuilder(CancelHyperLeapC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(CancelHyperLeapC2SPacket::encode)
            .decoder(CancelHyperLeapC2SPacket::decode)
            .consumerMainThread(CancelHyperLeapC2SPacket::handle)
            .add();
    }
}
