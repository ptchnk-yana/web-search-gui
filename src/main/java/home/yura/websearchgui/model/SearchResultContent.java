package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;

/**
 * Created by yura on 27.02.17.
 */
@AutoValue
public abstract class SearchResultContent implements AbstractModel {

    public static SearchResultContent create(int searchResultId, String content) {
        return new AutoValue_SearchResultContent(searchResultId, content);
    }

    @Override
    public final Integer getId() {
        return this.getSearchResultId();
    }

    public abstract int getSearchResultId();

    public abstract String getContent();
}
