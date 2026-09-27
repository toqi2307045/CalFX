package com.CalFX.controller;

import com.CalFX.Navigator;
import javafx.fxml.FXML;

public class CalculationMenuController {
    private final Navigator navigator;

    public CalculationMenuController(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML private void onHome() { navigator.showHome(); }
    @FXML private void onBasic() { navigator.showCalculator(); }
    @FXML private void onMatrix() { navigator.showCalculationMode("Matrix"); }
    @FXML private void onComplex() { navigator.showCalculationMode("Complex numbers"); }
    @FXML private void onPolynomial() { navigator.showCalculationMode("Polynomial"); }
    @FXML private void onSolver() { navigator.showCalculationMode("Functions solver"); }
    @FXML private void onCalculus() { navigator.showCalculationMode("Differentiation and integration"); }
    @FXML private void onCombinatorics() { navigator.showCalculationMode("Permutations and combinations"); }
    @FXML private void onConstants() { navigator.showCalculationMode("Scientific constants and unit conversion"); }
}
