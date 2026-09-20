package com.mathx;

import javafx.application.Application;
<<<<<<< HEAD
=======
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
>>>>>>> 33ec5413285c2807519383fde2942eb074fc2d77
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
<<<<<<< HEAD
    public void start(Stage stage) {
        new Navigator(stage).showHome();
        stage.show();
=======
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 640, 480);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("MathX - Graphing Scientific Calculator");
        primaryStage.setScene(scene);
        primaryStage.show();
>>>>>>> 33ec5413285c2807519383fde2942eb074fc2d77
    }

    public static void main(String[] args) {
        launch(args);
    }
}
