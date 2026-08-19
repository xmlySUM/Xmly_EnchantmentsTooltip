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
    private final boolean onEnable;
    private final short lvl;

    public PacketTipSpecificEnchant(int slot, TipType type, ResourceLocation enchantId, short lvl, boolean onEnable) {
        this.slot = slot;
        this.type = type;
        this.enchantId = enchantId;
        this.lvl = lvl;
        this.onEnable = onEnable;
    }

    public PacketTipSpecificEnchant(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
        this.type = TipType.values()[buf.readInt()];
        this.enchantId = ResourceLocation.tryParse(buf.readUtf());
        this.lvl = buf.readShort();
        this.onEnable = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeInt(type.ordinal());
        buf.writeUtf(enchantId.toString());
        buf.writeShort(lvl);
        buf.writeBoolean(onEnable);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {

            ServerPlayer player = context.getSender();

            if (player == null) return;

            ItemStack stack = getStack(player, slot);

            if (stack.isEmpty() || notAllowed(type, enchantId)) return;

            tipSpecificEnchant(player, stack, enchantId, lvl, type, onEnable);

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

    private static boolean notAllowed(TipType type, ResourceLocation id) {
        return !switch (type) {
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

    private static void tipSpecificEnchant(ServerPlayer player, ItemStack stack, ResourceLocation enchantId, short lvl, TipType type, boolean clientOnEnable) {

        ListTag enchantList = stack.getEnchantmentTags();
        CompoundTag root = stack.getOrCreateTag();
        ListTag enchantipList;

        if (root.contains(NBT_NAME, Tag.TAG_LIST)) {
            enchantipList = root.getList(NBT_NAME, Tag.TAG_COMPOUND);
        } else {
            enchantipList = new ListTag();
            root.put(NBT_NAME, enchantipList);
        }

        if (!clientOnEnable) {
            enableEnchant(player, stack, enchantId, lvl, type, enchantList, enchantipList);
        } else {
            disableEnchant(player, stack, enchantId, lvl, type, enchantList, enchantipList);
        }
    }

    private static void disableEnchant(ServerPlayer player, ItemStack stack, ResourceLocation enchantId, short targetLvl, TipType type, ListTag enchantList, ListTag enchantipList) {
        /*
         * 找到 id + lvl 匹配的最前一个正常附魔
         */
        for (int i = 0; i < enchantList.size(); i++) {
            CompoundTag tag = enchantList.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id == null || !id.equals(enchantId)) {
                continue;
            }
            short lvl = tag.getShort("lvl");
            if (lvl != targetLvl) {
                continue;
            }
            CompoundTag save = new CompoundTag();
            save.putInt("index", i);
            save.putString("id", enchantId.toString());
            save.putShort("lvl", lvl);
            enchantipList.add(save);
            tag.putString("id", Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.toString());
            tag.putShort("lvl", (short) 0);
            sendMessage(player, enchantId, type, "disabled");
            return;
        }
        /*
         * 找不到指定等级。
         *
         * 这是为了兼容：
         * - 客户端数据已经过时
         * - 附魔等级发生变化
         * - 某些其它模组修改了 ItemStack
         *
         * 按你的设计，找不到时取最前面的同 ID 附魔。
         */
        for (int i = 0; i < enchantList.size(); i++) {
            CompoundTag tag = enchantList.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id == null || !id.equals(enchantId)) {
                continue;
            }
            short lvl = tag.getShort("lvl");
            CompoundTag save = new CompoundTag();
            save.putInt("index", i);
            save.putString("id", enchantId.toString());
            save.putShort("lvl", lvl);
            enchantipList.add(save);
            tag.putString("id", Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.toString());
            tag.putShort("lvl", (short) 0);
            if (shouldMergeEnchantments()) {
                mergeEnchant(stack, enchantId, false);
            }
            sendMessage(player, enchantId, type, "disabled");
            return;
        }
    }

    private static void enableEnchant(ServerPlayer player, ItemStack stack, ResourceLocation enchantId, short targetLvl, TipType type, ListTag enchantList, ListTag enchantipList) {
        int bestEntry = -1;
        int bestIndex = Integer.MAX_VALUE;
        /*
         * 第一轮：
         * 找 id + lvl 匹配的、index 仍然对应 DISABLED_ENCHANT 的记录。
         */
        for (int i = 0; i < enchantipList.size(); i++) {
            CompoundTag saved = enchantipList.getCompound(i);
            if (!enchantId.toString().equals(saved.getString("id"))) {
                continue;
            }
            if (saved.getShort("lvl") != targetLvl) {
                continue;
            }
            int index = saved.getInt("index");
            if (index < 0 || index >= enchantList.size()) {
                continue;
            }
            CompoundTag current = enchantList.getCompound(index);
            ResourceLocation currentId = ResourceLocation.tryParse(current.getString("id"));
            if (!Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.equals(currentId)) {
                continue;
            }
            if (index < bestIndex) {
                bestIndex = index;
                bestEntry = i;
            }
        }
        /*
         * 第二轮：
         * 如果指定等级找不到，则按照最前面的同 ID 禁用附魔。
         */
        if (bestEntry == -1) {
            bestIndex = Integer.MAX_VALUE;
            for (int i = 0; i < enchantipList.size(); i++) {
                CompoundTag saved = enchantipList.getCompound(i);
                if (!enchantId.toString().equals(saved.getString("id"))) {
                    continue;
                }
                int index = saved.getInt("index");
                if (index < 0 || index >= enchantList.size()) {
                    continue;
                }
                CompoundTag current = enchantList.getCompound(index);
                ResourceLocation currentId = ResourceLocation.tryParse(current.getString("id"));
                if (!Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.equals(currentId)) {
                    continue;
                }
                if (index < bestIndex) {
                    bestIndex = index;
                    bestEntry = i;
                }
            }
        }
        /*
         * 没有可启用的附魔
         */
        if (bestEntry == -1) {
            return;
        }
        CompoundTag saved = enchantipList.getCompound(bestEntry);
        restoreDisableEnchant(stack, saved);
        enchantipList.remove(bestEntry);
        if (shouldMergeEnchantments()) {
            mergeEnchant(stack, enchantId, true);
        }
        sendMessage(player, enchantId, type, "enabled");
    }

    private static void restoreDisableEnchant(ItemStack stack, CompoundTag saved) {
        ListTag list = stack.getEnchantmentTags();
        int index = saved.getInt("index");
        /*
         * 第一优先：原位置
         */
        if (index >= 0 && index < list.size()) {
            CompoundTag tag = list.getCompound(index);
            ResourceLocation currentId = ResourceLocation.tryParse(tag.getString("id"));
            if (Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.equals(currentId)) {
                tag.putString("id", saved.getString("id"));
                tag.putShort("lvl", saved.getShort("lvl"));
                return;
            }
        }
        /*
         * 原位置失效：
         * 不删除其他 disabled，只追加恢复。
         */
        int newIndex = list.size();
        CompoundTag restore = new CompoundTag();
        restore.putString("id", saved.getString("id"));
        restore.putShort("lvl", saved.getShort("lvl"));
        list.add(restore);
        saved.putInt("index", newIndex);
    }

    private static void mergeEnchant(ItemStack stack, ResourceLocation enchantId, boolean enabled) {
        ListTag enchantList = stack.getEnchantmentTags();
        CompoundTag root = stack.getOrCreateTag();
        ListTag enchantipList = root.getList(NBT_NAME, Tag.TAG_COMPOUND);

        List<Integer> levels = new ArrayList<>();

        // 合并前记录最靠前的位置
        int firstIndex = Integer.MAX_VALUE;

        /*
         * 收集正常附魔
         */
        for (int i = 0; i < enchantList.size(); i++) {
            CompoundTag tag = enchantList.getCompound(i);

            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));

            if (!enchantId.equals(id)) {
                continue;
            }

            levels.add(tag.getInt("lvl"));

            // 记录最前的正常附魔位置
            firstIndex = Math.min(firstIndex, i);
        }

        /*
         * 收集禁用附魔
         */
        for (int i = 0; i < enchantipList.size(); i++) {
            CompoundTag tag = enchantipList.getCompound(i);

            if (!enchantId.toString().equals(tag.getString("id"))) {
                continue;
            }

            levels.add(tag.getInt("lvl"));

            /*
             * disabled 保存的 index 是原 enchantList 中的位置，
             * 因此也要参与 firstIndex 的比较。
             */
            int index = tag.getInt("index");

            if (index >= 0) {
                firstIndex = Math.min(firstIndex, index);
            }
        }

        if (levels.isEmpty()) {
            return;
        }

        /*
         * 合并等级
         */
        int mergedLevel = mergeEnchantLevels(levels);

        /*
         * 删除所有同 ID 的正常附魔
         */
        for (int i = enchantList.size() - 1; i >= 0; i--) {
            CompoundTag tag = enchantList.getCompound(i);

            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));

            if (enchantId.equals(id)) {
                enchantList.remove(i);
            }
        }

        /*
         * 删除所有同 ID 的禁用记录
         */
        for (int i = enchantipList.size() - 1; i >= 0; i--) {
            CompoundTag tag = enchantipList.getCompound(i);

            if (enchantId.toString().equals(tag.getString("id"))) {
                enchantipList.remove(i);
            }
        }

        /*
         * 如果之前完全没有有效 index，
         * 才退化到末尾。
         */
        if (firstIndex == Integer.MAX_VALUE) {
            firstIndex = enchantList.size();
        }

        /*
         * 启用状态：
         * 恢复为正常附魔，并放回最前的原位置。
         */
        if (enabled) {
            CompoundTag enchant = new CompoundTag();
            enchant.putString("id", enchantId.toString());
            enchant.putInt("lvl", mergedLevel);

            int insertIndex = Math.min(firstIndex, enchantList.size());
            enchantList.add(insertIndex, enchant);

        } else {
            /*
             * 禁用状态：
             * 正常 enchantList 中没有该附魔，
             * 所以只在 enchantipList 中保存。
             */
            CompoundTag disabled = new CompoundTag();

            disabled.putInt("index", firstIndex);
            disabled.putString("id", enchantId.toString());
            disabled.putInt("lvl", mergedLevel);

            enchantipList.add(disabled);
        }
    }

    private static int mergeEnchantLevels(List<Integer> levels) {
        int[] cnt = new int[256];
        for (int level : levels) {
            level = Math.max(0, Math.min(255, level));
            cnt[level]++;
        }
        for (int level = 0; level < 255; level++) {
            int pair = cnt[level] / 2;

            cnt[level] %= 2;
            cnt[level + 1] += pair;
        }
        for (int level = 255; level >= 0; level--) {
            if (cnt[level] > 0) {
                return level;
            }
        }
        return 0;
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

    private static boolean shouldMergeEnchantments() {
        return !HandlerConfig.enchantip_tooltip.get();
    }
}