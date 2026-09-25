package com.mathx.controller;

import com.mathx.Navigator;
import com.mathx.db.CalculationHistoryStore;
import com.mathx.db.CalculationRecord;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/** Shows the last 100 saved calculations, newest first. Read-only: nothing is edited here. */
public class HistoryController {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private final Navigator navigator;
    private final CalculationHistoryStore historyStore;

    @FXML private TableView<CalculationRecord> table;
    @FXML private TableColumn<CalculationRecord, String> typeColumn;
    @FXML private TableColumn<CalculationRecord, String> expressionColumn;
    @FXML private TableColumn<CalculationRecord, String> resultColumn;
    @FXML private TableColumn<CalculationRecord, String> timeColumn;
    @FXML private Label statusLabel;

    public HistoryController(Navigator navigator, CalculationHistoryStore historyStore) {
        this.navigator = navigator;
        this.historyStore = historyStore;
    }

    @FXML
    private void initialize() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        expressionColumn.setCellValueFactory(new PropertyValueFactory<>("expression"));
        resultColumn.setCellValueFactory(new PropertyValueFactory<>("result"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        table.setPlaceholder(new Label("No calculations saved yet."));

        load();
    }

    @FXML
    private void onRefresh() {
        load();
    }

    @FXML
    private void onHome() {
        navigator.showHome();
    }

    private void load() {
        statusLabel.pseudoClassStateChanged(ERROR, false);
        statusLabel.setText("Loading\u2026");
        historyStore.loadRecentAsync(
                records -> {
                    table.setItems(FXCollections.observableArrayList(records));
                    statusLabel.setText(records.size() + " of the last " + 100 + " calculations");
                },
                error -> {
                    statusLabel.setText("Could not load the saved calculations.");
                    statusLabel.pseudoClassStateChanged(ERROR, true);
                });
    }
}
