package com.mathx.controller;

import com.mathx.Navigator;
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
    private void onExit() {
        navigator.exit();
    }
}
