package com.xmly.enchantip.client.config;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

import static org.joml.Math.clamp;

@OnlyIn(Dist.CLIENT)
public class ColorPickerScreen extends Screen {

    private final Screen parent;
    // 移除全局静态INSTANCE单例，每次新建屏幕杜绝残留递归
    private final EnchantData data;

    private EditBox rBox, gBox, bBox;
    private EditBox hexBox;

    private HSVPicker hsvPicker;
    private RGBSlider rSlider, gSlider, bSlider;
    // 递归锁：防止 syncWidgets 循环触发回调
    private boolean syncLock = false;

    // 每次打开实例
    public static void open(Screen parent, EnchantData data) {
        Minecraft.getInstance().setScreen(new ColorPickerScreen(parent, data));
    }

    public ColorPickerScreen(Screen parent, EnchantData data) {
        super(Component.literal("颜色拾取"));
        this.parent = parent;
        this.data = data;
    }

    @Override
    protected void init() {
        rBox = createNumberBox(width / 2 + 36, 80);
        gBox = createNumberBox(width / 2 + 36, 110);
        bBox = createNumberBox(width / 2 + 36, 140);
        hexBox = new EditBox(font, width / 2 - 50, height - 80, 100, 20, Component.literal("HEX"));

        addRenderableWidget(rBox);
        addRenderableWidget(gBox);
        addRenderableWidget(bBox);
        addRenderableWidget(hexBox);

        hsvPicker = new HSVPicker(width / 2 - 350, 50, 256, 256);
        hsvPicker.setRGB(getColor());

        rSlider = new RGBSlider(width / 2 + 80, 80, 120, 20, "R", this::updateFromSlider);
        gSlider = new RGBSlider(width / 2 + 80, 110, 120, 20, "G", this::updateFromSlider);
        bSlider = new RGBSlider(width / 2 + 80, 140, 120, 20, "B", this::updateFromSlider);
        addRenderableWidget(rSlider);
        addRenderableWidget(gSlider);
        addRenderableWidget(bSlider);

        // HEX输入增加锁避免递归
        hexBox.setResponder(text -> {
            if (syncLock) return;
            String s = text.trim();
            if (s.matches("#?[0-9a-fA-F]{6}")) {
                setHex(s);
            }
        });

        addRenderableWidget(Button.builder(Component.literal("确定"), btn -> onClose()).bounds(width / 2 - 40, height - 40, 80, 20).build());

        syncWidgets();
    }

    private EditBox createNumberBox(int x, int y) {
        EditBox box = new EditBox(font, x, y, 40, 20, Component.empty());
        box.setFilter(s -> s.matches("\\d{0,3}"));
        box.setResponder(text -> {
            if (syncLock) return;
            try {
                int val = Integer.parseInt(text);
                val = clamp(val, 0, 255);
                updateRGB();
            } catch (Exception ignored) {
            }
        });
        return box;
    }

    // RGB输入框更新颜色
    private void updateRGB() {
        if (syncLock) return;
        try {
            int r = Integer.parseInt(rBox.getValue());
            int g = Integer.parseInt(gBox.getValue());
            int b = Integer.parseInt(bBox.getValue());
            setColor(r, g, b);
            syncWidgets();
        } catch (Exception ignored) {
        }
    }

    // Hex赋值
    private void setHex(String value) {
        if (syncLock) return;
        value = value.replace("#", "").toUpperCase(Locale.ROOT);
        if (value.length() != 6) return;
        data.color = "#" + value;
        syncWidgets();
    }

    // 同步所有控件，加锁阻断递归
    private void syncWidgets() {
        if (syncLock || data == null) return;
        syncLock = true;

        int rgb;
        try {
            rgb = Integer.parseInt(data.color.substring(1), 16);
        } catch (Exception e) {
            rgb = 0xFFFFFF;
        }
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        rBox.setValue(String.valueOf(r));
        gBox.setValue(String.valueOf(g));
        bBox.setValue(String.valueOf(b));
        hexBox.setValue(data.color.toUpperCase(Locale.ROOT));

        // 正确设置滑块比例 0~255 → 0.0~1.0
        rSlider.setRGBValue(r);
        gSlider.setRGBValue(g);
        bSlider.setRGBValue(b);
        hsvPicker.setRGB(rgb);

        syncLock = false;
    }

    // 滑块拖动回调
    private void updateFromSlider() {
        if (syncLock) return;
        int r = rSlider.getRGBValue();
        int g = gSlider.getRGBValue();
        int b = bSlider.getRGBValue();
        setColor(r, g, b);
        syncWidgets();
    }

    private void setColor(int r, int g, int b) {
        r = clamp(r, 0, 255);
        g = clamp(g, 0, 255);
        b = clamp(b, 0, 255);
        data.color = String.format("#%02X%02X%02X", r, g, b);
    }

    private int getColor() {
        try {
            return Integer.parseInt(data.color.substring(1), 16);
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partial) {
        renderBackground(gui);
        if (data != null) {
            gui.drawCenteredString(font, data.id.toString(), width / 2, 15, 0xFFFFFF);
            gui.drawCenteredString(font, data.enchantName.copy().withStyle(style -> style.withColor(getColor())), width / 2, 35, getColor());
        }
        // 预览色块
        gui.fill(width / 2 - 25, height / 2, width / 2 + 25, height / 2 + 50, 0xFF000000 | getColor());
        hsvPicker.render(gui);
        super.render(gui, mouseX, mouseY, partial);
    }

    @Override
    public void onClose() {

        if (parent instanceof EnchantConfigScreen screen) {
            screen.refreshRows();
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void removed() {
        hsvPicker.closeTexture();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (hsvPicker.mouseClicked(x, y, button)) {
            updateFromHSV();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (hsvPicker.mouseDragged(mouseX, mouseY, button)) {
            updateFromHSV();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        hsvPicker.mouseReleased();
        return super.mouseReleased(x, y, button);
    }

    private void updateFromHSV() {

        int rgb = hsvPicker.getRGB();

        data.color = String.format("#%06X", rgb);
        syncWidgets();
    }

    // 修复滑块类，不再覆盖父类value，区分滑块比例与RGB数值
    static class RGBSlider extends AbstractSliderButton {
        private final String name;
        private final Runnable callback;

        RGBSlider(int x, int y, int w, int h, String name, Runnable callback) {
            super(x, y, w, h, Component.literal(name), 0.0D);
            this.name = name;
            this.callback = callback;
        }

        // 传入0~255 RGB值，转为滑块0~1D比例
        public void setRGBValue(int rgbVal) {
            this.value = rgbVal / 255.0D;
            updateMessage();
        }

        // 取出滑块当前值转为 0~255 RGB
        public int getRGBValue() {
            return (int) Math.round(this.value * 255);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(name + ": " + getRGBValue()));
        }

        @Override
        protected void applyValue() {
            callback.run();
        }
    }

    static class HSVPicker {
        // 布局常量
        private static final int V_STRIP_WIDTH = 15;
        private static final int GAP = 0;

        private final int x, y;
        private final int hsWidth, hsHeight;
        private final int fullWidth;

        private float hue, saturation, value;
        private boolean draggingHS = false;
        private boolean draggingV = false;

        // 纹理缓存
        private DynamicTexture texture;
        private ResourceLocation texLoc;
        // 标记是否需要重新生成纹理
        private boolean dirty = true;

        public HSVPicker(int x, int y, int hsWidth, int hsHeight) {
            this.x = x;
            this.y = y;
            this.hsWidth = hsWidth;
            this.hsHeight = hsHeight;
            this.fullWidth = hsWidth + GAP + V_STRIP_WIDTH;
            this.value = 1F;
        }

        // 设置HSV并标记纹理脏，下次渲染重建贴图
        public void setHSV(float h, float s, float v) {
            if (this.hue == h && this.saturation == s && this.value == v) return;
            this.hue = h;
            this.saturation = s;
            this.value = v;
            dirty = true;
        }

        private static int hsvToRgbNEW(float h, float s, float v) {
            if (s <= 0F) {
                int gray = (int) (v * 255F + 0.5F);
                return gray << 16 | gray << 8 | gray;
            }
            h = h - (float) Math.floor(h);
            if (h < 0F) h += 1F;

            float h6 = h * 6F;
            int sector = (int) h6;
            float frac = h6 - sector;

            float p = (1F - s);
            float q = (1F - s * frac);
            float t = (1F - s * (1F - frac));

            float r, g, b;
            switch (sector) {
                case 0:
                    r = 1;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = 1;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = 1;
                    b = t;
                    break;
                case 3:
                    r = p;
                    g = q;
                    b = 1;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = 1;
                    break;
                default:
                    r = 1;
                    g = p;
                    b = q;
                    break;
            }

            int ir = (int) (v * 255F * r + 0.5F);
            int ig = (int) (v * 255F * g + 0.5F);
            int ib = (int) (v * 255F * b + 0.5F);
            return (ir << 16) | (ig << 8) | ib;
        }

        private static void rgbToHsvNEW(int r, int g, int b, float[] hsvOut) {
            int max = Math.max(Math.max(r, g), b);
            int min = Math.min(Math.min(r, g), b);
            float delta = max - min;

            float h = 0F, s = 0F, v = max / 255F;

            if (delta > 0) {
                if (max == r) {
                    h = ((g - b) / delta) % 6F;
                } else if (max == g) {
                    h = ((b - r) / delta) + 2F;
                } else {
                    h = ((r - g) / delta) + 4F;
                }
                h /= 6F;
                if (h < 0F) h += 1F;
                s = delta / max;
            }

            hsvOut[0] = h;
            hsvOut[1] = s;
            hsvOut[2] = v;
        }

        public void setRGB(int rgb) {
            float[] hsv = new float[3];
//            FastColor.RGVtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
            rgbToHsvNEW((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, hsv);
            setHSV(hsv[0], hsv[1], hsv[2]);
        }

        public int getRGB() {
            return hsvToRgbNEW(hue, saturation, value) & 0x00FFFFFF;
        }

        // ========== 核心：按需生成纹理 ==========
        private void rebuildTexture() {
            // 整体画布大小：HS区域 + 间隙 + V竖条

            NativeImage image = new NativeImage(fullWidth, hsHeight, false);

            // 1. 绘制HS主色域（左区域）
            for (int px = 0; px < hsWidth; px++) {
                for (int py = 0; py < hsHeight; py++) {
                    float h = 2 / 3F - (px / (float) (hsWidth));
                    float s = 1F - (py / (float) hsHeight);
                    int color = hsvToRgbNEW(h, s, value);
                    image.setPixelRGBA(px, py, 0xFF000000 | color);
                }
            }

            // 2. 绘制右侧 Value 明度竖条
            int vStartX = hsWidth + GAP;
            for (int py = 0; py < hsHeight; py++) {
                float v = 1F - (py / (float) (hsHeight - 1));
                int color = hsvToRgbNEW(2 / 3F - this.hue, this.saturation, v);
                for (int px = 0; px < V_STRIP_WIDTH; px++) {
                    image.setPixelRGBA(vStartX + px, py, 0xFF000000 | color);
                }
            }

            // 销毁旧纹理防止显存泄漏
            if (texture != null) {
                texture.close();
            }
            texture = new DynamicTexture(image);
            texLoc = Minecraft.getInstance().getTextureManager().register("xmly_enchantip/hsv_picker", texture);
            dirty = false;
        }

        // 渲染纹理 + 选取光标
        public void render(GuiGraphics gui) {
            // 只有颜色变动时重建纹理
            if (dirty || texLoc == null) {
                rebuildTexture();
            }

            // 贴图渲染整张HSV面板
            RenderSystem.setShaderTexture(0, texLoc);
            RenderSystem.texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST);
            RenderSystem.texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST);
            gui.blit(texLoc, x, y, fullWidth, hsHeight, 0, 0, fullWidth, hsHeight, fullWidth, hsHeight);

            // ========== 绘制HS选区白色光标 ==========
            int cursorX = x + (int) (hue * hsWidth);
            int cursorY = y + (int) ((1F - saturation) * hsHeight);
            gui.renderOutline(cursorX - 2, cursorY - 2, 5, 5, 0xFFFFFFFF);

            // ========== 绘制Value明度条光标 ==========
            int vBarX = x + hsWidth + GAP;
            int vCursorY = y + (int) ((1F - value) * hsHeight);
            gui.renderOutline(vBarX - 2, vCursorY - 2, V_STRIP_WIDTH + 4, 5, 0xFFFFFFFF);
        }

        // 鼠标按下
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0) return false;
            // HS区域
            if (mx >= x && mx <= x + hsWidth && my >= y && my <= y + hsHeight) {
                draggingHS = true;
                updateHS(mx, my);
                return true;
            }
            // V明度条区域
            int vXStart = x + hsWidth + GAP;
            if (mx >= vXStart && mx <= vXStart + V_STRIP_WIDTH && my >= y && my <= y + hsHeight) {
                draggingV = true;
                updateV(my);
                return true;
            }
            return false;
        }

        // 拖拽
        public boolean mouseDragged(double mx, double my, int button) {
            boolean changed = false;
            if (draggingHS) {
                updateHS(mx, my);
                changed = true;
            }
            if (draggingV) {
                updateV(my);
                changed = true;
            }
            return changed;
        }

        public void mouseReleased() {
            draggingHS = false;
            draggingV = false;
        }

        private void updateHS(double mx, double my) {
            float newH = (float) ((mx - x) / (hsWidth));
            float newS = 1F - clamp((float) ((my - y) / hsHeight), 0F, 1F);
            if (hue != newH || saturation != newS) {
                hue = newH;
                saturation = newS;
                dirty = true;
            }
        }

        private void updateV(double my) {
            float newV = 1F - clamp((float) ((my - y) / (hsHeight - 1)), 0F, 1F);
            if (value != newV) {
                value = newV;
                dirty = true;
            }
        }

        private float clamp(float v, float min, float max) {
            return Math.max(min, Math.min(max, v));
        }

        // 屏幕关闭时释放纹理，避免内存泄漏
        public void closeTexture() {
            if (texture != null) {
                texture.close();
                texture = null;
                texLoc = null;
            }
        }
    }
}