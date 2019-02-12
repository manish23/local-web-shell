package org.lazycat.webshell.client;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import io.vavr.control.Try;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.lazycat.webshell.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServingClientLauncher
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    public static void main(String[] args)
    {
        Map<String, String> argsMap = ArrayUtils.isEmpty(args) ? new HashMap<>() : Arrays.stream(args)
                .filter(StringUtils::isNotEmpty)
                .map(arg -> StringUtils.split(arg, "="))
                .filter(arg -> ArrayUtils.isNotEmpty(arg) && arg.length == 2)
                .map(arg -> new AbstractMap.SimpleEntry<>(arg[0], arg[1]))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String serverHost = MapUtils.getString(argsMap, Constants.serverHost, "127.0.0.1");
        int serverPort = MapUtils.getInteger(argsMap, Constants.serverPort, 7070);
        String protocol = MapUtils.getString(argsMap, Constants.protocol, "ws");
        String topic = MapUtils.getString(argsMap, Constants.topic, "terminal");

        new ServingClientLauncher().startClient(serverHost, serverPort, protocol, topic);

    }

    public void startClient(String serverHost, int serverPort, String protocol, String topic)
    {
        String uriPath = protocol + "://" + serverHost + ":" + serverPort + "/" + topic;

        WebSocketClient websocketclient = new WebSocketClient();
        ServingClientWebSocket socket = new ServingClientWebSocket();
        Session session = null;

        try
        {
            URI wsUri= new URI(uriPath);

            websocketclient.start();
            websocketclient.setMaxIdleTimeout(Constants.WS_SESSION_IDLE_TIMEOUT);

            ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
            clientUpgradeRequest.setHeader(Constants.REGISTER_AS_SERVING_CLIENT, "true");

            session = websocketclient.connect(socket, wsUri, clientUpgradeRequest).get();
            session.setIdleTimeout(Constants.WS_SESSION_IDLE_TIMEOUT);

            logger.info("Connected to : " + wsUri);

            // wait for closed socket connection.
            socket.awaitClose(5, TimeUnit.HOURS);
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
