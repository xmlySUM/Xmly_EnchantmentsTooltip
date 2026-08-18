package com.xmly.enchantip.client.config;

import com.xmly.enchantip.Xmly_EnchantmentsTooltip;
import com.xmly.enchantip.client.screen.TipType;
import com.xmly.enchantip.handler.HandlerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class EnchantConfigScreen extends Screen {

    private final Screen parent;
    private EditBox searchBox;
    private static final int ROW_HEIGHT = 35;
    private static final int LIST_TOP = 60;
    private final int listTop = 60;
    private int listBottom;
    private int scrollbarHeight;

    private boolean enableTooltip;
    private final List<EnchantData> allEnchantments = new ArrayList<>();
    private final List<EnchantData> displayEnchantments = new ArrayList<>();
    private final List<EnchantRow> rows = new ArrayList<>();
    private boolean loaded = false;

    // 滚动条拖拽状态
    private boolean draggingScrollBar = false;
    private double dragStartMouseY;
    private double dragStartScrollY;
    // 滚动条边界缓存
    private int scrollBarX;
    private int scrollBarTop;
    private int scrollBarBottom;
    private double scrollY;

    enum SortMode {
        DEFAULT, ID, NAME;

        public Component getName() {
            return switch (this) {
                case DEFAULT -> Component.literal("默认");
                case ID -> Component.literal("ID");
                case NAME -> Component.literal("名称");
            };
        }
    }

    enum FilterMode {
        ALL, HAND, HELMET, CHESTPLATE, LEGGINGS, BOOTS, ARMOR, NONE, ALLSTATE;

        public Component getName() {
            return switch (this) {
                case ALL -> Component.literal("全部");
                case HAND -> Component.literal("主手");
                case HELMET -> Component.literal("头盔");
                case CHESTPLATE -> Component.literal("胸甲");
                case LEGGINGS -> Component.literal("护腿");
                case BOOTS -> Component.literal("靴子");
                case ARMOR -> Component.literal("盔甲");
                case NONE -> Component.literal("未配置");
                case ALLSTATE -> Component.literal("已配置");
            };
        }
    }

    private SortMode sortMode = SortMode.DEFAULT;
    private FilterMode filterMode = FilterMode.ALL;

    public EnchantConfigScreen(Screen parent) {
        super(Component.literal("Xmly's Enchantments Tooltip"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!loaded) {
            loadEnchantments();
        }

        enableTooltip = HandlerConfig.enchantip_tooltip.get();

        listBottom = height - 60;

        searchBox = new EditBox(font, width / 2 - 150, 30, 300, 20, Component.literal("搜索"));
        searchBox.setResponder(s -> refreshList());

        // 更新文字
        Button sortButton = Button.builder(getSortText(), b -> {
            SortMode[] modes = SortMode.values();
            sortMode = modes[(sortMode.ordinal() + 1) % modes.length];
            b.setMessage(getSortText()); // 更新文字
            refreshList();
        }).bounds(width / 2 - 260, 27, 80, 20).build();

        // 更新文字
        Button filterButton = Button.builder(getFilterText(), b -> {
            FilterMode[] values = FilterMode.values();
            filterMode = values[(filterMode.ordinal() + 1) % values.length];
            b.setMessage(getFilterText()); // 更新文字
            refreshList();
        }).bounds(width / 2 + 180, 27, 80, 20).build();

        addRenderableWidget(filterButton);
        addRenderableWidget(sortButton);
        addRenderableWidget(searchBox);

        Button tooltipButton = Button.builder(Component.literal("Tooltip: " + (enableTooltip ? "§aON" : "§cOFF")), b -> {
            enableTooltip = !enableTooltip;
            b.setMessage(Component.literal("Tooltip: " + (enableTooltip ? "§aON" : "§cOFF")));
        }).bounds(width / 2 - 250, height - 35, 70, 20).build();
        addRenderableWidget(tooltipButton);

        Button saveButton = Button.builder(Component.translatable("text.xmlyenchantip.config.save"), b -> save()).bounds(width / 2 - 50, height - 35, 100, 20).build();
        addRenderableWidget(saveButton);

        refreshList();
        loaded = true;
    }

    private void loadEnchantments() {
        allEnchantments.clear();

        /*
         * 读取当前配置
         */
        Map<ResourceLocation, String> map = new HashMap<>();

        map.putAll(HandlerConfig.EnchantipHand);
        map.putAll(HandlerConfig.EnchantipHelmet);
        map.putAll(HandlerConfig.EnchantipChestplate);
        map.putAll(HandlerConfig.EnchantipLeggings);
        map.putAll(HandlerConfig.EnchantipBoots);
        map.putAll(HandlerConfig.EnchantipArmor);

        for (ResourceLocation id : ForgeRegistries.ENCHANTMENTS.getKeys()) {

            TipType type = TipType.NONE;

            String color = "#FFFFFF";

            if (map.containsKey(id)) {
                color = map.get(id);

                if (HandlerConfig.EnchantipHand.containsKey(id)) type = TipType.HAND;
                else if (HandlerConfig.EnchantipHelmet.containsKey(id)) type = TipType.HELMET;
                else if (HandlerConfig.EnchantipChestplate.containsKey(id)) type = TipType.CHESTPLATE;
                else if (HandlerConfig.EnchantipLeggings.containsKey(id)) type = TipType.LEGGINGS;
                else if (HandlerConfig.EnchantipBoots.containsKey(id)) type = TipType.BOOTS;
                else if (HandlerConfig.EnchantipArmor.containsKey(id)) type = TipType.ARMOR;
            }

            allEnchantments.add(new EnchantData(id, type, color));
        }

        Xmly_EnchantmentsTooltip.LOGGER.info("Enchantments Loaded");
    }

    void refreshList() {
        displayEnchantments.clear();

        String key = searchBox == null ? "" : searchBox.getValue().toLowerCase();

        for (EnchantData data : allEnchantments) {

            if (match(data, key) && filter(data)) {
                displayEnchantments.add(data);
            }
        }
        sort();
        scrollY = 0;

        rebuildRows();
    }

    private Component getSortText() {
        return Component.literal("排序:").append(sortMode.getName());
    }

    private Component getFilterText() {
        return Component.literal("筛选:").append(filterMode.getName());
    }

    private boolean filter(EnchantData data) {
        return switch (filterMode) {
            case ALL -> true;
            case HAND -> data.type == TipType.HAND;
            case HELMET -> data.type == TipType.HELMET;
            case CHESTPLATE -> data.type == TipType.CHESTPLATE;
            case LEGGINGS -> data.type == TipType.LEGGINGS;
            case BOOTS -> data.type == TipType.BOOTS;
            case ARMOR -> data.type == TipType.ARMOR;
            case NONE -> data.type == TipType.NONE;
            case ALLSTATE -> data.type != TipType.NONE;
        };
    }

    private void sort() {
        switch (sortMode) {

            case NAME -> displayEnchantments.sort(Comparator.comparing(e -> e.enchantName.getString()));

            case ID -> displayEnchantments.sort(Comparator.comparing(e -> e.id.toString()));

//            case DEFAULT -> displayEnchantments.sort(Comparator.comparingInt(e -> getTypeOrder(e.type)));
            case DEFAULT -> displayEnchantments.sort((a, b) -> {
                int orderA = getTypeOrder(a.type);
                int orderB = getTypeOrder(b.type);
                if (orderA != orderB) {
                    return Integer.compare(orderA, orderB);
                }
                // 同装备类型，再按名称排序
                return a.enchantName.getString().compareTo(b.enchantName.getString());
            });
        }
    }

    private int getTypeOrder(TipType type) {

        return switch (type) {
            case HAND -> 0;
            case HELMET -> 1;
            case CHESTPLATE -> 2;
            case LEGGINGS -> 3;
            case BOOTS -> 4;
            case ARMOR -> 5;
            case NONE -> 6;
        };
    }

    private boolean match(EnchantData data, String text) {
        if (text.isBlank()) return true;

        String namespace = data.id.getNamespace().toLowerCase();

        String id = data.id.toString().toLowerCase();

        String[] words = text.split("\\s+");

        for (String word : words) {
            if (word.startsWith("@")) {
                String mod = word.substring(1);
                if (!namespace.contains(mod)) return false;
            } else if (!id.contains(word) && !data.enchantName.getString().contains(word)) return false;
        }

        return true;
    }

    private void rebuildRows() {
        for (EnchantRow row : rows) {
            removeWidget(row);
        }

        rows.clear();

        int y = LIST_TOP;

        for (EnchantData data : displayEnchantments) {

            EnchantRow row = new EnchantRow(this, data, width / 2 - 250, y);

            rows.add(row);
            addRenderableWidget(row);

            y += ROW_HEIGHT;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY < listTop || mouseY > listBottom) return false;

        int total = displayEnchantments.size() * ROW_HEIGHT;
        int visible = listBottom - listTop;
        int max = Math.max(0, total - visible);

        if (mouseX < 260 + width / 2.0) {
            scrollY -= delta * 15;
        } else {
            scrollY += delta < 0 ? ROW_HEIGHT : -ROW_HEIGHT;
        }
        scrollY = Math.max(0, Math.min(scrollY, max));

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int total = displayEnchantments.size() * ROW_HEIGHT;
        int visible = listBottom - listTop;
        int maxScroll = Math.max(0, total - visible);
        boolean hasScroll = total > visible;

        // 滚动条区域：整条竖轨道（listTop ~ listBottom）x 滚动条宽度
        boolean inScrollTrack = mouseX >= scrollBarX && mouseX <= scrollBarX + 5 && mouseY >= listTop && mouseY <= listBottom;

        if (hasScroll && inScrollTrack) {
            if (button == 0) {
                // 左键按下：开始拖拽
                draggingScrollBar = true;
                dragStartMouseY = mouseY;
                dragStartScrollY = scrollY;
                return true;
            } else if (button == 1) {
                // 右键：点击位置在滑块上方 → 上滚一行；下方 → 下滚一行
                if (mouseY < (scrollBarTop + scrollBarBottom) / 2.0) {
                    scrollY -= ROW_HEIGHT;
                } else {
                    scrollY += ROW_HEIGHT;
                }
                scrollY = Math.max(0, Math.min(scrollY, maxScroll));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int total = displayEnchantments.size() * ROW_HEIGHT;
        int visible = listBottom - listTop;
        int maxScroll = Math.max(0, total - visible);

        if (draggingScrollBar && button == 0 && maxScroll > 0) {
            // 计算鼠标拖动差值，映射成滚动距离
            double deltaY = mouseY - dragStartMouseY;
            double scrollRatio = deltaY / (visible - scrollbarHeight);
            scrollY = dragStartScrollY + scrollRatio * maxScroll;
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollBar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void save() {

        HandlerConfig.enchantip_tooltip.set(enableTooltip);

        HandlerConfig.enchantip_hand.set(export(TipType.HAND));

        HandlerConfig.enchantip_helmet.set(export(TipType.HELMET));

        HandlerConfig.enchantip_chestplate.set(export(TipType.CHESTPLATE));

        HandlerConfig.enchantip_leggings.set(export(TipType.LEGGINGS));

        HandlerConfig.enchantip_boots.set(export(TipType.BOOTS));

        HandlerConfig.enchantip_armor.set(export(TipType.ARMOR));

        HandlerConfig.load();

        Minecraft.getInstance().setScreen(parent);

        loaded = false;

        Xmly_EnchantmentsTooltip.LOGGER.info("Xmly's Enchantments Tooltip 配置文件保存完成。");
    }

    private List<String> export(TipType type) {
        List<String> list = new ArrayList<>();

        for (EnchantData e : allEnchantments) {
            if (e.type == type) {
                list.add(e.id + ";" + e.color);
            }
        }
        return list;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partial) {

        renderBackground(gui);
        gui.fill(width / 2 - 260, listTop - 5, width / 2 + 260, listBottom + 5, 0x88000000);
        gui.hLine(width / 2 - 260, width / 2 + 260, listBottom + 5, 0xffffffff);

        int total = displayEnchantments.size() * ROW_HEIGHT;
        int visible = listBottom - listTop;

        if (total > visible) {
            scrollbarHeight = Math.max(20, visible * visible / total);

            scrollBarX = width / 2 + 265;
            scrollBarTop = listTop + (int) (scrollY / (total - visible) * (visible - scrollbarHeight));
            scrollBarBottom = scrollBarTop + scrollbarHeight;

            gui.fill(scrollBarX, scrollBarTop, scrollBarX + 5, scrollBarBottom, 0xffffffff);
        }

        gui.drawCenteredString(font, title, width / 2, 10, 0xffffff);

//        gui.enableScissor(0, 50, width, height - 50);
        gui.enableScissor(0, listTop, width, listBottom);

        for (int i = 0; i < rows.size(); i++) {
            EnchantRow row = rows.get(i);
            int y = LIST_TOP + i * ROW_HEIGHT - (int) scrollY;
            row.setY(y);

            // if (y + ROW_HEIGHT < listTop || y > listBottom) continue; row.render(gui, mouseX, mouseY, partial);
            row.setVisible(y + 5 > listTop && y + ROW_HEIGHT / 2 < listBottom);
        }

        gui.disableScissor();

        super.render(gui, mouseX, mouseY, partial);
    }

    public void refreshRows() {
        for (EnchantRow row : rows) {
            row.refresh();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return super.isPauseScreen();
    }
}