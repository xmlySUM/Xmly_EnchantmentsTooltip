package com.xmly.enchantip.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.awt.*;
import java.util.function.Supplier;

public class PacketClientMessageDisplay {
    private final ResourceLocation id;
    private final String enchantipColor;
    private final String enchantip;

    public PacketClientMessageDisplay(ResourceLocation id, String enchantColor, String tip) {
        this.id = id;
        this.enchantipColor = enchantColor;
        this.enchantip = tip;
    }

    public PacketClientMessageDisplay(FriendlyByteBuf buf) {
        id = ResourceLocation.tryParse(buf.readUtf());
        this.enchantipColor = buf.readUtf();
        this.enchantip = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(id.toString());
        buf.writeUtf(enchantipColor);
        buf.writeUtf(enchantip);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {

        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {

            LocalPlayer localPlayer = Minecraft.getInstance().player;

            if (localPlayer == null) return;

            if (id == null) return;

            MutableComponent enchantComponent = Component.translatable("enchantment." + id.getNamespace() + "." + id.getPath());

            int color = 0xFFFFFF;
            try {
                color = Color.decode(enchantipColor).getRGB();
            } catch (Exception ignored) {
            }
            localPlayer.displayClientMessage(enchantComponent.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))).append(Component.translatable("text.xmlyenchantip.tooltip." + enchantip)), true);

        });

        context.setPacketHandled(true);
    }

    public static class DisplayEnchant {

        public ResourceLocation id;
        public int level;
        public boolean enabled;

        public DisplayEnchant(ResourceLocation id, int level, boolean enabled) {
            this.id = id;
            this.level = level;
            this.enabled = enabled;
        }
    }
}
