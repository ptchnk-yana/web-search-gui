package home.yura.websearchgui.service.job;

import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.service.ValueEvaluator;
import home.yura.websearchgui.util.LocalHttpUtils;
import home.yura.websearchgui.util.bean.BiTuple;
import org.apache.http.impl.client.CloseableHttpClient;
import org.easybatch.core.reader.RecordReader;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.core.record.Header;
import org.easybatch.core.record.Record;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Iterables.getFirst;
import static home.yura.websearchgui.util.LocalBeans.extractLong;
import static java.lang.String.format;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * @author yuriy.dunko on 04.03.17.
 */
public class SearchRecordReader implements RecordReader {

    // services

    private final ValueEvaluator valueEvaluator;
    private final Supplier<CloseableHttpClient> httpClientSupplier;
    private CloseableHttpClient client;

    // input data

    private final Search search;
    private final ResultEntryDefinition resultEntryDefinition;
    /**
     * The final internalId which the reader should stop on. It should be the first read id from previous run
     */
    private final Long finalInternalId;
    /**
     * Number of <b>pages</b> that should be read
     */
    private final int readLimit;

    // processing step item

    /**
     * The last internalId which the reader read
     */
    private AtomicLong lastInternalId;
    private AtomicInteger readAmount;
    /**
     * The first internalId which the reader read. Needed to store job result
     */
    private Long firstInternalId;
    private String nextUrlLink;


    public SearchRecordReader(final ValueEvaluator valueEvaluator,
                              final Supplier<CloseableHttpClient> httpClientSupplier,
                              final Long finalInternalId,
                              final int readLimit,
                              final BiTuple<Search, ResultEntryDefinition> searchTuple) {
        requireNonNull(searchTuple, "searchTuple");
        this.search = requireNonNull(searchTuple.getFirst(), "search");
        this.resultEntryDefinition = requireNonNull(searchTuple.getSecond(), "resultEntryDefinition");
        this.finalInternalId = finalInternalId; // can be null if this job has never run before.
        this.readLimit = readLimit;
        this.valueEvaluator = requireNonNull(valueEvaluator, "valueEvaluator");
        this.httpClientSupplier = requireNonNull(httpClientSupplier, "httpClientSupplier");
    }

    @Override
    public void open() throws Exception {
        this.client = httpClientSupplier.get();//HttpClientBuilder.create().build();
        this.lastInternalId = new AtomicLong();
        this.readAmount = new AtomicInteger();
        this.nextUrlLink = this.search.getUrl();
    }

    @Override
    public Record<List<SearchResult>> readRecord() throws Exception {
        if (this.nextUrlLink == null
                || (this.finalInternalId != null && this.lastInternalId.get() >= this.finalInternalId)
                || this.readLimit <= this.readAmount.get()) {
            return null;
        }

        final Document document = LocalHttpUtils.readDocument(this.client, this.nextUrlLink);
        final Elements entryBlocks = getEntryBlocks(document);
        final List<SearchResult> searchResults = entryBlocks
                .parallelStream()
                .map(this::buildSearchResult)
                .sorted(Comparator.comparingLong(SearchResult::getInternalId))
                .collect(toList());

        this.nextUrlLink = evaluateNextUrlLinkValue(document);

        if (this.firstInternalId == null) {
            this.firstInternalId = requireNonNull(getFirst(searchResults, null)).getInternalId();
        }

        this.lastInternalId.set(searchResults
                .stream()
                .map(SearchResult::getInternalId)
                .max(Long::compare)
                .orElseThrow(IllegalStateException::new));
        this.readAmount.incrementAndGet();

        return new GenericRecord<>(
                new Header(this.lastInternalId.get(),
                        document.baseUri(),
                        new Date()), // FIXME: Get date from some util!!!
                searchResults.stream()
                        .filter(sr -> this.finalInternalId == null || sr.getInternalId() < this.finalInternalId)
                        .collect(toList()));
    }

    @Override
    public void close() throws Exception {
        this.client.close();
    }

    private Elements getEntryBlocks(final Document document) {
        final Elements entryBlocks = document.select(this.resultEntryDefinition.getEntryBlockLocation());
        checkState(!entryBlocks.isEmpty(),
                format("Cannot find resultEntryDefinition using [%s] query on [%s] address",
                        this.resultEntryDefinition.getEntryBlockLocation(),
                        document.baseUri()));
        return entryBlocks;
    }

    private String evaluateNextUrlLinkValue(final Document document) {
        return emptyToNull(nullToEmpty(this.valueEvaluator.evaluate(this.search.getNextLinkLocation(), document)).trim());
    }

    private SearchResult buildSearchResult(final Element element) {
        return SearchResult.create(null, // id
                this.valueEvaluator.evaluate(this.resultEntryDefinition.getNameExtractionChain(), element),
                element.outerHtml(),
                this.resultEntryDefinition.getId(),
                null, // filterId
                extractLong(this.valueEvaluator.evaluate(this.resultEntryDefinition.getInternalIdExtractionChain(), element)),
                this.valueEvaluator.evaluate(this.resultEntryDefinition.getContentLinkExtractionChain(), element),
                false);
    }
}
