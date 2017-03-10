package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;

/**
 * @author yuriy.dunko on 26.02.17.
 */
@AutoValue
public abstract class SearchResult implements AbstractNamedModel {

    public static SearchResult create(final Integer id,
                                      final String name,
                                      final String description,
                                      final Integer resultEntryDefinitionId,
                                      final Integer filterItemId,
                                      final Long internalId,
                                      final String url,
                                      final Boolean viewed) {
        return new AutoValue_SearchResult(id, name, description, resultEntryDefinitionId, filterItemId, internalId, url,
                ObjectUtils.firstNonNull(viewed, false));
    }

    public SearchResult copyWithId(final int id) {
        return create(id, getName(), getDescription(), getResultEntryDefinitionId(), getFilterItemId(), getInternalId(),
                getUrl(), isViewed());
    }

    public SearchResult copyWithFilterItemId(final int filterItemId) {
        return create(getId(), getName(), getDescription(), getResultEntryDefinitionId(), filterItemId, getInternalId(),
                getUrl(), isViewed());
    }

    public abstract Integer getResultEntryDefinitionId();

    @Nullable
    public abstract Integer getFilterItemId();

    public abstract Long getInternalId();

    public abstract String getUrl();

    public abstract boolean isViewed();

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", getId())
                .add("name", getName())
                .add("description", getDescription().length())
                .add("resultEntryDefinitionId", getResultEntryDefinitionId())
                .add("filterItemId", getFilterItemId())
                .add("internalId", getInternalId())
                .add("url", getUrl())
                .add("viewed", isViewed())
                .toString();
    }
}
