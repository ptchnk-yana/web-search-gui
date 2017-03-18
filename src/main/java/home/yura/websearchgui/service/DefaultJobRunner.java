package home.yura.websearchgui.service;

import home.yura.websearchgui.dao.FilterDao;
import home.yura.websearchgui.dao.LocalJobDao;
import home.yura.websearchgui.dao.SearchResultDao;
import home.yura.websearchgui.dao.rsource.SearchResultContentResource;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.service.job.*;
import home.yura.websearchgui.util.bean.BiTuple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.easybatch.core.job.Job;
import org.easybatch.core.job.JobBuilder;
import org.easybatch.core.reader.RecordReader;
import org.easybatch.core.writer.RecordWriter;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * @author yuriy.dunko on 16.03.17.
 */
public class DefaultJobRunner implements JobRunner {
    private static final Log LOG = LogFactory.getLog(DefaultJobRunner.class);

    private final Supplier<CloseableHttpClient> httpClientSupplier;
    private final ExecutorService httpQueryPool;
    private final FilterMatcher filterMatcher;
    private final ValueEvaluator valueEvaluator;
    private final LocalJobDao localJobDao;
    private final FilterDao filterDao;
    private final SearchResultDao searchResultDao;
    private final SearchResultContentResource searchResultContentResource;

    private final int batchSize;
    private final long errorThreshold;

    public DefaultJobRunner(final Supplier<CloseableHttpClient> httpClientSupplier,
                            final ExecutorService httpQueryPool,
                            final FilterMatcher filterMatcher,
                            final ValueEvaluator valueEvaluator,
                            final LocalJobDao localJobDao,
                            final FilterDao filterDao,
                            final SearchResultDao searchResultDao,
                            final SearchResultContentResource searchResultContentResource,
                            final int batchSize,
                            final long errorThreshold) {
        this.httpClientSupplier = requireNonNull(httpClientSupplier, "httpClientSupplier");
        this.httpQueryPool = requireNonNull(httpQueryPool, "httpQueryPool");
        this.filterMatcher = requireNonNull(filterMatcher, "filterMatcher");
        this.valueEvaluator = requireNonNull(valueEvaluator, "valueEvaluator");
        this.localJobDao = requireNonNull(localJobDao, "localJobDao");
        this.filterDao = requireNonNull(filterDao, "filterDao");
        this.searchResultDao = requireNonNull(searchResultDao, "searchResultDao");
        this.searchResultContentResource = searchResultContentResource; // can be null
        this.batchSize = batchSize;
        this.errorThreshold = errorThreshold;
    }

    @Override
    public Job createSearchJob(final BiTuple<Search, ResultEntryDefinition> searchTuple,
                               final int readLimit) {
        checkTupleArgument(searchTuple);
        LOG.debug("Creating search job [" + searchTuple + "] with limit [" + readLimit + "]");
        final SearchJobListener jobListener = new SearchJobListener(
                this.localJobDao, searchTuple.getFirst(), "append_results");

        return createAbstractSearchJob(searchTuple,
                this.errorThreshold,
                jobListener,
                new SearchRecordReader(this.valueEvaluator, this.httpClientSupplier, jobListener.getRequiredStep(), readLimit, searchTuple),
                SearchResultRecordWriter.createFromDao(this.searchResultDao, ofNullable(this.searchResultContentResource)));
    }

    @Override
    public Job createFilterJob(final BiTuple<Search, ResultEntryDefinition> searchTuple) {
        checkTupleArgument(searchTuple);
        LOG.debug("Creating filtering job [" + searchTuple + "]");
        final SearchJobListener jobListener = new SearchJobListener(
                this.localJobDao, searchTuple.getFirst(), "filter_results-" + Instant.now());

        return createAbstractSearchJob(searchTuple,
                this.errorThreshold,
                jobListener,
                new ExistingSearchRecordReader(this.searchResultDao, searchTuple.getFirst(), SearchResultDao.BATCH_SIZE),
                new SearchResultRecordWriter(this.searchResultDao::updateBatch, Optional.empty()));
    }

    private void checkTupleArgument(final BiTuple<Search, ResultEntryDefinition> searchTuple) {
        requireNonNull(searchTuple, "searchTuple");
        requireNonNull(searchTuple.getFirst(), "Search");
        requireNonNull(searchTuple.getSecond(), "ResultEntryDefinition");
    }

    private Job createAbstractSearchJob(final BiTuple<Search, ResultEntryDefinition> searchTuple,
                                        final long errorThreshold,
                                        final SearchJobListener jobListener,
                                        final RecordReader recordReader,
                                        final RecordWriter recordWriter) {
        return new JobBuilder()
                .named(jobListener.getJobName())
                .jobListener(jobListener)
                .batchListener(new SearchBatchListener(
                        this.localJobDao,
                        jobListener::getJobId))
                .reader(recordReader)
                .processor(new SearchRecordProcessor(
                        this.httpQueryPool,
                        this.valueEvaluator,
                        this.httpClientSupplier,
                        searchTuple.getSecond()))
                .processor(new SearchApplyFiltersRecordProcessor(
                        this.filterDao,
                        this.filterMatcher,
                        searchTuple.getSecond()))
                .writer(recordWriter)
                .errorThreshold(errorThreshold)
                .batchSize(this.batchSize)
                .build();
    }
}
