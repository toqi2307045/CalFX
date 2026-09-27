package com.CalFX.controller;

import com.CalFX.Navigator;
import com.CalFX.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.Map;

/** Lets users choose a preset palette or tune primary/background RGB values. */
public class PersonalizationController {
    private record Palette(String primary, String background) { }

    private static final String CUSTOM = "Custom RGB";
    private static final Map<String, Palette> PRESETS = new LinkedHashMap<>();
    static {
        PRESETS.put("Default - Blue and White", new Palette("#1A56DB", "#F3F7FF"));
        PRESETS.put("Black and Orange", new Palette("#FF9800", "#151515"));
        PRESETS.put("Red and White", new Palette("#D62828", "#FFF7F7"));
        PRESETS.put("Pink and White", new Palette("#E84393", "#FFF5F8"));
        PRESETS.put("Purple and Blue", new Palette("#7B2CBF", "#DCEBFF"));
    }

    private final Navigator navigator;
    private boolean syncingSliders;
    private Color customPrimary;
    private Color customBackground;

    @FXML private ComboBox<String> presetChoice;
    @FXML private Slider primaryRed;
    @FXML private Slider primaryGreen;
    @FXML private Slider primaryBlue;
    @FXML private Slider backgroundRed;
    @FXML private Slider backgroundGreen;
    @FXML private Slider backgroundBlue;
    @FXML private Region primaryPreview;
    @FXML private Region backgroundPreview;
    @FXML private Label primaryHex;
    @FXML private Label backgroundHex;
    @FXML private Label primaryRedValue;
    @FXML private Label primaryGreenValue;
    @FXML private Label primaryBlueValue;
    @FXML private Label backgroundRedValue;
    @FXML private Label backgroundGreenValue;
    @FXML private Label backgroundBlueValue;
    @FXML private Label statusLabel;

    public PersonalizationController(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        presetChoice.getItems().setAll(PRESETS.keySet());
        presetChoice.getItems().add(CUSTOM);
        ThemeManager themes = navigator.getThemeManager();
        customPrimary = themes.getPrimary();
        customBackground = themes.getBackground();
        syncSliders();
        refreshPreview();
        String currentPreset = PRESETS.entrySet().stream()
                .filter(entry -> Color.web(entry.getValue().primary()).equals(customPrimary)
                        && Color.web(entry.getValue().background()).equals(customBackground))
                .map(Map.Entry::getKey).findFirst().orElse(CUSTOM);
        presetChoice.setValue(currentPreset);
        presetChoice.valueProperty().addListener((observable, oldValue, newValue) -> choosePreset(newValue));
        for (Slider slider : new Slider[]{primaryRed, primaryGreen, primaryBlue,
                backgroundRed, backgroundGreen, backgroundBlue}) {
            slider.valueProperty().addListener((observable, oldValue, newValue) -> updateCustomColors());
        }
    }

    @FXML
    private void onHome() {
        navigator.showHome();
    }

    @FXML
    private void onApplyCustom() {
        navigator.getThemeManager().setColors(customPrimary, customBackground);
        statusLabel.setText("Custom colors applied and saved.");
    }

    private void choosePreset(String name) {
        if (name == null || CUSTOM.equals(name)) {
            statusLabel.setText("Adjust the RGB sliders, then apply your colors.");
            return;
        }
        Palette palette = PRESETS.get(name);
        customPrimary = Color.web(palette.primary());
        customBackground = Color.web(palette.background());
        syncSliders();
        refreshPreview();
        navigator.getThemeManager().setColors(customPrimary, customBackground);
        statusLabel.setText(name + " theme applied and saved.");
    }

    private void updateCustomColors() {
        if (syncingSliders) return;
        customPrimary = rgb(primaryRed, primaryGreen, primaryBlue);
        customBackground = rgb(backgroundRed, backgroundGreen, backgroundBlue);
        refreshPreview();
        if (!CUSTOM.equals(presetChoice.getValue())) presetChoice.setValue(CUSTOM);
        statusLabel.setText("Previewing custom colors. Select Apply custom colors to save them.");
    }

    private void syncSliders() {
        syncingSliders = true;
        setSliders(customPrimary, primaryRed, primaryGreen, primaryBlue);
        setSliders(customBackground, backgroundRed, backgroundGreen, backgroundBlue);
        syncingSliders = false;
    }

    private void refreshPreview() {
        String primary = ThemeManager.toHex(customPrimary);
        String background = ThemeManager.toHex(customBackground);
        primaryPreview.setStyle("-fx-background-color: " + primary + "; -fx-background-radius: 8;");
        backgroundPreview.setStyle("-fx-background-color: " + background + "; -fx-background-radius: 8;");
        primaryHex.setText(primary);
        backgroundHex.setText(background);
        primaryRedValue.setText(Integer.toString((int) primaryRed.getValue()));
        primaryGreenValue.setText(Integer.toString((int) primaryGreen.getValue()));
        primaryBlueValue.setText(Integer.toString((int) primaryBlue.getValue()));
        backgroundRedValue.setText(Integer.toString((int) backgroundRed.getValue()));
        backgroundGreenValue.setText(Integer.toString((int) backgroundGreen.getValue()));
        backgroundBlueValue.setText(Integer.toString((int) backgroundBlue.getValue()));
    }

    private static Color rgb(Slider red, Slider green, Slider blue) {
        return Color.rgb((int) red.getValue(), (int) green.getValue(), (int) blue.getValue());
    }

    private static void setSliders(Color color, Slider red, Slider green, Slider blue) {
        red.setValue(Math.round(color.getRed() * 255));
        green.setValue(Math.round(color.getGreen() * 255));
        blue.setValue(Math.round(color.getBlue() * 255));
    }
}
