package home.yura.websearchgui.gui.jfx;

import home.yura.websearchgui.gui.beans.AbstractBean;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine;
import home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.NumberStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.CSS_QUERY_SEARCH;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.EXTRACT_CONTENT;
import static home.yura.websearchgui.util.LocalCollections.index;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * @author yuriy.dunko on 31.03.17.
 */
public class SearchPanelController implements Initializable {
    @FXML
    private TextField nameTextField;
    @FXML
    private TextArea descriptionTextField;
    @FXML
    private TextField urlTextField;
    @FXML
    private TableView<ValueEvaluationDefinitionBean> linkBackTable;
    @FXML
    private TableColumn<ValueEvaluationDefinitionBean, Number> linkBackTableIdColumn;
    @FXML
    private TableColumn<ValueEvaluationDefinitionBean, ValueEvaluationDefinitionType> linkBackTableTypeColumn;
    @FXML
    private TableColumn<ValueEvaluationDefinitionBean, ValueEvaluationDefinitionEngine> linkBackTableEngineColumn;
    @FXML
    private TableColumn<ValueEvaluationDefinitionBean, String> linkBackTableExpressionColumn;
    @FXML
    private TableView<ValueEvaluationDefinitionBean> linkNextTable;
    @FXML
    private TableColumn<ValueEvaluationDefinitionBean, Number> linkNextTableIdColumn;
    @FXML
    private TableColumn<ValueEvaluationDefinitionBean, ValueEvaluationDefinitionType> linkNextTableTypeColumn;
    @FXML
    private TableColumn<ValueEvaluationDefinitionBean, ValueEvaluationDefinitionEngine> linkNextTableEngineColumn;
    @FXML
    private TableColumn<ValueEvaluationDefinitionBean, String> linkNextTableExpressionColumn;

    private static void initTable(final TableColumn<ValueEvaluationDefinitionBean, Number> tableIdColumn,
                                  final TableColumn<ValueEvaluationDefinitionBean, ValueEvaluationDefinitionType> tableTypeColumn,
                                  final TableColumn<ValueEvaluationDefinitionBean, ValueEvaluationDefinitionEngine> tableEngineColumn,
                                  final TableColumn<ValueEvaluationDefinitionBean, String> tableExpressionColumn,
                                  final TableView<ValueEvaluationDefinitionBean> table) {
        tableIdColumn.setCellFactory(param -> new TextFieldTableCell<>(new NumberStringConverter()));
        tableTypeColumn.setCellFactory(param -> new ComboBoxTableCell<>(ValueEvaluationDefinitionType.values()));
        tableEngineColumn.setCellFactory(param -> new ComboBoxTableCell<>(ValueEvaluationDefinitionEngine.values()));
        tableExpressionColumn.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));

        tableIdColumn.setCellValueFactory(param -> new SimpleIntegerProperty(ofNullable(param.getValue().getId()).orElse(0)));
        tableTypeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(ofNullable(param.getValue().getType()).orElse(EXTRACT_CONTENT)));
        tableEngineColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(ofNullable(param.getValue().getEngine()).orElse(CSS_QUERY_SEARCH)));
        tableExpressionColumn.setCellValueFactory(param -> new SimpleStringProperty(ofNullable(param.getValue().getExpression()).orElse("")));

        tableIdColumn.setOnEditCommit(event -> event.getRowValue().setId(event.getNewValue().intValue()));
        tableTypeColumn.setOnEditCommit(event -> event.getRowValue().setType((event.getNewValue())));
        tableEngineColumn.setOnEditCommit(event -> event.getRowValue().setEngine(event.getNewValue()));
        tableExpressionColumn.setOnEditCommit(event -> event.getRowValue().setExpression(event.getNewValue()));

        table.getItems().add(ValueEvaluationDefinitionBean.defaultValue());
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initTable(this.linkBackTableIdColumn, this.linkBackTableTypeColumn, this.linkBackTableEngineColumn,
                this.linkBackTableExpressionColumn, this.linkBackTable);
        initTable(this.linkNextTableIdColumn, this.linkNextTableTypeColumn, this.linkNextTableEngineColumn,
                this.linkNextTableExpressionColumn, this.linkNextTable);
    }

    @FXML
    private void urlPreviewButtonAction(final ActionEvent actionEvent) {
        final WebView web = new WebView();
        web.getEngine().load(this.urlTextField.getText());

        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Web page preview");
        alert.setHeaderText("Start page preview");
        alert.getDialogPane().setContent(web);
        alert.showAndWait();
    }

    @FXML
    private void linkBackBtnAddAction(final ActionEvent actionEvent) {
        this.linkBackTable.getItems().add(ValueEvaluationDefinitionBean.defaultValue());
    }

    @FXML
    private void linkBackBtnDeleteAction(final ActionEvent actionEvent) {
        of(this.linkBackTable.getSelectionModel().getSelectedIndex())
                .filter(integer -> integer >= 0)
                .ifPresent(integer -> this.linkBackTable.getItems().remove((int) integer));

    }

    @FXML
    private void linkNextBtnAddAction(final ActionEvent actionEvent) {
        this.linkNextTable.getItems().add(ValueEvaluationDefinitionBean.defaultValue());
    }

    @FXML
    private void linkNextBtnDeleteAction(final ActionEvent actionEvent) {
        of(this.linkNextTable.getSelectionModel().getSelectedIndex())
                .filter(integer -> integer >= 0)
                .ifPresent(integer -> this.linkNextTable.getItems().remove((int) integer));
    }

    /**
     * @deprecated Use this method only for tests!!!
     */
    @Deprecated
    public static void main(final String... args) throws Exception {
        new JFXPanel();// initializes JavaFX environment
        Platform.runLater(() -> { // FX components need to be managed by JavaFX
            final ResourceBundle resources = ResourceBundle.getBundle("i18n");

            final Dialog<Search> dialog = new Dialog<>();
            dialog.setTitle(resources.getString("search_panel.title.new"));
            dialog.setHeaderText(resources.getString("search_panel.header.new"));
            dialog.setGraphic(new ImageView(SearchPanelController.class.getResource(
                    "/com/sun/javafx/scene/control/skin/modena/HTMLEditor-Background-Color@2x.png").toString()));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.CANCEL);

            final FXMLLoader loader = new FXMLLoader(SearchPanelController.class.getResource("SearchPanel.fxml"), resources);
            try {
                dialog.getDialogPane().setContent(loader.load());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            final SearchPanelController controller = loader.getController();
            dialog.setResultConverter(btn -> btn != ButtonType.YES ? null : Search.create(null,
                    controller.nameTextField.getText(),
                    controller.descriptionTextField.getText(),
                    controller.urlTextField.getText(),
                    index(controller.linkBackTable.getItems().stream().map(AbstractBean::toModel).collect(toList())),
                    index(controller.linkNextTable.getItems().stream().map(AbstractBean::toModel).collect(toList()))));

            final Optional<Search> search = dialog.showAndWait();
            System.out.println(search);
        });
    }
}
