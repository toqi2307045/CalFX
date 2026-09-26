package com.CalFX.controller;

import com.CalFX.Navigator;
import com.CalFX.calculator.ResultFormatter;
import com.CalFX.currency.CurrencyCode;
import com.CalFX.currency.ExchangeRateService;
import com.CalFX.db.CalculationHistoryStore;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;

/**
 * Connects the currency screen to ExchangeRateService (rates) and CalculationHistoryStore
 * (saved conversions). Both do their work on background threads; this controller only
 * touches JavaFX controls, and only from the callbacks those threads hand back.
 */
public class CurrencyController {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

    private final Navigator navigator;
    private final CalculationHistoryStore historyStore;
    private final ExchangeRateService rateService;

    /** Rates for 1 USD, filled in once the first fetch succeeds; null while still loading. */
    private Map<String, Double> latestRates;

    @FXML private ComboBox<CurrencyCode> fromCombo;
    @FXML private ComboBox<CurrencyCode> toCombo;
    @FXML private TextField amountField;
    @FXML private Label resultLabel;
    @FXML private Label statusLabel;

    public CurrencyController(Navigator navigator, CalculationHistoryStore historyStore, ExchangeRateService rateService) {
        this.navigator = navigator;
        this.historyStore = historyStore;
        this.rateService = rateService;
    }

    @FXML
    private void initialize() {
        fromCombo.getItems().setAll(CurrencyCode.values());
        toCombo.getItems().setAll(CurrencyCode.values());
        fromCombo.setValue(CurrencyCode.USD);
        toCombo.setValue(CurrencyCode.BDT);

        loadRates(false);
        Platform.runLater(amountField::requestFocus);
    }

    // ---------------------------------------------------------------- button handlers

    @FXML
    private void onConvert() {
        if (latestRates == null) {
            showStatus("Rates are still loading, please wait a moment.", true);
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException e) {
            showStatus("Enter a valid amount.", true);
            return;
        }

        CurrencyCode from = fromCombo.getValue();
        CurrencyCode to = toCombo.getValue();
        Double fromRate = latestRates.get(from.name());
        Double toRate = latestRates.get(to.name());
        if (fromRate == null || toRate == null) {
            showStatus("No rate available for " + (fromRate == null ? from : to) + ".", true);
            return;
        }

        double converted = amount / fromRate * toRate;
        String formattedResult = ResultFormatter.format(converted) + " " + to.name();
        resultLabel.setText(formattedResult);
        showStatus("1 " + from.name() + " = " + ResultFormatter.format(toRate / fromRate) + " " + to.name(), false);

        String expression = ResultFormatter.format(amount) + " " + from.name() + " \u2192 " + to.name();
        historyStore.insertAsync("Currency", expression, formattedResult);
    }

    @FXML
    private void onSwap() {
        CurrencyCode from = fromCombo.getValue();
        fromCombo.setValue(toCombo.getValue());
        toCombo.setValue(from);
        if (!amountField.getText().isBlank()) {
            onConvert();
        }
    }

    @FXML
    private void onRefreshRates() {
        loadRates(true);
    }

    @FXML
    private void onHome() {
        navigator.showHome();
    }

    // ---------------------------------------------------------------- rate loading

    private void loadRates(boolean forceRefresh) {
        showStatus("Fetching exchange rates\u2026", false);
        rateService.fetchRatesAsync(forceRefresh,
                snapshot -> {
                    latestRates = snapshot.rates();
                    String when = TIME_FORMAT.format(snapshot.fetchedAt());
                    showStatus((snapshot.fromCache() ? "Showing cached rates from " : "Rates updated ") + when, false);
                },
                error -> showStatus("Could not fetch exchange rates. Check your internet connection.", true));
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.pseudoClassStateChanged(ERROR, isError);
    }
}
