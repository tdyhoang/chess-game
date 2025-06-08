package org.group13.chessgame;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("chess-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());

            stage.setMinWidth(1050);
            stage.setMinHeight(700);

            Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

            double initialWidth = Math.min(1100, primaryScreenBounds.getWidth() * 0.85);
            double initialHeight = Math.min(800, primaryScreenBounds.getHeight() * 0.85);

            stage.setWidth(initialWidth);
            stage.setHeight(initialHeight);

            stage.setTitle("JavaFX Chess Game");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}