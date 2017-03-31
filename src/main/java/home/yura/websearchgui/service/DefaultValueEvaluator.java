package home.yura.websearchgui.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import home.yura.websearchgui.model.ValueEvaluationDefinition;
import home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.CSS_QUERY_SEARCH;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.REG_EXP;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.DELETE_CONTENT_PART;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.EXTRACT_CONTENT;
import static home.yura.websearchgui.util.LocalFunctions.findFirstGroup;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.lang.String.format;
import static java.util.Map.Entry;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.joining;

/**
 * Default implementation of {@link ValueEvaluator}
 *
 * @author yuriy.dunko on 04.03.17.
 */
public class DefaultValueEvaluator implements ValueEvaluator {
    private static final Log LOG = LogFactory.getLog(DefaultValueEvaluator.class);
    private static final EvaluatorFactory EVALUATORS_FACTORY = new EvaluatorFactory();

    // TODO: Should be configured for this class and not in it (use same cache here and in DefaultFilterMatcher)
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
    public String evaluate(final Map<Integer, ValueEvaluationDefinition> definitionChain, final Element document) {
        LOG.debug("Evaluating [" + definitionChain + "] for uri [" + document.baseUri() + "]");
        LOG.trace("Evaluating [" + definitionChain + "] for document [" + document + "]");
        final Pair<Supplier<Element>, Supplier<String>> tuple =
                definitionChain.values().stream().anyMatch(d -> d.getType().isContentModifying())
                        ? content(() -> document, document.outerHtml())
                        : document(document);
        return Optional.ofNullable(definitionChain
                .entrySet()
                .stream()
                .sorted(comparingByKey())
                .map(Entry::getValue)
                .reduce(tuple,
                        (base, entry) -> {
                            if (base == null) {
                                return null;
                            }
                            return EVALUATORS_FACTORY.get(entry).evaluate(base.getLeft(), base.getRight(), entry.getExpression());
                        },
                        (f, s) -> {
                            // just in case if we run in parallel stream
                            throw new IllegalArgumentException("Combine action isn't supported");
                        }))
                .map(t -> t.getRight().get())
                .orElse(null);
    }

    interface Evaluator {

        Pair<Supplier<Element>, Supplier<String>> evaluate(final Supplier<Element> document,
                                                              final Supplier<String> documentContent,
                                                              final String expression);

    }

    static class EvaluatorFactory {
        Map<Pair<ValueEvaluationDefinition.ValueEvaluationDefinitionType, ValueEvaluationDefinitionEngine>, Evaluator> map;

        EvaluatorFactory() {
            this.map = ImmutableMap.of(
                    Pair.of(EXTRACT_CONTENT, REG_EXP), (d, c, e) ->
                            content(d, findFirstGroup(PATTERN_CACHE.getUnchecked(e), c.get())),
                    Pair.of(EXTRACT_CONTENT, CSS_QUERY_SEARCH), (d, c, e) ->
                            document(d.get().select(e).first()),
                    Pair.of(DELETE_CONTENT_PART, REG_EXP), (d, c, e) ->
                            content(d, PATTERN_CACHE.getUnchecked(e).splitAsStream(c.get()).collect(joining())),
                    Pair.of(DELETE_CONTENT_PART, CSS_QUERY_SEARCH), (d, c, e) -> {
                        final Element element = d.get();
                        element.select(e).remove();
                        return document(element);
                    });
        }

        Evaluator get(final ValueEvaluationDefinition processor) {
            requireNonNull(processor, "processor");
            return requireNonNull(this.map.get(Pair.of(processor.getType(), processor.getEngine())),
                    format("Evaluator for %s not found", processor));
        }
    }

    static Pair<Supplier<Element>, Supplier<String>> content(final Supplier<Element> original, final String content) {
        if (content == null) {
            return null;
        }
        return Pair.of(() -> {
            final Document document = Jsoup.parse(content);
            document.setBaseUri(original.get().baseUri());
            return document;
        }, () -> content);
    }

    static Pair<Supplier<Element>, Supplier<String>> document(final Element document) {
        if (document == null) {
            return null;
        }
        return Pair.of(() -> document, document::outerHtml);
    }
}
