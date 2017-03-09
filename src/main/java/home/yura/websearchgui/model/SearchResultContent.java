package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;

/**
 * @author yuriy.dunko on 27.02.17.
 */
@AutoValue
public abstract class SearchResultContent implements AbstractModel {

    public static SearchResultContent create(final String content) {
        return create(0, content);
    }

    public static SearchResultContent create(final int searchResultId, final String content) {
        return new AutoValue_SearchResultContent(searchResultId, content);
    }

    public SearchResultContent copyWithSearchResultId(int id) {
        return create(id, this.getContent());
    }

    @Override
    public final Integer getId() {
        return this.getSearchResultId();
    }

    public abstract int getSearchResultId();

    public abstract String getContent();
}
