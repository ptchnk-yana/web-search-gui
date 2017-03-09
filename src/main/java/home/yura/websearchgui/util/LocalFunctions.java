package home.yura.websearchgui.util;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * @author yuriy.dunko on 03.03.17.
 */
public final class LocalFunctions {
    private LocalFunctions() {
    }

    public static <K> K process(final Callable<K> call, final Function<Exception, RuntimeException> function) {
        try {
            return call.call();
        } catch (final Exception e) {
            throw function.apply(e);
        }
    }
}
