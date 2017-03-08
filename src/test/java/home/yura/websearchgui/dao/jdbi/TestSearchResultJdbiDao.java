package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.SearchResult;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author yuriy.dunko on 02.03.17.
 */
public class TestSearchResultJdbiDao extends AbstractJdbiTest {

    @Test
    public void add() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));
        assertThat(searchResult, is(notNullValue()));
        assertThat(searchResult.getId(), is(notNullValue()));
    }

    @Test
    public void addWithFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));

        final SearchResult searchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), filter.getId()));
        assertThat(searchResult, is(notNullValue()));
        assertThat(searchResult.getId(), is(notNullValue()));
    }

    @Test(expected = RuntimeException.class)
    public void addNotExistingDefinition() throws Exception {
        this.searchResultDao.add(randomSearchResult(-1, null));
    }

    @Test(expected = NullPointerException.class)
    public void addNullDefinition() throws Exception {
        this.searchResultDao.add(randomSearchResult(null, null));
    }

    @Test(expected = RuntimeException.class)
    public void addWithNotExistingFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));

        final SearchResult searchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), -1));
        assertThat(searchResult, is(notNullValue()));
        assertThat(searchResult.getId(), is(notNullValue()));
    }

    @Test
    public void setViewed() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));
        assertThat(searchResult.isViewed(), is(false));
        this.searchResultDao.setViewed(searchResult.getId(), true);
        assertThat(this.searchResultDao.get(searchResult.getId()).isViewed(), is(true));
    }

    @Test
    public void findByFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));
        final SearchResult firstSearchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), filter.getId()));
        final SearchResult secondSearchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));

        assertThat(this.searchResultDao.findByFilterId(filter.getId()), both(hasItem(firstSearchResult)).and(not(hasItem(secondSearchResult))));
    }

    @Test
    public void findByNullFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));
        assertThat(this.searchResultDao.findByFilterId(null), hasItem(searchResult));
    }

    @Test
    public void delete() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));
        assertThat(this.searchResultDao.delete(searchResult), is(1));
        assertThat(this.searchResultDao.delete(searchResult), is(0));
    }

    @Test
    public void get() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));
        assertThat(this.searchResultDao.get(searchResult.getId()), equalTo(searchResult));
    }

    @Test
    public void list() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(randomSearchResult(definition.getId(), null));
        assertThat(this.searchResultDao.list(), hasItem(searchResult));
    }

}