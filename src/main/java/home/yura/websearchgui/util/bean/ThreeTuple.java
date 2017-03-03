package home.yura.websearchgui.util.bean;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * @author yuriy.dunko on 04.03.17.
 */
public class ThreeTuple<F, S, T> extends BiTuple<F, S> {

    private final T third;

    public ThreeTuple(final F first, final S second, final T third) {
        super(first, second);
        this.third = third;
    }

    public T getThird() {
        return this.third;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        final ThreeTuple<?, ?, ?> that = (ThreeTuple<?, ?, ?>) o;
        return Objects.equals(this.third, that.third);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.third);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("first", this.getFirst())
                .add("second", this.getSecond())
                .add("third", this.third)
                .toString();
    }
}
