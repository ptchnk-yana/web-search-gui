package home.yura.websearchgui.service.job;

import com.google.common.util.concurrent.Futures;
import home.yura.websearchgui.dao.SearchResultDao;
import home.yura.websearchgui.dao.rsource.SearchResultContentResource;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import org.apache.commons.lang3.tuple.Pair;
import org.easybatch.core.record.Batch;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.core.record.Header;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import static home.yura.websearchgui.TestUtils.createDao;
import static home.yura.websearchgui.TestUtils.randomString;
import static home.yura.websearchgui.service.job.SearchResultRecordWriter.createFromDao;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * @author yuriy.dunko on 12.03.17.
 */
public class TestSearchResultRecordWriter {
    @Test
    public void writeRecords() throws Exception {
        final SearchResultDao searchResultDao = createDao(SearchResultDao.class);
        final SearchResultContentResource contentResource = mock(SearchResultContentResource.class);
        final SearchResultRecordWriter writer = createFromDao(searchResultDao, Optional.of(contentResource));

        writer.writeRecords(new Batch(record(), record(), record()));

        verify(searchResultDao, times(3)).addBatch(anyCollection());
        verify(contentResource, times(3)).addBatch(anyCollection());
    }

    private GenericRecord<List<Future<Pair<SearchResult, SearchResultContent>>>> record() {
        return new GenericRecord<>(header(), input(40));
    }

    private Header header() {
        return new Header(System.currentTimeMillis(), randomString(), new Date());
    }

    private List<Future<Pair<SearchResult, SearchResultContent>>> input(final int number) {
        return range(0, number).mapToObj(Integer::valueOf).map(this::futureTuple).collect(toList());
    }

    private Future<Pair<SearchResult, SearchResultContent>> futureTuple(final int id) {
        return Futures.immediateFuture(Pair.of(
                SearchResult.create(null, randomString(), randomString(), 1, null, System.currentTimeMillis(), randomString(), false),
                SearchResultContent.create(range(0, 1000).mapToObj(Integer::valueOf).collect(toList()).toString())
        ));
    }

}