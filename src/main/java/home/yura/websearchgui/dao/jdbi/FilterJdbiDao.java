package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.dao.FilterDao;
import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.FilterItem;
import home.yura.websearchgui.util.LocalFunctions;
import home.yura.websearchgui.util.LocalJdbis;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.util.IntegerColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static home.yura.websearchgui.util.LocalBeans.beanToMap;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yura on 27.02.17.
 */
public class FilterJdbiDao extends AbstractJdbiDao<Filter> implements FilterDao {

    /**
     * Requires: FilterItem fi
     */
    private static final String INSERT_ITEM_SQL = "" +
            // batch processing mace us have simple formatting in favour fo the batch size
            "INSERT INTO filter_item (filter_id, filter_location, filter_engine, filter_pre_formatting, expression) " +
            "VALUES (:fi.filterId, :fi.filterLocation, :fi.filterEngine, :fi.filterPreFormatting, :fi.expression) ";

    /**
     * Requires: Filter f
     */
    private static final String INSERT_FILTER_SQL = "" +
            "INSERT INTO filter (                       " +
            "   name, description, search_id            " +
            ") VALUES (                                 " +
            "   :f.name, :f.description, :f.searchId)   ";

    /**
     * Requires: Integer id - the id of the filter
     */
    private static final String RESET_SEARCH_RESULT_SQL = "" +
            "UPDATE                                                     " +
            "   search_result s                                         " +
            "   INNER JOIN filter_item fi ON fi.id = s.filter_item_id   " +
            "   INNER JOIN filter f ON f.id = fi.filter_id              " +
            "SET                                                        " +
            "   s.filter_item_id = NULL                                 " +
            "WHERE                                                      " +
            "   f.id = :id                                              ";

    /**
     * No requires
     */
    private static final String SELECT_FILTERS_SQL = "" +
            "SELECT                                                     " +
            "   f.id AS f_id,                                           " +
            "   f.name AS f_name,                                       " +
            "   f.description AS f_description,                         " +
            "   f.search_id AS f_search_id,                             " +
            "   fi.id AS fi_id,                                         " +
            "   fi.filter_location AS fi_filter_location,               " +
            "   fi.filter_engine AS fi_filter_engine,                   " +
            "   fi.filter_pre_formatting AS fi_filter_pre_formatting,   " +
            "   fi.expression AS fi_expression                          " +
            "FROM                                                       " +
            "   filter f                                                " +
            "   LEFT JOIN filter_item fi ON fi.filter_id = f.id        ";

    /**
     * Requires: Integer id - the id of the filter
     */
    private static final String SELECT_FILTER_SQL = SELECT_FILTERS_SQL + " WHERE f.id = :id ";

    /**
     * Requires: Integer sid - the id of the search
     */
    private static final String SELECT_FILTERS_BY_SEARCH_ID_SQL = SELECT_FILTERS_SQL + " WHERE f.search_id = :sid ";

    public FilterJdbiDao(final DBI dbi) {
        super(dbi);
    }

    @Override
    public Filter add(final Filter filter) {
        final Filter.Builder builder = requireNonNull(filter, "filter").buildNew().setFilterItems(null);

        try (final Handle h = this.dbi.open()) {
            return h.inTransaction((conn, status) -> {
                final Integer filterId = conn.createStatement(INSERT_FILTER_SQL)
                        .bindFromMap(beanToMap("f", filter))
                        .executeAndReturnGeneratedKeys(IntegerColumnMapper.PRIMITIVE)
                        .first();

                Optional.ofNullable(filter.getFilterItems())
                        .ifPresent(items -> items
                                .stream()
                                .map(item -> item.copyWithFilterId(filterId))
                                .forEach(item -> requireNonNull(builder).addFilterItem(item.copyWithId(conn
                                        .createStatement(INSERT_ITEM_SQL)
                                        .bindFromMap(beanToMap("fi", item))
                                        .executeAndReturnGeneratedKeys(IntegerColumnMapper.PRIMITIVE)
                                        .first()
                                ))));

                return requireNonNull(builder).setId(filterId).build();
            });
        }
    }

    @Override
    public FilterItem addItem(final FilterItem item) {
        requireNonNull(item, "item");
        requireNonNull(item.getFilterId(), "item.filterId");
        try (Handle h = this.dbi.open()) {
            return item.copyWithId(h.createStatement(INSERT_ITEM_SQL)
                    .bindFromMap(beanToMap("fi", item))
                    .executeAndReturnGeneratedKeys(IntegerColumnMapper.PRIMITIVE)
                    .first());
        }
    }

    @Override
    public int delete(final Filter filter) {
        requireNonNull(filter, "filter");
        try (Handle h = this.dbi.open()) {
            return h.inTransaction((conn, status) -> {
                conn.createStatement(RESET_SEARCH_RESULT_SQL).bind("id", filter.getId()).execute();
                return conn.createStatement("DELETE f, fi FROM filter f LEFT JOIN filter_item fi on fi.filter_id = f.id WHERE f.id = :id")
                        .bind("id", filter.getId())
                        .execute();
            });
        }
    }

    @Override
    public Filter get(final int id) {
        try (Handle h = this.dbi.open()) {
            return h.createQuery(SELECT_FILTER_SQL)
                    .bind("id", id)
                    .fold(new HashMap<>(), this::foldFilters)
                    .values()
                    .stream()
                    .map(Filter.Builder::build)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public List<Filter> list() {
        try (Handle h = this.dbi.open()) {
            return h.createQuery(SELECT_FILTERS_SQL)
                    .fold(new HashMap<>(), this::foldFilters)
                    .values()
                    .stream()
                    .map(Filter.Builder::build)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Filter> findBySearchId(final int searchId) {
        try (Handle h = this.dbi.open()) {
            return h.createQuery(SELECT_FILTERS_BY_SEARCH_ID_SQL)
                    .bind("sid", searchId)
                    .fold(new HashMap<>(), this::foldFilters)
                    .values()
                    .stream()
                    .map(Filter.Builder::build)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Class<Filter> getGenericType() {
        return Filter.class;
    }

    private HashMap<Integer, Filter.Builder> foldFilters(final HashMap<Integer, Filter.Builder> map,
                                                         final ResultSet rs,
                                                         final StatementContext ctx) throws SQLException {
        final int currentId = rs.getInt("f_id");

        Filter.Builder builder = map.get(currentId);
        if (builder == null) {
            map.put(currentId, builder = getFilterBuilder(rs));
        }

        final FilterItem filterItem = getFilterItem(rs);
        if (filterItem != null) {
            builder.addFilterItem(filterItem);
        }

        return map;
    }

    private Filter.Builder getFilterBuilder(final ResultSet rs) throws SQLException {
        return Filter.builder()
                .setId(rs.getInt("f_id"))
                .setName(rs.getString("f_name"))
                .setDescription(rs.getString("f_description"))
                .setSearchId(rs.getInt("f_search_id"));
    }

    private FilterItem getFilterItem(final ResultSet rs) throws SQLException {
        final Integer itemId = LocalJdbis.extractIntValue(rs, "fi_id", null);
        return itemId == null ? null : FilterItem.create(
                itemId,
                rs.getInt("f_id"),
                Enum.valueOf(FilterItem.FilterLocation.class, rs.getString("fi_filter_location")),
                Enum.valueOf(FilterItem.FilterEngine.class, rs.getString("fi_filter_engine")),
                Enum.valueOf(FilterItem.FilterPreFormatting.class, rs.getString("fi_filter_pre_formatting")),
                rs.getString("fi_expression"));
    }
}
