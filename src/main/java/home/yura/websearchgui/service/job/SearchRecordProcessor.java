package home.yura.websearchgui.service.job;

import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import home.yura.websearchgui.service.ValueEvaluator;
import home.yura.websearchgui.util.LocalHttpUtils;
import home.yura.websearchgui.util.bean.BiTuple;
import org.apache.http.impl.client.CloseableHttpClient;
import org.easybatch.core.processor.RecordProcessor;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.core.record.Record;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yuriy.dunko on 09.03.17.
 */
public class SearchRecordProcessor implements
        RecordProcessor<
                Record<List<SearchResult>>,
                Record<List<Future<BiTuple<SearchResult, SearchResultContent>>>>> {

    private final ExecutorService poolExecutor;
    private final ValueEvaluator valueEvaluator;
    private final CloseableHttpClient client;
    private final ResultEntryDefinition resultEntryDefinition;

    public SearchRecordProcessor(final ExecutorService poolExecutor,
                                 final ValueEvaluator valueEvaluator,
                                 final Supplier<CloseableHttpClient> httpClientSupplier,
                                 final ResultEntryDefinition resultEntryDefinition) {
        this.poolExecutor = requireNonNull(poolExecutor, "poolExecutor");
        this.valueEvaluator = requireNonNull(valueEvaluator, "");
        this.client = requireNonNull(requireNonNull(httpClientSupplier, "httpClientSupplier").get(),
                "httpClient");
        this.resultEntryDefinition = requireNonNull(resultEntryDefinition, "resultEntryDefinition");
    }

    @Override
    public Record<List<Future<BiTuple<SearchResult, SearchResultContent>>>> processRecord(
            final Record<List<SearchResult>> record) throws Exception {

        return new GenericRecord<>(
                record.getHeader(),
                record.getPayload().stream().map(searchResult ->
                        this.poolExecutor.submit(() -> new BiTuple<>(
                                searchResult,
                                SearchResultContent.create(
                                        this.valueEvaluator.evaluate(
                                                this.resultEntryDefinition.getContentExtractionChain(),
                                                LocalHttpUtils.readDocument(this.client, searchResult.getUrl())))))
                ).collect(Collectors.toList()));
    }
}
