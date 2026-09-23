package dev.reticle.client.gui;

import dev.reticle.client.ReticleClient;
import dev.reticle.config.ReticleConfig;
import dev.reticle.config.ReticlePolicy;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;

public final class ReticleSettingsScreen extends OptionsSubScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public ReticleSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("reticle.options.title"));
    }

    @Override
    protected void addOptions() {
        ReticleConfig config = ReticleClient.config();

        list.addSmall(List.of(
                toggleButton("reticle.options.enabled", "reticle.options.enabled.tooltip",
                        config::enabled, config::setEnabled),
                toggleButton("reticle.options.instruments", null,
                        config::showInstruments, config::setShowInstruments)));

        list.addSmall(List.of(
                toggleButton("reticle.options.flight_path_marker", null,
                        config::showFlightPathMarker, config::setShowFlightPathMarker),
                toggleButton("reticle.options.heading", null,
                        config::showHeading, config::setShowHeading)));

        list.addSmall(List.of(
                toggleButton("reticle.options.target_boxes", null,
                        config::showTargetBoxes, config::setShowTargetBoxes),
                toggleButton("reticle.options.pull_up_warning", null,
                        config::showPullUpWarning, config::setShowPullUpWarning)));

        list.addSmall(List.of(
                toggleButton("reticle.options.pitch_ladder", null,
                        config::showPitchLadder, config::setShowPitchLadder),
                toggleButton("reticle.options.visor", null,
                        config::showVisor, config::setShowVisor)));

        list.addSmall(List.of(
                toggleButton("reticle.options.hide_crosshair", null,
                        config::hideVanillaCrosshair, config::setHideVanillaCrosshair),
                toggleButton("reticle.options.sounds", null,
                        config::soundsEnabled, config::setSoundsEnabled)));

        list.addSmall(List.of(
                toggleButton("reticle.options.night_vision", "reticle.options.night_vision.tooltip",
                        config::showNightVision, config::setShowNightVision)));

        AbstractWidget rangeSlider = new StepSlider(
                "reticle.options.target_range", "reticle.options.target_range.tooltip",
                ReticlePolicy.MIN_TARGET_RANGE, ReticlePolicy.MAX_TARGET_RANGE, 1.0,
                config.targetRange(),
                value -> String.format(Locale.ROOT, "%.0f", value),
                config::setTargetRange);
        AbstractWidget warningSlider = new StepSlider(
                "reticle.options.pull_up_seconds", "reticle.options.pull_up_seconds.tooltip",
                ReticlePolicy.MIN_PULL_UP_SECONDS, ReticlePolicy.MAX_PULL_UP_SECONDS, 0.5,
                config.pullUpWarningSeconds(),
                value -> String.format(Locale.ROOT, "%.1fs", value),
                config::setPullUpWarningSeconds);
        list.addSmall(List.of(rangeSlider, warningSlider));

        list.addSmall(List.of(
                intSlider("reticle.options.alpha", (config.primaryColor() >>> 24) & 0xFF, value -> setChannel(config, 24, value)),
                intSlider("reticle.options.red", (config.primaryColor() >>> 16) & 0xFF, value -> setChannel(config, 16, value))));

        list.addSmall(List.of(
                intSlider("reticle.options.green", (config.primaryColor() >>> 8) & 0xFF, value -> setChannel(config, 8, value)),
                intSlider("reticle.options.blue", config.primaryColor() & 0xFF, value -> setChannel(config, 0, value))));

        list.addSmall(List.of(resetButton(config)));
    }

    @Override
    public void removed() {
        super.removed();
        ReticleClient.saveConfig();
    }

    private AbstractWidget toggleButton(String key, String tooltipKey, BooleanSupplier getter, Consumer<Boolean> setter) {
        Button.Builder builder = Button.builder(onOffLabel(key, getter.getAsBoolean()), button -> {
                    boolean next = !getter.getAsBoolean();
                    setter.accept(next);
                    button.setMessage(onOffLabel(key, next));
                })
                .width(WIDTH);
        if (tooltipKey != null) {
            builder.tooltip(Tooltip.create(Component.translatable(tooltipKey)));
        }
        return builder.build();
    }

    private static Component onOffLabel(String key, boolean value) {
        Component state = Component.translatable(value ? "options.on" : "options.off");
        return Component.translatable("options.generic_value", Component.translatable(key), state);
    }

    private AbstractWidget resetButton(ReticleConfig config) {
        return Button.builder(Component.translatable("reticle.options.reset"), button -> {
                    config.resetToDefaults();
                    rebuildWidgets();
                })
                .width(WIDTH)
                .build();
    }

    private static void setChannel(ReticleConfig config, int shift, int value) {
        int mask = ~(0xFF << shift);
        config.setPrimaryColor((config.primaryColor() & mask) | ((value & 0xFF) << shift));
    }

    private static AbstractWidget intSlider(String captionKey, int initial, IntConsumer onChange) {
        return new StepSlider(
                captionKey,
                "reticle.options.color.tooltip",
                0,
                255,
                1,
                initial,
                value -> Integer.toString((int) value),
                value -> onChange.accept((int) value));
    }

    private static final class StepSlider extends AbstractSliderButton {
        private final String captionKey;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<String> format;
        private final DoubleConsumer onChange;

        StepSlider(
                String captionKey,
                String tooltipKey,
                double min,
                double max,
                double step,
                double initial,
                DoubleFunction<String> format,
                DoubleConsumer onChange) {
            super(0, 0, WIDTH, HEIGHT, Component.empty(), 0.0);
            this.captionKey = captionKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.onChange = onChange;
            this.value = (snap(initial) - min) / (max - min);
            setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
            updateMessage();
        }

        private double snap(double raw) {
            double clamped = Math.max(min, Math.min(max, raw));
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        private double current() {
            return snap(min + value * (max - min));
        }

        @Override
        protected void updateMessage() {
            Component shown = Component.literal(format.apply(current()));
            setMessage(Component.translatable("options.generic_value", Component.translatable(captionKey), shown));
        }

        @Override
        protected void applyValue() {
            double snapped = current();
            value = (snapped - min) / (max - min);
            onChange.accept(snapped);
        }
    }
}
