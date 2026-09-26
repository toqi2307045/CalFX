package com.CalFX.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;

/**
 * Controller for MainView.fxml.
 * Owns the header (mode switcher, settings, help) and swaps the content
 * shown in contentArea when the user picks a different mode.
 */
public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    private ToggleButton calculatorModeButton;

    @FXML
    private ToggleButton graphModeButton;

    @FXML
    private ToggleButton numericalModeButton;

    // fx:include with fx:id="calculatorView" auto-injects the included
    // controller as "<fx:id>Controller" — JavaFX does this by convention.
    @FXML
    private CalculatorController calculatorViewController;

    @FXML
    private void handleModeSwitch() {
        if (calculatorModeButton.isSelected()) {
            // Calculator view is already loaded via fx:include.
            // TODO (feature/graph, feature/numerical-methods): swap in
            // GraphView.fxml / NumericalView.fxml here once those exist.
        } else if (graphModeButton.isSelected()) {
            // Placeholder until the Graph mode view is built.
        } else if (numericalModeButton.isSelected()) {
            // Placeholder until the Numerical/Solver mode view is built.
        }
    }

    @FXML
    private void handleSettings() {
        // TODO: open a settings dialog (theme, decimal precision, etc.)
    }

    @FXML
    private void handleHelp() {
        // TODO: open a help view or dialog explaining supported functions.
    }
}
