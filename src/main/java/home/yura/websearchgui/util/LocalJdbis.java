package home.yura.websearchgui.util;

import home.yura.websearchgui.model.ValueEvaluationDefinition;
import home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine;
import home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType;
import home.yura.websearchgui.util.bean.BiTuple;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.PreparedBatch;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.IntegerColumnMapper;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static home.yura.websearchgui.util.LocalBeans.beanToMap;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.lang.Enum.valueOf;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * @author yuriy.dunko on 08.03.17.
 */
public final class LocalJdbis {

    private LocalJdbis() {
    }

    public static void extractSubValueEvaluationDefinitions(final ResultSet rs,
                                                            final Map<String, Map<Integer, ValueEvaluationDefinition>> map,
                                                            final String valueAlias,
                                                            final String mappingAlias) {
        try {
            final Map<Integer, ValueEvaluationDefinition> positionMap = map.get(rs.getString(mappingAlias + "_destination"));
            if (positionMap != null) {
                positionMap.put(extractIntValue(rs, mappingAlias + "_position", null),
                        ValueEvaluationDefinition.create(
                                extractIntValue(rs, valueAlias + "_id", null),
                                valueOf(ValueEvaluationDefinitionType.class,
                                        rs.getString(valueAlias + "_processing_type")),
                                valueOf(ValueEvaluationDefinitionEngine.class,
                                        rs.getString(valueAlias + "_result_processing_engine")),
                                rs.getString(valueAlias + "_expression")));
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createInsertFromSelectQuery(final String tableName,
                                                     final Map<String, BiTuple<String, String>> beanFieldToColumnAndType,
                                                     final Map<String, String> uniqueFieldToColumns,
                                                     final String extraCondition) {
        requireNonNull(tableName, "tableName");
        requireNonNull(beanFieldToColumnAndType, "beanFields");
        requireNonNull(uniqueFieldToColumns, "tableColumns");

        return "INSERT INTO " + tableName + " (" +
                beanFieldToColumnAndType.values().stream().map(BiTuple::getFirst).collect(Collector.of(
                        () -> new StringJoiner(", "),
                        StringJoiner::add,
                        StringJoiner::merge,
                        StringJoiner::toString)) +
                ") SELECT * FROM (SELECT " +
                beanFieldToColumnAndType.entrySet().stream().collect(Collector.of(
                        () -> new StringJoiner(", "),
                        (j, e) -> j.add(format("CAST(:%s AS %s) AS %s", e.getKey(), e.getValue().getSecond(), e.getValue().getFirst())),
                        StringJoiner::merge,
                        StringJoiner::toString)) +
                ") AS tmp WHERE NOT EXISTS (" +
                "SELECT * FROM " + tableName + " WHERE " +
                uniqueFieldToColumns.entrySet().stream().collect(Collector.of(
                        () -> new StringJoiner(" AND "),
                        (j, e) -> j.add(format("%s = :%s", e.getValue(), e.getKey())),
                        StringJoiner::merge,
                        StringJoiner::toString)) +
                ofNullable(extraCondition).map(s -> " AND " + s).orElse("") +
                ")";
    }

    public static <T> Integer insertWithSubLists(final Handle handle,
                                                 final String mainQuery,
                                                 final String valueQuery,
                                                 final String mappingQuery,
                                                 final Map<String, ?> mainQueryParameters,
                                                 final Map<String, Map<Integer, T>> valueEvaluationDefinitionsMap) {
        final Integer mainObjectResultId = handle.createStatement(mainQuery)//INSERT_DEFINITION_SQL
                .bindFromMap(mainQueryParameters)//beanToMap("d", definition)
                .executeAndReturnGeneratedKeys(IntegerColumnMapper.PRIMITIVE)
                .first();

        final PreparedBatch batchProcessors = handle.prepareBatch(valueQuery);//INSERT_DEFINITION_PROCESSOR_SQL
        final List<String> destination = new ArrayList<>();
        final List<Integer> position = new ArrayList<>();
        valueEvaluationDefinitionsMap
                .entrySet()
                .forEach(destinationEntry -> destinationEntry
                        .getValue()
                        .entrySet()
                        .forEach(positionEntry -> {
                            batchProcessors.add().bindFromMap(beanToMap("p", positionEntry.getValue()));
                            destination.add(destinationEntry.getKey());
                            position.add(positionEntry.getKey());
                        }));

        final List<Integer> resultEntryProcessorId = batchProcessors.size() == 0 ? Collections.emptyList()
                : batchProcessors.executeAndGenerateKeys(IntegerColumnMapper.PRIMITIVE).list();

        final PreparedBatch batchMapping = handle.prepareBatch(mappingQuery);//INSERT_DEFINITION_PROCESSOR_MAPPING_SQL
        for (int i = 0; i < resultEntryProcessorId.size(); i++) {
            batchMapping.add().bind("destination", destination.get(i))
                    .bind("position", position.get(i))
                    .bind("resultEntryProcessorId", resultEntryProcessorId.get(i))
                    // TODO: Should be configurable
                    .bind("resultEntryDefinitionId", mainObjectResultId);
        }

        final int[] mappingInsertResult = batchMapping.size() == 0 ? new int[]{} : batchMapping.execute();
        checkState(Arrays.stream(mappingInsertResult).sum() == resultEntryProcessorId.size(),
                "mapping was created incorrectly");

        return mainObjectResultId;
    }

    public static <T> Stream<T> queryManyToOne(final Handle handle,
                                               final String query,
                                               final Map<String, ?> parameters,
                                               final ResultSetMapper<T> mapper,
                                               final Function<T, Integer> getIdFunction,
                                               final BinaryOperator<T> mergeFunction) {
        return handle.createQuery(query)
                .bindFromMap(parameters)
                .map(mapper)
                .list()
                .stream()
                .collect(Collectors.groupingBy(getIdFunction))
                .values()
                .stream()
                .map(list -> list
                        .stream()
                        .reduce(mergeFunction)
                        .get());
    }

    public static Integer extractIntValue(final ResultSet r, final String label, @Nullable final Integer defaultValue)
            throws SQLException {
        final Object userIdObject = r.getObject(label);
        if (!r.wasNull() && userIdObject != null) {
            return ((Number) userIdObject).intValue();
        }
        return defaultValue;
    }

    public static Long extractLongValue(final ResultSet r, final String label, @Nullable final Long defaultValue)
            throws SQLException {
        final Object userIdObject = r.getObject(label);
        if (!r.wasNull() && userIdObject != null) {
            return ((Number) userIdObject).longValue();
        }
        return defaultValue;
    }
}
