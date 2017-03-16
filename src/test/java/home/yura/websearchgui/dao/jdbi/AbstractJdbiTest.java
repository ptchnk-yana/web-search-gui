package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.TestUtils;
import home.yura.websearchgui.model.*;
import org.apache.commons.io.IOUtils;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.logging.FormattedLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.lang.System.arraycopy;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author yuriy.dunko on 02.03.17.
 */
public class AbstractJdbiTest {
    private static final ExecutorService PRINTER = Executors.newSingleThreadExecutor();
    private static final String JDBC_URL = "jdbc:h2:mem:test";
    private static final boolean LOG_SQL = false;

    protected final DBI dbi = new DBI(JdbcConnectionPool.create(JDBC_URL, "", "")) {
        {
            setSQLLog(new FormattedLog() {
                @Override
                protected boolean isEnabled() {
                    return LOG_SQL;
                }

                @Override
                protected void log(final String msg) {
                    PRINTER.submit(() -> System.out.println("JDBI: " + msg.replaceAll("(\\s)+", " ")));
                }
            });
        }
    };
    protected ResultEntryDefinitionJdbiDao resultEntryDefinitionDao = new ResultEntryDefinitionJdbiDao(this.dbi);
    protected SearchJdbiDao searchDao = new SearchJdbiDao(this.dbi);
    protected FilterJdbiDao filterDao = new FilterJdbiDao(this.dbi);
    protected final LocalJobJdbiDao localJobJdbiDao = new LocalJobJdbiDao(this.dbi);
    protected SearchResultJdbiResourceDao searchResultDao = new SearchResultJdbiResourceDao(this.dbi);

    @BeforeClass
    public static void setupTestClass() throws IOException {
        try (InputStream is = AbstractJdbiTest.class.getResourceAsStream("/db/patches/create_database.sql")) {
            new DBI(JdbcConnectionPool.create(JDBC_URL, "", ""))
                    .inTransaction((conn, status) -> conn.createStatement(IOUtils.toString(is)).execute());
        }
    }

    @AfterClass
    public static void teardownTestClass() throws IOException {
        try (InputStream is = AbstractJdbiTest.class.getResourceAsStream("/db/patches/drop_database.sql")) {
            new DBI(JdbcConnectionPool.create(JDBC_URL, "", ""))
                    .inTransaction((conn, status) -> conn.createStatement(IOUtils.toString(is)).execute());
        }
    }

    @After
    public void teardown() throws IOException {
        try (InputStream is = AbstractJdbiTest.class.getResourceAsStream("/db/patches/delete_data_in_database.sql")) {
            this.dbi.inTransaction((conn, status) -> conn.createStatement(IOUtils.toString(is)).execute());
        }
    }

    static Search randomSearch(final int... processorsCountInput) {
        final int[] processorsCount = new int[]{1, 1};
        arraycopy(processorsCountInput, 0, processorsCount, 0, processorsCountInput.length);

        final List<Map<Integer, ValueEvaluationDefinition>> list = Arrays
                .stream(processorsCount)
                .mapToObj(value -> IntStream.range(1, value + 1)
                        .boxed()
                        .collect(toMap(
                                identity(),
                                k -> ValueEvaluationDefinition.create(null,
                                        TestUtils.random(ValueEvaluationDefinition.ValueEvaluationDefinitionType.values()),
                                        TestUtils.random(ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.values()),
                                        TestUtils.randomString())))
                ).collect(toList());
        return Search.create(null,
                TestUtils.randomString(),
                TestUtils.randomString(),
                TestUtils.randomString(),
                list.get(0),
                list.get(1));
    }

    static ResultEntryDefinition randomResultDefinition(final Integer searchId, final int... processorsCountInput) {
        final int[] processorsCount = new int[]{1, 1, 1, 1};
        arraycopy(processorsCountInput, 0, processorsCount, 0, processorsCountInput.length);

        final List<Map<Integer, ValueEvaluationDefinition>> list = Arrays
                .stream(processorsCount)
                .mapToObj(value -> IntStream.range(1, value + 1)
                        .boxed()
                        .collect(toMap(
                                identity(),
                                k -> ValueEvaluationDefinition.create(null,
                                        TestUtils.random(ValueEvaluationDefinition.ValueEvaluationDefinitionType.values()),
                                        TestUtils.random(ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.values()),
                                        TestUtils.randomString())))
                ).collect(toList());

        return ResultEntryDefinition.create(null,
                searchId,
                TestUtils.randomString(),
                list.get(0),
                list.get(1),
                list.get(2),
                list.get(3));
    }

    static Filter randomFilter(final Integer searchId, final int... itemCount) {
        final Filter.Builder builder = Filter.builder()
                .setName(TestUtils.randomString())
                .setDescription(TestUtils.randomString())
                .setSearchId(searchId);
        IntStream.range(0, itemCount.length == 0 ? 1 : itemCount[0]).forEach(value -> builder
                .addFilterItem(randomFilterItem(null)));
        return builder.build();
    }

    static FilterItem randomFilterItem(final Integer filterId) {
        return FilterItem.create(
                null,
                filterId,
                TestUtils.random(FilterItem.FilterLocation.values()),
                TestUtils.random(FilterItem.FilterEngine.values()),
                TestUtils.random(FilterItem.FilterPreFormatting.values()),
                TestUtils.randomString());
    }

}
