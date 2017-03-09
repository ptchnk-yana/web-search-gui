package home.yura.websearchgui.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Optional.ofNullable;
import static org.apache.http.entity.ContentType.TEXT_HTML;

/**
 * @author yuriy.dunko on 09.03.17.
 */
public final class LocalHttpUtils {

    private LocalHttpUtils() {
    }

    public static final String HTTP_ATTRIBUTE_TARGET_HOST = "http.target_host";
    public static final String HTTP_HEADER_LOCATION = "Location";

    public static Document readDocument(final CloseableHttpClient client, final String url) throws IOException {
        final BasicHttpContext httpContext = new BasicHttpContext();
        try (final CloseableHttpResponse response = client.execute(new HttpGet(url), httpContext)) {
            final HttpEntity entity = response.getEntity();
            final Document document;
            try (final InputStream input = entity.getContent()) {
                document = Jsoup.parse(IOUtils.toString(input,
                        ofNullable(ContentType.get(entity)).orElse(TEXT_HTML).getCharset().name())).normalise();
            }
            document.setBaseUri(evaluateBaseUri(httpContext, response, url));
            return document;
        }
    }

    public static String evaluateBaseUri(final BasicHttpContext httpContext,
                                         final CloseableHttpResponse response,
                                         final String url) {
        return ofNullable(httpContext.getAttribute(HTTP_ATTRIBUTE_TARGET_HOST))
                .orElseGet(() ->
                        ofNullable(response.getFirstHeader(HTTP_HEADER_LOCATION))
                                .orElse(new BasicHeader(HTTP_HEADER_LOCATION, url))
                                .getValue())
                .toString();
    }
}
