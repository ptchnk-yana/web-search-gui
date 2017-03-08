package home.yura.websearchgui.dao.jdbi;


import home.yura.websearchgui.dao.SearchResultDao;
import home.yura.websearchgui.dao.jdbi.SearchResultJdbiResourceDao.SearchResultJdbiResource;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.util.LocalJdbis;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static home.yura.websearchgui.model.SearchResult.copyWithId;
import static java.util.Objects.requireNonNull;

/**
 * @author yuriy.dunko on 02.03.17.
 */
public class SearchResultJdbiResourceDao extends AbstractJdbiResourceDao<SearchResult, SearchResultJdbiResource> implements SearchResultDao {

    protected SearchResultJdbiResourceDao(final DBI dbi) {
        super(dbi, SearchResult.class, SearchResultJdbiResource.class);
    }

    @Override
    public SearchResult add(final SearchResult searchResult) {
        return super.handle(r -> copyWithId(r.insert(requireNonNull(searchResult, "searchResult cannot be null")), searchResult));
    }

    @Override
    public SearchResult get(final int id) {
        return handle(s -> s.findById(id));
    }

    @Override
    public void setViewed(final int id, final boolean viewed) {
        super.handle(r -> r.setViewed(id, viewed));
    }

    @Override
    public List<SearchResult> findByFilterId(final Integer filterId) {
        return super.handle(r -> r.findByFilterId(filterId));
    }

    @RegisterMapper(SearchResultJdbiMapper.class)
    public static interface SearchResultJdbiResource extends AbstractJdbiResourceDao.SqlObjectType<SearchResult> {

        @SqlUpdate("UPDATE search_result SET viewed = :viewed WHERE id = :id")
        int setViewed(@Bind("id") final int id, @Bind("viewed") final boolean viewed);

        @SqlUpdate("INSERT INTO search_result (                                                                     " +
                "   name, description, result_entry_definition_id, filter_item_id, internal_id, viewed              " +
                ") VALUES (                                                                                         " +
                "   :s.name, :s.description, :s.resultEntryDefinitionId, :s.filterItemId, :s.internalId, :s.viewed) ")
        @GetGeneratedKeys
        int insert(@BindBean("s") SearchResult s);

        @SqlUpdate("DELETE FROM search_result WHERE id = :id")
        int delete(@Bind("id") int id);

        @SqlQuery("SELECT                                                                                   " +
                "   id, name, description, result_entry_definition_id, filter_item_id, internal_id, viewed  " +
                "FROM                                                                                       " +
                "   search_result                                                                           " +
                "WHERE                                                                                      " +
                "   id = :id                                                                                ")
        SearchResult findById(@Bind("id") int id);

        @SqlQuery("SELECT                                                                                   " +
                "   id, name, description, result_entry_definition_id, filter_item_id, internal_id, viewed  " +
                "FROM                                                                                       " +
                "   search_result                                                                           ")
        List<SearchResult> findAll();

        @SqlQuery("SELECT                                                                                                       " +
                "   sr.id, sr.name, sr.description, sr.result_entry_definition_id, sr.filter_item_id, sr.internal_id, sr.viewed " +
                "FROM                                                                                                           " +
                "   search_result sr                                                                                            " +
                "   LEFT JOIN filter_item fi ON fi.id = sr.filter_item_id                                                       " +
                "   LEFT JOIN filter f ON f.id = fi.filter_id                                                                   " +
                "WHERE                                                                                                          " +
                "   (:filterId IS NULL AND sr.filter_item_id IS NULL) OR f.id = :filterId                                       ")
        List<SearchResult> findByFilterId(@Bind("filterId") final Integer filterId);
    }

    public static class SearchResultJdbiMapper implements ResultSetMapper<SearchResult> {
        @Override
        public SearchResult map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            return SearchResult.create(
                    r.getInt("id"),
                    r.getString("name"),
                    r.getString("description"),
                    r.getInt("result_entry_definition_id"),
                    LocalJdbis.extractIntValue(r, "filter_item_id", null),
                    r.getLong("internal_id"),
                    r.getBoolean("viewed"));
        }
    }
}
