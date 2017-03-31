package home.yura.websearchgui.gui;

import home.yura.websearchgui.model.Search;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author yuriy.dunko on 01.04.17.
 */
public final class JfxUtils {

    public static void showError(final String headerText, final String contentText, final Exception exception) {
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Dialog"); // TODO: use i18n
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        final StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));

        final TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        final GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        // TODO: use i18n
        expContent.add(new Label("The exception stacktrace was:"), 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    public static <T> void setChoiceBoxValue(final ChoiceBox<T> choiceBox, final Predicate<T> predicate) {
        choiceBox.setValue(choiceBox
                .getItems()
                .filtered(predicate)
                .stream()
                .findFirst()
                .orElse(null));
    }

    public static <T> StringConverter<T> converter(final Function<T, String> toStringFunction) {
        return new StringConverter<T>() {
            @Override
            public String toString(final T object) {
                return toStringFunction.apply(object);
            }

            @Override
            public T fromString(final String string) {
                throw new IllegalStateException("Unsupported method call");
            }
        };
    }
}
