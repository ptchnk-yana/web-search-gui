package home.yura.websearchgui.dao;

import home.yura.websearchgui.model.SearchResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author yuriy.dunko on 27.02.17.
 */
public interface SearchResultDao extends AbstractDao<SearchResult> {

    void setViewed(int id, boolean viewed);

    List<SearchResult> findByFilterId(@Nullable Integer filterId);
}
