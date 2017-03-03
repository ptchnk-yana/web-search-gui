package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;

/**
 * @author yuriy.dunko on 26.02.17.
 */
@AutoValue
public abstract class SearchResult implements AbstractNamedModel {

    public static SearchResult create(Integer id,
                                      String name,
                                      String description,
                                      Integer resultEntryDefinitionId,
                                      Integer filterItemId,
                                      Long internalId,
                                      Boolean viewed) {
        return new AutoValue_SearchResult(id, name, description, resultEntryDefinitionId, filterItemId, internalId,
                ObjectUtils.firstNonNull(viewed, false));
    }

    public static SearchResult copyWithId(final int id, final SearchResult sr) {
        return create(id, sr.getName(), sr.getDescription(), sr.getResultEntryDefinitionId(),
                sr.getFilterItemId(), sr.getInternalId(), sr.isViewed());
    }

    public abstract Integer getResultEntryDefinitionId();

    @Nullable
    public abstract Integer getFilterItemId();

    public abstract Long getInternalId();

    public abstract boolean isViewed();
}
