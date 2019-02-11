package org.lazycat.webshell.operation.impl;

import io.javalin.websocket.WsSession;
import io.vavr.control.Try;
import org.eclipse.jetty.websocket.api.Session;
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


public class OperationsHandlerForServingClient implements OperationsHandler<LocalProcessInfo>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private Map<String, LocalProcessInfo> processInfoMap = new HashMap<>();

    @Override
    public LocalProcessInfo findProcessInfo(String processUuid)
    {
        return processInfoMap.get(processUuid);
    }

    @Override
    public LocalProcessInfo addProcessInfoIntoMap(String processUuid, LocalProcessInfo localProcessInfo)
    {
        return processInfoMap.put(processUuid, localProcessInfo);
    }

    @Override
    public LocalProcessInfo onConnect(String processUuid, WsSession currentWebsocketSesssion) throws Exception
    {
        return null;
    }

    public LocalProcessInfo createNewProcess(String processUuid) throws IOException
    {
        LocalProcessInfo localProcessInfo = ProcessUtils.startShellProcess(processUuid);
        addProcessInfoIntoMap(processUuid, localProcessInfo);

        return localProcessInfo;
    }

    public void onMessageStartNewProcess(String message, Session websocketSession) throws Exception
    {
        WebsocketMessage websocketMessage = WebsocketMessage.fromJson(message);
        String processUuid = websocketMessage.getProcessUuid();
        String receivingSessionUuid = websocketMessage.getProcessUuid();

        LocalProcessInfo localProcessInfo = createNewProcess(processUuid);

        Executors.newSingleThreadExecutor().submit(() ->
                Try.run(() -> ProcessUtils.startReadingFromProcess(localProcessInfo,
                        websocketSession, MessageType.TERMINAL_OUTPUT, receivingSessionUuid))
                        .onFailure(ex -> logger.error("got exception while starting shell", ex)));
    }

    public void onMessageTerminalResize(String message) throws Exception
    {
        WebsocketMessage websocketMessage = WebsocketMessage.fromJson(message);

        LocalProcessInfo processInfo = findProcessInfo(websocketMessage.getProcessUuid());

        ProcessUtils.resizeProcessWindow(processInfo.getProcess(), websocketMessage.getCols(), websocketMessage.getRows());
    }

    public void onMessageTerminalCommand(String message) throws Exception {
        WebsocketMessage websocketMessage = WebsocketMessage.fromJson(message);

        LocalProcessInfo localProcessInfo = findProcessInfo(websocketMessage.getProcessUuid());

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
