package home.yura.websearchgui.gui.jfx;

import home.yura.websearchgui.gui.JfxUtils;
import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.FilterItem;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static java.util.Optional.ofNullable;

/**
 * @author yuriy.dunko on 02.04.17.
 */
public class FilterItemPanelController implements Initializable {
    @FXML
    private Label idLbl;
    @FXML
    private ChoiceBox<Filter> filterIdLst;
    @FXML
    private ChoiceBox<FilterItem.FilterLocation> filterLocationLst;
    @FXML
    private ChoiceBox<FilterItem.FilterEngine> filterEngineLst;
    @FXML
    private ChoiceBox<FilterItem.FilterPreFormatting> filterPreFormattingLst;
    @FXML
    private TextField expressionTxtFiled;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.filterIdLst.setConverter(JfxUtils.converter(Filter::getName));
        this.filterLocationLst.getItems().addAll(FilterItem.FilterLocation.values());
        this.filterEngineLst.getItems().addAll(FilterItem.FilterEngine.values());
        this.filterPreFormattingLst.getItems().addAll(FilterItem.FilterPreFormatting.values());
    }

    public void setFilters(final List<Filter> filters) {
        this.filterIdLst.getItems().clear();
        this.filterIdLst.getItems().addAll(filters);
    }

    public void setFilterItem(final FilterItem filterItem) {
        this.idLbl.setText(ofNullable(filterItem.getId()).map(Object::toString).orElse(""));
        JfxUtils.setChoiceBoxValue(this.filterIdLst, filter -> Objects.equals(filter.getId(), filterItem.getFilterId()));
        JfxUtils.setChoiceBoxValue(this.filterLocationLst, item -> Objects.equals(item, filterItem.getFilterLocation()));
        JfxUtils.setChoiceBoxValue(this.filterEngineLst, item -> Objects.equals(item, filterItem.getFilterEngine()));
        JfxUtils.setChoiceBoxValue(this.filterPreFormattingLst, item -> Objects.equals(item, filterItem.getFilterPreFormatting()));
        this.expressionTxtFiled.setText(ofNullable(filterItem.getExpression()).orElse(""));
    }

    public FilterItem getFilterItem() {
        return FilterItem.create(
                ofNullable(this.idLbl.getText()).filter(StringUtils::isNotBlank).map(Integer::parseInt).orElse(null),
                ofNullable(this.filterIdLst.getValue()).map(Filter::getId).orElse(null),
                this.filterLocationLst.getValue(),
                this.filterEngineLst.getValue(),
                this.filterPreFormattingLst.getValue(),
                this.expressionTxtFiled.getText());
    }

    /**
     * @deprecated Use this method only for tests!!!
     */
    @Deprecated
    public static void main(final String... args) throws Exception {
        new JFXPanel();// initializes JavaFX environment
        Platform.runLater(() -> { // FX components need to be managed by JavaFX
            final ResourceBundle resources = ResourceBundle.getBundle("i18n");

            final Dialog<FilterItem> dialog = new Dialog<>();
            dialog.setTitle(resources.getString("filter_item_panel.title.new"));
            dialog.setHeaderText(resources.getString("filter_item_panel.header.new"));
            dialog.setGraphic(new ImageView(FilterItemPanelController.class.getResource(
                    "/com/sun/javafx/scene/control/skin/modena/HTMLEditor-Background-Color@2x.png").toString()));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.CANCEL);
            dialog.setResizable(true);

            final FXMLLoader loader = new FXMLLoader(FilterItemPanelController.class.getResource("FilterItemPanel.fxml"), resources);
            try {
                dialog.getDialogPane().setContent(loader.load());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            final List<Exception> exceptions = new ArrayList<>();
            final FilterItemPanelController controller = loader.getController();
            controller.setFilters(Arrays.asList(Filter.builder().setSearchId(1).setName("filter 1").setDescription("filter 1").build(),
                    Filter.builder().setSearchId(1).setName("filter 2").setDescription("filter 2").build()));

            dialog.setResultConverter(btn -> {
                try {
                    return btn != ButtonType.YES ? null : controller.getFilterItem();
                } catch (final Exception ex) {
                    exceptions.add(ex);
                    return null;
                }
            });

            Optional<FilterItem> search = null;
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
