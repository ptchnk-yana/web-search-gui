package home.yura.websearchgui.dao;

import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.FilterItem;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yura on 27.02.17.
 */
public interface FilterDao extends AbstractDao<Filter>{

    FilterItem addItem(FilterItem item);

    List<Filter> findBySearchId(final int searchId);
}
