package home.yura.websearchgui.util;


import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static home.yura.websearchgui.util.LocalFunctions.process;
import static java.beans.Introspector.getBeanInfo;
import static java.lang.System.arraycopy;
import static java.nio.charset.StandardCharsets.UTF_8;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yuriy.dunko on 03.03.17.
 */
public final class LocalBeans {
    private LocalBeans() {

    }

    /**
     * @see org.skife.jdbi.v2.sqlobject.BindBeanFactory
     */
    public static Map<String, ?> beanToMap(@Nullable final String alias, final Object bean) {
        final String prefix = alias != null ? alias + "." : "";
        final Map<String, Object> map = new HashMap<>();
        Arrays.stream(process(() -> getBeanInfo(bean.getClass()), RuntimeException::new).getPropertyDescriptors())
                .filter(o -> o.getReadMethod() != null)
                // We cannot use {@code .map()} here as it can't process a null value
                .forEach(o -> map.put(
                        prefix + o.getName(),
                        process(() -> o.getReadMethod().invoke(bean), RuntimeException::new)));
        return map;
    }

    public static long extractLong(final String content) {
        final String trimmedContent = Strings.nullToEmpty(content).trim();
        Preconditions.checkArgument(!trimmedContent.isEmpty(), "content cannot be blank");

        Long aLong = Longs.tryParse(trimmedContent);

        if (aLong == null) {
            aLong = Longs.tryParse(trimmedContent, Character.MAX_RADIX);
        }

        if (aLong == null) {
            final byte[] trimmedContentBytes = trimmedContent.getBytes(UTF_8);
            final byte[] buffer = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
            if (trimmedContentBytes.length > 8) {
                arraycopy(trimmedContentBytes, trimmedContentBytes.length - Long.BYTES, buffer, 0, Long.BYTES);
            } else {
                arraycopy(trimmedContentBytes, 0, buffer, Long.BYTES - trimmedContentBytes.length, trimmedContentBytes.length);
            }
            return ((ByteBuffer) ByteBuffer.allocate(Long.BYTES).put(buffer).flip()).getLong();
        }
        return aLong;
    }

    public static byte[] gzip(final String content) {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (final GZIPOutputStream output = new GZIPOutputStream(bout)) {
            output.write(requireNonNull(content, "content").getBytes(UTF_8));
        } catch (final IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return bout.toByteArray();
    }

    public static String gunzip(final byte[] content) {
        try (final GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(requireNonNull(content, "content")))) {
            return IOUtils.toString(input, UTF_8.name());
        } catch (final IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
