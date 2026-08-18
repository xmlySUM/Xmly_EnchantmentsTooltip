package com.xmly.enchantip;

import com.xmly.enchantip.client.screen.EnchantipScreen;
import com.xmly.enchantip.client.screen.TipType;
import com.xmly.enchantip.client.config.EnchantConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public class Xmly_EnchantmentsTooltipClient {
    public static Minecraft client = Minecraft.getInstance();
    public static KeyMapping ENCHANTIP_HAND = new KeyMapping("key." + Xmly_EnchantmentsTooltip.MOD_ID + ".enchantip_hand", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, "key." + Xmly_EnchantmentsTooltip.MOD_ID + "." + "title");
    public static KeyMapping ENCHANTIP_OFFHAND = new KeyMapping("key." + Xmly_EnchantmentsTooltip.MOD_ID + ".enchantip_offhand", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key." + Xmly_EnchantmentsTooltip.MOD_ID + "." + "title");
    public static KeyMapping ENCHANTIP_HELMET = new KeyMapping("key." + Xmly_EnchantmentsTooltip.MOD_ID + ".enchantip_helmet", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_DIVIDE, "key." + Xmly_EnchantmentsTooltip.MOD_ID + "." + "title");
    public static KeyMapping ENCHANTIP_CHESTPLATE = new KeyMapping("key." + Xmly_EnchantmentsTooltip.MOD_ID + ".enchantip_chestplate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_MULTIPLY, "key." + Xmly_EnchantmentsTooltip.MOD_ID + "." + "title");
    public static KeyMapping ENCHANTIP_LEGGINGS = new KeyMapping("key." + Xmly_EnchantmentsTooltip.MOD_ID + ".enchantip_leggings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_SUBTRACT, "key." + Xmly_EnchantmentsTooltip.MOD_ID + "." + "title");
    public static KeyMapping ENCHANTIP_BOOTS = new KeyMapping("key." + Xmly_EnchantmentsTooltip.MOD_ID + ".enchantip_boots", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_ADD, "key." + Xmly_EnchantmentsTooltip.MOD_ID + "." + "title");

    @EventBusSubscriber(modid = Xmly_EnchantmentsTooltip.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void checkKeyboard(InputEvent.Key event) {
            if (client.player != null && client.level != null) {
                if (ENCHANTIP_HAND.consumeClick()) {
                    Minecraft.getInstance().setScreen(new EnchantipScreen(TipType.HAND, "hand_"));
                }
                if (ENCHANTIP_OFFHAND.consumeClick()) {
                    Minecraft.getInstance().setScreen(new EnchantipScreen(TipType.NONE, "offhand_"));
                }
                if (ENCHANTIP_HELMET.consumeClick()) {
                    Minecraft.getInstance().setScreen(new EnchantipScreen(TipType.HELMET, "helmet_"));
                }
                if (ENCHANTIP_CHESTPLATE.consumeClick()) {
                    Minecraft.getInstance().setScreen(new EnchantipScreen(TipType.CHESTPLATE, "chestplate_"));
                }
                if (ENCHANTIP_LEGGINGS.consumeClick()) {
                    Minecraft.getInstance().setScreen(new EnchantipScreen(TipType.LEGGINGS, "leggings_"));
                }
                if (ENCHANTIP_BOOTS.consumeClick()) {
                    Minecraft.getInstance().setScreen(new EnchantipScreen(TipType.BOOTS, "boots_"));
                }
            }
        }
    }

    @EventBusSubscriber(modid = Xmly_EnchantmentsTooltip.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
    public static class ClientProxy {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new EnchantConfigScreen(screen)));
        }

        @SubscribeEvent
        public static void registerKeys(final RegisterKeyMappingsEvent event) {
            event.register(Xmly_EnchantmentsTooltipClient.ENCHANTIP_HAND);
            event.register(Xmly_EnchantmentsTooltipClient.ENCHANTIP_OFFHAND);
            event.register(Xmly_EnchantmentsTooltipClient.ENCHANTIP_HELMET);
            event.register(Xmly_EnchantmentsTooltipClient.ENCHANTIP_CHESTPLATE);
            event.register(Xmly_EnchantmentsTooltipClient.ENCHANTIP_LEGGINGS);
            event.register(Xmly_EnchantmentsTooltipClient.ENCHANTIP_BOOTS);
        }
    }
}
