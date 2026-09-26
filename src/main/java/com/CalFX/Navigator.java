package com.CalFX;

import com.CalFX.currency.ExchangeRateService;
import com.CalFX.db.CalculationHistoryStore;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Switches between screens by replacing the root of one Scene.
 * Using a single Scene keeps the window size and loads the stylesheet only once.
 *
 * Also owns the two things that must outlive any single screen: the history database and the
 * exchange-rate service. Both are created once here and handed to whichever controller asks
 * for them, so re-visiting a screen never starts a second background thread for the same job.
 */
public final class Navigator {

    private static final String STYLESHEET = "/css/CalFX.css";
    private static final String HOME_FXML = "/fxml/home.fxml";
    private static final String CALCULATOR_FXML = "/fxml/calculator.fxml";
    private static final String GRAPH_FXML = "/fxml/graph.fxml";
    private static final String CURRENCY_FXML = "/fxml/currency.fxml";
    private static final String HISTORY_FXML = "/fxml/history.fxml";

    private final Scene scene;
    private final CalculationHistoryStore historyStore = new CalculationHistoryStore();
    private final ExchangeRateService rateService = new ExchangeRateService();

    public Navigator(Stage stage) {
        scene = new Scene(new StackPane(), 1000, 720);
        scene.getStylesheets().add(Navigator.class.getResource(STYLESHEET).toExternalForm());

        stage.setTitle("CalFX \u2013 Graphing Scientific Calculator");
        stage.setMinWidth(900);
        stage.setMinHeight(680);
        stage.setScene(scene);
    }

    public void showHome() {
        show(HOME_FXML);
    }

    public void showCalculator() {
        show(CALCULATOR_FXML);
    }

    public void showGraph() {
        show(GRAPH_FXML);
    }

    public void showCurrency() {
        show(CURRENCY_FXML);
    }

    public void showHistory() {
        show(HISTORY_FXML);
    }

    public void exit() {
        Platform.exit();
    }

    /** Stops the shared background threads. Call once, when the application closes. */
    public void shutdown() {
        historyStore.shutdown();
        rateService.shutdown();
    }

    private void show(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath));
        loader.setControllerFactory(this::createController);
        try {
            scene.setRoot(loader.load());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + fxmlPath, e);
        }
    }

    /**
     * Builds whichever controller the FXML asks for. Tries the most specific constructor first
     * (Navigator, history, rates), then (Navigator, history), then plain (Navigator), so each
     * controller only declares the constructor it actually needs.
     */
    private Object createController(Class<?> type) {
        try {
            return type.getConstructor(Navigator.class, CalculationHistoryStore.class, ExchangeRateService.class)
                    .newInstance(this, historyStore, rateService);
        } catch (NoSuchMethodException ignored) {
            // falls through to the next constructor shape
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create controller " + type.getName(), e);
        }
        try {
            return type.getConstructor(Navigator.class, CalculationHistoryStore.class)
                    .newInstance(this, historyStore);
        } catch (NoSuchMethodException ignored) {
            // falls through to the plain constructor
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create controller " + type.getName(), e);
        }
        try {
            return type.getConstructor(Navigator.class).newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create controller " + type.getName(), e);
        }
    }
}
