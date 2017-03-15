package home.yura.websearchgui.dao;

import home.yura.websearchgui.model.SearchResult;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author yuriy.dunko on 27.02.17.
 */
public interface SearchResultDao extends AbstractDao<SearchResult> {

    int BATCH_SIZE = 10;

    void setViewed(final int id, final boolean viewed);

    List<SearchResult> findByFilterId(@Nullable final Integer filterId);

    int[] addBatch(final Collection<SearchResult> batch);
}
