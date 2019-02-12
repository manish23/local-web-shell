package org.lazycat.webshell.operation.impl;

import io.javalin.websocket.WsSession;
import io.vavr.control.Try;
import org.lazycat.webshell.operation.interfaces.OperationsHandler;
import org.lazycat.webshell.process.impl.local.LocalProcessInfo;
import org.lazycat.webshell.utils.ProcessUtils;
import org.lazycat.webshell.websocket.message.MessageType;
import org.lazycat.webshell.websocket.message.WebsocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;


public class OperationsHandlerForServerUsingServerMode implements OperationsHandler<LocalProcessInfo>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private Map<String, LocalProcessInfo> processInfoMap = new HashMap<>();

    @Override
    public LocalProcessInfo onConnect(String processUuid, WsSession currentWebsocketSesssion) throws Exception
    {
//        websocketMsgType = "TERMINAL_PRINT";

//        String processUuid = websocketSessionUuid;
        LocalProcessInfo localProcessInfo = createNewProcess(processUuid);

//        Session websocketSession = WebsocketSessionManager.getInstance().getSession(websocketSessionUuid);

        Executors.newSingleThreadExecutor().submit(() ->
                Try.run(() -> ProcessUtils.startReadingFromProcess(localProcessInfo,
                        currentWebsocketSesssion, MessageType.TERMINAL_PRINT, currentWebsocketSesssion.getId()))
                        .onFailure(ex -> logger.error("got exception while starting shell", ex)));

        return localProcessInfo;
    }

    public LocalProcessInfo createNewProcess(String processUuid) throws IOException
    {
        LocalProcessInfo localProcessInfo = ProcessUtils.startShellProcess(processUuid);
        return addProcessInfoIntoMap(processUuid, localProcessInfo);
    }

    public void onMessageTerminalResize(String processUuid, String message) throws Exception
    {
        WebsocketMessage websocketMessage = WebsocketMessage.fromJson(message);
        MessageType messageType = websocketMessage.getType();

        logger.info("onMessage() : sessionId = " + processUuid + " : " + message + " : " + messageType);

        LocalProcessInfo localProcessInfo = findProcessInfo(processUuid);

        ProcessUtils.resizeProcessWindow(localProcessInfo.getProcess(), websocketMessage.getCols(), websocketMessage.getRows());
    }

    public void onMessageTerminalCommand(String processUuid, String message) throws Exception {
        WebsocketMessage websocketMessage = WebsocketMessage.fromJson(message);

        logger.info("onMessage() : sessionId = " + processUuid + " : " + message);

        LocalProcessInfo localProcessInfo = findProcessInfo(processUuid);

        ProcessUtils.writeToProcess(localProcessInfo.getOutputWriter(), websocketMessage.getCommand());
    }

    public Map<String, LocalProcessInfo> getProcessInfoMap()
    {
        return processInfoMap;
    }

    public void setProcessInfoMap(Map<String, LocalProcessInfo> processInfoMap) {
        this.processInfoMap = processInfoMap;
    }
}
