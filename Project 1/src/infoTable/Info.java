package infoTable;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.lang.*;

/**
 * The main class; loads the GUI file and launches the primary stage
 */
public class Info extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("healthTable.fxml"));
        primaryStage.setTitle("Health Behavior Data in the US");
        primaryStage.setScene(new Scene(root, 960, 540));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

}


