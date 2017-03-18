package home.yura.websearchgui.dao.jdbi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import home.yura.websearchgui.dao.ResultEntryDefinitionDao;
import home.yura.websearchgui.model.AbstractModel;
import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.ValueEvaluationDefinition;
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

import static home.yura.websearchgui.util.LocalBeans.beanToMap;
import static home.yura.websearchgui.util.LocalJdbis.extractIntValue;
import static home.yura.websearchgui.util.LocalJdbis.extractSubValueEvaluationDefinitions;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * @author yuriy.dunko on 01.03.17.
 */
public class ResultEntryDefinitionJdbiDao extends AbstractJdbiDao<ResultEntryDefinition> implements ResultEntryDefinitionDao {
    private static final String NAME_DESTINATION = "name";
    private static final String CONTENT_LINK_DESTINATION = "content_link";
    private static final String INTERNAL_ID_DESTINATION = "internal_id";
    private static final String CONTENT_DESTINATION = "content";
    private static final List<String> DESTINATIONS = ImmutableList
            .of(NAME_DESTINATION, CONTENT_LINK_DESTINATION, INTERNAL_ID_DESTINATION, CONTENT_DESTINATION);


    /**
     * Requires: ResultEntryDefinition d - the ResultEntryDefinition which should be stored
     */
    private static final String INSERT_DEFINITION_SQL = "" +
            "INSERT INTO result_entry_definition (  " +
            "   search_id, entry_block_location     " +
            ") VALUES (                             " +
            "   :d.searchId, :d.entryBlockLocation) ";

    /**
     * Requires: ValueEvaluationDefinition p - the ValueEvaluationDefinition (part of a ResultEntryDefinition)
     */
    private static final String INSERT_VALUE_EVALUATION_DEFINITION_SQL = "" +
            // batch processing mace us have simple formatting in favour fo the batch size
            "INSERT INTO value_evaluation_definition (processing_type, result_processing_engine, expression) " +
            "VALUES (:p.type, :p.engine, :p.expression)";

    /**
     * Requires: destination, position, resultEntryProcessorId, resultEntryDefinitionId
     */
    private static final String INSERT_VALUE_EVALUATION_DEFINITION_MAPPING_SQL = "" +
            // batch processing mace us have simple formatting in favour fo the batch size
            "INSERT INTO value_evaluation_in_result_entry_mapping (destination, position, value_evaluation_definition_id, result_entry_definition_id) " +
            "VALUES (:destination, :position, :resultEntryProcessorId, :resultEntryDefinitionId)";

    /**
     * Requires: Integer id - the id of the ResultEntryDefinition
     */
    private static final String DELETE_DEFINITION_SQL = "" +
            "DELETE FROM                                                                                " +
            "   rrm, red, rep                                                                           " +
            "FROM result_entry_definition red                                                           " +
            "   LEFT JOIN value_evaluation_in_result_entry_mapping rrm ON rrm.result_entry_definition_id = red.id " +
            "   LEFT JOIN value_evaluation_definition rep ON rep.id = rrm.value_evaluation_definition_id          " +
            "WHERE                                                                                      " +
            "   red.id = :id                                                                            ";

    /**
     * No requires
     */
    private static final String SELECT_DEFINITIONS_SQL = "" +
            "SELECT                                                                                     " +
            "   red.id AS red_id,                                                                       " +
            "   red.search_id AS red_search_id,                                                         " +
            "   red.entry_block_location AS red_entry_block_location,                                   " +
            "   rep.id AS rep_id,                                                                       " +
            "   rep.processing_type AS rep_processing_type,                                             " +
            "   rep.result_processing_engine AS rep_result_processing_engine,                           " +
            "   rep.expression AS rep_expression,                                                       " +
            "   rrm.destination AS rrm_destination,                                                     " +
            "   rrm.position  AS rrm_position                                                           " +
            "FROM result_entry_definition red                                                           " +
            "   LEFT JOIN value_evaluation_in_result_entry_mapping rrm ON rrm.result_entry_definition_id = red.id " +
            "   LEFT JOIN value_evaluation_definition rep ON rep.id = rrm.value_evaluation_definition_id          ";

    /**
     * Requires: Integer id - the id of the ResultEntryDefinition
     */
    private static final String SELECT_DEFINITION_SQL = SELECT_DEFINITIONS_SQL + " WHERE red.id = :id";

    /**
     * Requires: Integer sid - the id of the Search
     */
    private static final String SELECT_DEFINITION_BY_SEARCH_ID_SQL = SELECT_DEFINITIONS_SQL + " WHERE red.search_id = :sid";

    public ResultEntryDefinitionJdbiDao(final DBI dbi) {
        super(dbi);
    }

    @Override
    public ResultEntryDefinition add(final ResultEntryDefinition definition) {
        this.log.debug("Adding [" + definition + "]");
        requireNonNull(definition, "resultEntryDefinition");
        final ImmutableMap<String, Map<Integer, ValueEvaluationDefinition>> valueEvaluationDefinitionsMap = ImmutableMap
                .of(NAME_DESTINATION, definition.getNameExtractionChain(),
                        CONTENT_LINK_DESTINATION, definition.getContentLinkExtractionChain(),
                        INTERNAL_ID_DESTINATION, definition.getInternalIdExtractionChain(),
                        CONTENT_DESTINATION, definition.getContentExtractionChain());

        final Integer resultEntryDefinitionId = this.dbi.<Integer>inTransaction((handle, status) ->
                LocalJdbis.insertWithSubLists(handle,
                        INSERT_DEFINITION_SQL,
                        INSERT_VALUE_EVALUATION_DEFINITION_SQL,
                        INSERT_VALUE_EVALUATION_DEFINITION_MAPPING_SQL,
                        beanToMap("d", definition),
                        valueEvaluationDefinitionsMap)
        );
        // TODO: Build the object from existing variables instead of queering database
        return get(resultEntryDefinitionId);
    }

    @Override
    public int delete(final ResultEntryDefinition definition) {
        this.log.debug("Deleting [" + definition + "]");
        requireNonNull(definition, "resultEntryDefinition");
        return this.dbi.inTransaction((handle, status) ->
                handle.createStatement(DELETE_DEFINITION_SQL).bind("id", definition.getId()).execute());
    }

    @Override
    public ResultEntryDefinition get(final int id) {
        this.log.debug("Getting by id [" + id + "]");
        try (Handle handle = this.dbi.open()) {
            return queryManyToOne(handle, SELECT_DEFINITION_SQL, ImmutableMap.of("id", id))
                    .findFirst()
                    .orElseGet(null);
        }
    }

    @Override
    public ResultEntryDefinition findBySearchId(final int searchId) {
        this.log.debug("Getting by searchId [" + searchId + "]");
        try (Handle handle = this.dbi.open()) {
            return queryManyToOne(handle, SELECT_DEFINITION_BY_SEARCH_ID_SQL, ImmutableMap.of("sid", searchId))
                    .findFirst()
                    .orElseGet(null);
        }
    }

    @Override
    public List<ResultEntryDefinition> list() {
        this.log.debug("Getting all");
        try (Handle handle = this.dbi.open()) {
            return queryManyToOne(handle, SELECT_DEFINITIONS_SQL, ImmutableMap.of())
                    .collect(toList());
        }
    }

    @Override
    public Class<ResultEntryDefinition> getGenericType() {
        return ResultEntryDefinition.class;
    }

    private Stream<ResultEntryDefinition> queryManyToOne(final Handle handle, final String sql, final Map<String, ?> parameters) {
        return LocalJdbis.queryManyToOne(handle,
                sql,
                parameters,
                this::map,
                AbstractModel::getId,
                ResultEntryDefinition::merge);
    }

    private ResultEntryDefinition map(final int index, final ResultSet rs, final StatementContext ctx) throws SQLException {
        final Map<String, Map<Integer, ValueEvaluationDefinition>> map = DESTINATIONS.stream()
                .collect(Collectors.toMap(k -> k, v -> new HashMap<Integer, ValueEvaluationDefinition>()));
        extractSubValueEvaluationDefinitions(rs, map, "rep", "rrm");

        return ResultEntryDefinition.create(extractIntValue(rs, "red_id", null),
                extractIntValue(rs, "red_search_id", null),
                rs.getString("red_entry_block_location"),
                map.get(NAME_DESTINATION),
                map.get(CONTENT_LINK_DESTINATION),
                map.get(INTERNAL_ID_DESTINATION),
                map.get(CONTENT_DESTINATION));
    }
}
