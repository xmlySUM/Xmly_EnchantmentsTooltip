package com.xmly.enchantip.client.config;

import com.xmly.enchantip.client.screen.TipType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnchantData {

    public final ResourceLocation id;

    // 当前配置位置
    public TipType type;

    // #RRGGBB
    public String color;

    // 搜索用
    public final Component enchantName;

    public EnchantData(ResourceLocation id, TipType type, String color) {

        this.id = id;
        this.type = type;
        this.color = color;
        this.enchantName = Component.translatable("enchantment." + id.toLanguageKey());
    }
}