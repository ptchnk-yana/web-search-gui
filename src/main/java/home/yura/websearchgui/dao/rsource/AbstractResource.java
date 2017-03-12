package home.yura.websearchgui.dao.rsource;

import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import home.yura.websearchgui.util.bean.BiTuple;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * @author yuriy.dunko on 12.03.17.
 */
public interface AbstractResource<Metadata, Content> {

    Future<Void> add(final Metadata m, final Content c);

    Future<Void> addBatch(final Collection<BiTuple<SearchResult, SearchResultContent>> batch);

    Future<Boolean> delete(final Metadata m);

    Future<Optional<Content>> get(final Metadata m);
}
