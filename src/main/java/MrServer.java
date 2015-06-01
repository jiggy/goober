
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.commons.io.IOUtils;
import org.xnio.IoUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class MrServer {
    public static void main(String[] args) throws Exception {

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(getKeyManagers(), null, null);

        Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .addHttpListener(8080, "localhost")
                .addHttpsListener(8081, "localhost", sslContext)
                .setHandler(Handlers.path().addPrefixPath("/pages", exchange2 -> {
                    exchange2.dispatch(Handlers.resource(new ClassPathResourceManager(MrServer.class.getClassLoader(),"pages")));
                }).addExactPath("/demo", exchange -> {
                    System.out.println("Serving /demo");
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,
                            "text/html");
                    exchange.getResponseSender().send("<html><body>Hello World<iframe src=\"/pushy\"></iframe></body></html>");
                    if (exchange.getConnection().isPushSupported()) {
                        System.out.println("Pushing pushy");
                        exchange.getConnection().pushResource("/pushy", Methods.GET, exchange.getRequestHeaders());
                    } else {
                        System.out.println("Push not supported :(");
                    }
                }).addExactPath("/pushy", exchange1 -> {
                    System.out.println("Serving pushy");
                    exchange1.getResponseHeaders().put(Headers.CONTENT_TYPE,
                            "text/html");
                    exchange1.getResponseSender().send("<html><body>Hello Frame</body></html>");
                })).build().start();
    }

    private static KeyManager[] getKeyManagers() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(MrServer.class.getResourceAsStream("test.jks"),
                    "secret".toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "password".toCharArray());
            return keyManagerFactory.getKeyManagers();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException |
                UnrecoverableKeyException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }
}
