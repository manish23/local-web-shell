package org.lazycat.webshell.client;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.lazycat.webshell.operation.impl.OperationsHandlerForServingClient;
import org.lazycat.webshell.utils.WebsocketUtils;
import org.lazycat.webshell.websocket.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket//(maxTextMessageSize = 64 * 1024 * 1024, maxBinaryMessageSize = 1024 * 1024 * 1024)
public class ServingClientWebSocket
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private final CountDownLatch closeLatch;

    private Session session;

    OperationsHandlerForServingClient operationsHandler = null;

    public ServingClientWebSocket(OperationsHandlerForServingClient operationsHandler)
    {
        this.operationsHandler = operationsHandler;
        this.closeLatch = new CountDownLatch(1);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException
    {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason)
    {
        logger.info("Connection closed: " + statusCode + " : " + reason);
        this.session = null;
        this.closeLatch.countDown(); // trigger latch
    }

    @OnWebSocketConnect
    public void onConnect(Session session)
    {
        logger.info("got connect: " + session);
        this.session = session;
    }

    @OnWebSocketMessage
    public void onMessage(String message) throws Exception
    {
        WebsocketUtils.processMsgByServingClient(operationsHandler, session, message);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error)
    {
        logger.error("got error", error);
    }
}
