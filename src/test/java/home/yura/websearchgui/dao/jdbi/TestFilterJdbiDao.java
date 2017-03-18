package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.TestUtils;
import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.FilterItem;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

/**
 * @author yuriy.dunko on 03.03.17.
 */
public class TestFilterJdbiDao extends AbstractJdbiTest {

    @Test
    public void add() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));
        assertThat(filter, notNullValue());
        assertThat(filter.getId(), notNullValue());
        assertThat(filter.getFilterItems(), hasItems(notNullValue()));
        assertThat(filter.getFilterItems().get(0).getId(), notNullValue());
    }

    @Test
    public void addWithoutItems() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final Filter filter = this.filterDao.add(randomFilter(search.getId(), 0));
        assertThat(filter.getFilterItems(), is(empty()));
        assertThat(this.filterDao.get(filter.getId()), equalTo(filter));
    }

    @Test(expected = RuntimeException.class)
    public void addNotExistingSearch() throws Exception {
        this.filterDao.add(randomFilter(-1));
    }

    @Test(expected = IllegalStateException.class)
    public void addNullSearch() throws Exception {
        this.filterDao.add(randomFilter(null));
    }

    @Test
    public void addItem() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final Filter initial = this.filterDao.add(randomFilter(search.getId()));
        final FilterItem filterItem = this.filterDao.addItem(randomFilterItem(initial.getId()));
        assertThat(filterItem, notNullValue());
        assertThat(filterItem.getId(), notNullValue());

        final Filter actual = this.filterDao.get(initial.getId());
        assertThat(initial.getFilterItems(), not(hasItems(filterItem)));
        assertThat(actual.getFilterItems(), hasItems(filterItem));
    }

    @Test(expected = RuntimeException.class)
    public void addItemNotExistingFilter() throws Exception {
        this.filterDao.addItem(randomFilterItem(-1));
    }

    @Test(expected = NullPointerException.class)
    public void addItemNnllFilter() throws Exception {
        this.filterDao.addItem(randomFilterItem(null));
    }

    @Test
    @Ignore("Should be performed on a real database")
    public void delete() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));
        final Integer filterItemId = filter.getFilterItems().get(0).getId();
        final ResultEntryDefinition resultDefinition = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        this.searchResultDao.add(TestUtils.randomSearchResult(resultDefinition.getSearchId(), filterItemId));

        int deleteNumber = this.filterDao.delete(filter);
        assertThat(deleteNumber, is(1));
        assertThat(this.searchResultDao.findByFilterId(filterItemId, null, Integer.MAX_VALUE), is(empty()));

        deleteNumber = this.filterDao.delete(filter);
        assertThat(deleteNumber, is(0));
    }

    @Test
    public void get() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));
        assertThat(filter, equalTo(this.filterDao.get(filter.getId())));
    }

    @Test
    public void list() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final Filter filter = this.filterDao.add(randomFilter(search.getId()));
        assertThat(this.filterDao.list(), hasItems(filter));
    }

    @Test
    public void findBySearchId() throws Exception {
        final Search firstSearch = this.searchDao.add(randomSearch());
        final Filter firstFilter = this.filterDao.add(randomFilter(firstSearch.getId()));
        final Search secondSearch = this.searchDao.add(randomSearch());
        final Filter secondFilter = this.filterDao.add(randomFilter(secondSearch.getId()));

        final List<Filter> byFirstSearchId = this.filterDao.findBySearchId(firstSearch.getId());
        assertThat(byFirstSearchId, hasItems(firstFilter));
        assertThat(byFirstSearchId, not(hasItems(secondFilter)));

        final List<Filter> bySecondSearchId = this.filterDao.findBySearchId(secondSearch.getId());
        assertThat(bySecondSearchId, hasItems(secondFilter));
        assertThat(bySecondSearchId, not(hasItems(firstFilter)));
    }

}