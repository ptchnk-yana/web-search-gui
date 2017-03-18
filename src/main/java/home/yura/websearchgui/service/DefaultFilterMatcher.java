package home.yura.websearchgui.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import home.yura.websearchgui.model.Filter;
import home.yura.websearchgui.model.FilterItem;
import home.yura.websearchgui.model.FilterItem.FilterEngine;
import home.yura.websearchgui.model.FilterItem.FilterLocation;
import home.yura.websearchgui.model.FilterItem.FilterPreFormatting;
import home.yura.websearchgui.util.bean.ThreeTuple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.annotation.Nullable;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.nullToEmpty;
import static home.yura.websearchgui.model.FilterItem.FilterEngine.REG_EXP;
import static home.yura.websearchgui.model.FilterItem.FilterEngine.STRING_SEARCH;
import static home.yura.websearchgui.model.FilterItem.FilterLocation.CONTENT;
import static home.yura.websearchgui.model.FilterItem.FilterLocation.URL;
import static home.yura.websearchgui.model.FilterItem.FilterPreFormatting.CLEAR_HTML;
import static home.yura.websearchgui.model.FilterItem.FilterPreFormatting.ESCAPE_URL;
import static home.yura.websearchgui.model.FilterItem.FilterPreFormatting.NO;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yuriy.dunko on 10.03.17.
 */
public class DefaultFilterMatcher implements FilterMatcher {
    private static final Log LOG = LogFactory.getLog(DefaultFilterMatcher.class);
    private static final MatcherFactory MATCHER_FACTORY = new MatcherFactory();

    // TODO: Should be configured for this class and not in it (use same cache here and in DefaultValueEvaluator)
    private static final LoadingCache<String, Pattern> PATTERN_CACHE = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Pattern>() {
                @Override
                public Pattern load(@Nullable final String key) {
                    return Pattern.compile(requireNonNull(key, "key"), Pattern.MULTILINE);
                }
            });

    @Override
    public Integer getMatchedItemId(final Filter filter, final Document document) {
        LOG.debug("Check matching [" + filter + "] for uri [" + document.baseUri() + "]");
        LOG.trace("Check matching [" + filter + "] for document [" + document + "]");
        return requireNonNull(requireNonNull(filter, "filter").getFilterItems())
                .stream()
                .filter(item -> isMatching(item, document))
                .findFirst()
                .map(FilterItem::getId)
                .orElse(null);
    }

    private boolean isMatching(final FilterItem item, final Document document) {
        return MATCHER_FACTORY
                .get(requireNonNull(item, "filterItem"))
                .match(item.getExpression(), requireNonNull(document, "document"));
    }

    interface Matcher {
        boolean match(final String itemExpression, final Document document);
    }

    static class MatcherFactory {
        final Map<ThreeTuple<FilterLocation, FilterEngine, FilterPreFormatting>, Matcher> map;

        MatcherFactory() {
            this.map = ImmutableMap.<ThreeTuple<FilterLocation, FilterEngine, FilterPreFormatting>, Matcher>builder()
                    .put(new ThreeTuple<>(URL, REG_EXP, NO), (e, d) ->
                            PATTERN_CACHE.getUnchecked(e).matcher(getBaseUri(d)).find())
                    .put(new ThreeTuple<>(URL, REG_EXP, CLEAR_HTML), (e, d) ->
                            PATTERN_CACHE.getUnchecked(e).matcher(Jsoup.parse(getBaseUri(d)).text()).find())
                    .put(new ThreeTuple<>(URL, REG_EXP, ESCAPE_URL), (e, d) ->
                            PATTERN_CACHE.getUnchecked(e).matcher(decodeUrl(getBaseUri(d))).find())
                    .put(new ThreeTuple<>(URL, STRING_SEARCH, NO), (e, d) ->
                            getBaseUri(d).contains(e))
                    .put(new ThreeTuple<>(URL, STRING_SEARCH, CLEAR_HTML), (e, d) ->
                            Jsoup.parse(getBaseUri(d)).text().contains(e))
                    .put(new ThreeTuple<>(URL, STRING_SEARCH, ESCAPE_URL), (e, d) ->
                            decodeUrl(getBaseUri(d)).contains(e))
                    .put(new ThreeTuple<>(CONTENT, REG_EXP, NO), (e, d) ->
                            PATTERN_CACHE.getUnchecked(e).matcher(d.outerHtml()).find())
                    .put(new ThreeTuple<>(CONTENT, REG_EXP, CLEAR_HTML), (e, d) ->
                            PATTERN_CACHE.getUnchecked(e).matcher(d.text()).find())
                    .put(new ThreeTuple<>(CONTENT, REG_EXP, ESCAPE_URL), (e, d) ->
                            PATTERN_CACHE.getUnchecked(e).matcher(decodeUrl(d.text())).find())
                    .put(new ThreeTuple<>(CONTENT, STRING_SEARCH, NO), (e, d) ->
                            d.outerHtml().contains(e))
                    .put(new ThreeTuple<>(CONTENT, STRING_SEARCH, CLEAR_HTML), (e, d) ->
                            d.text().contains(e))
                    .put(new ThreeTuple<>(CONTENT, STRING_SEARCH, ESCAPE_URL), (e, d) ->
                            decodeUrl(d.text()).contains(e))
                    .build();
        }

        private String getBaseUri(final Document d) {
            return nullToEmpty(d.baseUri()).trim();
        }

        private String decodeUrl(final String url) {
            return process(() -> URLDecoder.decode(url, "utf-8"), RuntimeException::new);
        }

        Matcher get(final FilterItem item) {
            return requireNonNull(this.map.get(new ThreeTuple<>(
                    requireNonNull(item.getFilterLocation(), "FilterItemLocation"),
                    requireNonNull(item.getFilterEngine(), "FilterItemEngine"),
                    requireNonNull(item.getFilterPreFormatting(), "FilterItemPreFormatting"))));
        }
    }
}
