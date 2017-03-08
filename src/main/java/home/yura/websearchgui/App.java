package home.yura.websearchgui;

import com.google.common.io.CharStreams;
import java.awt.BorderLayout;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

import javax.swing.*;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

/**
 * Entry point into the application
 */
public class App {

    public static void main(String... args) throws Exception {
        String renderContent;
        CloseableHttpClient client = HttpClientBuilder.create().build();
        final String address = "https://www.olx.ua/nedvizhimost/arenda-komnat/";
//                "http://en.wikipedia.org/wiki/Main_Page";
        final BasicHttpContext httpContext = new BasicHttpContext();
        try (CloseableHttpResponse response = client.execute(new HttpGet(address), httpContext)) {
            final HttpEntity entity = response.getEntity();
            final Charset charset = ContentType.get(entity).getCharset();
            final String htmlString = CharStreams.toString(new InputStreamReader(entity.getContent(), charset));
            final Document document = Jsoup.parse(htmlString).normalise();

            document.setBaseUri(ObjectUtils
                    .firstNonNull(
                            httpContext.getAttribute("http.target_host"),
                            response.containsHeader("Location") ? response.getFirstHeader("Location") : address)
                    .toString());

            document.getElementsByTag("img").forEach((img) -> {
                img.attr("src", img.absUrl("src"));
            });

            renderContent = "<html>"
                    + document.head().outerHtml()
                    + "<body>"
                    // see more https://jsoup.org/cookbook/extracting-data/selector-syntax
//                    + document.select("html body div[id=content] div[id=bodyContent] div[id=mw-content-text] table[id=mp-upper] table[id=mp-left] ")
                    + document.select("div[class='rel listHandler '] table table")
                        .stream()
                        .map(Node::outerHtml)
                        .map(e -> e + "<br/><br/><br/><br/>")
                        .collect(Collectors.toList())
                    + "</body>"
                    + "</html>";

        }

        JFXPanel jfxPanel = new JFXPanel(); // Scrollable JCompenent
        JFrame f = new JFrame("test");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.getContentPane().add(jfxPanel, BorderLayout.CENTER);
        f.setSize(640, 480);
        f.setVisible(true);
        
        Platform.runLater(() -> { // FX components need to be managed by JavaFX
            WebView webView = new WebView();
            webView.getEngine().loadContent(renderContent);
            jfxPanel.setScene(new Scene(webView));
        });
    }
}
