package home.yura.websearchgui.util;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

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

    public static <K> K process(final Callable<K> call) {
        try {
            return call.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String findFirstGroup(final Pattern pattern, final String input) {
        final Matcher matcher = requireNonNull(pattern, "pattern").matcher(input);
        checkState(matcher.find(), format("Cannot match [%s] to a pattern [%s]", input, pattern));
        return matcher.group(1);
    }

    public static <T> T requireNonNull(final T obj) {
        return Objects.requireNonNull(obj, "unexpected null value");
    }

    public static <T> T requireNonNull(final T obj, final String name) {
        return Objects.requireNonNull(obj, String.format("%s cannot be null", name));
    }
}
