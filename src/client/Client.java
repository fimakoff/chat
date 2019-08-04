package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Client extends Application {

    private final int timeToLogin = 120;
    private final int width = 600;
    private final int height = 275;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("client.fxml"));
        primaryStage.getIcons().add(new Image("file:img/icon.png"));
        Controller ct = new Controller();
        ct.timeOut(timeToLogin);
        primaryStage.setTitle("Client");
        primaryStage.setScene(new Scene(root, width, height));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}