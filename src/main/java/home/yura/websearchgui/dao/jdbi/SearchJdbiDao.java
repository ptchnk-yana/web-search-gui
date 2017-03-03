package home.yura.websearchgui.dao.jdbi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import home.yura.websearchgui.dao.SearchDao;
import home.yura.websearchgui.model.AbstractModel;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.model.ValueEvaluationDefinition;
import home.yura.websearchgui.util.LocalBeans;
import home.yura.websearchgui.util.LocalJdbis;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author yuriy.dunko on 26.02.17.
 */
public class SearchJdbiDao extends AbstractJdbiDao<Search> implements SearchDao {
    private static final String PREVIOUS_LINK_DESTINATION = "previous_link";
    private static final String NEXT_LINK_DESTINATION = "next_link";
    private static final List<String> DESTINATIONS = ImmutableList.of(PREVIOUS_LINK_DESTINATION, NEXT_LINK_DESTINATION);

    /**
     * Requires: Search s - the Search which should be stored
     */
    private static final String INSERT_SEARCH_SQL = "" +
            "INSERT INTO search (name, description, url) VALUES (:s.name, :s.description, :s.url)";

    /**
     * Requires: ValueEvaluationDefinition p - the ValueEvaluationDefinition (part of a ResultEntryDefinition)
     */
    private static final String INSERT_VALUE_EVALUATION_DEFINITION_SQL = "" +
            // batch processing make us have simple formatting in favour of the batch size
            "INSERT INTO value_evaluation_definition (processing_type, result_processing_engine, expression) " +
            "VALUES (:p.type, :p.engine, :p.expression)";

    /**
     * Requires: destination, position, resultEntryProcessorId, resultEntryDefinitionId
     */
    private static final String INSERT_VALUE_EVALUATION_DEFINITION_MAPPING_SQL = "" +
            // batch processing make us have simple formatting in favour of the batch size
            "INSERT INTO value_evaluation_in_search_mapping (destination, position, value_evaluation_definition_id, search_id) " +
            "VALUES (:destination, :position, :resultEntryProcessorId, :resultEntryDefinitionId)";

    /**
     * Requires: Integer id - the id of the Search
     */
    private static final String DELETE_SEARCH_SQL = "" +
            "DELETE                                                                                                         " +
            "   ve_in_s, ve_in_red, ved, sr, red, fi, f, s                                                                  " +
            "FROM                                                                                                           " +
            "   search s                                                                                                    " +
            "   JOIN filter f ON f.search_id = s.id                                                                         " +
            "   JOIN filter_item fi ON fi.filter_id = f.id                                                                  " +
            "   JOIN result_entry_definition red ON red.search_id = s.id                                                    " +
            "   JOIN search_result sr ON sr.result_entry_definition_id = red.id                                             " +
            "   JOIN value_evaluation_in_result_entry_mapping ve_in_red ON ve_in_red.result_entry_definition_id = red.id    " +
            "   JOIN value_evaluation_in_search_mapping ve_in_s ON ve_in_s.search_id = s.id                                 " +
            "   JOIN value_evaluation_definition ved                                                                        " +
            "      ON ved.id = ve_in_red.value_evaluation_definition_id                                                     " +
            "      OR ved.id = ve_in_s.value_evaluation_definition_id                                                       " +
            "WHERE                                                                                                          " +
            "   s.id = :id                                                                                                  ";
    /**
     * No requires
     */
    private static final String SELECT_SEARCHES_SQL = "" +
            "SELECT                                                                                     " +
            "   s.id AS s_id,                                                                           " +
            "   s.name AS s_name,                                                                       " +
            "   s.description AS s_description,                                                         " +
            "   s.url AS s_url,                                                                          " +
            "   rep.id AS rep_id,                                                                       " +
            "   rep.processing_type AS rep_processing_type,                                             " +
            "   rep.result_processing_engine AS rep_result_processing_engine,                           " +
            "   rep.expression AS rep_expression,                                                       " +
            "   rrm.destination AS rrm_destination,                                                     " +
            "   rrm.position AS rrm_position                                                            " +
            "FROM search s                                                                              " +
            "   LEFT JOIN value_evaluation_in_search_mapping rrm ON rrm.search_id = s.id                " +
            "   LEFT JOIN value_evaluation_definition rep ON rep.id = rrm.value_evaluation_definition_id ";
    /**
     * Requires: Integer id - the id of the Search
     */
    private static final String SELECT_SEARCH_SQL = SELECT_SEARCHES_SQL + " WHERE s.id = :id";

    public SearchJdbiDao(final DBI dbi) {
        super(dbi);
    }

    @Override
    public Search add(final Search search) {
        requireNonNull(search, "search cannot be null");
        final Map<String, Map<Integer, ValueEvaluationDefinition>> valueEvaluationDefinitionsMap = ImmutableMap
                .of(PREVIOUS_LINK_DESTINATION, search.getPreviousLinkLocation(),
                        NEXT_LINK_DESTINATION, search.getNextLinkLocation());

        final Integer resultSearchId = this.dbi.<Integer>inTransaction((handle, status) ->
                LocalJdbis.insertWithSubLists(handle,
                        INSERT_SEARCH_SQL,
                        INSERT_VALUE_EVALUATION_DEFINITION_SQL,
                        INSERT_VALUE_EVALUATION_DEFINITION_MAPPING_SQL,
                        LocalBeans.beanToMap("s", search),
                        valueEvaluationDefinitionsMap)
        );
        // TODO: Build the object from existing variables instead of queering database
        return get(resultSearchId);
    }

    @Override
    public int delete(final Search search) {
        requireNonNull(search, "search cannot be null");
        return this.dbi.inTransaction((handle, status) ->
                handle.createStatement(DELETE_SEARCH_SQL).bind("id", search.getId()).execute());
    }

    @Override
    public Search get(final int id) {
        try (Handle handle = this.dbi.open()) {
            return queryManyToOne(handle, SELECT_SEARCH_SQL, ImmutableMap.of("id", id))
                    .findFirst()
                    .orElseGet(null);
        }
    }

    @Override
    public List<Search> list() {
        try (Handle handle = this.dbi.open()) {
            return queryManyToOne(handle, SELECT_SEARCHES_SQL, ImmutableMap.of())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Class<Search> getGenericType() {
        return Search.class;
    }

    private Stream<Search> queryManyToOne(final Handle handle, final String sql, final Map<String, ?> parameters) {
        return LocalJdbis.queryManyToOne(handle,
                sql,
                parameters,
                this::map,
                AbstractModel::getId,
                Search::merge);
    }

    public Search map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException {
        final Map<String, Map<Integer, ValueEvaluationDefinition>> map = DESTINATIONS.stream()
                .collect(Collectors.toMap(k -> k, v -> new HashMap<Integer, ValueEvaluationDefinition>()));
        LocalJdbis.extractSubValueEvaluationDefinitions(r, map, "rep", "rrm");

        return Search.create(
                r.getInt("id"),
                r.getString("name"),
                r.getString("description"),
                r.getString("url"),
                map.get(PREVIOUS_LINK_DESTINATION),
                map.get(NEXT_LINK_DESTINATION));
    }
}
