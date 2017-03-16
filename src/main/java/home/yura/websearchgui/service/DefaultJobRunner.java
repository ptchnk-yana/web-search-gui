package home.yura.websearchgui.service;

import home.yura.websearchgui.dao.FilterDao;
import home.yura.websearchgui.dao.LocalJobDao;
import home.yura.websearchgui.dao.SearchResultDao;
import home.yura.websearchgui.dao.rsource.SearchResultContentResource;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.service.job.*;
import home.yura.websearchgui.util.bean.BiTuple;
import org.apache.http.impl.client.CloseableHttpClient;
import org.easybatch.core.job.Job;
import org.easybatch.core.job.JobBuilder;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yuriy.dunko on 16.03.17.
 */
public class DefaultJobRunner implements JobRunner {
    private final Supplier<CloseableHttpClient> httpClientSupplier;
    private final ExecutorService httpQueryPool;
    private final FilterMatcher filterMatcher;
    private final ValueEvaluator valueEvaluator;
    private final LocalJobDao localJobDao;
    private final FilterDao filterDao;
    private final SearchResultDao searchResultDao;
    private final SearchResultContentResource searchResultContentResource;

    private final int batchSize;

    public DefaultJobRunner(final Supplier<CloseableHttpClient> httpClientSupplier,
                            final ExecutorService httpQueryPool,
                            final FilterMatcher filterMatcher,
                            final ValueEvaluator valueEvaluator,
                            final LocalJobDao localJobDao,
                            final FilterDao filterDao,
                            final SearchResultDao searchResultDao,
                            final SearchResultContentResource searchResultContentResource,
                            final int batchSize) {
        this.httpClientSupplier = requireNonNull(httpClientSupplier, "httpClientSupplier");
        this.httpQueryPool = requireNonNull(httpQueryPool, "httpQueryPool");
        this.filterMatcher = requireNonNull(filterMatcher, "filterMatcher");
        this.valueEvaluator = requireNonNull(valueEvaluator, "valueEvaluator");
        this.localJobDao = requireNonNull(localJobDao, "localJobDao");
        this.filterDao = requireNonNull(filterDao, "filterDao");
        this.searchResultDao = requireNonNull(searchResultDao, "searchResultDao");
        this.searchResultContentResource = searchResultContentResource; // can be null
        this.batchSize = batchSize;
    }

    @Override
    public Job createSearchJob(final BiTuple<Search, ResultEntryDefinition> searchTuple,
                               final int readLimit,
                               final long errorThreshold) {
        requireNonNull(searchTuple, "searchTuple");
        requireNonNull(searchTuple.getFirst(), "Search");
        requireNonNull(searchTuple.getSecond(), "ResultEntryDefinition");

        final SearchJobListener jobListener = new SearchJobListener(this.localJobDao, searchTuple.getFirst());
        return new JobBuilder()
                .named(jobListener.getJobName())
                .jobListener(jobListener)
                .batchListener(new SearchBatchListener(
                        this.localJobDao,
                        jobListener::getJobId))
                .reader(new SearchRecordReader(
                        this.valueEvaluator,
                        this.httpClientSupplier,
                        jobListener.getRequiredStep(),
                        readLimit,
                        searchTuple))
                .processor(new SearchRecordProcessor(
                        this.httpQueryPool,
                        this.valueEvaluator,
                        this.httpClientSupplier,
                        searchTuple.getSecond()))
                .processor(new SearchApplyFiltersRecordProcessor(
                        this.filterDao,
                        this.filterMatcher,
                        searchTuple.getSecond()))
                .writer(SearchResultRecordWriter.createFromDao(
                        this.searchResultDao,
                        Optional.ofNullable(this.searchResultContentResource)))
                .errorThreshold(errorThreshold)
                .batchSize(this.batchSize)
                .build();
    }
}
