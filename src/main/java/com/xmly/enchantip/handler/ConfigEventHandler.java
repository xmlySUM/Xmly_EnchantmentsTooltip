package com.xmly.enchantip.handler;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Xmly_EnchantmentsTooltip.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigEventHandler {

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {

        Xmly_EnchantmentsTooltip.LOGGER.info(
                "Loading config: {}",
                event.getConfig().getFileName()
        );
        HandlerConfig.reloadFromFile();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {

        Xmly_EnchantmentsTooltip.LOGGER.info(
                "Reload config: {}",
                event.getConfig().getFileName()
        );
        HandlerConfig.reloadFromFile();
    }
}