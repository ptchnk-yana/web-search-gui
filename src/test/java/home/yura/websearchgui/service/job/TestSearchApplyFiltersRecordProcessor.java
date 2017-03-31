package home.yura.websearchgui.service.job;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import home.yura.websearchgui.dao.FilterDao;
import home.yura.websearchgui.model.*;
import home.yura.websearchgui.service.DefaultFilterMatcher;
import org.apache.commons.lang3.tuple.Pair;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.core.record.Header;
import org.easybatch.core.record.Record;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import static com.google.common.collect.ImmutableList.of;
import static home.yura.websearchgui.TestUtils.*;
import static home.yura.websearchgui.model.FilterItem.FilterEngine.REG_EXP;
import static home.yura.websearchgui.model.FilterItem.FilterLocation.CONTENT;
import static home.yura.websearchgui.model.FilterItem.FilterPreFormatting.NO;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * @author yuriy.dunko on 10.03.17.
 */
public class TestSearchApplyFiltersRecordProcessor {
    private static final String PAGE_1_LOCATION = "/home/yura/websearchgui/service/job/search/page1_item1.html.gz";
    private static final String PAGE_2_LOCATION = "/home/yura/websearchgui/service/job/search/page1_item2.html.gz";
    private static final String PAGE_3_LOCATION = "/home/yura/websearchgui/service/job/search/page1_item3.html.gz";

    private static final String ANY_WORD = "\\w+";

    @Test
    public void testProcessRecord() throws Exception {
        final int firstFilterId = 1;
        final int secondFilterId = 2;
        final FilterDao filterDao = createDao(FilterDao.class);
        when(filterDao.findBySearchId(anyInt())).thenReturn(of(filter(firstFilterId, ANY_WORD), filter(secondFilterId, ANY_WORD)));

        final SearchApplyFiltersRecordProcessor processor = new SearchApplyFiltersRecordProcessor(filterDao,
                // on this step it's not really matter what ResultEntryDefinition we process, except it searchId
                new DefaultFilterMatcher(), ResultEntryDefinition.create(null, 1, "", null, null, null, null));


        final ImmutableList<Future<Pair<SearchResult, SearchResultContent>>> request = of(futureTuple(PAGE_1_LOCATION),
                futureTuple(PAGE_2_LOCATION), futureTuple(PAGE_3_LOCATION));

        final Record<List<Future<Pair<SearchResult, SearchResultContent>>>> result = processor
                .processRecord(new GenericRecord<>(new Header(1L, "", new Date()), request));

        assertThat(result.getPayload(), both(notNullValue()).and(not(sameInstance(request))));

        final List<Integer> filterIds = result.getPayload().stream()
                .map(f -> process(f::get).getLeft().getFilterItemId()).collect(toList());
        assertThat(filterIds, hasSize(3));
        assertThat(filterIds, hasItems(firstFilterId, firstFilterId, firstFilterId));
    }

    @Test
    public void testProcessRecordWithNoFilters() throws Exception {
        final FilterDao filterDao = createDao(FilterDao.class);
        when(filterDao.findBySearchId(anyInt())).thenReturn(of());

        final SearchApplyFiltersRecordProcessor processor = new SearchApplyFiltersRecordProcessor(filterDao,
                // on this step it's not really matter what ResultEntryDefinition we process, except it searchId
                new DefaultFilterMatcher(), ResultEntryDefinition.create(null, 1, "", null, null, null, null));


        final ImmutableList<Future<Pair<SearchResult, SearchResultContent>>> request = of(
                Futures.immediateFailedFuture(new AssertionError()));

        final Record<List<Future<Pair<SearchResult, SearchResultContent>>>> result = processor
                .processRecord(new GenericRecord<>(new Header(1L, "", new Date()), request));

        assertThat(result.getPayload(), both(notNullValue()).and(sameInstance(request)));
    }

    private Filter filter(final int id, final String expression) {
        return Filter
                .builder()
                .setId(id)
                .setName(randomString())
                .setDescription(randomString())
                .setSearchId(1)
                // this test doesn't test filtering it self
                .addFilterItem(FilterItem.create(id, id, CONTENT, REG_EXP, NO, expression))
                .build();
    }

    private Future<Pair<SearchResult, SearchResultContent>> futureTuple(final String contentPath) {
        return Futures.immediateFuture(Pair.of(
                SearchResult.create(null, randomString(), randomString(), 1, null, System.currentTimeMillis(), randomString(), false),
                SearchResultContent.create(readGzipResource(contentPath))
        ));
    }
}
