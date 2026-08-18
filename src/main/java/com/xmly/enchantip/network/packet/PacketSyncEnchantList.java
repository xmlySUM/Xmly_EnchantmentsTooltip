package com.xmly.enchantip.network.packet;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import com.xmly.enchantip.client.screen.EnchantipScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncEnchantList {

    public static final List<Entry> enchantments = Collections.synchronizedList(new ArrayList<>());
    private final List<Entry> entries;

    public PacketSyncEnchantList(List<Entry> entries) {
        this.entries = entries;
    }

    public PacketSyncEnchantList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int slot = buf.readInt();
            ResourceLocation id = ResourceLocation.tryParse(buf.readUtf());
            if (Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.equals(id)) continue;
            int level = buf.readInt();
            boolean enabled = buf.readBoolean();
            String color = buf.readUtf();
            entries.add(new Entry(slot, id, level, enabled, color));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entries.size());
        for (Entry e : entries) {
            buf.writeInt(e.slot);
            buf.writeUtf(e.id.toString());
            buf.writeInt(e.level);
            buf.writeBoolean(e.enabled);
            buf.writeUtf(e.color);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            enchantments.clear();
            enchantments.addAll(entries);

            if (Minecraft.getInstance().screen instanceof EnchantipScreen screen) {
                screen.reloadButtons();
            }
        });
        context.setPacketHandled(true);
    }

    public record Entry(int slot, ResourceLocation id, int level, boolean enabled, String color) {
    }
}