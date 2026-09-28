package com.CalFX.controller;

import com.CalFX.Navigator;
import com.CalFX.calculator.AngleMode;
import com.CalFX.calculator.ExpressionEvaluator;
import com.CalFX.calculator.PlaceholderEvaluator;
import com.CalFX.db.CalculationHistoryStore;
import com.CalFX.exception.ExpressionException;
import com.CalFX.graph.GraphPane;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Connects the graph screen to the graph window. Graphs use radians. */
public class GraphController {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final String HINT = "Choose a plot type and enter an expression. Scroll to zoom, drag to pan.";
    private static final double ZOOM_STEP = 1.25;

    private final Navigator navigator;
    private final CalculationHistoryStore historyStore;
    private final ExpressionEvaluator evaluator = new PlaceholderEvaluator(); // swap in phase 3

    @FXML private TextField functionField;
    @FXML private TextField secondaryFunctionField;
    @FXML private TextField rangeMinField;
    @FXML private TextField rangeMaxField;
    @FXML private ComboBox<PlotType> plotTypeBox;
    @FXML private Label expressionLabel;
    @FXML private Label secondaryExpressionLabel;
    @FXML private Label rangeLabel;
    @FXML private HBox secondaryInputRow;
    @FXML private Label messageLabel;
    @FXML private Label coordinatesLabel;
    @FXML private StackPane graphContainer;
    private GraphPane graphPane;

    public GraphController(Navigator navigator, CalculationHistoryStore historyStore) {
        this.navigator = navigator;
        this.historyStore = historyStore;
    }

    @FXML
    private void initialize() {
        graphPane = new GraphPane();
        graphContainer.getChildren().setAll(graphPane);
        coordinatesLabel.textProperty().bind(graphPane.coordinatesTextProperty());
        showMessage(HINT, false);
        plotTypeBox.getItems().setAll(PlotType.values());
        plotTypeBox.getSelectionModel().select(PlotType.EXPLICIT);
        Platform.runLater(functionField::requestFocus);
    }

    @FXML
    private void onPlotTypeChanged() {
        PlotType type = plotTypeBox.getValue();
        if (type == null) return;
        boolean twoExpressions = type == PlotType.PARAMETRIC;
        secondaryInputRow.setVisible(type == PlotType.PARAMETRIC || type == PlotType.POLAR);
        secondaryInputRow.setManaged(type == PlotType.PARAMETRIC || type == PlotType.POLAR);
        secondaryFunctionField.setVisible(twoExpressions);
        secondaryFunctionField.setManaged(twoExpressions);
        secondaryExpressionLabel.setVisible(twoExpressions);
        secondaryExpressionLabel.setManaged(twoExpressions);
        rangeLabel.setText(type == PlotType.POLAR ? "theta range" : "t range");
        expressionLabel.setText(switch (type) {
            case EXPLICIT -> "f(x) =";
            case PARAMETRIC -> "x(t) =";
            case POLAR -> "r(theta) =";
            case IMPLICIT -> "f(x,y) = 0:";
        });
        functionField.setPromptText(switch (type) {
            case EXPLICIT -> "e.g. sin(x) + x^2 / 5";
            case PARAMETRIC -> "e.g. cos(t)";
            case POLAR -> "e.g. 2 + cos(3*theta)";
            case IMPLICIT -> "e.g. x^2 + y^2 - 9";
        });
        secondaryFunctionField.setPromptText("e.g. sin(t)");
        if (type == PlotType.POLAR) {
            rangeMinField.setText("0");
            rangeMaxField.setText("6.283185");
        } else if (type == PlotType.PARAMETRIC) {
            rangeMinField.setText("-10");
            rangeMaxField.setText("10");
        }
        showMessage(HINT, false);
    }

    @FXML
    private void onPlot() {
        String expression = functionField.getText().trim();
        if (expression.isEmpty()) {
            showMessage("Enter an expression first.", true);
            return;
        }
        PlotType type = plotTypeBox.getValue();
        String historyExpression = "";
        try {
            switch (type) {
                case EXPLICIT -> {
                    evaluator.evaluate(expression, 0, AngleMode.RADIANS);
                    graphPane.setFunction(x -> evaluateSafely(expression, x));
                    historyExpression = "y = " + expression;
                }
                case PARAMETRIC -> {
                    String yExpression = secondaryFunctionField.getText().trim();
                    if (yExpression.isEmpty()) throw new ExpressionException("Enter both x(t) and y(t).");
                    double[] range = readRange();
                    String xFormula = asXVariable(expression);
                    String yFormula = asXVariable(yExpression);
                    evaluator.evaluate(xFormula, 0, AngleMode.RADIANS);
                    evaluator.evaluate(yFormula, 0, AngleMode.RADIANS);
                    graphPane.setParametricCurve(t -> evaluateSafely(xFormula, t),
                            t -> evaluateSafely(yFormula, t), range[0], range[1]);
                    historyExpression = "x(t) = " + expression + ", y(t) = " + yExpression
                            + "; t = [" + range[0] + ", " + range[1] + "]";
                }
                case POLAR -> {
                    double[] range = readRange();
                    String radius = expression.replaceAll("(?i)theta", "x");
                    evaluator.evaluate(radius, 0, AngleMode.RADIANS);
                    graphPane.setParametricCurve(t -> evaluateSafely(radius, t) * Math.cos(t),
                            t -> evaluateSafely(radius, t) * Math.sin(t), range[0], range[1]);
                    historyExpression = "r(theta) = " + expression + "; theta = [" + range[0] + ", " + range[1] + "]";
                }
                case IMPLICIT -> {
                    evaluator.evaluate(expression, 0, 0, AngleMode.RADIANS);
                    graphPane.setImplicitFunction((x, y) -> evaluateSafely(expression, x, y));
                    historyExpression = expression + " = 0";
                }
            }
        } catch (ExpressionException | IllegalArgumentException e) {
            showMessage(e.getMessage(), true);
            return;
        }
        historyStore.insertAsync("Graph - " + type.description, historyExpression, "Graph plotted");
        showMessage("Plotting " + type.description + ": " + expression, false);
    }

    @FXML
    private void onClear() {
        functionField.clear();
        secondaryFunctionField.clear();
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
    private void onZoomXIn() {
        graphPane.zoomXAxis(ZOOM_STEP);
    }

    @FXML
    private void onZoomXOut() {
        graphPane.zoomXAxis(1 / ZOOM_STEP);
    }

    @FXML
    private void onZoomYIn() {
        graphPane.zoomYAxis(ZOOM_STEP);
    }

    @FXML
    private void onZoomYOut() {
        graphPane.zoomYAxis(1 / ZOOM_STEP);
    }

    @FXML
    private void onResetView() {
        graphPane.resetView();
    }

    @FXML
    private void onHome() {
        navigator.showHome();
    }

    @FXML
    private void onHistory() {
        navigator.showHistory();
    }

    private double evaluateSafely(String expression, double x) {
        try {
            return evaluator.evaluate(expression, x, AngleMode.RADIANS);
        } catch (ExpressionException e) {
            return Double.NaN;
        }
    }

    private double evaluateSafely(String expression, double x, double y) {
        try {
            return evaluator.evaluate(expression, x, y, AngleMode.RADIANS);
        } catch (ExpressionException e) {
            return Double.NaN;
        }
    }

    private double[] readRange() {
        try {
            double min = Double.parseDouble(rangeMinField.getText().trim());
            double max = Double.parseDouble(rangeMaxField.getText().trim());
            if (!Double.isFinite(min) || !Double.isFinite(max) || min >= max) throw new NumberFormatException();
            return new double[]{min, max};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid range where the minimum is less than the maximum.");
        }
    }

    private static String asXVariable(String expression) {
        return expression.replaceAll("(?i)\\bt\\b", "x");
    }

    private enum PlotType {
        EXPLICIT("Explicit y=f(x)"), PARAMETRIC("Parametric x(t), y(t)"),
        POLAR("Polar r(theta)"), IMPLICIT("Implicit f(x,y)=0");
        private final String description;
        PlotType(String description) { this.description = description; }
        @Override public String toString() { return description; }
    }

    private void showMessage(String text, boolean isError) {
        messageLabel.setText(text);
        messageLabel.pseudoClassStateChanged(ERROR, isError);
    }
}
