package com.mathx;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Switches between the three screens by replacing the root of one Scene.
 * Using a single Scene keeps the window size and loads the stylesheet only once.
 */
public final class Navigator {

    private static final String STYLESHEET = "/css/mathx.css";
    private static final String HOME_FXML = "/fxml/home.fxml";
    private static final String CALCULATOR_FXML = "/fxml/calculator.fxml";
    private static final String GRAPH_FXML = "/fxml/graph.fxml";

    private final Scene scene;

    public Navigator(Stage stage) {
        scene = new Scene(new StackPane(), 1000, 720);
        scene.getStylesheets().add(Navigator.class.getResource(STYLESHEET).toExternalForm());

        stage.setTitle("MathX \u2013 Graphing Scientific Calculator");
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

    public void exit() {
        Platform.exit();
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

    /** Every controller receives the Navigator through its constructor. */
    private Object createController(Class<?> type) {
        try {
            return type.getConstructor(Navigator.class).newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create controller " + type.getName(), e);
        }
    }
}
