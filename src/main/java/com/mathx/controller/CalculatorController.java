package com.mathx.controller;

import com.mathx.Navigator;
import com.mathx.calculator.AngleMode;
import com.mathx.calculator.ExpressionBuffer;
import com.mathx.calculator.ExpressionEvaluator;
import com.mathx.calculator.PlaceholderEvaluator;
import com.mathx.calculator.ResultFormatter;
import com.mathx.exception.ExpressionException;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

/** Connects the calculator screen to the calculator classes. No math lives here. */
public class CalculatorController {

    private static final PseudoClass PREVIEW = PseudoClass.getPseudoClass("preview");
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final String TYPEABLE = "0123456789.+^!()%";

    private final Navigator navigator;
    private final ExpressionBuffer buffer = new ExpressionBuffer();
    private final ExpressionEvaluator evaluator = new PlaceholderEvaluator(); // swap in phase 3

    @FXML private BorderPane rootPane;
    @FXML private Label expressionLabel;
    @FXML private Label resultLabel;
    @FXML private ToggleGroup angleGroup;
    @FXML private ToggleButton degreesButton;

    public CalculatorController(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        // a segmented control must always keep one side selected
        angleGroup.selectedToggleProperty().addListener((observable, previous, current) -> {
            if (current == null) {
                previous.setSelected(true);
            } else if (!buffer.isShowingResult()) {
                refresh();
            }
        });

        // keyboard support: the filters see every key, whichever node has focus
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        rootPane.addEventFilter(KeyEvent.KEY_TYPED, this::onKeyTyped);
        rootPane.setFocusTraversable(true);
        Platform.runLater(rootPane::requestFocus);

        refresh();
    }

    // ---------------------------------------------------------------- button handlers

    @FXML
    private void onInsert(ActionEvent event) {
        Button button = (Button) event.getSource();
        Object data = button.getUserData();
        insert(data != null ? data.toString() : button.getText());
    }

    @FXML
    private void onClear() {
        buffer.clear();
        refresh();
    }

    @FXML
    private void onBackspace() {
        buffer.backspace();
        refresh();
    }

    @FXML
    private void onEquals() {
        if (buffer.isEmpty() || buffer.isShowingResult()) {
            return;
        }
        String expression = buffer.text();
        try {
            double value = evaluator.evaluate(expression, 0, selectedMode());
            if (!Double.isFinite(value)) {
                showError("Math error");
                return;
            }
            String result = ResultFormatter.format(value);
            buffer.showResult(result);
            expressionLabel.setText(expression + " =");
            resultLabel.setText(result);
            resultLabel.pseudoClassStateChanged(PREVIEW, false);
            resultLabel.pseudoClassStateChanged(ERROR, false);
        } catch (ExpressionException e) {
            showError("Syntax error");
        }
    }

    @FXML
    private void onHome() {
        navigator.showHome();
    }

    // ---------------------------------------------------------------- keyboard

    private void onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ENTER -> {
                onEquals();
                event.consume();
            }
            case BACK_SPACE -> {
                onBackspace();
                event.consume();
            }
            case ESCAPE, DELETE -> {
                onClear();
                event.consume();
            }
            case SPACE -> event.consume();
            default -> { }
        }
    }

    private void onKeyTyped(KeyEvent event) {
        String typed = event.getCharacter();
        if (typed.equals("=")) {
            onEquals();
            return;
        }
        String token = switch (typed) {
            case "*" -> "\u00D7";
            case "/" -> "\u00F7";
            case "-" -> "\u2212";
            default -> !typed.isEmpty() && TYPEABLE.contains(typed) ? typed : null;
        };
        if (token != null) {
            insert(token);
        }
    }

    // ---------------------------------------------------------------- display

    private void insert(String token) {
        buffer.append(token);
        refresh();
    }

    /** Shows the typed text and, while typing, a live preview of the value. */
    private void refresh() {
        expressionLabel.setText(buffer.text());
        resultLabel.pseudoClassStateChanged(ERROR, false);
        resultLabel.pseudoClassStateChanged(PREVIEW, true);
        if (buffer.isEmpty()) {
            resultLabel.setText("0");
            return;
        }
        try {
            double value = evaluator.evaluate(buffer.text(), 0, selectedMode());
            if (Double.isFinite(value)) {
                resultLabel.setText(ResultFormatter.format(value));
            }
        } catch (ExpressionException ignored) {
            // unfinished input such as "5+": keep the last preview
        }
    }

    private void showError(String message) {
        resultLabel.setText(message);
        resultLabel.pseudoClassStateChanged(PREVIEW, false);
        resultLabel.pseudoClassStateChanged(ERROR, true);
    }

    private AngleMode selectedMode() {
        return degreesButton.isSelected() ? AngleMode.DEGREES : AngleMode.RADIANS;
    }
}
