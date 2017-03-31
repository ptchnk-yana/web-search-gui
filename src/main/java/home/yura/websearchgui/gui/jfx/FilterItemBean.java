package home.yura.websearchgui.gui.jfx;

import home.yura.websearchgui.gui.beans.AbstractBean;
import home.yura.websearchgui.model.FilterItem;

/**
 * @author yuriy.dunko on 01.04.17.
 */
final class FilterItemBean extends FilterItem implements AbstractBean<FilterItem> {
    private Integer id = null;
    private Integer filterId;
    private FilterItem.FilterLocation filterLocation = FilterLocation.CONTENT;
    private FilterItem.FilterEngine filterEngine = FilterEngine.REG_EXP;
    private FilterItem.FilterPreFormatting filterPreFormatting = FilterPreFormatting.NO;
    private String expression = "";

    FilterItemBean(final FilterItem filterItem) {
        this.id = filterItem.getId();
        this.filterId = filterItem.getFilterId();
        this.filterLocation = filterItem.getFilterLocation();
        this.filterEngine = filterItem.getFilterEngine();
        this.filterPreFormatting = filterItem.getFilterPreFormatting();
        this.expression = filterItem.getExpression();
    }

    FilterItemBean(final Integer filterId) {
        this.filterId = filterId;
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    void setId(final Integer id) {
        this.id = id;
    }

    @Override
    public Integer getFilterId() {
        return this.filterId;
    }

    private void setFilterId(final Integer filterId) {
        this.filterId = filterId;
    }

    @Override
    public FilterItem.FilterLocation getFilterLocation() {
        return this.filterLocation;
    }

    public void setFilterLocation(final FilterItem.FilterLocation filterLocation) {
        this.filterLocation = filterLocation;
    }

    @Override
    public FilterItem.FilterEngine getFilterEngine() {
        return this.filterEngine;
    }

    void setFilterEngine(final FilterItem.FilterEngine filterEngine) {
        this.filterEngine = filterEngine;
    }

    @Override
    public FilterItem.FilterPreFormatting getFilterPreFormatting() {
        return this.filterPreFormatting;
    }

    void setFilterPreFormatting(final FilterItem.FilterPreFormatting filterPreFormatting) {
        this.filterPreFormatting = filterPreFormatting;
    }

    @Override
    public String getExpression() {
        return this.expression;
    }

    void setExpression(final String expression) {
        this.expression = expression;
    }

    @Override
    public FilterItem toModel() {
        return FilterItem.create(getId(), getFilterId(), getFilterLocation(), getFilterEngine(),
                getFilterPreFormatting(), getExpression());
    }
}
