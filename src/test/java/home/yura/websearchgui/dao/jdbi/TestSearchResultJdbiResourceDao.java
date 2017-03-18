package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.TestUtils;
import home.yura.websearchgui.dao.SearchResultDao;
import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.SearchResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

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
        assertThat("unique entity", result, equalTo(Arrays.stream(result).collect(toSet()).toArray(new Integer[]{})));
    }

    @Test
    public void updateBatch() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final int size = SearchResultDao.BATCH_SIZE * 5 / 2;

        final List<SearchResult> searchResults = Arrays.stream(this.searchResultDao.addBatch(
                IntStream
                        .range(0, size)
                        .mapToObj(value -> TestUtils.randomSearchResult(definition.getId(), null))
                        .collect(Collectors.toList())))
                .mapToObj(this.searchResultDao::get)
                .collect(Collectors.toList());

        final Filter filter = this.filterDao.add(randomFilter(search.getId()));
        @SuppressWarnings("ConstantConditions")
        final List<SearchResult> updatedSearchResults = searchResults
                .stream()
                .map(searchResult -> searchResult.copyWithFilterItemId(filter.getFilterItems().get(0).getId()))
                .collect(Collectors.toList());
        final int updates = this.searchResultDao.updateBatch(updatedSearchResults);

        assertThat(updates, is(size));
        assertThat(this.searchResultDao.findByFilterId(filter.getId(), null, Integer.MAX_VALUE), equalTo(updatedSearchResults));
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
    public void findAllByFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));
        final SearchResult firstSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), filter.getId()));
        final SearchResult secondSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));

        assertThat(this.searchResultDao.findByFilterId(filter.getId(), null, Integer.MAX_VALUE),
                both(hasItem(firstSearchResult)).and(not(hasItem(secondSearchResult))));
    }

    @Test
    public void findByFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));
        final SearchResult firstSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), filter.getId()));
        final SearchResult secondSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), filter.getId()));
        final SearchResult thirdSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));

        List<SearchResult> list = this.searchResultDao.findByFilterId(filter.getId(), null, 1);
        assertThat(list, hasSize(1));
        assertThat(list, both(hasItem(firstSearchResult)).and(not(hasItem(secondSearchResult))).and(not(hasItem(thirdSearchResult))));

        list = this.searchResultDao.findByFilterId(filter.getId(), list.stream().findFirst().orElse(null).getId(), 2);
        assertThat(list, hasSize(1));
        assertThat(list, both(hasItem(secondSearchResult)).and(not(hasItem(firstSearchResult))).and(not(hasItem(thirdSearchResult))));
    }

    @Test
    public void findBySearchId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult firstSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));
        final SearchResult secondSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));
        final SearchResult thirdSearchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));

        assertThat(this.searchResultDao.delete(secondSearchResult), is(1));

        List<SearchResult> list = this.searchResultDao.findBySearchId(search.getId(), null, 1);
        assertThat(list, hasSize(1));
        assertThat(list, both(hasItem(firstSearchResult)).and(not(hasItem(secondSearchResult))).and(not(hasItem(thirdSearchResult))));

        list = this.searchResultDao.findBySearchId(search.getId(), list.stream().findFirst().orElse(null).getId(), 2);
        assertThat(list, hasSize(1));
        assertThat(list, both(hasItem(thirdSearchResult)).and(not(hasItem(firstSearchResult))).and(not(hasItem(secondSearchResult))));
    }

    @Test
    public void findAllByNullFilterId() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final SearchResult searchResult = this.searchResultDao.add(TestUtils.randomSearchResult(definition.getId(), null));
        assertThat(this.searchResultDao.findByFilterId(null, null, Integer.MAX_VALUE), hasItem(searchResult));
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