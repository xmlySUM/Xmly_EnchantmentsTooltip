package com.xmly.enchantip.client.screen;

import com.xmly.enchantip.network.ServerNetworking;
import com.xmly.enchantip.network.packet.PacketRequestEnchantList;
import com.xmly.enchantip.network.packet.PacketSyncEnchantList;
import com.xmly.enchantip.network.packet.PacketTipSpecificEnchant;
import com.xmly.enchantip.Xmly_EnchantmentsTooltipClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class EnchantipScreen extends Screen {
    private final TipType type;

    // 排序模式枚举
    public enum SortMode {
        DEFAULT, BY_ID, BY_NAME, ONLY_ENABLE, ONLY_DISABLE;

        public Component getName() {
            return switch (this) {
                case DEFAULT -> Component.literal("默认");
                case BY_ID -> Component.literal("按ID");
                case BY_NAME -> Component.literal("按名称");
                case ONLY_ENABLE -> Component.literal("仅启用");
                case ONLY_DISABLE -> Component.literal("仅禁用");
            };
        }
    }

    private SortMode currentSort = SortMode.DEFAULT;

    private final List<Button> enchantButtons = new ArrayList<>();

    // 滚动参数
    private double scrollY = 0;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_WIDTH = 200;
    private static final int VIEW_TOP = 60;
    private int viewBottom;
    private Button sortBtn;
    private final Map<Button, Component> buttonTexts = new HashMap<>();

    public EnchantipScreen(TipType type, String title) {
        super(Component.translatable("screen.xmlyenchantip." + title + "enchantip"));
        this.type = type;
    }

    @Override
    protected void init() {
        super.init();
        enchantButtons.clear();
        scrollY = 0;
        viewBottom = this.height - 40;

        // 排序切换按钮 右上角
        sortBtn = Button.builder(Component.literal("排序: ").append(currentSort.getName()), btn -> {
            // 左键：下一个排序
            nextSortMode();
        }).bounds(this.width - 140, 20, 130, 20).build();
        sortBtn.setTooltip(Tooltip.create(Component.literal("左键下一种 / 右键上一种")));
        addRenderableWidget(sortBtn);

        // 打开GUI时请求服务器同步
        ServerNetworking.sendToServer(new PacketRequestEnchantList(type));
        refreshEnchantButtons();
    }

    // 安全刷新按钮
    public void refreshEnchantButtons() {
        // 1. 一次性移除所有附魔按钮（避免遍历中删除）
        for (Button btn : enchantButtons) {
            removeWidget(btn);
        }
        enchantButtons.clear();
        buttonTexts.clear();

        List<PacketSyncEnchantList.Entry> list = new ArrayList<>(PacketSyncEnchantList.enchantments);
        // 根据排序模式过滤+排序
        switch (currentSort) {
            case ONLY_ENABLE:
                list = list.stream().filter(PacketSyncEnchantList.Entry::enabled).collect(Collectors.toList());
                break;
            case ONLY_DISABLE:
                list = list.stream().filter(e -> !e.enabled()).collect(Collectors.toList());
                break;
            case BY_ID:
                list.sort(Comparator.comparing(e -> e.id().toString()));
                break;
            case BY_NAME:
                list.sort((a, b) -> {
                    String nameA = Minecraft.getInstance().font.getSplitter().splitLines(Component.translatable("enchantment." + a.id().getNamespace() + "." + a.id().getPath()), 1000, Style.EMPTY).get(0).getString();
                    String nameB = Minecraft.getInstance().font.getSplitter().splitLines(Component.translatable("enchantment." + b.id().getNamespace() + "." + b.id().getPath()), 1000, Style.EMPTY).get(0).getString();
                    return nameA.compareTo(nameB);
                });
                break;
            case DEFAULT:
            default:
                break;
        }

        // 创建按钮，纵向布局，由 scrollY 控制整体偏移
        int baseX = this.width / 2 - (BUTTON_WIDTH / 2);
        int startY = VIEW_TOP;

        for (PacketSyncEnchantList.Entry entry : list) {
            Button btn = Button.builder(Component.empty(), b -> enchantip(entry)).bounds(baseX, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            // 自定义渲染实现文字左对齐 btn.setMessage(getButtonText(entry));
            buttonTexts.put(btn, getButtonText(entry));
            enchantButtons.add(btn);
            addRenderableWidget(btn);
            startY += BUTTON_HEIGHT;
        }
    }

    // 切换下一个排序
    private void nextSortMode() {
        SortMode[] modes = SortMode.values();
        int idx = (currentSort.ordinal() + 1) % modes.length;
        currentSort = modes[idx];
        sortBtn.setMessage(Component.literal("排序: ").append(currentSort.getName()));
        refreshEnchantButtons();
    }

    // 上一个排序（右键调用）
    private void prevSortMode() {
        SortMode[] modes = SortMode.values();
        int idx = (currentSort.ordinal() - 1 + modes.length) % modes.length;
        currentSort = modes[idx];
        sortBtn.setMessage(Component.literal("排序: ").append(currentSort.getName()));
        refreshEnchantButtons();
    }

    private Component getButtonText(PacketSyncEnchantList.Entry entry) {
        String state = entry.enabled() ? "§aON" : "§cOFF";
        ResourceLocation id = entry.id();
        Component enchantName = Component.translatable("enchantment." + id.getNamespace() + "." + id.getPath());
        return Component.literal(state + " ").append(enchantName).append(Component.literal(" Lv." + entry.level() + " Slot." + entry.slot()));
    }

    private void enchantip(PacketSyncEnchantList.Entry entry) {
        ServerNetworking.sendToServer(new PacketTipSpecificEnchant(entry.slot(), type, entry.id()));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 22, 0xFFFFFF);

        // 滚动裁剪区域：只在 VIEW_TOP ~ viewBottom 之间渲染附魔按钮
        graphics.enableScissor(0, VIEW_TOP, width, viewBottom);
        int baseX = width / 2 - BUTTON_WIDTH / 2;
        int yOffset = VIEW_TOP - (int) scrollY;

        for (Button btn : enchantButtons) {
            btn.setY(yOffset);
            // 自定义绘制按钮文字：左对齐
            btn.render(graphics, mouseX, mouseY, partialTick);
            // BUTTON_HEIGHT=24，字体默认高度约9，垂直居中公式：y + (按钮高度-字体高度)/2
            graphics.drawString(font, buttonTexts.get(btn), baseX + 6, yOffset + (BUTTON_HEIGHT - font.lineHeight) / 2, 0xffffff);
            yOffset += BUTTON_HEIGHT;
        }
        graphics.disableScissor();

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // 鼠标滚轮滚动
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int totalHeight = enchantButtons.size() * BUTTON_HEIGHT;
        int visibleHeight = viewBottom - VIEW_TOP;
        if (totalHeight > visibleHeight) {
            double maxScroll = totalHeight - visibleHeight;
            scrollY += scrollDelta * 8;
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
        }
        return true;
    }

    // 排序按钮右键 = 上一种排序
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && sortBtn.isMouseOver(mouseX, mouseY)) {
            prevSortMode();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // 监听按键：E物品栏键、绑定快捷键、ESC 都关闭界面
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        // ESC
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        // E 物品栏按键
        if (mc.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        // 注册的快捷键
        if (Xmly_EnchantmentsTooltipClient.ENCHANTIP_HAND.matches(keyCode, scanCode) || Xmly_EnchantmentsTooltipClient.ENCHANTIP_OFFHAND.matches(keyCode, scanCode) || Xmly_EnchantmentsTooltipClient.ENCHANTIP_HELMET.matches(keyCode, scanCode) || Xmly_EnchantmentsTooltipClient.ENCHANTIP_CHESTPLATE.matches(keyCode, scanCode) || Xmly_EnchantmentsTooltipClient.ENCHANTIP_LEGGINGS.matches(keyCode, scanCode) || Xmly_EnchantmentsTooltipClient.ENCHANTIP_BOOTS.matches(keyCode, scanCode)) {

            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void reloadButtons() {
        refreshEnchantButtons();
    }
}