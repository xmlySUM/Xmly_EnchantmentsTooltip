package com.xmly.enchantip;

import com.xmly.enchantip.command.ServerCommand;
import com.xmly.enchantip.handler.HandlerConfig;
import com.xmly.enchantip.network.ServerNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Xmly_EnchantmentsTooltip.MOD_ID)
public class Xmly_EnchantmentsTooltip {
    public static final String MOD_ID = "xmlyenchantip";
    public static final String MOD_NAME = "Xmly's Enchantments Tooltip";
    public static final String MOD_CONFIG = "-common.toml";
    public static final String NBT_NAME = "XmlyEnchantmentsTip";

    public static final ResourceLocation DISABLED_ENCHANT = new ResourceLocation(Xmly_EnchantmentsTooltip.MOD_ID, "disable");

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Xmly_EnchantmentsTooltip() {
        ServerNetworking.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HandlerConfig.SPEC, MOD_ID + MOD_CONFIG);
        new ServerCommand();
        LOGGER.info("Xmly's Enchantments Tooltip 模组加载完成");
    }
}
