package com.mathx.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

/**
 * Controller for CalculatorView.fxml.
 * For now this only builds up the expression string shown on screen —
 * actual evaluation is wired in once the expression parser (Phase 3) exists.
 */
public class CalculatorController {

    @FXML
    private Label expressionLabel;

    @FXML
    private Label resultLabel;

    @FXML
    private ToggleButton degreeButton;

    @FXML
    private ToggleButton radianButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button equalsButton;

    @FXML
    private VBox historyPanel;

    @FXML
    private ListView<String> historyListView;

    @FXML
    private void handleDigitButton(ActionEvent event) {
        appendToExpression(((Button) event.getSource()).getText());
    }

    @FXML
    private void handleOperatorButton(ActionEvent event) {
        appendToExpression(((Button) event.getSource()).getText());
    }

    @FXML
    private void handleFunctionButton(ActionEvent event) {
        // Functions like sin/cos/log open a parenthesis so the user
        // types the argument straight after, e.g. "sin(".
        appendToExpression(((Button) event.getSource()).getText() + "(");
    }

    @FXML
    private void handleConstantButton(ActionEvent event) {
        appendToExpression(((Button) event.getSource()).getText());
    }

    @FXML
    private void handleClear() {
        expressionLabel.setText("");
        resultLabel.setText("0");
    }

    @FXML
    private void handleEquals() {
        // TODO: pass expressionLabel.getText() to the expression parser
        // (Phase 3) and display the evaluated result here.
    }

    @FXML
    private void handleAngleModeSwitch() {
        // TODO: store degree/radian mode so trig functions use it.
    }

    @FXML
    private void handleToggleHistory() {
        historyPanel.setVisible(!historyPanel.isVisible());
        historyPanel.setManaged(historyPanel.isVisible());
    }

    private void appendToExpression(String token) {
        expressionLabel.setText(expressionLabel.getText() + token);
    }
}
