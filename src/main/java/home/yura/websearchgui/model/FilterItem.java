package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * @author yura.dunko on 26.02.17.
 */
@AutoValue
public abstract class FilterItem implements AbstractModel {

    public static enum FilterLocation {
        URL,
        CONTENT
    }

    public static enum FilterEngine {
        REG_EXP,
        STRING_SEARCH
    }

    public static enum FilterPreFormatting {
        NO,
        /** remove all tags from html */
        CLEAR_HTML,
        /**
         * For example, replace this:
         * https://ru.wikipedia.org/wiki/%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F_%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0
         * on this:
         * https://ru.wikipedia.org/wiki/Заглавная_страница
         */
        ESCAPE_URL
    }

    public static FilterItem create(final Integer id,
                                    final Integer filterId,
                                    final FilterLocation filterLocation,
                                    final FilterEngine filterEngine,
                                    final FilterPreFormatting filterPreFormatting,
                                    final String expression) {
        return new AutoValue_FilterItem(id, filterId, filterLocation, filterEngine, filterPreFormatting, expression);
    }

    public FilterItem copyWithId(final int id) {
        return create(id, getFilterId(), getFilterLocation(), getFilterEngine(), getFilterPreFormatting(), getExpression());
    }

    public FilterItem copyWithFilterId(final int filterId) {
        return create(getId(), filterId, getFilterLocation(), getFilterEngine(), getFilterPreFormatting(), getExpression());
    }

    @Nullable
    public abstract Integer getFilterId();

    public abstract FilterLocation getFilterLocation();

    public abstract FilterEngine getFilterEngine();

    public abstract FilterPreFormatting getFilterPreFormatting();

    public abstract String getExpression();
}
