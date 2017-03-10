package home.yura.websearchgui.util.bean;

import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author yuriy.dunko on 04.03.17.
 */
public class Tuple<F> implements Supplier<F> {
    private final F first;

    public Tuple(final F first) {
        this.first = first;
    }

    public F getFirst() {
        return this.first;
    }

    @Override
    public F get() {
        return getFirst();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Tuple<?> tuple = (Tuple<?>) o;
        return Objects.equals(this.first, tuple.first);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.first);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("first", this.first)
                .toString();
    }
}
