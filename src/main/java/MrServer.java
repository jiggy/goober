
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.attribute.RequestURLAttribute;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

import javax.net.ssl.*;

public class MrServer {
    public static void main(String[] args) throws Exception {

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(getKeyManagers(), null, null);
        Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .addHttpListener(8080, "localhost")
                .addHttpsListener(8081, "localhost", sslContext)
                .setHandler(new HttpHandler() {
                                @Override
                                public void handleRequest(HttpServerExchange exchange) throws Exception {
                                    Handlers.resource(
                                            new ClassPathResourceManager(MrServer.class.getClassLoader(), "")).handleRequest(exchange);
                                    if (exchange.getConnection().isPushSupported()) {

                                        exchange.getConnection().pushResource("/css/styles.css", Methods.GET, exchange.getRequestHeaders());
                                    }
                                }
                            }
                        //.setAllowed(Predicates.contains(ExchangeAttributes.requestURL(), "pages"))
                ).build().start();
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
