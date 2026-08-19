package com.xmly.enchantip.network;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import com.xmly.enchantip.network.packet.*;
import com.xmly.enchantip.network.packet.PacketClientMessageDisplay;
import com.xmly.enchantip.network.packet.PacketRequestEnchantList;
import com.xmly.enchantip.network.packet.PacketSyncEnchantList;
import com.xmly.enchantip.network.packet.PacketTipSpecificEnchant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ServerNetworking {
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static SimpleChannel instance;

    public static void init() {
        final String PROTOCOL_VER = "1.0.1";

        SimpleChannel net = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(Xmly_EnchantmentsTooltip.MOD_ID, "network")).networkProtocolVersion(() -> PROTOCOL_VER).clientAcceptedVersions(PROTOCOL_VER::equals).serverAcceptedVersions(PROTOCOL_VER::equals).simpleChannel();

        instance = net;

        /*
         * 客户端 -> 服务端
         * 请求同步当前可切换附魔
         */
        net.messageBuilder(PacketRequestEnchantList.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(PacketRequestEnchantList::new).encoder(PacketRequestEnchantList::toBytes).consumerMainThread(PacketRequestEnchantList::handle).add();

        /*
         * 服务端 -> 客户端
         * 返回附魔列表
         */
        net.messageBuilder(PacketSyncEnchantList.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(PacketSyncEnchantList::new).encoder(PacketSyncEnchantList::toBytes).consumerMainThread(PacketSyncEnchantList::handle).add();

        /*
         * 客户端 -> 服务端
         * GUI点击指定附魔
         */
        net.messageBuilder(PacketTipSpecificEnchant.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(PacketTipSpecificEnchant::new).encoder(PacketTipSpecificEnchant::toBytes).consumerMainThread(PacketTipSpecificEnchant::handle).add();

        /*
         * 原有客户端提示
         */
        net.messageBuilder(PacketClientMessageDisplay.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(PacketClientMessageDisplay::new).encoder(PacketClientMessageDisplay::toBytes).consumerMainThread(PacketClientMessageDisplay::handle).add();
    }

    public static <MSG> void sendToServer(MSG message) {
        instance.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        instance.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}