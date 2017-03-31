package home.yura.websearchgui.service;

import com.google.common.io.Files;
import home.yura.websearchgui.dao.jdbi.AbstractJdbiTest;
import home.yura.websearchgui.dao.rsource.file.SearchResultContentFileResource;
import home.yura.websearchgui.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.easybatch.core.job.JobReport;
import org.easybatch.core.job.JobStatus;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.of;
import static home.yura.websearchgui.TestUtils.randomString;
import static home.yura.websearchgui.model.FilterItem.FilterEngine;
import static home.yura.websearchgui.model.FilterItem.FilterLocation.CONTENT;
import static home.yura.websearchgui.model.FilterItem.FilterPreFormatting.NO;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.CSS_QUERY_SEARCH;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.REG_EXP;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.DELETE_CONTENT_PART;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.EXTRACT_CONTENT;
import static home.yura.websearchgui.util.LocalCollections.index;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link DefaultJobRunner}
 *
 * @author yuriy.dunko on 16.03.17.
 */
public class IntTestDefaultJobRunner extends AbstractJdbiTest {

    private static final File TEMP_DIR = Files.createTempDir();

    private final ExecutorService httpQueryPool = Executors.newFixedThreadPool(100);
    private final ValueEvaluator valueEvaluator = new DefaultValueEvaluator();
    private final FilterMatcher filterMatcher = new DefaultFilterMatcher();
    private final SearchResultContentFileResource fileResource = SearchResultContentFileResource.gzipFiles(
            "utf-8", TEMP_DIR, Executors.newSingleThreadExecutor());

    @Override
    public void teardown() throws IOException {
        super.teardown();
        FileUtils.forceDelete(TEMP_DIR);
    }

    @Test
    public void createSearchJob() throws Exception {
        final Search search = this.searchDao.add(Search.create(null,
                "odessa-life",
                "odessa-life articles",
                "http://odessa-life.od.ua/gue/articlelist",
                index(of(ValueEvaluationDefinition.create(EXTRACT_CONTENT, CSS_QUERY_SEARCH, "div.pagerlinkleft a"),
                        ValueEvaluationDefinition.create(EXTRACT_CONTENT, REG_EXP, "href=\"([^\"]+)\""))),
                index(of(ValueEvaluationDefinition.create(EXTRACT_CONTENT, CSS_QUERY_SEARCH, "div.pagerlinkright a"),
                        ValueEvaluationDefinition.create(EXTRACT_CONTENT, REG_EXP, "href=\"([^\"]+)\""),
                        ValueEvaluationDefinition.create(DELETE_CONTENT_PART, REG_EXP, "amp;")))));
        final ResultEntryDefinition definition = this.resultEntryDefinitionDao.add(ResultEntryDefinition.create(null,
                search.getId(),
                "a.mainarticlelist_link",
                index(of(ValueEvaluationDefinition.create(EXTRACT_CONTENT, CSS_QUERY_SEARCH, "h3"),
                        ValueEvaluationDefinition.create(EXTRACT_CONTENT, REG_EXP, "<\\w+>(.+)</\\w+>"))),
                index(of(ValueEvaluationDefinition.create(EXTRACT_CONTENT, CSS_QUERY_SEARCH, "a"),
                        ValueEvaluationDefinition.create(EXTRACT_CONTENT, REG_EXP, "href=\"([^\"]+)\""))),
                index(of(ValueEvaluationDefinition.create(EXTRACT_CONTENT, CSS_QUERY_SEARCH, "a"),
                        ValueEvaluationDefinition.create(EXTRACT_CONTENT, REG_EXP, "(\\d+)"))),
                index(of(ValueEvaluationDefinition.create(EXTRACT_CONTENT, CSS_QUERY_SEARCH, "div.contentarticle"),
                        ValueEvaluationDefinition.create(DELETE_CONTENT_PART, CSS_QUERY_SEARCH, "table")))));

        final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(200);
        connManager.setDefaultMaxPerRoute(100);
        final DefaultJobRunner jobRunner = new DefaultJobRunner(
                HttpClientBuilder.create().setConnectionManager(connManager).setConnectionManagerShared(true)::build,
                this.httpQueryPool,
                this.filterMatcher,
                this.valueEvaluator,
                this.localJobJdbiDao,
                this.filterDao,
                this.searchResultDao,
                this.fileResource,
                1,
                1);

        // search job
        final JobReport searchReport = jobRunner.createSearchJob(Pair.of(search, definition), 3).call();
        assertThat(searchReport.getStatus(), is(JobStatus.COMPLETED));

        final LocalJob lastRun = this.localJobJdbiDao.findLastRun(searchReport.getJobName(), requireNonNull(search.getId()));
        assertThat(lastRun, notNullValue());
        assertThat(lastRun.getStatus(), is(LocalJob.Status.FINISHED));

        final List<SearchResult> results = this.searchResultDao.list();
        assertThat(results, hasSize(greaterThan(40)));

        final List<String> contents = results.parallelStream()
                .map(this.fileResource::get)
                .map(optionalFuture -> process(optionalFuture::get))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(SearchResultContent::getContent)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        assertThat(contents, hasSize(results.size()));

        // filter job
        final Filter filter = this.filterDao.add(Filter.builder()
                .setName(randomString())
                .setDescription(randomString())
                .setSearchId(search.getId())
                .addFilterItem(FilterItem.create(null, null, CONTENT, FilterEngine.REG_EXP, NO, "(.)"))
                .build());

        final JobReport filterReport = jobRunner.createFilterJob(Pair.of(search, definition)).call();
        assertThat(filterReport.getStatus(), is(JobStatus.COMPLETED));

        final List<Integer> filterIds = this.searchResultDao.list()
                .stream()
                .map(SearchResult::getFilterItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(filterIds, hasSize(results.size()));
        assertThat(new HashSet<>(filterIds), hasSize(1));
        assertThat(filterIds.stream().findFirst().orElse(null),
                is(requireNonNull(filter.getFilterItems()).stream().findFirst().orElse(null).getId()));
    }

}