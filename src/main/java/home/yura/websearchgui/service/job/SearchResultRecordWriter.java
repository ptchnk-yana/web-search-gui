package home.yura.websearchgui.service.job;

import home.yura.websearchgui.dao.SearchResultDao;
import home.yura.websearchgui.dao.rsource.SearchResultContentResource;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import home.yura.websearchgui.util.LocalFunctions;
import home.yura.websearchgui.util.bean.BiTuple;
import home.yura.websearchgui.util.bean.Tuple;
import org.easybatch.core.record.Batch;
import org.easybatch.core.record.Record;
import org.easybatch.core.writer.RecordWriter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * @author yuriy.dunko on 11.03.17.
 */
public class SearchResultRecordWriter implements RecordWriter {

    private final Function<List<SearchResult>, ?> saveSearchResultFunction;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Function<List<BiTuple<SearchResult, SearchResultContent>>, Future<?>>> saveSearchResultContentFunction;

    private final List<Future<?>> savedFutures = new ArrayList<>();

    public static SearchResultRecordWriter createFromDao(final SearchResultDao searchResultDao,
                                                          @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                                                          final Optional<SearchResultContentResource> saveSearchResultContentFunction) {
        requireNonNull(searchResultDao, "searchResultDao");
        requireNonNull(searchResultDao, "saveSearchResultContentFunction");
        return new SearchResultRecordWriter(searchResultDao::addBatch,
                saveSearchResultContentFunction.map(r -> r::addBatch));
    }

    public SearchResultRecordWriter(final Function<List<SearchResult>, ?> saveSearchResultFunction,
                                    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                                    final Optional<Function<List<BiTuple<SearchResult, SearchResultContent>>, Future<?>>> saveSearchResultContentFunction) {
        this.saveSearchResultFunction = requireNonNull(saveSearchResultFunction, "saveSearchResultFunction");
        this.saveSearchResultContentFunction = requireNonNull(saveSearchResultContentFunction);
    }

    @Override
    public void open() throws Exception {
        this.savedFutures.clear();
    }

    @Override
    public void close() throws Exception {
        for(final Future<?> future : this.savedFutures) {
            future.get();
        }
        this.savedFutures.clear();
    }

    @Override
    public void writeRecords(final Batch batch) throws Exception {
        for (@SuppressWarnings("unchecked") final Record<List<Future<BiTuple<SearchResult, SearchResultContent>>>> record : batch) {
            final List<BiTuple<SearchResult, SearchResultContent>> request = record
                    .getPayload()
                    .stream()
                    .map(LocalFunctions::getFromFuture)
                    .sorted(Comparator.comparingLong(o -> o.getFirst().getInternalId()))
                    .collect(toList());
            this.saveSearchResultContentFunction.ifPresent(f -> savedFutures.add(f.apply(request)));
            this.saveSearchResultFunction.apply(request.stream().map(Tuple::getFirst).collect(toList()));
        }
    }
}
