package home.yura.websearchgui;

import home.yura.websearchgui.dao.AbstractDao;
import home.yura.websearchgui.model.AbstractModel;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.util.LocalHttpUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static home.yura.websearchgui.util.LocalBeans.gunzip;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author yuriy.dunko on 06.03.17.
 */
public class TestUtils {

    public static String readGzipResource(final String location) {
        return gunzip(process(() -> toByteArray(getResourceAsStream(location)), RuntimeException::new));
    }

    public static InputStream getResourceAsStream(final String location) {
        return TestUtils.class.getResourceAsStream(location);
    }

    public static long randomLong() {
        return RandomUtils.nextLong();
    }

    public static int randomInt() {
        return RandomUtils.nextInt();
    }

    public static String randomString() {
        return UUID.randomUUID().toString();
    }

    public static <T> T random(final T[] values) {
        return values[RandomUtils.nextInt(0, values.length)];
    }

    public static CloseableHttpClient createHttpClient(final Map<String, Supplier<InputStream>> contentMap)  {
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(process(() -> httpClient.execute(any(HttpGet.class), any(HttpContext.class)), RuntimeException::new)).then(invocation -> {
            final HttpGet request = invocation.getArgument(0);
            final BasicHttpContext context = invocation.getArgument(1);

            context.setAttribute(LocalHttpUtils.HTTP_ATTRIBUTE_TARGET_HOST, request.getURI());

            final HttpEntity httpEntity = mock(HttpEntity.class);
            when(httpEntity.getContent()).thenReturn(contentMap.get(request.getURI().toString()).get());
            when(httpEntity.getContentType()).thenReturn(new BasicHeader("Content-Type", "text/html; charset=utf-8"));

            final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getEntity()).thenReturn(httpEntity);

            return response;
        });
        return httpClient;
    }

    @SafeVarargs
    public static <T extends AbstractModel, D extends AbstractDao<T>> D createDao(final Class<D> daoClass, final T... array) {
        final D daoMock = mock(daoClass);
        when(daoMock.list()).thenReturn(Arrays.asList(array));
        when(daoMock.get(anyInt())).thenAnswer(invocation -> array[invocation.<Integer>getArgument(0)]);
        return daoMock;
    }

    public static SearchResult randomSearchResult(final Integer resultEntryDefinitionId, final Integer filterItemId) {
        return SearchResult.create(null,
                randomString(),
                randomString(),
                resultEntryDefinitionId,
                filterItemId,
                (long) randomInt(),
                randomString(),
                false);
    }
}
