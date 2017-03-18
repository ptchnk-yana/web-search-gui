package home.yura.websearchgui.dao;

import home.yura.websearchgui.model.SearchResult;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author yuriy.dunko on 27.02.17.
 */
public interface SearchResultDao extends AbstractDao<SearchResult> {

    int BATCH_SIZE = 50;

    /**
     * @return the array of new IDs
     */
    int[] addBatch(final Collection<SearchResult> batch);

    /**
     * @return the number of updated records
     */
    // TODO: return single int instead of array
    int updateBatch(final Collection<SearchResult> batch);

    void setViewed(final int id, final boolean viewed);

    List<SearchResult> findByFilterId(@Nullable final Integer filterId, Integer startFrom, int limit);

    List<SearchResult> findBySearchId(@Nullable final Integer searchId, Integer startFrom, int limit);
}
