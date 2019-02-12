package org.lazycat.webshell;

import org.lazycat.webshell.operation.impl.OperationsHandlerForServerUsingServerMode;
import org.lazycat.webshell.operation.impl.OperationsHandlerForServerUsingServingClientMode;
import org.lazycat.webshell.websocket.session.WebsocketSessionManager;
import io.javalin.Javalin;

import java.lang.invoke.MethodHandles;

import org.lazycat.webshell.utils.ProcessUtils;
import org.lazycat.webshell.utils.WebsocketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebShellLauncher {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private WebsocketSessionManager websocketSessionManager = WebsocketSessionManager.getInstance();

    private static boolean serverNeedsServingClient = true;
    private static int serverPort = 7070;

    public static void main(String[] args)
    {
        ProcessUtils.printHostname();

        new WebShellLauncher().startServer();
    }

    public void startServer()
    {
        OperationsHandlerForServerUsingServerMode operationsHandlerForServerUsingServerMode
                = new OperationsHandlerForServerUsingServerMode();
        OperationsHandlerForServerUsingServingClientMode operationsHandlerForServerUsingServingClientMode
                = new OperationsHandlerForServerUsingServingClientMode();

        Javalin.create()
            .port(serverPort)
            .enableStaticFiles("/public")
            .ws("/terminal", ws ->
            {
                ws.onConnect(session ->
                {
                    if(! WebsocketUtils.isValidClient(session, serverNeedsServingClient))
                        return;

                    session.setIdleTimeout(Constants.WS_SESSION_IDLE_TIMEOUT);
                    websocketSessionManager.addSession(session.getId(), session);

                    if(serverNeedsServingClient)
                        operationsHandlerForServerUsingServingClientMode.onConnect(session.getId(), session);
                    else
                        operationsHandlerForServerUsingServerMode.onConnect(session.getId(), session);
                });

                ws.onClose((session, status, message) ->
                {
                    logger.info(session.getId() + " : closing websocket, status = " + status + " : message = " + message);
                    websocketSessionManager.removeSession(session.getId());
                });

                ws.onError((session, error) -> logger.error(error.getMessage(), error));

                ws.onMessage((session, message) ->
                {
                    logger.info("WS : onMessage : " + session.getId() + " : " + message);

                    if(serverNeedsServingClient)
                        WebsocketUtils.processMsgByServerUsingServingClient(operationsHandlerForServerUsingServingClientMode,
                                session.getId(), message, websocketSessionManager.getServingClientInfo());
                    else
                        WebsocketUtils.processMsgByServerLocally(operationsHandlerForServerUsingServerMode,
                                session.getId(), message);

                });

            })
            .start();

    }


}
