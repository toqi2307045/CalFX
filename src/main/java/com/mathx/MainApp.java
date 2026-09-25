package com.mathx;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    private Navigator navigator;

    @Override
    public void start(Stage stage) {
        navigator = new Navigator(stage);
        navigator.showHome();
        stage.show();
    }

    /** Called by the JavaFX runtime when the application is closing. */
    @Override
    public void stop() {
        navigator.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
