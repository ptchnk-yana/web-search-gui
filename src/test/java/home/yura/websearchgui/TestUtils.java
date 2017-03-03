package home.yura.websearchgui;

import org.apache.commons.lang3.RandomUtils;

import java.io.InputStream;
import java.util.UUID;

import static home.yura.websearchgui.util.LocalBeans.gunzip;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static org.apache.commons.io.IOUtils.toByteArray;

/**
 * @author yuriy.dunko on 06.03.17.
 */
public class TestUtils {

    public static String readGzipResource(String location) {
        return gunzip(process(() -> toByteArray(getResourceAsStream(location)), RuntimeException::new));
    }

    public static InputStream getResourceAsStream(final String location) {
        return TestUtils.class.getResourceAsStream(location);
    }

    public static long randomLong() {
        return RandomUtils.nextLong();
    }

    public static String randomString() {
        return UUID.randomUUID().toString();
    }

    public static <T> T random(T[] values) {
        return values[RandomUtils.nextInt(0, values.length)];
    }
}
