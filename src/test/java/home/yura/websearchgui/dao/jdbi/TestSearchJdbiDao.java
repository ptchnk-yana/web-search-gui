package home.yura.websearchgui.dao.jdbi;

import com.google.common.collect.ImmutableMap;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.ValueEvaluationDefinition;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author yuriy.dunko on 02.03.17.
 */
public class TestSearchJdbiDao extends AbstractJdbiTest {

    @Test
    public void add() throws Exception {
        final Search search = randomSearch();
        final Search fromDb = super.searchDao.add(search);
        assertThat("value read from database is null", fromDb, notNullValue());
        assertThat("value read from database is null", fromDb.getId(), notNullValue());
        assertThat(fromDb, equalTo(Search.create(
                fromDb.getId(),
                search.getName(),
                search.getDescription(),
                search.getUrl(),
                ImmutableMap.of(1, ValueEvaluationDefinition.create(
                        fromDb.getPreviousLinkLocation().get(1).getId(),
                        search.getPreviousLinkLocation().get(1).getType(),
                        search.getPreviousLinkLocation().get(1).getEngine(),
                        search.getPreviousLinkLocation().get(1).getExpression()
                )), ImmutableMap.of(1, ValueEvaluationDefinition.create(
                        fromDb.getNextLinkLocation().get(1).getId(),
                        search.getNextLinkLocation().get(1).getType(),
                        search.getNextLinkLocation().get(1).getEngine(),
                        search.getNextLinkLocation().get(1).getExpression()
                )))));
    }

    @Test
    @Ignore("Should be performed on a real database")
    public void delete() throws Exception {
        final Search fromDb = super.searchDao.add(randomSearch());
        int deleteNumber = super.searchDao.delete(fromDb);
        assertThat("Deleted wrong number of items", deleteNumber, is(1));

        deleteNumber = super.searchDao.delete(fromDb);
        assertThat("Same item was deleted more than one time", deleteNumber, is(0));
    }

    @Test
    public void get() throws Exception {
        final Search fromDb = super.searchDao.add(randomSearch());
        final Search readItem = super.searchDao.get(Objects.requireNonNull(fromDb.getId()));
        assertThat("Stored and read items are different", fromDb, is(equalTo(readItem)));
    }

    @Test
    public void list() throws Exception {
        final Search first = super.searchDao.add(randomSearch());
        final Search second = super.searchDao.add(randomSearch());

        final List<Search> list = super.searchDao.list();
        assertThat("Stored and read items are different", list, hasItems(first, second));
    }

}