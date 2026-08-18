package com.xmly.enchantip.network.packet;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import com.xmly.enchantip.client.screen.TipType;
import com.xmly.enchantip.handler.HandlerConfig;
import com.xmly.enchantip.network.ServerNetworking;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.xmly.enchantip.Xmly_EnchantmentsTooltip.NBT_NAME;

public class PacketRequestEnchantList {

    private final TipType type;

    public PacketRequestEnchantList(TipType type) {
        this.type = type;
    }

    public PacketRequestEnchantList(FriendlyByteBuf buf) {
        int ordinal = buf.readInt();
        this.type = TipType.values()[ordinal];
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.type.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) return;

            List<PacketSyncEnchantList.Entry> result = new ArrayList<>();

            // 根据TipType 仅遍历对应槽，不再全量遍历
            switch (this.type) {
                case HAND -> {
                    // 主手选中槽：player.getInventory().selected
                    int handSlot = player.getInventory().selected;
                    ItemStack stack = player.getInventory().getItem(handSlot);
                    scanItem(handSlot, stack, result, TipType.HAND);
                }
                case NONE -> scanSlot(player, 40, result, TipType.NONE);
                case HELMET -> scanSlot(player, 39, result, TipType.HELMET);
                case CHESTPLATE -> scanSlot(player, 38, result, TipType.CHESTPLATE);
                case LEGGINGS -> scanSlot(player, 37, result, TipType.LEGGINGS);
                case BOOTS -> scanSlot(player, 36, result, TipType.BOOTS);
            }

            /*
             * 返回客户端
             */
            ServerNetworking.sendToPlayer(new PacketSyncEnchantList(result), player);
        });

        context.setPacketHandled(true);
    }

    // 单独扫描指定盔甲槽工具方法
    static void scanSlot(ServerPlayer player, int slotId, List<PacketSyncEnchantList.Entry> result, TipType type) {
        ItemStack stack = player.getInventory().getItem(slotId);
        scanItem(slotId, stack, result, type);
    }

    static void scanItem(int slot, ItemStack stack, List<PacketSyncEnchantList.Entry> result, TipType type) {
        if (stack.isEmpty()) return;

        ListTag enchantments = stack.getEnchantmentTags();

        for (int i = 0; i < enchantments.size(); i++) {

            CompoundTag tag = enchantments.getCompound(i);

            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));

            if (id == null) continue;
            if (id.equals(Xmly_EnchantmentsTooltip.DISABLED_ENCHANT)) continue;

            String color = getColor(id, TipType.ARMOR);
            if (color == null) color = getColor(id, type);

            if (color == null) continue;

            result.add(new PacketSyncEnchantList.Entry(slot, id, tag.getInt("lvl"), true, color));
        }

        CompoundTag root = stack.getTag();
        if (root == null) return;
        if (!root.contains(NBT_NAME)) return;

        ListTag disabled = root.getList(NBT_NAME, Tag.TAG_COMPOUND);

        for (int i = 0; i < disabled.size(); i++) {
            CompoundTag tag = disabled.getCompound(i);

            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));

            if (id == null) continue;

            String color = getColor(id, TipType.ARMOR);
            if (color == null) color = getColor(id, type);

            if (color == null) continue;

            result.add(new PacketSyncEnchantList.Entry(slot, id, tag.getShort("lvl"), false, color));
        }

    }

    private static String getColor(ResourceLocation id, TipType type) {
        return switch (type) {
            case HAND, NONE -> HandlerConfig.EnchantipHand.get(id);

            case HELMET -> HandlerConfig.EnchantipHelmet.get(id);

            case CHESTPLATE -> HandlerConfig.EnchantipChestplate.get(id);

            case LEGGINGS -> HandlerConfig.EnchantipLeggings.get(id);

            case BOOTS -> HandlerConfig.EnchantipBoots.get(id);

            case ARMOR -> HandlerConfig.EnchantipArmor.get(id);
        };
    }
}