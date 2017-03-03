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
        ESCAPE_HTML,
        ESCAPE_URL
    }

    public static FilterItem create(Integer id,
                                    Integer filterId,
                                    FilterLocation filterLocation,
                                    FilterEngine filterEngine,
                                    FilterPreFormatting filterPreFormatting,
                                    String expression) {
        return new AutoValue_FilterItem(id, filterId, filterLocation, filterEngine, filterPreFormatting, expression);
    }

    public FilterItem copyWithId(int id) {
        return create(id, getFilterId(), getFilterLocation(), getFilterEngine(), getFilterPreFormatting(), getExpression());
    }

    public FilterItem copyWithFilterId(int filterId) {
        return create(getId(), filterId, getFilterLocation(), getFilterEngine(), getFilterPreFormatting(), getExpression());
    }

    @Nullable
    public abstract Integer getFilterId();

    public abstract FilterLocation getFilterLocation();

    public abstract FilterEngine getFilterEngine();

    public abstract FilterPreFormatting getFilterPreFormatting();

    public abstract String getExpression();
}
