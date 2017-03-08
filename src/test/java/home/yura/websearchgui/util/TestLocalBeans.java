package home.yura.websearchgui.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

/**
 * @author yuriy.dunko on 05.03.17.
 */
public class TestLocalBeans {

    @Test
    public void bean() throws Exception {
        final class T1 {
            public int getI() {
                return 0;
            }

            public String getS() {
                return null;
            }
        }
        Map<String, ?> map = LocalBeans.beanToMap("b", new T1());
        assertThat(map, hasEntry(is("b.i"), is(0)));
        assertThat(map, hasEntry(is("b.s"), nullValue()));
    }

    @Test
    public void gzip() throws Exception {
        final String content = IOUtils.toString(getClass().getResourceAsStream("/db/patches/create_database.sql"));
        final byte[] gzip = LocalBeans.gzip(content);
        final Byte[] arr = new Byte[gzip.length];
        for (int i = 0; i < gzip.length; i++) {
            arr[i] = gzip[i];
        }
        assertThat(arr, arrayWithSize(lessThan(content.length() / 3)));
    }

    @Test
    public void gunzip() throws Exception {
        String content = UUID.randomUUID().toString();
        String gunzip = LocalBeans.gunzip(LocalBeans.gzip(content));
        assertThat(gunzip, equalTo(content));
    }

    @Test
    public void extractLong() throws Exception {
        assertThat(LocalBeans.extractLong("1"), is(1L));
        assertThat(LocalBeans.extractLong("-1"), is(-1L));
        assertThat(LocalBeans.extractLong("A"), is(10L));
        // not a long and byte array size less than 8
        assertThat(LocalBeans.extractLong("+1"), is(11057L));
        // not a long and byte array size greater than 8
        assertThat(LocalBeans.extractLong("Hello world!"), is(8007531458009523233L));
    }

}