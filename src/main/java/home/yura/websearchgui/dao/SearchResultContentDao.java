package home.yura.websearchgui.dao;

import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author yuriy.dunko on 27.02.17.
 */
public interface SearchResultContentDao extends AbstractDao<SearchResultContent> {

    int BATCH_SIZE = 100;

    SearchResultContent get(SearchResult definition);

    /**
     * @deprecated use {@link #get(SearchResult)}
     */
    @Override
    SearchResultContent get(int id);

    Supplier<Integer> batchDelete(Function<Integer, List<Integer>> idSupplier);

}
