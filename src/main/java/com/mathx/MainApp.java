package com.mathx;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        new Navigator(stage).showHome();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
