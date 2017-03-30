package home.yura.websearchgui.service.job;

import home.yura.websearchgui.dao.jdbi.AbstractJdbiTest;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.SearchResult;
import org.easybatch.core.record.Record;
import org.junit.Test;

import java.util.List;

import static home.yura.websearchgui.TestUtils.randomSearchResult;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link ExistingSearchRecordReader}
 * @author yuriy.dunko on 19.03.17.
 */
public class TestExistingSearchRecordReader extends AbstractJdbiTest {

    @Test
    public void readRecord() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult firstSearchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));
        final SearchResult secondSearchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));
        final SearchResult thirdSearchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));

        final ExistingSearchRecordReader searchRecordReader = new ExistingSearchRecordReader(
                this.searchResultDao, search, 2);
        searchRecordReader.open();

        @SuppressWarnings("unchecked")
        final Record<List<SearchResult>> record1 = searchRecordReader.readRecord();
        assertThat(record1, notNullValue());
        assertThat(record1.getPayload(), hasSize(2));
        assertThat(record1.getPayload(), hasItems(firstSearchResult, secondSearchResult));

        @SuppressWarnings("unchecked")
        final Record<List<SearchResult>> record2 = searchRecordReader.readRecord();
        assertThat(record2, notNullValue());
        assertThat(record2.getPayload(), hasSize(1));
        assertThat(record2.getPayload(), hasItem(thirdSearchResult));

        assertThat(searchRecordReader.readRecord(), is(nullValue()));

        searchRecordReader.close();
    }

}