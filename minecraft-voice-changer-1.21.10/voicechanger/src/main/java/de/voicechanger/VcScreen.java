package de.voicechanger;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Menü „Minecraft Voice Changer“. */
public class VcScreen extends Screen {
    public VcScreen() {
        super(Text.literal("Minecraft Voice Changer"));
    }

    private ButtonWidget btn(String text, int x, int y, int w, int h, ButtonWidget.PressAction action) {
        return ButtonWidget.builder(Text.literal(text), action).dimensions(x, y, w, h).build();
    }

    private static String onOff(String name, boolean on) {
        return name + ": " + (on ? "EIN" : "AUS");
    }

    private void preset(float semitones, float formant) {
        VcConfig c = VcConfig.get();
        c.enabled = true;
        c.semitones = semitones;
        c.formant = formant;
        VcConfig.save();
        clearAndInit();
    }

    @Override
    protected void init() {
        VcConfig c = VcConfig.get();
        int w = Math.min(width - 16, 260);
        int x = (width - w) / 2;
        int y = 28;
        int step = 24;

        addDrawableChild(btn(onOff("Stimmenverzerrer", c.enabled), x, y, w, 20, b -> {
            c.enabled = !c.enabled;
            VcConfig.save();
            b.setMessage(Text.literal(onOff("Stimmenverzerrer", c.enabled)));
        }));
        y += step;

        // Voreinstellungen
        int pw = (w - 4) / 2;
        addDrawableChild(btn("Mädchen leicht", x, y, pw, 20, b -> preset(5f, 1.10f)));
        addDrawableChild(btn("Mädchen", x + pw + 4, y, pw, 20, b -> preset(8f, 1.15f)));
        y += 22;
        addDrawableChild(btn("Mädchen hoch", x, y, pw, 20, b -> preset(10f, 1.20f)));
        addDrawableChild(btn("Kind", x + pw + 4, y, pw, 20, b -> preset(12f, 1.30f)));
        y += step + 4;

        // Regler
        addDrawableChild(new ValueSlider(x, y, w, 20, "Tonhöhe", c.semitones / 14.0,
                v -> "+" + Math.round(v * 14 * 10) / 10.0 + " Halbtöne",
                v -> { c.semitones = (float) (v * 14.0); }));
        y += step;
        addDrawableChild(new ValueSlider(x, y, w, 20, "Stimmfarbe", (c.formant - 0.8) / 0.7,
                v -> "x" + Math.round((0.8 + v * 0.7) * 100) / 100.0,
                v -> { c.formant = (float) (0.8 + v * 0.7); }));
        y += step;
        addDrawableChild(new ValueSlider(x, y, w, 20, "Lautstärke", c.volume - 0.5,
                v -> Math.round((0.5 + v) * 100) + " %",
                v -> { c.volume = (float) (0.5 + v); }));
        y += step;
        addDrawableChild(btn(onOff("Selbst hören (Test)", c.monitor), x, y, w, 20, b -> {
            c.monitor = !c.monitor;
            VcConfig.save();
            b.setMessage(Text.literal(onOff("Selbst hören (Test)", c.monitor)));
        }));
        y += step;
        addDrawableChild(btn("Fertig", x, y, w, 20, b -> close()));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 10, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Zum Testen \"Selbst hören\" nur mit Kopfhörern einschalten."),
                width / 2, height - 14, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        VcConfig.save();
    }
}
