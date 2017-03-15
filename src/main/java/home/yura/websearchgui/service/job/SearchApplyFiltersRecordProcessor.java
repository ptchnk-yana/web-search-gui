package home.yura.websearchgui.service.job;

import com.google.common.util.concurrent.Futures;
import home.yura.websearchgui.dao.FilterDao;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import home.yura.websearchgui.service.FilterMatcher;
import home.yura.websearchgui.util.LocalCollections;
import home.yura.websearchgui.util.bean.BiTuple;
import org.easybatch.core.processor.RecordProcessor;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.core.record.Record;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static home.yura.websearchgui.util.LocalFunctions.process;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author yuriy.dunko on 10.03.17.
 */
public class SearchApplyFiltersRecordProcessor implements
        RecordProcessor<
                Record<List<Future<BiTuple<SearchResult, SearchResultContent>>>>,
                Record<List<Future<BiTuple<SearchResult, SearchResultContent>>>>> {

    private final FilterDao filterDao;
    private final FilterMatcher filterMatcher;
    private final ResultEntryDefinition resultEntryDefinition;

    public SearchApplyFiltersRecordProcessor(final FilterDao filterDao,
                                             final FilterMatcher filterMatcher,
                                             final ResultEntryDefinition resultEntryDefinition) {
        this.filterDao = requireNonNull(filterDao, "filterDao");
        this.filterMatcher = requireNonNull(filterMatcher, "filterMatcher");
        this.resultEntryDefinition = requireNonNull(resultEntryDefinition, "resultEntryDefinition");
    }


    @Override
    public Record<List<Future<BiTuple<SearchResult, SearchResultContent>>>> processRecord(
            final Record<List<Future<BiTuple<SearchResult, SearchResultContent>>>> record) throws Exception {

        final Map<Long, Document> documentCache = new HashMap<>();

        final Set<BiTuple<SearchResult, SearchResultContent>> set = this.filterDao.findBySearchId(this.resultEntryDefinition.getSearchId())
                .stream()
                .map(filter -> record.getPayload()
                        .stream()
                        .map(futureTuple -> {
                            final BiTuple<SearchResult, SearchResultContent> tuple = process(futureTuple::get);
                            return Optional
                                    // TODO: chose a filter id by most matches
                                    .ofNullable(this.filterMatcher.getMatchedItemId(filter, document(documentCache, tuple)))
                                    .map(fItemId -> tuple.copyWithFirst(tuple.getFirst().copyWithFilterItemId(fItemId)));
                        }).filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet()))
                .flatMap(Collection::stream)
                .collect(Collector.of(
                        () -> new TreeSet<>(Comparator.comparingLong(o -> o.getFirst().getInternalId())),
                        LocalCollections::addIfNotContains,
                        LocalCollections::addAllIfNotContains));

        if (!set.isEmpty()) {
            return new GenericRecord<>(record.getHeader(),
                    LocalCollections
                            .addAllIfNotContains(
                                    set,
                                    record.getPayload().stream().map(future -> process(future::get)).collect(toSet()))
                            .stream()
                            .map(Futures::immediateFuture)
                            .collect(toList()));
        }

        return record;
    }

    private Document document(final Map<Long, Document> documentCache,
                              final BiTuple<SearchResult, SearchResultContent> tuple) {
        final Long internalId = tuple.getFirst().getInternalId();
        Document document = documentCache.get(internalId);
        if (document == null) {
            documentCache.put(internalId, document = Jsoup.parse(tuple.getSecond().getContent()));
            document.setBaseUri(tuple.getFirst().getUrl());
        }
        return document;
    }
}
