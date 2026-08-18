package com.xmly.enchantip.mixin;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import com.xmly.enchantip.handler.HandlerConfig;
import com.xmly.enchantip.network.packet.PacketClientMessageDisplay.DisplayEnchant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.xmly.enchantip.Xmly_EnchantmentsTooltip.NBT_NAME;

@Mixin({Item.class})
public class MixItem {

    @Inject(method = "appendHoverText", at = @At("HEAD"))
    private void appendHoverText(ItemStack p_41421_, Level p_41422_, List<Component> p_41423_, TooltipFlag p_41424_, CallbackInfo info) {
        ListTag enchantList = p_41421_.getEnchantmentTags();
        List<DisplayEnchant> displayList = new ArrayList<>();

        if (HandlerConfig.enchantip_tooltip.get()) {

            ListTag enchantipList = p_41421_.getOrCreateTag().getList(NBT_NAME, Tag.TAG_COMPOUND);

            //正常附魔
            for (int i = 0; i < enchantList.size(); i++) {

                CompoundTag tag = enchantList.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
                if (id == null) continue;

                if (Xmly_EnchantmentsTooltip.DISABLED_ENCHANT.equals(id)) continue;
                displayList.add(new DisplayEnchant(id, tag.getInt("lvl"), true));
            }

            //禁用附魔
            for (int i = 0; i < enchantipList.size(); i++) {

                CompoundTag tag = enchantipList.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));

                if (id == null) continue;

                displayList.add(new DisplayEnchant(id, tag.getInt("lvl"), false));
            }

            for (DisplayEnchant enchant : displayList) {
                ResourceLocation id = enchant.id;
                String enchantColor = "";

                if (id == null) continue;

                if (enchant.enabled) {
                    if (p_41421_.getItem() instanceof ArmorItem armor) {
                        enchantColor = HandlerConfig.EnchantipArmor.get(id);
                        if (enchantColor == null || enchantColor.isBlank()) {
                            switch (armor.getEquipmentSlot()) {
                                case HEAD -> enchantColor = HandlerConfig.EnchantipHelmet.get(id);
                                case CHEST -> enchantColor = HandlerConfig.EnchantipChestplate.get(id);
                                case LEGS -> enchantColor = HandlerConfig.EnchantipLeggings.get(id);
                                case FEET -> enchantColor = HandlerConfig.EnchantipBoots.get(id);
                                default -> {
                                }
                            }
                        }
                    } else {
                        enchantColor = HandlerConfig.EnchantipHand.get(id);
                    }
                } else { // ========== 禁用附魔规则：不管配置是什么，一律显示 ==========
                    enchantColor = HandlerConfig.EnchantipHand.get(id);
                    if (enchantColor == null || enchantColor.isBlank()) HandlerConfig.EnchantipHelmet.get(id);
                    if (enchantColor == null || enchantColor.isBlank()) HandlerConfig.EnchantipChestplate.get(id);
                    if (enchantColor == null || enchantColor.isBlank()) HandlerConfig.EnchantipLeggings.get(id);
                    if (enchantColor == null || enchantColor.isBlank()) HandlerConfig.EnchantipBoots.get(id);
                    if (enchantColor == null || enchantColor.isBlank()) HandlerConfig.EnchantipArmor.get(id);
                    if (enchantColor == null || enchantColor.isBlank()) { // 禁用附魔没有存颜色，给默认白色
                        enchantColor = "FFFFFF";
                    }
                }

                //不是可切换或禁用附魔
                if (enchantColor == null) continue;

                Component name = Component.translatable("enchantment." + id.toLanguageKey());

                int color = 0xFFFFFF;
                try {
                    color = Color.decode(enchantColor).getRGB();
                } catch (Exception ignored) {
                }
                Style style = Style.EMPTY.withColor(TextColor.fromRgb(color));

                if (enchant.enabled) {
                    p_41423_.add(name.copy().setStyle(style).append(Component.translatable("text.xmlyenchantip.tooltip.enabled")));
                } else {
                    p_41423_.add(name.copy().setStyle(style).append(Component.translatable("text.xmlyenchantip.tooltip.disabled")));
                }
            }
        }
    }
}
