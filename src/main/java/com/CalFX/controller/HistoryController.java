package com.CalFX.controller;

import com.CalFX.Navigator;
import com.CalFX.db.CalculationHistoryStore;
import com.CalFX.db.CalculationRecord;
import javafx.collections.FXCollections;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.scene.control.TableCell;
import javafx.util.Callback;

/** Shows and manages the last 100 saved calculations, newest first. */
public class HistoryController {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private final Navigator navigator;
    private final CalculationHistoryStore historyStore;

    @FXML private TableView<CalculationRecord> table;
    @FXML private TableColumn<CalculationRecord, String> typeColumn;
    @FXML private TableColumn<CalculationRecord, String> expressionColumn;
    @FXML private TableColumn<CalculationRecord, String> resultColumn;
    @FXML private TableColumn<CalculationRecord, String> timeColumn;
    @FXML private TableColumn<CalculationRecord, CalculationRecord> actionsColumn;
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
        actionsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionsColumn.setCellFactory(actionCellFactory());
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

    private Callback<TableColumn<CalculationRecord, CalculationRecord>, TableCell<CalculationRecord, CalculationRecord>> actionCellFactory() {
        return column -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button delete = new Button("Delete");
            private final javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(6, edit, delete);
            {
                edit.getStyleClass().add("history-action-button");
                delete.getStyleClass().add("history-action-button");
                edit.setOnAction(event -> editRecord(getTableView().getItems().get(getIndex())));
                delete.setOnAction(event -> deleteRecord(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(CalculationRecord record, boolean empty) {
                super.updateItem(record, empty);
                setGraphic(empty || record == null ? null : buttons);
            }
        };
    }

    private void editRecord(CalculationRecord record) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit history entry");
        dialog.setHeaderText("Update the saved calculation");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField type = new TextField(record.getType());
        TextField expression = new TextField(record.getExpression());
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.setPadding(new Insets(12));
        fields.addRow(0, new Label("Type"), type);
        fields.addRow(1, new Label("Expression"), expression);
        pane.setContent(fields);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
            if (type.getText().isBlank() || expression.getText().isBlank()) {
                showError("Type and expression cannot be empty.");
                return;
            }
            historyStore.updateAsync(record.getId(), type.getText().trim(), expression.getText().trim(),
                    this::load, error -> showError("Could not update the history entry."));
        });
    }

    private void deleteRecord(CalculationRecord record) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this history entry?", javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);
        confirmation.setTitle("Delete history entry");
        confirmation.setHeaderText(null);
        confirmation.showAndWait().filter(javafx.scene.control.ButtonType.OK::equals).ifPresent(button ->
                historyStore.deleteAsync(record.getId(), this::load,
                        error -> showError("Could not delete the history entry.")));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.show();
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
