package home.yura.websearchgui.dao.rsource.file;

import com.google.common.io.Files;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import home.yura.websearchgui.util.bean.BiTuple;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

import static home.yura.websearchgui.TestUtils.randomSearchResult;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author yuriy.dunko on 12.03.17.
 */
public class TestSearchResultContentFileResource {

    private static final File TEMP_DIR = Files.createTempDir();
    private static final SearchResultContentFileResource RESOURCE = SearchResultContentFileResource.gzipFiles(
            "utf-8", TEMP_DIR, Executors.newSingleThreadExecutor());

    static {
        try {
            FileUtils.forceDeleteOnExit(TEMP_DIR);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Test
    public void add() throws Exception {
        final BiTuple<SearchResult, SearchResultContent> tuple = input(1).stream().findFirst().orElse(null);
        RESOURCE.add(tuple.getFirst(), tuple.getSecond()).get();
    }

    @Test
    public void addBatch() throws Exception {
        final List<BiTuple<SearchResult, SearchResultContent>> batch = input(3);
        RESOURCE.addBatch(batch).get();
    }

    @Test
    public void delete() throws Exception {
        final BiTuple<SearchResult, SearchResultContent> tuple = input(1).stream().findFirst().orElse(null);
        RESOURCE.add(tuple.getFirst(), tuple.getSecond()).get();
        assertThat(RESOURCE.delete(tuple.getFirst()).get(), is(true));
        assertThat(RESOURCE.delete(tuple.getFirst()).get(), is(false));
    }

    @Test
    public void get() throws Exception {
        final BiTuple<SearchResult, SearchResultContent> tuple = input(1).stream().findFirst().orElse(null);
        RESOURCE.add(tuple.getFirst(), tuple.getSecond()).get();

        final SearchResultContent content = RESOURCE.get(tuple.getFirst()).get().orElse(null);
        assertThat(content, notNullValue());
        assertThat(content, equalTo(tuple.getSecond()));

        RESOURCE.delete(tuple.getFirst()).get();
        assertThat(RESOURCE.get(tuple.getFirst()).get().isPresent(), is(false));
    }

    private List<BiTuple<SearchResult, SearchResultContent>> input(final int number) {
        return range(0, number).mapToObj(Integer::valueOf).map(id ->
            new BiTuple<>(
                    randomSearchResult(id, null),
                    SearchResultContent.create(range(0, 1000).mapToObj(Integer::valueOf).collect(toList()).toString()))
        ).collect(toList());
    }
}