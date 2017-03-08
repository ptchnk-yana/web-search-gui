package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;
import home.yura.websearchgui.util.LocalCollections;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author yuriy.dunko on 26.02.17.
 */
@AutoValue
public abstract class Search implements AbstractNamedModel {

    public static Search create(final Integer id,
                                final String name,
                                final String description,
                                final String url,
                                final Map<Integer, ValueEvaluationDefinition> previousLinkLocation,
                                final Map<Integer, ValueEvaluationDefinition> nextLinkLocation) {
        return new AutoValue_Search(id, name, description, url, previousLinkLocation, nextLinkLocation);
    }

    public static Search merge(final Search left, final Search right) {
        checkArgument(Objects.equals(left.getId(), right.getId()), "id should be equal");
        checkArgument(Objects.equals(left.getName(), right.getName()), "name should be equal");
        checkArgument(Objects.equals(left.getDescription(), right.getDescription()), "description should be equal");
        checkArgument(Objects.equals(left.getUrl(), right.getUrl()), "url should be equal");
        return create(left.getId(),
                left.getName(),
                left.getDescription(),
                left.getUrl(),
                LocalCollections.merge(left.getPreviousLinkLocation(), right.getPreviousLinkLocation()),
                LocalCollections.merge(left.getNextLinkLocation(), right.getNextLinkLocation()));
    }

    public Search copyWithId(final Integer id) {
        return create(id, getName(), getDescription(), getUrl(), getPreviousLinkLocation(), getNextLinkLocation());
    }

    public abstract String getUrl();

    /**
     * CssQuery to the link on the page
     * @return CssQuery to the ling on the previous page for the search
     */
    public abstract Map<Integer, ValueEvaluationDefinition> getPreviousLinkLocation();

    /**
     * CssQuery to the link on the page
     * @return CssQuery to the ling on the previous page for the search
     */
    public abstract Map<Integer, ValueEvaluationDefinition> getNextLinkLocation();
}
