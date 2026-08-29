package com.xmly.enchantip.client.implement;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;

public class OptionalRegistry {
    public List<Component> list;
    public ListTag tags;

    public OptionalRegistry(List<Component> list, ListTag tags) {
        this.list = list;
        this.tags = tags;
    }

    public void getTooltip() {
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag nbt = tags.getCompound(i);
            String idString = nbt.getString("id");
            if (idString == null || idString.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(idString);
            if (id == null) {
                Xmly_EnchantmentsTooltip.LOGGER.warn("Skipping invalid enchantment ID: {}", idString);
                continue;
            }
            // Enchantip disabled placeholder
            if (Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.equals(id)) {
                continue;
            }
            var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
            if (enchantment == null) {
                Xmly_EnchantmentsTooltip.LOGGER.warn("Skipping unregistered enchantment: {}", id);
                continue;
            }
            list.add(enchantment.getFullname(nbt.getInt("lvl")));
        }
    }
}
