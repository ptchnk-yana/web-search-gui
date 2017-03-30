package home.yura.websearchgui.dao.jdbi;


import com.google.common.collect.ImmutableMap;
import home.yura.websearchgui.dao.SearchResultDao;
import home.yura.websearchgui.dao.jdbi.SearchResultJdbiResourceDao.SearchResultJdbiResource;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.util.LocalJdbis;
import home.yura.websearchgui.util.bean.BiTuple;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.PreparedBatch;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.IntegerColumnMapper;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static home.yura.websearchgui.util.LocalBeans.beanToMap;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * @author yuriy.dunko on 02.03.17.
 */
public class SearchResultJdbiResourceDao extends AbstractJdbiResourceDao<SearchResult, SearchResultJdbiResource>
        implements SearchResultDao {

    private static final String INSERT_QUERY = LocalJdbis.createInsertFromSelectQuery(
            "search_result",
            ImmutableMap.<String, BiTuple<String, String>>builder()
                    .put("s.name", new BiTuple<>("name", "CHAR(128)"))
                    // TODO: use binary
                    .put("s.description", new BiTuple<>("description", "CHAR(1024)"))
                    .put("s.resultEntryDefinitionId", new BiTuple<>("result_entry_definition_id", "SIGNED"))
                    .put("s.filterItemId", new BiTuple<>("filter_item_id", "SIGNED"))
                    .put("s.internalId", new BiTuple<>("internal_id", "SIGNED"))
                    .put("s.url", new BiTuple<>("url", "CHAR(256)"))
                    .put("s.viewed", new BiTuple<>("viewed", "SIGNED")).build(),
            ImmutableMap.of("s.resultEntryDefinitionId", "result_entry_definition_id", "s.internalId", "internal_id"),
            null);

    public SearchResultJdbiResourceDao(final DBI dbi) {
        super(dbi, SearchResult.class, SearchResultJdbiResource.class);
    }

    @Override
    public SearchResult add(final SearchResult searchResult) {
        this.log.debug("Adding [" + searchResult + "]");
        return ofNullable(this.dbi.inTransaction(
                (conn, s) -> conn.createStatement(INSERT_QUERY)
                        .bindFromMap(beanToMap("s", searchResult))
                        .executeAndReturnGeneratedKeys(IntegerColumnMapper.PRIMITIVE)
                        .first()))
                .map(searchResult::copyWithId)
                .orElseThrow(() -> new IllegalStateException("Cannot add " + searchResult));
    }

    @Override
    public int[] addBatch(final Collection<SearchResult> collection) {
        this.log.debug("Adding batch " + collection);
        return this.dbi.inTransaction((conn, s) -> {
            final List<Integer> result = new ArrayList<>();
            final AtomicInteger iterator = new AtomicInteger();
            final PreparedBatch batch = conn.prepareBatch(INSERT_QUERY);
            collection.forEach(sr -> {
                batch.add().bindFromMap(beanToMap("s", sr));
                if (iterator.incrementAndGet() % BATCH_SIZE == 0) {
                    result.addAll(batch.executeAndGenerateKeys(IntegerColumnMapper.PRIMITIVE).list());
                }
            });
            if (batch.size() != 0) {
                result.addAll(batch.executeAndGenerateKeys(IntegerColumnMapper.PRIMITIVE).list());
            }

            return result.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).toArray();
        });
    }

    @Override
    public int updateBatch(final Collection<SearchResult> batch) {
        this.log.debug("Updating batch " + batch);
        return Arrays.stream((int[]) inTransaction(handle -> handle.updateBatch(batch))).sum();
    }

    @Override
    public void setViewed(final int id, final boolean viewed) {
        this.log.debug("Set viewed [" + id + "] to [" + viewed + "]");
        super.inTransaction(r -> r.setViewed(id, viewed));
    }

    @Override
    public SearchResult get(final int id) {
        this.log.debug("Getting by id [" + id + "]");
        return inTransaction(s -> s.findById(id));
    }

    @Override
    public List<SearchResult> findByFilterId(@Nullable final Integer filterId, final Integer startFrom, final int limit) {
        this.log.debug("Getting all by filterId [" + filterId+ "] stating from [" + startFrom + "] limited to [" + limit + "]");
        return this.dbi.withHandle(handle -> handle.attach(this.sqlObjectType).findByFilterId(filterId, startFrom, limit));
    }

    @Override
    public List<SearchResult> findBySearchId(@Nullable final Integer searchId, final Integer startFrom, final int limit) {
        this.log.debug("Getting all by searchId [" + searchId+ "] stating from [" + startFrom + "] limited to [" + limit + "]");
        requireNonNull(searchId, "searchId");
        return this.dbi.withHandle(handle -> handle.attach(this.sqlObjectType).findBySearchId(searchId, startFrom, limit));
    }

    @RegisterMapper(SearchResultJdbiMapper.class)
    public static interface SearchResultJdbiResource extends AbstractJdbiResourceDao.SqlObjectType<SearchResult> {

        @SqlBatch("UPDATE search_result " +
                "SET name = :sr.name," +
                " description = :sr.description," +
                " result_entry_definition_id = :sr.resultEntryDefinitionId," +
                " filter_item_id = :sr.filterItemId," +
                " internal_id = :sr.internalId," +
                " url = :sr.url," +
                " viewed = :sr.viewed " +
                "WHERE id = :sr.id")
        @BatchChunkSize(BATCH_SIZE)
        int[] updateBatch(@BindBean("sr") Collection<SearchResult> batch);

        @SqlUpdate("UPDATE search_result SET viewed = :viewed WHERE id = :id")
        int setViewed(@Bind("id") int id, @Bind("viewed") boolean viewed);

        @SqlUpdate("DELETE FROM search_result WHERE id = :id")
        int delete(@Bind("id") int id);

        @SqlQuery("SELECT                                                                                       " +
                "   id, name, description, result_entry_definition_id, filter_item_id, internal_id, url, viewed " +
                "FROM                                                                                           " +
                "   search_result                                                                               " +
                "WHERE                                                                                          " +
                "   id = :id                                                                                    ")
        SearchResult findById(@Bind("id") int id);

        @SqlQuery("SELECT                                                                                       " +
                "   id, name, description, result_entry_definition_id, filter_item_id, internal_id, url, viewed " +
                "FROM                                                                                           " +
                "   search_result                                                                               ")
        List<SearchResult> findAll();

        @SqlQuery("SELECT                                                                   " +
                "   sr.id, sr.name, sr.description, sr.result_entry_definition_id,          " +
                "   sr.filter_item_id, sr.internal_id, sr.url, sr.viewed                    " +
                "FROM                                                                       " +
                "   search_result sr                                                        " +
                "   LEFT JOIN filter_item fi ON fi.id = sr.filter_item_id                   " +
                "   LEFT JOIN filter f ON f.id = fi.filter_id                               " +
                "WHERE                                                                      " +
                "   ((:filterId IS NULL AND sr.filter_item_id IS NULL) OR f.id = :filterId) " +
                "   AND (:startFrom IS NULL OR sr.id > :startFrom)                          " +
                "LIMIT                                                                      " +
                "   :limit                                                                  ")
        List<SearchResult> findByFilterId(@Bind("filterId") Integer filterId,
                                          @Bind("startFrom") Integer startFrom,
                                          @Bind("limit") int limit);

        @SqlQuery("SELECT                                                                   " +
                "   sr.id, sr.name, sr.description, sr.result_entry_definition_id,          " +
                "   sr.filter_item_id, sr.internal_id, sr.url, sr.viewed                    " +
                "FROM                                                                       " +
                "   search_result sr                                                        " +
                "   JOIN result_entry_definition d ON d.id = sr.result_entry_definition_id  " +
                "   JOIN search s ON s.id = d.search_id                                     " +
                "WHERE                                                                      " +
                "   s.id = :searchId                                                        " +
                "   AND (:startFrom IS NULL OR sr.id > :startFrom)                          " +
                "LIMIT                                                                      " +
                "   :limit                                                                  ")
        List<SearchResult> findBySearchId(@Bind("searchId") Integer searchId,
                                          @Bind("startFrom") Integer startFrom,
                                          @Bind("limit") int limit);
    }

    public static class SearchResultJdbiMapper implements ResultSetMapper<SearchResult> {
        @Override
        public SearchResult map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException {
            return SearchResult.create(
                    r.getInt("id"),
                    r.getString("name"),
                    // TODO: SMART encrypt/decrypt description as it CAN be very big
                    // TODO: lazy load description only when it needed
                    r.getString("description"),
                    r.getInt("result_entry_definition_id"),
                    LocalJdbis.extractIntValue(r, "filter_item_id", null),
                    r.getLong("internal_id"),
                    r.getString("url"),
                    r.getBoolean("viewed"));
        }
    }
}
