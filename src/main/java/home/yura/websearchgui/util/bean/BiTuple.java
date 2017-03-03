package home.yura.websearchgui.util.bean;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * @author yuriy.dunko on 04.03.17.
 */
public class BiTuple<F, S> extends Tuple<F> {
    private final S second;

    public BiTuple(final F first, final S second) {
        super(first);
        this.second = second;
    }

    public S getSecond() {
        return this.second;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        final BiTuple<?, ?> biTuple = (BiTuple<?, ?>) o;
        return Objects.equals(this.second, biTuple.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), second);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("first", this.getFirst())
                .add("second", this.second)
                .toString();
    }
}
