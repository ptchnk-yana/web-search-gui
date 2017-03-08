package home.yura.websearchgui.util;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yuriy.dunko on 03.03.17.
 */
public class LocalCollections {
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
}
