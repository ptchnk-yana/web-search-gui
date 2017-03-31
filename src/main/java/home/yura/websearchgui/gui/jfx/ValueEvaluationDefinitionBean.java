package home.yura.websearchgui.gui.jfx;

import home.yura.websearchgui.gui.beans.AbstractBean;
import home.yura.websearchgui.model.ValueEvaluationDefinition;

import java.util.Objects;

import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionEngine.CSS_QUERY_SEARCH;
import static home.yura.websearchgui.model.ValueEvaluationDefinition.ValueEvaluationDefinitionType.EXTRACT_CONTENT;

/**
 * @author yuriy.dunko on 31.03.17.
 */
final class ValueEvaluationDefinitionBean extends ValueEvaluationDefinition implements AbstractBean<ValueEvaluationDefinition> {
    private Integer id;
    private ValueEvaluationDefinitionType type;
    private ValueEvaluationDefinitionEngine engine;
    private String expression;

    static ValueEvaluationDefinitionBean defaultValue() {
        return new ValueEvaluationDefinitionBean(null, EXTRACT_CONTENT, CSS_QUERY_SEARCH, "");
    }

    private ValueEvaluationDefinitionBean(final Integer id, final ValueEvaluationDefinitionType type,
                                         final ValueEvaluationDefinitionEngine engine, final String expression) {
        this.id = id;
        this.type = type;
        this.engine = engine;
        this.expression = expression;
        toModel();// to verify parameters
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    void setId(final Integer id) {
        this.id = id;
    }

    @Override
    public ValueEvaluationDefinitionType getType() {
        return this.type;
    }

    void setType(final ValueEvaluationDefinitionType type) {
        this.type = type;
    }

    @Override
    public ValueEvaluationDefinitionEngine getEngine() {
        return this.engine;
    }

    void setEngine(final ValueEvaluationDefinitionEngine engine) {
        this.engine = engine;
    }

    @Override
    public String getExpression() {
        return this.expression;
    }

    void setExpression(final String expression) {
        this.expression = expression;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ValueEvaluationDefinitionBean)) {
            return false;
        }
        final ValueEvaluationDefinitionBean that = (ValueEvaluationDefinitionBean) o;
        return Objects.equals(this.id, that.id) &&
                this.type == that.type &&
                this.engine == that.engine &&
                Objects.equals(this.expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.type, this.engine, this.expression);
    }

    @Override
    public ValueEvaluationDefinition toModel() {
        return create(this.id, this.type, this.engine, this.expression);
    }
}
