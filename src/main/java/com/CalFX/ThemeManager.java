package com.CalFX;

import javafx.scene.Parent;
import javafx.scene.paint.Color;

import java.util.prefs.Preferences;

/** Stores the selected theme and exposes its colors as stylesheet variables. */
public final class ThemeManager {
    private static final Color DEFAULT_PRIMARY = Color.web("#1A56DB");
    private static final Color DEFAULT_BACKGROUND = Color.web("#F3F7FF");
    private final Preferences preferences = Preferences.userNodeForPackage(ThemeManager.class);
    private final Navigator navigator;
    private Color primary;
    private Color background;

    public ThemeManager(Navigator navigator) {
        this.navigator = navigator;
        primary = readColor("primary", DEFAULT_PRIMARY);
        background = readColor("background", DEFAULT_BACKGROUND);
    }

    public Color getPrimary() { return primary; }
    public Color getBackground() { return background; }

    public void setColors(Color primary, Color background) {
        this.primary = primary;
        this.background = background;
        preferences.put("primary", toHex(primary));
        preferences.put("background", toHex(background));
        navigator.applyCurrentTheme();
    }

    public void apply(Parent root) {
        boolean darkBackground = luminance(background) < 0.45;
        Color text = darkBackground ? Color.web("#F5F5F5") : Color.web("#172033");
        Color surface = darkBackground ? background.interpolate(Color.BLACK, 0.12)
                : background.interpolate(Color.WHITE, 0.45);
        Color muted = text.interpolate(background, 0.38);
        Color border = text.interpolate(background, 0.78);
        Color primarySoft = background.interpolate(primary, 0.12);
        Color primaryTint = background.interpolate(primary, 0.22);
        String variables = "-mx-primary: " + toHex(primary) + ";"
                + "-mx-primary-dark: " + toHex(primary.interpolate(Color.BLACK, 0.20)) + ";"
                + "-mx-primary-deep: " + toHex(primary.interpolate(Color.BLACK, 0.42)) + ";"
                + "-mx-primary-soft: " + toHex(primarySoft) + ";"
                + "-mx-primary-tint: " + toHex(primaryTint) + ";"
                + "-mx-bg: " + toHex(background) + ";"
                + "-mx-text: " + toHex(text) + ";"
                + "-mx-muted: " + toHex(muted) + ";"
                + "-mx-border: " + toHex(border) + ";"
                + "-mx-surface: " + toHex(surface) + ";"
                + "-mx-on-primary: " + toHex(contrastColor(primary)) + ";"
                + "-mx-on-surface: " + toHex(contrastColor(surface)) + ";";
        root.setStyle(variables);
    }

    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }

    private Color readColor(String key, Color fallback) {
        try {
            return Color.web(preferences.get(key, toHex(fallback)));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static Color contrastColor(Color color) {
        return luminance(color) < 0.5 ? Color.WHITE : Color.web("#171717");
    }

    private static double luminance(Color color) {
        return 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
    }
}
