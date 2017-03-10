package home.yura.websearchgui.service;

import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.FilterItem;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static home.yura.websearchgui.TestUtils.randomString;
import static home.yura.websearchgui.TestUtils.readGzipResource;
import static home.yura.websearchgui.model.FilterItem.FilterEngine.REG_EXP;
import static home.yura.websearchgui.model.FilterItem.FilterEngine.STRING_SEARCH;
import static home.yura.websearchgui.model.FilterItem.FilterLocation.CONTENT;
import static home.yura.websearchgui.model.FilterItem.FilterLocation.URL;
import static home.yura.websearchgui.model.FilterItem.FilterPreFormatting.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.jsoup.Jsoup.parse;
import static org.junit.Assert.assertThat;

/**
 * @author yuriy.dunko on 10.03.17.
 */
public class TestDefaultFilterMatcher {
    private static final String FILE_CONTENT = readGzipResource(
            "/home/yura/websearchgui/service/TestResultEntryProcessService.html.tar.gz");

    private static final DefaultFilterMatcher FILTER_MATCHER = new DefaultFilterMatcher();

    @Test
    public void isMatching() throws Exception {
        final Document document = parse(FILE_CONTENT);
        document.setBaseUri("https://ru.wikipedia.org/wiki/header=<h1>Title</h1>/" +
                // Заглавная_страница
                "%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F_%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0");

        assertThat(getMatchedItemId(URL, REG_EXP, NO, "^http[s]://\\w+\\.(wikipedia)\\.\\w+", document), is(1));
        assertThat(getMatchedItemId(URL, REG_EXP, NO, "^http[s]://\\w+\\.(google)\\.\\w+", document), nullValue());

        assertThat(getMatchedItemId(URL, REG_EXP, CLEAR_HTML, "\\/header=\\s*Title\\/", document), is(1));
        assertThat(getMatchedItemId(URL, REG_EXP, CLEAR_HTML, "\\/header=<h1>Title</h1>\\/", document), nullValue());

        assertThat(getMatchedItemId(URL, REG_EXP, ESCAPE_URL, "(\\p{InCyrillic}+.?\\p{InCyrillic}*)", document), is(1));
        assertThat(getMatchedItemId(URL, REG_EXP, ESCAPE_URL, "(\\%..){10,18}", document), nullValue());

        assertThat(getMatchedItemId(URL, STRING_SEARCH, NO, ".wikipedia", document), is(1));
        assertThat(getMatchedItemId(URL, STRING_SEARCH, NO, "\\.wikipedia", document), nullValue());

        assertThat(getMatchedItemId(URL, STRING_SEARCH, CLEAR_HTML, "header= Title", document), is(1));
        assertThat(getMatchedItemId(URL, STRING_SEARCH, CLEAR_HTML, "header=<h1>Title</h1>", document), nullValue());

        assertThat(getMatchedItemId(URL, STRING_SEARCH, ESCAPE_URL, "Заглавная", document), is(1));
        assertThat(getMatchedItemId(URL, STRING_SEARCH, ESCAPE_URL, "%D0%97%D0%B0%D0%B3", document), nullValue());

        assertThat(getMatchedItemId(CONTENT, REG_EXP, NO, "href=\"(.+)\" class=\"link\"", document), is(1));
        assertThat(getMatchedItemId(CONTENT, REG_EXP, NO, "href=\"(.+)\" class=\"top\"", document), nullValue());

        assertThat(getMatchedItemId(CONTENT, REG_EXP, CLEAR_HTML, "579 133 грн.\\s+Одесса, Киевский\\s+", document), is(1));
        assertThat(getMatchedItemId(CONTENT, REG_EXP, CLEAR_HTML, "579 133 грн.\\s+</span>\\s+Одесса, Киевский\\s+", document), nullValue());

        assertThat(getMatchedItemId(CONTENT, REG_EXP, ESCAPE_URL, "\\p{InCyrillic}+", document), is(1));
        assertThat(getMatchedItemId(CONTENT, REG_EXP, ESCAPE_URL, "<\\w+", document), nullValue());

        assertThat(getMatchedItemId(CONTENT, STRING_SEARCH, NO, "{id:374447684}", document), is(1));
        assertThat(getMatchedItemId(CONTENT, STRING_SEARCH, NO, "\\{id\\:374447684\\}", document), nullValue());

        assertThat(getMatchedItemId(CONTENT, STRING_SEARCH, CLEAR_HTML, "579 133 грн. Одесса, Киевский", document), is(1));
        assertThat(getMatchedItemId(CONTENT, STRING_SEARCH, CLEAR_HTML, "579 133 грн.\nОдесса, Киевский", document), nullValue());

        assertThat(getMatchedItemId(CONTENT, STRING_SEARCH, ESCAPE_URL, "579 133 грн. Одесса, Киевский", document), is(1));
        assertThat(getMatchedItemId(CONTENT, STRING_SEARCH, ESCAPE_URL, "579 133 грн.\nОдесса, Киевский", document), nullValue());
    }

    private Integer getMatchedItemId(final FilterItem.FilterLocation filterLocation,
                                     final FilterItem.FilterEngine filterEngine,
                                     final FilterItem.FilterPreFormatting filterPreFormatting,
                                     final String expression,
                                     final Document document) {
        return FILTER_MATCHER.getMatchedItemId(
                createFilter(filterLocation, filterEngine, filterPreFormatting, expression),
                document);
    }

    private Filter createFilter(final FilterItem.FilterLocation filterLocation,
                                final FilterItem.FilterEngine filterEngine,
                                final FilterItem.FilterPreFormatting filterPreFormatting,
                                final String expression) {
        return Filter
                .builder()
                .setId(1)
                .setName(randomString())
                .setDescription(randomString())
                .setSearchId(1)
                .addFilterItem(FilterItem.create(1, 1, filterLocation, filterEngine, filterPreFormatting, expression))
                .build();
    }
}
