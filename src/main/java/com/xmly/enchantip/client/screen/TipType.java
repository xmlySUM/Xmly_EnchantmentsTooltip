package com.xmly.enchantip.client.screen;

import net.minecraft.network.chat.Component;

public enum TipType {

    HAND,

    HELMET,

    CHESTPLATE,

    LEGGINGS,

    BOOTS,

    ARMOR,

    NONE;

    public Component getName() {
        return switch (this) {

            case NONE -> Component.translatable("text.xmlyenchantip.slot.none");

            case HAND -> Component.translatable("text.xmlyenchantip.slot.hand");

            case HELMET -> Component.translatable("text.xmlyenchantip.slot.helmet");

            case CHESTPLATE -> Component.translatable("text.xmlyenchantip.slot.chestplate");

            case LEGGINGS -> Component.translatable("text.xmlyenchantip.slot.leggings");

            case BOOTS -> Component.translatable("text.xmlyenchantip.slot.boots");

            case ARMOR -> Component.translatable("text.xmlyenchantip.slot.armor");
        };
    }
}