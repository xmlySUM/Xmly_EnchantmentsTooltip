package com.xmly.enchantip.mixin;

import com.xmly.enchantip.client.implement.OptionalRegistry;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin({ItemStack.class})
public class MixItemStack {
    @Inject(method = "appendEnchantmentNames", at = @At("HEAD"), cancellable = true)
    private static void appendEnchantmentNames(List<Component> p_41710_, ListTag p_41711_, CallbackInfo info) {
        info.cancel();
        new OptionalRegistry(p_41710_, p_41711_).getTooltip();
    }
}
