package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.TestUtils;
import home.yura.websearchgui.dao.SearchResultDao;
import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.SearchResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.junit.Assert.*;

/**
 * Test for {@link SearchResultJdbiResourceDao}
 *
 * @author yuriy.dunko on 02.03.17.
 */
public class TestSearchResultJdbiResourceDao extends AbstractJdbiTest {

    @Test
    public void add() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));
        assertThat(searchResult, is(notNullValue()));
        assertThat(searchResult.getId(), is(notNullValue()));
    }

    @Test
    public void addBatch() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final int size = SearchResultDao.BATCH_SIZE * 5 / 2;
        final Integer[] result = Arrays.stream(this.searchResultDao.addBatch(
                IntStream
                        .range(0, size)
                        .mapToObj(value -> TestUtils.randomSearchResult(definition.getId(), null))
                        .collect(Collectors.toList())))
                .mapToObj(Integer::valueOf)
                .toArray(Integer[]::new);

        assertThat(result, arrayWithSize(size));
        assertThat(result, equalTo(Arrays.stream(result).collect(toSet()).toArray(new Integer[]{})));
    }

    @Test
    public void addWithFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));

        final SearchResult searchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), filter.getId()));
        assertThat(searchResult, is(notNullValue()));
        assertThat(searchResult.getId(), is(notNullValue()));
    }

    @Test(expected = RuntimeException.class)
    public void addNotExistingDefinition() throws Exception {
        this.searchResultDao.add(TestUtils.randomSearchResult(-1, null));
    }

    @Test(expected = NullPointerException.class)
    public void addNullDefinition() throws Exception {
        this.searchResultDao.add(TestUtils.randomSearchResult(null, null));
    }

    @Test(expected = RuntimeException.class)
    public void addWithNotExistingFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));

        final SearchResult searchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), -1));
        assertThat(searchResult, is(notNullValue()));
        assertThat(searchResult.getId(), is(notNullValue()));
    }

    @Test
    public void setViewed() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));
        assertThat(searchResult.isViewed(), is(false));
        this.searchResultDao.setViewed(requireNonNull(searchResult.getId()), true);
        assertThat(this.searchResultDao.get(searchResult.getId()).isViewed(), is(true));
    }

    @Test
    public void findByFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));
        final SearchResult firstSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), filter.getId()));
        final SearchResult secondSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));

        assertThat(this.searchResultDao.findByFilterId(filter.getId()), both(hasItem(firstSearchResult)).and(not(hasItem(secondSearchResult))));
    }

    @Test
    public void findByNullFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));
        assertThat(this.searchResultDao.findByFilterId(null), hasItem(searchResult));
    }

    @Test
    public void delete() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));
        assertThat(this.searchResultDao.delete(searchResult), is(1));
        assertThat(this.searchResultDao.delete(searchResult), is(0));
    }

    @Test
    public void get() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));
        assertThat(this.searchResultDao.get(requireNonNull(searchResult.getId())), equalTo(searchResult));
    }

    @Test
    public void list() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));
        assertThat(this.searchResultDao.list(), hasItem(searchResult));
    }

}