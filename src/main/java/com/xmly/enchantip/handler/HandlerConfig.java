package com.xmly.enchantip.handler;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.xmly.enchantip.Xmly_EnchantmentsTooltip.MOD_CONFIG;
import static com.xmly.enchantip.Xmly_EnchantmentsTooltip.MOD_ID;

public class HandlerConfig {

    public static boolean fy_enchantip_tooltip = true;
    public static List<String> fy_enchantip_hand = Arrays.asList("minecraft:silk_touch;#00AA00", "minecraft:fire_aspect;#FFAA00", "minecraft:flame;#FFAA00");
    public static List<String> fy_enchantip_helmet = new ArrayList<>();
    public static List<String> fy_enchantip_chestplate = new ArrayList<>();
    public static List<String> fy_enchantip_leggings = new ArrayList<>();
    public static List<String> fy_enchantip_boots = List.of("minecraft:frost_walker;#A5F2F3");
    public static List<String> fy_enchantip_armor = List.of("minecraft:protection;#5555FF");

    public static final Builder CONFIG = new Builder();
    public static final ForgeConfigSpec SPEC;

    public static final BooleanValue enchantip_tooltip;
    public static final ConfigValue<List<? extends String>> enchantip_hand;
    public static final ConfigValue<List<? extends String>> enchantip_helmet;
    public static final ConfigValue<List<? extends String>> enchantip_chestplate;
    public static final ConfigValue<List<? extends String>> enchantip_leggings;
    public static final ConfigValue<List<? extends String>> enchantip_boots;
    public static final ConfigValue<List<? extends String>> enchantip_armor;

    public static Map<ResourceLocation, String> EnchantipHand = new HashMap<>();
    public static Map<ResourceLocation, String> EnchantipHelmet = new HashMap<>();
    public static Map<ResourceLocation, String> EnchantipChestplate = new HashMap<>();
    public static Map<ResourceLocation, String> EnchantipLeggings = new HashMap<>();
    public static Map<ResourceLocation, String> EnchantipBoots = new HashMap<>();
    public static Map<ResourceLocation, String> EnchantipArmor = new HashMap<>();

    static {
        CONFIG.push("general");

        enchantip_tooltip = CONFIG.define("is_enable_tooltip", fy_enchantip_tooltip);
        enchantip_hand = CONFIG.defineList("enchantip_hand", fy_enchantip_hand, o -> o instanceof String);
        enchantip_helmet = CONFIG.defineList("enchantip_helmet", fy_enchantip_helmet, o -> o instanceof String);
        enchantip_chestplate = CONFIG.defineList("enchantip_chestplate", fy_enchantip_chestplate, o -> o instanceof String);
        enchantip_leggings = CONFIG.defineList("enchantip_leggings", fy_enchantip_leggings, o -> o instanceof String);
        enchantip_boots = CONFIG.defineList("enchantip_boots", fy_enchantip_boots, o -> o instanceof String);
        enchantip_armor = CONFIG.defineList("enchantip_armor", fy_enchantip_armor, o -> o instanceof String);

        CONFIG.pop();

        SPEC = CONFIG.build();

        // 初始化解析一次
        load();
    }

    public static Map<ResourceLocation, String> parseEnchantments(List<?> list) {
        Map<ResourceLocation, String> result = new HashMap<>();

        for (Object obj : list) {
            if (!(obj instanceof String entry)) {
                Xmly_EnchantmentsTooltip.LOGGER.warn(MOD_ID + " config, Invalid enchantment config entry: {}", obj);
                continue;
            }
            String[] split = entry.split(";");
            if (split.length != 2) {
                Xmly_EnchantmentsTooltip.LOGGER.warn(MOD_ID + " config, Invalid enchantment format: {}", entry);
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(split[0]);
            if (id == null) {
                Xmly_EnchantmentsTooltip.LOGGER.warn(MOD_ID + " config, Invalid enchantment id: {}", split[0]);
                continue;
            }
            String color = split[1];
            if (!color.matches("#[0-9a-fA-F]{6}")) {
                Xmly_EnchantmentsTooltip.LOGGER.warn(MOD_ID + " config, Invalid color: {}", color);
                color = "#FFFFFF";
            }
            result.put(id, color.toUpperCase());
        }
        return result;
    }

    public static String getConfigText(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        append(sb, "主手", EnchantipHand);
        append(sb, "头盔", EnchantipHelmet);
        append(sb, "胸甲", EnchantipChestplate);
        append(sb, "护腿", EnchantipLeggings);
        append(sb, "靴子", EnchantipBoots);
        append(sb, "盔甲", EnchantipArmor);

        return sb.toString();
    }

    private static void append(StringBuilder sb, String name, Map<ResourceLocation, String> map) {
        sb.append("[").append(name).append("]\n");

        if (map.isEmpty()) {
            sb.append("无\n");
            return;
        }
        for (Map.Entry<ResourceLocation, String> e : map.entrySet()) {
            String enchantName = Component.translatable("enchantment." + e.getKey().toLanguageKey()).getString();
            sb.append(enchantName).append(" (").append(e.getKey().toString()).append(") ").append(e.getValue()).append("\n");
        }
    }

    public static void load() {
        Xmly_EnchantmentsTooltip.LOGGER.info("醒目：加载配置");

        EnchantipHand.clear();
        EnchantipHand.putAll(parseEnchantments(enchantip_hand.get()));
        EnchantipHelmet.clear();
        EnchantipHelmet.putAll(parseEnchantments(enchantip_helmet.get()));
        EnchantipChestplate.clear();
        EnchantipChestplate.putAll(parseEnchantments(enchantip_chestplate.get()));
        EnchantipLeggings.clear();
        EnchantipLeggings.putAll(parseEnchantments(enchantip_leggings.get()));
        EnchantipBoots.clear();
        EnchantipBoots.putAll(parseEnchantments(enchantip_boots.get()));
        EnchantipArmor.clear();
        EnchantipArmor.putAll(parseEnchantments(enchantip_armor.get()));
    }

    public static boolean reloadFromFile() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(MOD_ID + MOD_CONFIG);

        // 文件不存在，直接退出，交给 Forge 生成默认配置
        if (!Files.exists(path)) {
            Xmly_EnchantmentsTooltip.LOGGER.info("醒目：" + MOD_ID + " - common.toml 配置文件不存在，跳过手动加载，由Forge初始化");
            return false;
        }
        Xmly_EnchantmentsTooltip.LOGGER.info("醒目：重载配置文件");

        try (CommentedFileConfig config = CommentedFileConfig.builder(path).autosave().build()) {
            config.load();

            enchantip_tooltip.set(config.getOrElse("general.enable_tooltip", true));

            EnchantipHand.clear();
            EnchantipHand.putAll(parseEnchantments(config.getOrElse("general.enchantip_hand", List.of())));
            EnchantipHelmet.clear();
            EnchantipHelmet.putAll(parseEnchantments(config.getOrElse("general.enchantip_helmet", List.of())));
            EnchantipChestplate.clear();
            EnchantipChestplate.putAll(parseEnchantments(config.getOrElse("general.enchantip_chestplate", List.of())));
            EnchantipLeggings.clear();
            EnchantipLeggings.putAll(parseEnchantments(config.getOrElse("general.enchantip_leggings", List.of())));
            EnchantipBoots.clear();
            EnchantipBoots.putAll(parseEnchantments(config.getOrElse("general.enchantip_boots", List.of())));
            EnchantipArmor.clear();
            EnchantipArmor.putAll(parseEnchantments(config.getOrElse("general.enchantip_armor", List.of())));

            return true;
        } catch (Exception e) {
            Xmly_EnchantmentsTooltip.LOGGER.error("读取配置文件: " + MOD_ID + "-common.toml 失败", e);
            return false;
        }
    }
}
