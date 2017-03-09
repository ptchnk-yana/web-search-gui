package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;

/**
 * Define how to evaluate a value
 * @author yuriy.dunko on 26.02.17.
 */
@AutoValue
public abstract class ValueEvaluationDefinition implements AbstractModel {

    public enum ValueEvaluationDefinitionType {
        EXTRACT_CONTENT(false),
        DELETE_CONTENT_PART(true);

        private final boolean contentModifying;

        ValueEvaluationDefinitionType(final boolean contentModifying) {
            this.contentModifying = contentModifying;
        }

        public boolean isContentModifying() {
            return contentModifying;
        }
    }

    public enum ValueEvaluationDefinitionEngine {
        REG_EXP,
        CSS_QUERY_SEARCH
    }

    public static ValueEvaluationDefinition create(final Integer id,
                                                   final ValueEvaluationDefinitionType evaluationDefinitionType,
                                                   final ValueEvaluationDefinitionEngine valueEvaluationDefinitionEngine,
                                                   final String expression) {
        return new AutoValue_ValueEvaluationDefinition(id, evaluationDefinitionType, valueEvaluationDefinitionEngine, expression);
    }

    public abstract ValueEvaluationDefinitionType getType();

    public abstract ValueEvaluationDefinitionEngine getEngine();

    public abstract String getExpression();
}
