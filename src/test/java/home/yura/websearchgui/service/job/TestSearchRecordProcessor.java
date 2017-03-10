package home.yura.websearchgui.service.job;

import com.google.common.collect.ImmutableMap;
import home.yura.websearchgui.TestUtils;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import home.yura.websearchgui.model.ValueEvaluationDefinition;
import home.yura.websearchgui.service.DefaultValueEvaluator;
import home.yura.websearchgui.service.ValueEvaluator;
import home.yura.websearchgui.util.LocalBeans;
import home.yura.websearchgui.util.bean.BiTuple;
import org.apache.commons.io.FileUtils;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.core.record.Header;
import org.easybatch.core.record.Record;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import static com.google.common.collect.ImmutableList.of;
import static home.yura.websearchgui.TestUtils.getResourceAsStream;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.CSS_QUERY_SEARCH;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.DELETE_CONTENT_PART;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.EXTRACT_CONTENT;
import static home.yura.websearchgui.util.LocalCollections.index;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link SearchRecordProcessor}
 *
 * @author yuriy.dunko on 09.03.17.
 */
public class TestSearchRecordProcessor {
    private static final String PAGE_1_URL = "https://www.olx.ua/obyavlenie/prodam-ford-escort-IDj6BT2.html";
    private static final String PAGE_1_LOCATION = "/home/yura/websearchgui/service/job/search/page1_item1.html.gz";
    private static final String PAGE_2_URL = "https://www.olx.ua/obyavlenie/prodayu-renault-kangoo-grand-komfort-IDmAIjs.html";
    private static final String PAGE_2_LOCATION = "/home/yura/websearchgui/service/job/search/page1_item2.html.gz";
    private static final String PAGE_3_URL = "https://www.olx.ua/obyavlenie/prodam-kadika-po-zapchastyam-IDnwNCq.html";
    private static final String PAGE_3_LOCATION = "/home/yura/websearchgui/service/job/search/page1_item3.html.gz";

    private static final Map<String, Supplier<InputStream>> CONTENT_MAP = ImmutableMap.of(
            PAGE_1_URL, () -> process(() -> new GZIPInputStream(getResourceAsStream(PAGE_1_LOCATION)), RuntimeException::new),
            PAGE_2_URL, () -> process(() -> new GZIPInputStream(getResourceAsStream(PAGE_2_LOCATION)), RuntimeException::new),
            PAGE_3_URL, () -> process(() -> new GZIPInputStream(getResourceAsStream(PAGE_3_LOCATION)), RuntimeException::new)
    );

    private static final ValueEvaluator VALUE_EVALUATOR = new DefaultValueEvaluator();

    @Test
    public void processRecord() throws Exception {
        final SearchRecordProcessor searchRecordProcessor = new SearchRecordProcessor(
                Executors.newFixedThreadPool(CONTENT_MAP.size()),
                VALUE_EVALUATOR,
                () -> TestUtils.createHttpClient(CONTENT_MAP),
                createResultEntryDefinition());
        final Record<List<Future<BiTuple<SearchResult, SearchResultContent>>>> result = searchRecordProcessor.processRecord(
                new GenericRecord<>(
                        new Header(1L, "", new Date()),
                        of(
                                createSearchResult(
                                        "Ford Escort",
                                        282325992L,
                                        "https://www.olx.ua/obyavlenie/prodam-ford-escort-IDj6BT2.html"),
                                createSearchResult(
                                        "RENAULT Kangoo",
                                        333829542L,
                                        "https://www.olx.ua/obyavlenie/prodayu-renault-kangoo-grand-komfort-IDmAIjs.html"),
                                createSearchResult(
                                        "кадик по запчастям",
                                        347434634L,
                                        "https://www.olx.ua/obyavlenie/prodam-kadika-po-zapchastyam-IDnwNCq.html")
                        )));
        assertThat(result, notNullValue());

        final List<String> list = result
                .getPayload()
                .stream()
                .map(f -> process(() -> f.get().getSecond().getContent(), RuntimeException::new))
                .collect(toList());
        assertThat(list, hasItems(not(isEmptyOrNullString()), not(isEmptyOrNullString()), not(isEmptyOrNullString())));

        // better to verify result manually
        final File tmpdir = new File(AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("java.io.tmpdir")));
        final File storage = new File(tmpdir, String.format("%s.html.gz", getClass().getSimpleName()));
        FileUtils.writeByteArrayToFile(storage, LocalBeans.gzip(list.get(0)));
        System.out.printf("%s.processRecord -> %s", getClass().getSimpleName(), storage).println();
    }

    private SearchResult createSearchResult(final String name, final long internalId, final String url) {
        return SearchResult.create(null, name, name + url, 1, null, internalId, url, false);
    }

    private ResultEntryDefinition createResultEntryDefinition() {
        return ResultEntryDefinition.create(
                1,
                1,
                "",
                null,
                null,
                null,
                index(of(ValueEvaluationDefinition.create(1, EXTRACT_CONTENT, CSS_QUERY_SEARCH, "div.offercontent div"),
                        ValueEvaluationDefinition.create(1, DELETE_CONTENT_PART, CSS_QUERY_SEARCH, "div#offerbottombar"),
                        ValueEvaluationDefinition.create(1, DELETE_CONTENT_PART, CSS_QUERY_SEARCH, "a[data-statkey='ad.observed.bigstar']"),
                        ValueEvaluationDefinition.create(1, DELETE_CONTENT_PART, CSS_QUERY_SEARCH, "a#enlargephoto"))));
    }

}