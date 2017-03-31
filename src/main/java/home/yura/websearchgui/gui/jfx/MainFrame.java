package home.yura.websearchgui.gui.jfx;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 *
 * @author yura
 */
public class MainFrame extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        ListView<String> searchesList = new ListView<>(FXCollections.observableArrayList("Hello world", "Grating sir"));
        StackPane jfxPanel1 = new StackPane();
        jfxPanel1.getChildren().add(searchesList);

        BorderPane jfxPanel2 = new BorderPane();
        WebView webView = new WebView();
        webView.getEngine().load("http://en.wikipedia.org/wiki/Main_Page");
        jfxPanel2.setCenter((webView));

        SplitPane sp = new SplitPane();
        sp.getItems().addAll(jfxPanel1, jfxPanel2);
        sp.setDividerPositions(0.3f, 0.6f, 0.9f);

        stage.setScene(new Scene(sp, 400, 550));
        stage.show();
    }

    public static void main(String[] args) {
        launch(MainFrame.class, args);
    }
}
