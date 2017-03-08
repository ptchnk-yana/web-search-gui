package home.yura.websearchgui.service.job;

import com.google.common.collect.ImmutableMap;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.ValueEvaluationDefinition;
import home.yura.websearchgui.service.DefaultValueEvaluator;
import home.yura.websearchgui.service.ValueEvaluator;
import home.yura.websearchgui.util.bean.BiTuple;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.easybatch.core.record.Record;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import static com.google.common.collect.ImmutableList.of;
import static home.yura.websearchgui.TestUtils.getResourceAsStream;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.CSS_QUERY_SEARCH;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.REG_EXP;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.EXTRACT_CONTENT;
import static home.yura.websearchgui.util.LocalBeans.index;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link SearchRecordReader}
 *
 * @author yuriy.dunko on 06.03.17.
 */
public class TestSearchRecordReader {
    private static final String PAGE_1_URL = "https://www.olx.ua/transport/legkovye-avtomobili/zhitomir/";
    private static final String PAGE_1_LOCATION = "/home/yura/websearchgui/service/job/search/page1.html.tar.gz";
    private static final String PAGE_2_URL = "https://www.olx.ua/transport/legkovye-avtomobili/zhitomir/?page=2";
    private static final String PAGE_2_LOCATION = "/home/yura/websearchgui/service/job/search/page2.html.tar.gz";
    private static final String PAGE_3_URL = "https://www.olx.ua/transport/legkovye-avtomobili/zhitomir/?page=3";
    private static final String PAGE_3_LOCATION = "/home/yura/websearchgui/service/job/search/page3.html.tar.gz";

    private static final Map<String, Supplier<InputStream>> CONTENT_MAP = ImmutableMap.of(
            PAGE_1_URL, () -> process(() -> new GZIPInputStream(getResourceAsStream(PAGE_1_LOCATION)), RuntimeException::new),
            PAGE_2_URL, () -> process(() -> new GZIPInputStream(getResourceAsStream(PAGE_2_LOCATION)), RuntimeException::new),
            PAGE_3_URL, () -> process(() -> new GZIPInputStream(getResourceAsStream(PAGE_3_LOCATION)), RuntimeException::new)
    );

    private final ValueEvaluator entryProcessService = new DefaultValueEvaluator();

    @Test
    public void testReadRecord() throws Exception {
        final CloseableHttpClient httpClient = createHttpClient();

        final SearchRecordReader reader = new SearchRecordReader(this.entryProcessService, () -> httpClient,
                null, Integer.MAX_VALUE, createSearchTuple());
        reader.open();
        Record<List<SearchResult>> record = reader.readRecord();
        assertThat(record, notNullValue());
        assertThat(record.getPayload(), hasSize(44));
        assertThat(record.getPayload().stream().map(SearchResult::getName).collect(toList()), containsInAnyOrder(
                "Продам ВАЗ 2115 газ", "Hyundai Accent 2013", "Рено Меган грандтур 2013", "Продам кадика по запчастям",
                "Volkswagen Passat B7", "Renault Laguna 2.0 TDI BOSE INITIALE 2012 самая макс ком-ция 180 л с",
                "продам авто", "продам ВАЗ 2106", "Мазда 626", "продам авто", "Москвич пирожок", "продам форд ескорт дизель",
                "BMW 530 дизель", "ВАЗ-2107", "Автомобиль", "Продам автомобиль", "Продам Mercedes-benz .Дизель 3.2!2001",
                "Bmw 520", "Чери Амулет", "Chevrolet Tacuma 2006", "Продам таврию в хорошем состоянии.", "Продам Ford Escort",
                "Hyundai H100 1996г.в., пассажир, очень экономный", "Срочно Opel Combo-c 1.7 CDTI", "Opel Omega 1996г. 2.0 газ/бензин",
                "Автомобиль", "Продам NISSAN bluebird универсал", "Nissan Primera газ-бензин", "Продам Mazda 323 F V (BA) 1.5 1995",
                "Продам ВАЗ 2108. 1993 г", "продам ваз 2106", "Автомобиль", "продам автомобиль", "продам неростоможеного жигуля пережену",
                "Продам Автомобиль Шкода Октавия", "Volkswagen golf 6", "Спеши успеть! Honda CR-V 2.4 AT Elegance Цена только зимой!",
                "Выгодные цены! FIAT Doblo PanoramaMT 5s1.9DActive Супер!", "Авто ВАЗ 2115 ДТП недорого",
                "Продаю RENAULT Kangoo Grand Komfort", "Продам Ваз 21011", "Daewoo Lanos 1998 1.5 бенз. поляк. нерастаможен",
                "Hyundai Sonata 2011", "ситроен джампи"));

        record = reader.readRecord();
        assertThat(record, notNullValue());
        assertThat(record.getPayload(), hasSize(44));

        record = reader.readRecord();
        assertThat(record, notNullValue());
        assertThat(record.getPayload(), hasSize(44));

        record = reader.readRecord();
        assertThat(record, nullValue());
        reader.close();
    }

    @Test
    public void testReadRecordLimi() throws Exception {
        final CloseableHttpClient httpClient = createHttpClient();

        final SearchRecordReader reader = new SearchRecordReader(this.entryProcessService, () -> httpClient,
                null, 1, createSearchTuple());
        reader.open();

        Record<List<SearchResult>> record = reader.readRecord();
        assertThat(record, notNullValue());
        assertThat(record.getPayload(), hasSize(44));

        record = reader.readRecord();
        assertThat(record, nullValue());
        reader.close();
    }

    @Test
    public void testReadRecordFinalInternalId() throws Exception {
        final CloseableHttpClient httpClient = createHttpClient();

        final SearchRecordReader reader = new SearchRecordReader(this.entryProcessService, () -> httpClient,
                382945664L, Integer.MAX_VALUE, createSearchTuple());
        reader.open();

        Record<List<SearchResult>> record = reader.readRecord();
        assertThat(record, notNullValue());
        assertThat(record.getPayload(), hasSize(9));

        record = reader.readRecord();
        assertThat(record, nullValue());
        reader.close();
    }

    private BiTuple<Search, ResultEntryDefinition> createSearchTuple() {
        return new BiTuple<>(
                Search.create(
                        1,
                        "name",
                        "description",
                        PAGE_1_URL,
                        index(of(ValueEvaluationDefinition.create(1, EXTRACT_CONTENT, CSS_QUERY_SEARCH, "span.prev a.pageNextPrev"),
                                ValueEvaluationDefinition.create(1, EXTRACT_CONTENT, REG_EXP, "href=\"(.+)\""))),
                        index(of(ValueEvaluationDefinition.create(1, EXTRACT_CONTENT, CSS_QUERY_SEARCH, "span.next a.pageNextPrev"),
                                ValueEvaluationDefinition.create(1, EXTRACT_CONTENT, REG_EXP, "href=\"(.+)\"")))),
                ResultEntryDefinition.create(
                        1,
                        1,
                        "tr.wrap td.offer table",
                        index(of(ValueEvaluationDefinition.create(1, EXTRACT_CONTENT, CSS_QUERY_SEARCH, "h3 a strong"),
                                ValueEvaluationDefinition.create(null, EXTRACT_CONTENT, REG_EXP, "<\\w+>(.+)</\\w+>"))),
                        index(of(ValueEvaluationDefinition.create(1, EXTRACT_CONTENT, CSS_QUERY_SEARCH, "h3 a"),
                                ValueEvaluationDefinition.create(null, EXTRACT_CONTENT, REG_EXP, "href=\"(.+)\""))),
                        index(of(ValueEvaluationDefinition.create(1, EXTRACT_CONTENT, CSS_QUERY_SEARCH, "tr:eq(1) td.tright div div a"),
                                ValueEvaluationDefinition.create(null, EXTRACT_CONTENT, REG_EXP, "class=\"\\{id:(\\d+)\\}"))),
                        null));
    }

    private CloseableHttpClient createHttpClient() throws IOException {
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).then(invocation -> {
            final HttpGet request = invocation.getArgument(0);
            final BasicHttpContext context = invocation.getArgument(1);

            context.setAttribute(SearchRecordReader.HTTP_ATTRIBUTE_TARGET_HOST, request.getURI());

            final HttpEntity httpEntity = mock(HttpEntity.class);
            when(httpEntity.getContent()).thenReturn(CONTENT_MAP.get(request.getURI().toString()).get());
            when(httpEntity.getContentType()).thenReturn(new BasicHeader("Content-Type", "text/html; charset=utf-8"));

            final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getEntity()).thenReturn(httpEntity);

            return response;
        });
        return httpClient;
    }

}