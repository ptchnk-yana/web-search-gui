package home.yura.websearchgui.service.job;

import home.yura.websearchgui.dao.SearchResultDao;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.SearchResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easybatch.core.reader.RecordReader;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.core.record.Header;
import org.easybatch.core.record.Record;

import java.util.Date;
import java.util.List;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yuriy.dunko on 19.03.17.
 */
public class ExistingSearchRecordReader implements RecordReader {
    private static final Log LOG = LogFactory.getLog(ExistingSearchRecordReader.class);

    private final SearchResultDao searchResultDao;

    private final Search search;
    private final int readBatchSize;

    private Integer lastReadId;

    public ExistingSearchRecordReader(final SearchResultDao searchResultDao,
                                      final Search search,
                                      final int readBatchSize) {
        this.searchResultDao = requireNonNull(searchResultDao, "searchResultDao");
        this.search = requireNonNull(search, "search");
        this.readBatchSize = readBatchSize;
    }

    @Override
    public void open() throws Exception {
        LOG.info("Opening reader");
        this.lastReadId = null;
    }

    @Override
    public Record readRecord() throws Exception {
        LOG.info("Reading next record");
        final List<SearchResult> searchResults = this.searchResultDao.findBySearchId(this.search.getId(),
                this.lastReadId, this.readBatchSize);
        if (searchResults.isEmpty()) {
            return null;
        }

        this.lastReadId = searchResults.stream().map(SearchResult::getId).max(Integer::compare).orElseThrow(IllegalStateException::new);
        LOG.info("Last read id [" + this.lastReadId + "]");

        return new GenericRecord<>(
                new Header((long) this.lastReadId, search.getUrl(), new Date()), // FIXME: Get date from some util!!!
                searchResults);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Closing reader");
        this.lastReadId = null;
    }
}
