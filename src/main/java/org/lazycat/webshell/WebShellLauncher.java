package org.lazycat.webshell;

import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.lazycat.webshell.file.FileReaderThread;
import org.lazycat.webshell.operation.impl.OperationsHandlerForServerUsingServerMode;
import org.lazycat.webshell.operation.impl.OperationsHandlerForServerUsingServingClientMode;
import org.lazycat.webshell.operation.interfaces.OperationsHandler;
import org.lazycat.webshell.process.impl.local.LocalProcessInfo;
import org.lazycat.webshell.server.ServingClientInfo;
import org.lazycat.webshell.websocket.session.WebsocketSessionManager;
import io.javalin.Javalin;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.concurrent.*;
import java.util.stream.Stream;

import org.lazycat.webshell.utils.ProcessUtils;
import org.lazycat.webshell.utils.WebsocketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lazycat.webshell.Constants.MB;

public class WebShellLauncher {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private WebsocketSessionManager websocketSessionManager = WebsocketSessionManager.getInstance();

    private static boolean serverNeedsServingClient = true;
    private static int serverPort = 7070;

    public static void main(String[] args) throws Exception
    {
        ProcessUtils.printHostname();

        OperationsHandlerForServerUsingServerMode operationsHandlerForServerUsingServerMode
                = new OperationsHandlerForServerUsingServerMode();
        OperationsHandlerForServerUsingServingClientMode operationsHandlerForServerUsingServingClientMode
                = new OperationsHandlerForServerUsingServingClientMode();

        new WebShellLauncher().startServer(serverPort, serverNeedsServingClient,
                operationsHandlerForServerUsingServerMode,
                operationsHandlerForServerUsingServingClientMode);
    }

    public Javalin startServer(int serverPort, boolean serverNeedsServingClient,
                               OperationsHandlerForServerUsingServerMode operationsHandlerForServerUsingServerMode,
                               OperationsHandlerForServerUsingServingClientMode operationsHandlerForServerUsingServingClientMode) throws Exception
    {
        OperationsHandler operationsHandler = serverNeedsServingClient ?
                operationsHandlerForServerUsingServingClientMode : operationsHandlerForServerUsingServerMode;

        Javalin javalinServer = Javalin.create()
            .port(serverPort)
            .enableStaticFiles("/public")
            .ws("/terminal", ws ->
            {
                ws.onConnect(session ->
                {
                    session.getPolicy().setMaxTextMessageSize(70 * MB);

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
                    logger.info(session.getId() + " : closing websocket, status = " + status
                            + " : message = " + message + " : " + operationsHandler.getProcessInfoMap());
                    websocketSessionManager.removeSession(session.getId());

                    if(operationsHandler.getProcessInfoMap().get(session.getId()) != null
                            && operationsHandler.getProcessInfoMap().get(session.getId()) instanceof LocalProcessInfo)
                    {
                        ProcessUtils.stopProcess(
                                (LocalProcessInfo) operationsHandler.getProcessInfoMap().get(session.getId()));
                    }

                    operationsHandler.getProcessInfoMap().remove(session.getId());
                });

                ws.onError((session, error) -> logger.error(error.getMessage(), error));

                ws.onMessage((session, message) ->
                {
                    logger.info("WS : onMessage : " + session.getId());

                    if(! WebsocketUtils.isValidJson(message))
                    {
                        logger.warn("invalid message, " + message);
                        return;
                    }

                    if(serverNeedsServingClient)
                        WebsocketUtils.processMsgByServerUsingServingClient(operationsHandlerForServerUsingServingClientMode,
                                session.getId(), message, websocketSessionManager.getServingClientInfo());
                    else
                        WebsocketUtils.processMsgByServerLocally(operationsHandlerForServerUsingServerMode,
                                session.getId(), message);

                });

            })
            .start();


        startFileTransferScheduler();

        return javalinServer;
    }

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public ScheduledFuture startFileTransferScheduler() throws Exception
    {
        File senderFolder = new File("/tmp/lazycat/sender/"); // TODO !!!
        FileUtils.forceMkdir(senderFolder);

        ScheduledFuture scheduledFuture = executorService.scheduleAtFixedRate(() ->
        {
            Stream.of(senderFolder.listFiles())
                    .filter(File::isFile)
                    .forEach(file ->
                    Try.run(() ->
                    {
                        ServingClientInfo servingClientInfo = websocketSessionManager.getServingClientInfo();

                        if(WebsocketUtils.isAlive(servingClientInfo))
                            new FileReaderThread().fileTransfer(servingClientInfo.getWebsocketSession(), file);
                        else
                            logger.warn("websocketSession is closed, so exiting, " + servingClientInfo);
                    })
                    .onSuccess(it -> Try.run(() -> FileUtils.moveFileToDirectory(file, new File("/tmp/lazycat/sender/done"), true)))
                    .onFailure(ex -> logger.error("file transfer failed, " + ex))
            );
        }, 0,15, TimeUnit.SECONDS);

        return scheduledFuture;
    }


}
