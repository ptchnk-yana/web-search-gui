package home.yura.websearchgui.gui.jfx;

import home.yura.websearchgui.gui.JfxUtils;
import home.yura.websearchgui.model.AbstractModel;
import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.FilterItem.FilterEngine;
import home.yura.websearchgui.model.FilterItem.FilterLocation;
import home.yura.websearchgui.model.FilterItem.FilterPreFormatting;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.util.LocalFunctions;
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
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.NumberStringConverter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * @author yuriy.dunko on 01.04.17.
 */
public class FilterPanelController implements Initializable {
    private Integer filterId = null;

    @FXML
    private Label idLbl;
    @FXML
    private ChoiceBox<Search> searchesChoiceBox;
    @FXML
    private TextField nameTxtField;
    @FXML
    private TextArea descriptionTxtArea;
    @FXML
    private TableView<FilterItemBean> itemsTbl;
    @FXML
    private TableColumn<FilterItemBean, Number> itemsIdClmn;
    @FXML
    private TableColumn<FilterItemBean, Number> itemsFilterIdClmn;
    @FXML
    private TableColumn<FilterItemBean, FilterLocation> itemsFLocationClmn;
    @FXML
    private TableColumn<FilterItemBean, FilterEngine> itemsFEngineClmn;
    @FXML
    private TableColumn<FilterItemBean, FilterPreFormatting> itemsFPreFormattingClmn;
    @FXML
    private TableColumn<FilterItemBean, String> itemsExpressionClmn;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.itemsIdClmn.setCellFactory(param -> new TextFieldTableCell<>(new NumberStringConverter()));
        this.itemsFilterIdClmn.setCellFactory(param -> new TextFieldTableCell<>(new NumberStringConverter()));
        this.itemsFLocationClmn.setCellFactory(param -> new ComboBoxTableCell<>(FilterLocation.values()));
        this.itemsFEngineClmn.setCellFactory(param -> new ComboBoxTableCell<>(FilterEngine.values()));
        this.itemsFPreFormattingClmn.setCellFactory(param -> new ComboBoxTableCell<>(FilterPreFormatting.values()));
        this.itemsExpressionClmn.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));

        this.itemsIdClmn.setCellValueFactory(param -> new SimpleIntegerProperty(ofNullable(param.getValue().getId()).orElse(0)));
        this.itemsFilterIdClmn.setCellValueFactory(param -> new SimpleIntegerProperty(ofNullable(param.getValue().getFilterId()).orElse(0)));
        this.itemsFLocationClmn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getFilterLocation()));
        this.itemsFEngineClmn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getFilterEngine()));
        this.itemsFPreFormattingClmn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getFilterPreFormatting()));
        this.itemsExpressionClmn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getExpression()));

        this.itemsIdClmn.setOnEditCommit(event -> event.getRowValue().setId(event.getNewValue().intValue()));
        this.itemsFLocationClmn.setOnEditCommit(event -> event.getRowValue().setFilterLocation(event.getNewValue()));
        this.itemsFEngineClmn.setOnEditCommit(event -> event.getRowValue().setFilterEngine(event.getNewValue()));
        this.itemsFPreFormattingClmn.setOnEditCommit(event -> event.getRowValue().setFilterPreFormatting(event.getNewValue()));
        this.itemsExpressionClmn.setOnEditCommit(event -> event.getRowValue().setExpression(event.getNewValue()));

        this.itemsTbl.getItems().add(new FilterItemBean(this.filterId));

        this.searchesChoiceBox.setConverter(JfxUtils.converter(Search::getName));
    }

    @FXML
    private void dltItemAction(final ActionEvent actionEvent) {
        of(this.itemsTbl.getSelectionModel().getSelectedIndex())
                .filter(integer -> integer >= 0)
                .ifPresent(integer -> this.itemsTbl.getItems().remove((int) integer));
    }

    @FXML
    private void addItemAction(final ActionEvent actionEvent) {
        this.itemsTbl.getItems().add(new FilterItemBean(this.filterId));
    }

    public void setSearches(final List<Search> searches) {
        this.searchesChoiceBox.getItems().addAll(searches);
    }

    public void setFilter(@Nonnull final Filter filter) {
        this.filterId = LocalFunctions.requireNonNull(filter).getId();
        this.idLbl.setText(ofNullable(filter.getId()).map(Object::toString).orElse("-"));
        this.nameTxtField.setText(ofNullable(filter.getName()).orElse("-"));
        this.descriptionTxtArea.setText(ofNullable(filter.getDescription()).orElse("-"));
        JfxUtils.setChoiceBoxValue(this.searchesChoiceBox, search -> Objects.equals(search.getId(), filter.getSearchId()));
        this.itemsTbl.getItems().clear();
        this.itemsTbl.getItems()
                .addAll(ofNullable(filter.getFilterItems())
                        .map(filterItems -> filterItems.stream()
                                .map(FilterItemBean::new)
                                .collect(Collectors.toList()))
                        .orElse(Collections.emptyList()));
    }

    public Filter getFilter() {
        return Filter.builder()
                .setId(this.filterId)
                .setName(this.nameTxtField.getText())
                .setDescription(this.descriptionTxtArea.getText())
                .setSearchId(ofNullable(this.searchesChoiceBox.getValue()).map(AbstractModel::getId).orElse(null))
                .setFilterItems(this.itemsTbl.getItems().stream().map(FilterItemBean::toModel).collect(Collectors.toList()))
                .build();
    }

    /**
     * @deprecated Use this method only for tests!!!
     */
    @Deprecated
    public static void main(final String... args) throws Exception {
        new JFXPanel();// initializes JavaFX environment
        Platform.runLater(() -> { // FX components need to be managed by JavaFX
            final ResourceBundle resources = ResourceBundle.getBundle("i18n");

            final Dialog<Filter> dialog = new Dialog<>();
            dialog.setTitle(resources.getString("filter_panel.title.new"));
            dialog.setHeaderText(resources.getString("filter_panel.header.new"));
            dialog.setGraphic(new ImageView(FilterPanelController.class.getResource(
                    "/com/sun/javafx/scene/control/skin/modena/HTMLEditor-Background-Color@2x.png").toString()));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.CANCEL);
            dialog.setResizable(true);

            final FXMLLoader loader = new FXMLLoader(FilterPanelController.class.getResource("FilterPanel.fxml"), resources);
            try {
                dialog.getDialogPane().setContent(loader.load());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            final List<Exception> exceptions = new ArrayList<>();
            final FilterPanelController controller = loader.getController();
            controller.setSearches(Arrays.asList(Search.create(1, "search 1", "description 1", "url", Collections.emptyMap(), Collections.emptyMap()),
                    Search.create(1, "search 2", "description 2", "url", Collections.emptyMap(), Collections.emptyMap())));
            dialog.setResultConverter(btn -> {
                try {
                    return btn != ButtonType.YES ? null : controller.getFilter();
                } catch (final Exception ex) {
                    exceptions.add(ex);
                    return null;
                }
            });

            Optional<Filter> search = null;
            while (search == null) {
                search = dialog.showAndWait();
                if (!exceptions.isEmpty()) {
                    JfxUtils.showError("Look, an Exception Dialog", "Could not create a filter", exceptions.get(0));
                    exceptions.clear();
                    search = null;
                    dialog.setHeight(550);
                    dialog.setWidth(670);
                }
            }
            System.out.println(search);
        });
    }
}
