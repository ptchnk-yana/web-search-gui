package home.yura.websearchgui.util;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author yuriy.dunko on 03.03.17.
 */
public final class LocalCollections {
    private LocalCollections() {
    }

    public static <K, V> Map<K, V> merge(@Nullable final Map<K, V> left, @Nullable final Map<K, V> right) {
        final Map<K, V> map = new HashMap<>();
        if (left != null) {
            map.putAll(left);
        }
        if (right != null) {
            map.putAll(right);
        }
        return map;
    }

    public static <T> Map<Integer, T> index(final List<T> list) {
        final Map<Integer, T> map = new HashMap<>();
        for(final ListIterator<T> iterator = list.listIterator(); iterator.hasNext();) {
            map.put(iterator.nextIndex(), iterator.next());
        }
        return map;
    }

    public static <T, L extends Collection<T>> L addAllIfNotContains(final L left, final L right) {
        right.forEach(t -> addIfNotContains(left, t));
        return left;
    }

    public static <T, L extends Collection<T>> void addIfNotContains(final L set, final T t) {
        if (!set.contains(t)) {
            set.add(t);
        }
    }

}
