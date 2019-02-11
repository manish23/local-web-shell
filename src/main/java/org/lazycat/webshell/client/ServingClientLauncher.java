package org.lazycat.webshell.client;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import io.vavr.control.Try;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.lazycat.webshell.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServingClientLauncher
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    public static String serverHost = "127.0.0.1";
    public static int serverPort = 7070;
    public static String protocol = "ws";
    public static String topic = "terminal";

    public static void main(String[] args)
    {
        String uri = protocol + "://" + serverHost + ":" + serverPort + "/" + topic;

        WebSocketClient websocketclient = new WebSocketClient();
        ServingClientWebSocket socket = new ServingClientWebSocket();
        Session session = null;

        try
        {
            URI echoUri = new URI(uri);

            websocketclient.start();
            websocketclient.setMaxIdleTimeout(1000 * 60 * 60 * 1); // 1 hour

            ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
            clientUpgradeRequest.setHeader(Constants.REGISTER_AS_SERVING_CLIENT, "true");

            session = websocketclient.connect(socket, echoUri, clientUpgradeRequest).get();
            session.setIdleTimeout(1000 * 60 * 60 * 1); // 1 hour

            logger.info("Connected to : " + echoUri);

            // wait for closed socket connection.
            socket.awaitClose(5, TimeUnit.HOURS);

            System.out.println("");
        }
        catch (Throwable ex)
        {
            logger.error("websocket client got error", ex);
        }
        finally
        {
            Try.run(() -> websocketclient.stop())
                    .onFailure(ex -> logger.error("exception while closing websocket", ex));
        }

    }
}
