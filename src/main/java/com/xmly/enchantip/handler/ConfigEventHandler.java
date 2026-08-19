package com.xmly.enchantip.handler;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Xmly_EnchantmentsTooltip.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigEventHandler {

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {

        if (event.getConfig().getSpec() != HandlerConfig.SPEC) {
            return;
        }

//        Xmly_EnchantmentsTooltip.LOGGER.info(
//                "Loading config: {}",
//                event.getConfig().getFileName()
//        );
        HandlerConfig.load();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {

        if (event.getConfig().getSpec() != HandlerConfig.SPEC) {
            return;
        }

//        Xmly_EnchantmentsTooltip.LOGGER.info(
//                "Reload config: {}",
//                event.getConfig().getFileName()
//        );
        HandlerConfig.reloadFromFile();
    }
}