package com.xmly.enchantip.client.config;

import com.xmly.enchantip.client.screen.TipType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class EnchantRow extends AbstractWidget {

    private final Screen parent;

    // MC原版16色
    private static final int[] MC_COLORS = {0x000000, 0xFFFFFF, 0xAAAAAA, 0x555555, 0xFF5555, 0x55FF55, 0x5555FF, 0xFFFF55, 0xFF55FF, 0x55FFFF, 0xAA0000, 0x00AA00, 0x0000AA, 0xAA00AA, 0xFFAA00, 0x00AAAA};

    private final EnchantData data;
    private static final int NAME_WIDTH = 300;
    private boolean visible = true;
    // 调色板开启标记
    private boolean paletteOpen = false;

    // 主色块位置与尺寸
    private static final int COLOR_BOX_X = 370;
    private static final int COLOR_BOX_SIZE = 20;

    // 小色板配置：9px方块，间隔1px
    private static final int PALETTE_CELL = 9;
    private static final int PALETTE_GAP = 1;
    private static final int PALETTE_COLS = 8;
    private static final int PALETTE_ROWS = 2;

    public void setVisible(boolean value) {
        this.visible = value;
    }

    public EnchantRow(Screen parent, EnchantData data, int x, int y) {

        super(x, y, 500, 25, Component.empty());

        this.parent = parent;
        this.data = data;
    }

    private void changeType(boolean up) {

        if (up) {
            data.type = switch (data.type) {
                case NONE -> TipType.HAND;
                case HAND -> TipType.HELMET;
                case HELMET -> TipType.CHESTPLATE;
                case CHESTPLATE -> TipType.LEGGINGS;
                case LEGGINGS -> TipType.BOOTS;
                case BOOTS -> TipType.ARMOR;
                case ARMOR -> TipType.NONE;
            };
        } else {
            data.type = switch (data.type) {
                case NONE -> TipType.ARMOR;
                case HAND -> TipType.NONE;
                case HELMET -> TipType.HAND;
                case CHESTPLATE -> TipType.HELMET;
                case LEGGINGS -> TipType.CHESTPLATE;
                case BOOTS -> TipType.LEGGINGS;
                case ARMOR -> TipType.BOOTS;
            };
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        // 调色板开启状态，优先检测色板点击
        if (paletteOpen) {
            if (clickPalette(mouseX, mouseY)) {
                paletteOpen = false;
                return true;
            }
            // 点击调色板以外区域，关闭调色板
            paletteOpen = false;
            return super.mouseClicked(mouseX, mouseY, button);
        }
        //颜色块
        if (isColorBox(mouseX, mouseY)) {
            paletteOpen = !paletteOpen;
            return super.mouseClicked(mouseX, mouseY, button); //测试触发多个色块
        }

        //右键HEX
        if (mouseX >= getX() + 400 && mouseX <= getX() + 500 && mouseY >= getY() && mouseY <= getY() + 25 && button == 1) {
            ColorPickerScreen.open(parent, data);
            return true;
        }

        if (mouseY < 50) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (mouseX > getX() + 250 && mouseX < getX() + 300) {
            switch (button) {
                case 0:
                    // 左键，原有点击逻辑
                    changeType(true);
                    break;
                case 1:
                    // ========== 右键逻辑写这里 ==========
                    changeType(false);
                    break;
                case 2:
                    // 中键
                    data.type = TipType.NONE;
                    break;
            }
        }

        setFocused(true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickPalette(double mx, double my) {

        // 调色板起点：紧贴主色块右侧
        int px = getX() + COLOR_BOX_X + COLOR_BOX_SIZE + PALETTE_GAP;
        int py = getY() + 2;

        for (int i = 0; i < MC_COLORS.length; i++) {

            int x = i % PALETTE_COLS;
            int y = i / PALETTE_COLS;

            int cellX = px + x * (PALETTE_CELL + PALETTE_GAP);
            int cellY = py + y * (PALETTE_CELL + PALETTE_GAP);

            if (mx >= cellX && mx <= cellX + PALETTE_CELL && my >= cellY && my <= cellY + PALETTE_CELL) {
                // 修改颜色
                data.color = String.format("#%06X", MC_COLORS[i]);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput p_259858_) {

    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {

        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();

        //附魔名称
        gui.drawString(mc.font, data.enchantName, getX(), getY() + 8, 0xffffff);

        //栏位
        gui.drawString(mc.font, data.type.getName(), getX() + 250, getY() + 8, 0xffff00);

        int rgb;
        try {
            rgb = Integer.parseInt(data.color.substring(1), 16);
        } catch (Exception e) {
            rgb = 0xffffff;
        }
        //颜色预览块
        gui.fill(getX() + COLOR_BOX_X, getY() + 2, getX() + COLOR_BOX_X + COLOR_BOX_SIZE, getY() + 22, 0xff000000 | rgb);
        // 绘制主20×20颜色方块
        int boxX = getX() + COLOR_BOX_X;
        int boxY = getY() + 2;
        gui.fill(boxX, boxY, boxX + COLOR_BOX_SIZE, boxY + COLOR_BOX_SIZE, 0xff000000 | rgb);

        // ========== 开启调色板时渲染 2行8列小色块 ==========
        if (paletteOpen) {
            int poxX = boxX + COLOR_BOX_SIZE + PALETTE_GAP;

            // 绘制底色背景，防止和背景融合
            int totalW = PALETTE_COLS * (PALETTE_CELL + PALETTE_GAP) - PALETTE_GAP;
            int totalH = PALETTE_ROWS * (PALETTE_CELL + PALETTE_GAP) - PALETTE_GAP;
            gui.fill(poxX - 2, boxY - 2, poxX + totalW + 2, boxY + totalH + 2, 0xCC222222);

            // 循环绘制16个小色块
            for (int i = 0; i < MC_COLORS.length; i++) {
                int col = i % PALETTE_COLS;
                int row = i / PALETTE_COLS;

                int cellX = poxX + col * (PALETTE_CELL + PALETTE_GAP);
                int cellY = boxY + row * (PALETTE_CELL + PALETTE_GAP);
                int color = MC_COLORS[i];

                gui.fill(cellX, cellY, cellX + PALETTE_CELL, cellY + PALETTE_CELL, 0xFF000000 | color);
                gui.renderOutline(cellX, cellY, PALETTE_CELL, PALETTE_CELL, 0xFF888888);
            }
        } else   //HEX文字
            gui.drawString(mc.font, data.color, getX() + 400, getY() + 8, 0xffffff);
    }

    private boolean isColorBox(double x, double y) {
        return x >= getX() + COLOR_BOX_X && x <= getX() + COLOR_BOX_X + 20 && y >= getY() && y <= getY() + 25;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);

        if (isMouseOver(mouseX, mouseY) && mouseX <= getX() + NAME_WIDTH && visible) {
            gui.renderTooltip(Minecraft.getInstance().font, getTooltipLines(), Optional.empty(), mouseX, mouseY);
        } else if (mouseX >= getX() + 400 && mouseX <= getX() + 500 && mouseY >= getY() && mouseY <= getY() + 25) {
            gui.renderTooltip(Minecraft.getInstance().font, List.of(Component.literal("右键更多颜色")), Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> getTooltipLines() {
        List<Component> list = new ArrayList<>();
        // ID
        list.add(Component.literal("ID: ").append(Component.literal(data.id.toString())));

        // NAME
        list.add(data.enchantName);

        return list;
    }

    public void refresh() {
        this.setMessage(Component.empty());
    }
}