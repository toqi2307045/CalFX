package com.CalFX.controller;

import com.CalFX.Navigator;
import com.CalFX.calculator.AngleMode;
import com.CalFX.calculator.ExpressionEvaluator;
import com.CalFX.calculator.PlaceholderEvaluator;
import com.CalFX.exception.ExpressionException;
import com.CalFX.graph.GraphPane;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Connects the graph screen to the graph window. Graphs use radians. */
public class GraphController {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final String HINT =
            "Use x as the variable, for example sin(x), x^2 - 4 or 2x + 1. Scroll to zoom, drag to pan.";
    private static final double ZOOM_STEP = 1.25;

    private final Navigator navigator;
    private final ExpressionEvaluator evaluator = new PlaceholderEvaluator(); // swap in phase 3

    @FXML private TextField functionField;
    @FXML private Label messageLabel;
    @FXML private Label coordinatesLabel;
    @FXML private GraphPane graphPane;

    public GraphController(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        coordinatesLabel.textProperty().bind(graphPane.coordinatesTextProperty());
        showMessage(HINT, false);
        Platform.runLater(functionField::requestFocus);
    }

    @FXML
    private void onPlot() {
        String expression = functionField.getText().trim();
        if (expression.isEmpty()) {
            showMessage("Type a function of x first.", true);
            return;
        }
        try {
            evaluator.evaluate(expression, 0, AngleMode.RADIANS); // catches syntax errors before plotting
        } catch (ExpressionException e) {
            showMessage(e.getMessage(), true);
            return;
        }
        // GraphPane calls this lambda on a background thread: it may only use thread-safe objects
        // (the evaluator builds a new parser for every call, so it shares no state)
        graphPane.setFunction(x -> evaluateSafely(expression, x));
        showMessage("Plotting f(x) = " + expression, false);
    }

    @FXML
    private void onClear() {
        functionField.clear();
        graphPane.setFunction(null);
        showMessage(HINT, false);
        functionField.requestFocus();
    }

    @FXML
    private void onZoomIn() {
        graphPane.zoomBy(ZOOM_STEP);
    }

    @FXML
    private void onZoomOut() {
        graphPane.zoomBy(1 / ZOOM_STEP);
    }

    @FXML
    private void onResetView() {
        graphPane.resetView();
    }

    @FXML
    private void onHome() {
        navigator.showHome();
    }

    private double evaluateSafely(String expression, double x) {
        try {
            return evaluator.evaluate(expression, x, AngleMode.RADIANS);
        } catch (ExpressionException e) {
            return Double.NaN;
        }
    }

    private void showMessage(String text, boolean isError) {
        messageLabel.setText(text);
        messageLabel.pseudoClassStateChanged(ERROR, isError);
    }
}
