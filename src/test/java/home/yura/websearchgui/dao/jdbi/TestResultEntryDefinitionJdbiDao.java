package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.ValueEvaluationDefinition;
import home.yura.websearchgui.model.Search;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.assertThat;

/**
 * @author yuriy.dunko on 03.03.17.
 */
public class TestResultEntryDefinitionJdbiDao extends AbstractJdbiTest {

    @Test
    public void add() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition actual = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId(), 2, 3, 0, 1));
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getNameExtractionChain(),
                hasEntry(anyOf(is(1), is(2)), notNullValue(ValueEvaluationDefinition.class)));
        assertThat(actual.getContentLinkExtractionChain(),
                hasEntry(anyOf(is(1), is(2), is(3)), notNullValue(ValueEvaluationDefinition.class)));
        assertThat(actual.getInternalIdExtractionChain().entrySet(), empty());
        assertThat(actual.getContentExtractionChain(), hasEntry(is(1), notNullValue(ValueEvaluationDefinition.class)));
        assertThat(actual.getSearchId(), is(equalTo(search.getId())));
        assertThat(actual.getEntryBlockLocation(), is(not(isEmptyOrNullString())));
    }

    @Test
    public void addWithoutProcessors() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition actual = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId(), 0, 0, 0, 0));
        assertThat(actual.getNameExtractionChain().entrySet(), empty());
        assertThat(actual.getContentLinkExtractionChain().entrySet(), empty());
        assertThat(actual.getInternalIdExtractionChain().entrySet(), empty());
        assertThat(actual.getContentExtractionChain().entrySet(),  empty());

        assertThat(this.resultEntryDefinitionDao.get(actual.getId()), equalTo(actual));
    }

    @Test(expected = RuntimeException.class)
    public void addNotExistingSearch() throws Exception {
        this.resultEntryDefinitionDao.add(randomResultDefinition(-1));
    }

    @Test(expected = NullPointerException.class)
    public void addNullSearch() throws Exception {
        this.resultEntryDefinitionDao.add(randomResultDefinition(null));
    }

    @Test
    @Ignore("Should be performed on a real database")
    public void delete() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition actual = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));

        int deletedNumber = this.resultEntryDefinitionDao.delete(actual);
        assertThat(deletedNumber, is(1));

        deletedNumber = this.resultEntryDefinitionDao.delete(actual);
        assertThat(deletedNumber, is(0));
    }

    @Test
    public void get() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition expected = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final ResultEntryDefinition actual = this.resultEntryDefinitionDao.get(expected.getId());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void list() throws Exception {
        final Search search = this.searchDao.add(randomSearch());
        final ResultEntryDefinition first = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId()));
        final ResultEntryDefinition second = this.resultEntryDefinitionDao.add(randomResultDefinition(search.getId(), 2, 3, 4, 5));
        final List<ResultEntryDefinition> list = this.resultEntryDefinitionDao.list();
        assertThat(list, hasItems(first, second));
    }

    @Test
    public void findBySearchId() throws Exception {
        final Search firstSearch = this.searchDao.add(randomSearch());
        final Search secondSearch = this.searchDao.add(randomSearch());
        final ResultEntryDefinition first = this.resultEntryDefinitionDao.add(randomResultDefinition(firstSearch.getId()));
        final ResultEntryDefinition second = this.resultEntryDefinitionDao.add(randomResultDefinition(secondSearch.getId(), 0, 2));

        ResultEntryDefinition value = this.resultEntryDefinitionDao.findBySearchId(firstSearch.getId());
        assertThat(value, equalTo(first));

        value = this.resultEntryDefinitionDao.findBySearchId(secondSearch.getId());
        assertThat(value, equalTo(second));
        assertThat(value, not(equalTo(first)));
    }

}