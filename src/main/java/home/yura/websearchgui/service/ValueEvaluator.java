package home.yura.websearchgui.service;

import home.yura.websearchgui.model.ValueEvaluationDefinition;
import org.jsoup.nodes.Element;

import java.util.Map;

/**
 * @author yuriy.dunko on 06.03.17.
 */
public interface ValueEvaluator {

    String evaluate(Map<Integer, ValueEvaluationDefinition> definitionChain, Element document);
}
