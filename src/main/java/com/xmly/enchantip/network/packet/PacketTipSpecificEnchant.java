package com.xmly.enchantip.network.packet;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import com.xmly.enchantip.client.screen.TipType;
import com.xmly.enchantip.handler.HandlerConfig;
import com.xmly.enchantip.network.ServerNetworking;
import com.xmly.enchantip.client.implement.InterfacePlayerInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.xmly.enchantip.Xmly_EnchantmentsTooltip.NBT_NAME;
import static com.xmly.enchantip.network.packet.PacketRequestEnchantList.scanSlot;
import static com.xmly.enchantip.network.packet.PacketRequestEnchantList.scanItem;

public class PacketTipSpecificEnchant {
    private final int slot;
    private final TipType type;
    private final ResourceLocation enchantId;

    public PacketTipSpecificEnchant(int slot, TipType type, ResourceLocation enchantId) {
        this.slot = slot;
        this.type = type;
        this.enchantId = enchantId;
    }

    public PacketTipSpecificEnchant(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
        this.type = TipType.values()[buf.readInt()];
        this.enchantId = ResourceLocation.tryParse(buf.readUtf());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeInt(type.ordinal());
        buf.writeUtf(enchantId.toString());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {

            ServerPlayer player = context.getSender();

            if (player == null) return;

            ItemStack stack = getStack(player, slot);

            if (stack.isEmpty() || !isAllowed(type, enchantId)) return;

            tipSpecificEnchant(player, stack, enchantId, type);

            List<PacketSyncEnchantList.Entry> result = new ArrayList<>();

            switch (type) {
                case HAND -> {
                    int handSlot = player.getInventory().selected;

                    scanItem(handSlot, player.getInventory().getItem(handSlot), result, type);
                }

                case NONE -> scanSlot(player, 40, result, type);
                case HELMET -> scanSlot(player, 39, result, type);
                case CHESTPLATE -> scanSlot(player, 38, result, type);
                case LEGGINGS -> scanSlot(player, 37, result, type);
                case BOOTS -> scanSlot(player, 36, result, type);
            }

            ServerNetworking.sendToPlayer(new PacketSyncEnchantList(result), player);
        });

        context.setPacketHandled(true);
    }

    private static boolean isAllowed(TipType type, ResourceLocation id) {
        return switch (type) {
            case HAND, NONE -> HandlerConfig.EnchantipHand.containsKey(id);
            case HELMET ->
                    HandlerConfig.EnchantipArmor.containsKey(id) || HandlerConfig.EnchantipHelmet.containsKey(id);
            case CHESTPLATE ->
                    HandlerConfig.EnchantipArmor.containsKey(id) || HandlerConfig.EnchantipChestplate.containsKey(id);
            case LEGGINGS ->
                    HandlerConfig.EnchantipArmor.containsKey(id) || HandlerConfig.EnchantipLeggings.containsKey(id);
            case BOOTS -> HandlerConfig.EnchantipArmor.containsKey(id) || HandlerConfig.EnchantipBoots.containsKey(id);
            case ARMOR -> HandlerConfig.EnchantipArmor.containsKey(id);
        };
    }

    private static ItemStack getStack(ServerPlayer player, int slot) {
        Inventory inv = ((InterfacePlayerInventory) player).Xmly_EnchantmentsTooltip$getInv();

        if (slot < 0 || slot >= inv.getContainerSize()) return ItemStack.EMPTY;

        return inv.getItem(slot);
    }

    private static void tipSpecificEnchant(ServerPlayer player, ItemStack stack, ResourceLocation enchantId, TipType type) {

        ListTag enchantList = stack.getEnchantmentTags();
        CompoundTag root = stack.getOrCreateTag();
        ListTag enchantipList;

        if (root.contains(NBT_NAME)) {
            enchantipList = root.getList(NBT_NAME, Tag.TAG_COMPOUND);
        } else {
            enchantipList = new ListTag();
            root.put(NBT_NAME, enchantipList);
        }
        /*
         * 查找目标附魔
         */
        for (int i = 0; i < enchantList.size(); i++) {
            CompoundTag tag = enchantList.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            /*
             * 已经禁用
             */
            for (int j = 0; j < enchantipList.size(); j++) {
                CompoundTag saved = enchantipList.getCompound(j);
                if (saved.getString("id").equals(enchantId.toString())) {
                    restoreDisableEnchant(stack, saved);
                    enchantipList.remove(j);
                    sendMessage(player, enchantId, type, "enabled");
                    return;
                }
            }
            /*
             * 找到正常附魔
             */
            if (id != null && id.equals(enchantId)) {
                short lvl = tag.getShort("lvl");
                /*
                 * 保存状态
                 */
                CompoundTag save = new CompoundTag();
                save.putInt("index", i);
                save.putString("id", enchantId.toString());
                save.putShort("lvl", lvl);
                enchantipList.add(save);
                /*
                 * 替换为disable
                 */
                tag.putString("id", Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.toString());
                tag.putShort("lvl", (short) 0);
                sendMessage(player, id, type, "disabled");
                return;
            }
        }
    }

    private static void restoreDisableEnchant(ItemStack stack, CompoundTag saved) {
        ListTag list = stack.getEnchantmentTags();
        int index = saved.getInt("index");
        /*
         * 第一优先：原位置
         */
        if (index >= 0 && index < list.size()) {
            CompoundTag tag = list.getCompound(index);
            if (Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.equals(ResourceLocation.tryParse(tag.getString("id")))) {
                tag.putString("id", saved.getString("id"));
                tag.putShort("lvl", saved.getShort("lvl"));
                return;
            }
        }
        /*
         * 找不到，清理旧disable
         */
        for (int i = list.size() - 1; i >= 0; i--) {
            if (Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.equals(ResourceLocation.tryParse(list.getCompound(i).getString("id")))) {
                list.remove(i);
            }
        }
        /*
         * 追加恢复
         */
        CompoundTag restore = new CompoundTag();
        restore.putString("id", saved.getString("id"));
        restore.putShort("lvl", saved.getShort("lvl"));
        list.add(restore);
    }

    private static void sendMessage(ServerPlayer player, ResourceLocation id, TipType type, String state) {

        String color = getColor(id, type);

        ServerNetworking.sendToPlayer(new PacketClientMessageDisplay(id, color, state), player);
    }

    private static String getColor(ResourceLocation id, TipType type) {
        if (id == null) return "FFFFFF";

        return switch (type) {
            case HAND, NONE -> HandlerConfig.EnchantipHand.getOrDefault(id, "FFFFFF");
            case HELMET -> HandlerConfig.EnchantipHelmet.getOrDefault(id, "FFFFFF");
            case CHESTPLATE -> HandlerConfig.EnchantipChestplate.getOrDefault(id, "FFFFFF");
            case LEGGINGS -> HandlerConfig.EnchantipLeggings.getOrDefault(id, "FFFFFF");
            case BOOTS -> HandlerConfig.EnchantipBoots.getOrDefault(id, "FFFFFF");
            case ARMOR -> HandlerConfig.EnchantipArmor.getOrDefault(id, "FFFFFF");
        };
    }
}