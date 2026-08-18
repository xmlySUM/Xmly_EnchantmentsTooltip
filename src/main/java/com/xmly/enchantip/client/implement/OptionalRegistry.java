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
            ResourceLocation id = ResourceLocation.tryParse(nbt.getString("id"));

            if (id == null) continue;

            if (id.equals(Xmly_EnchantmentsTooltip.DISABLED_ENCHANT)) continue;

            list.add(Objects.requireNonNull(ForgeRegistries.ENCHANTMENTS.getValue(id)).getFullname(nbt.getInt("lvl")));
        }
    }
}
