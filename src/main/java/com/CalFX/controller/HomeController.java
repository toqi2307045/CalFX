package com.CalFX.controller;

import com.CalFX.Navigator;
import javafx.fxml.FXML;

public class HomeController {

    private final Navigator navigator;

    public HomeController(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onCalculation() {
        navigator.showCalculator();
    }

    @FXML
    private void onGraph() {
        navigator.showGraph();
    }

    @FXML
    private void onCurrency() {
        navigator.showCurrency();
    }

    @FXML
    private void onHistory() {
        navigator.showHistory();
    }

    @FXML
    private void onExit() {
        navigator.exit();
    }
}
