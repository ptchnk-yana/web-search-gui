package home.yura.websearchgui.service;

import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static home.yura.websearchgui.TestUtils.readGzipResource;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.CSS_QUERY_SEARCH;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.REG_EXP;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.DELETE_CONTENT_PART;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.EXTRACT_CONTENT;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.create;
import static org.hamcrest.CoreMatchers.is;
import static org.jsoup.Jsoup.parse;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link DefaultValueEvaluator}
 *
 * @author yuriy.dunko on 05.03.17.
 */
public class TestDefaultValueEvaluator {
    private static final String CONTENT = readGzipResource(
            "/home/yura/websearchgui/service/TestResultEntryProcessService.html.tar.gz");

    private static final DefaultValueEvaluator VALUE_EVALUATOR = new DefaultValueEvaluator();

    @Test
    public void evaluate() throws Exception {
        final String evaluated = VALUE_EVALUATOR.evaluate(of(
                // remove 1st cyrillic word
                3, create(null, DELETE_CONTENT_PART, REG_EXP, "^(\\p{InCyrillic})+\\s+"),
                // remove punctuation
                4, create(null, DELETE_CONTENT_PART, REG_EXP, "(\\p{P})+"),
                // remove 1st entry from table of content
                0, create(null, DELETE_CONTENT_PART, CSS_QUERY_SEARCH, "table[class='fixed offers breakword  offers--top'] tr:eq(1)"),
                // search for a text between two tags
                2, create(null, EXTRACT_CONTENT, REG_EXP, "<\\w+>(.+)</\\w+>"),
                // search for a tag with an entry name
                1, create(null, EXTRACT_CONTENT, CSS_QUERY_SEARCH, "h3.lheight20 a strong")
        ), parse(CONTENT));

        assertThat(evaluated, is("продажа от строительной компании"));
    }

}