package org.lazycat.webshell.operation.impl;

import io.javalin.websocket.WsSession;
import org.eclipse.jetty.websocket.api.Session;
import org.lazycat.webshell.operation.interfaces.OperationsHandler;
import org.lazycat.webshell.process.impl.remote.RemoteProcessInfo;
import org.lazycat.webshell.server.ServingClientInfo;
import org.lazycat.webshell.websocket.session.WebsocketSessionManager;
import org.lazycat.webshell.utils.WebsocketUtils;
import org.lazycat.webshell.websocket.message.MessageType;
import org.lazycat.webshell.websocket.message.WebsocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;


public class OperationsHandlerForServerUsingServingClientMode implements OperationsHandler<RemoteProcessInfo>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private Map<String, RemoteProcessInfo> processInfoMap = new HashMap<>();

    @Override
    public RemoteProcessInfo onConnect(String processUuid, WsSession currentSesssion) throws Exception
    {
        if(WebsocketUtils.isServingClient(currentSesssion))
            return null;

        String message = WebsocketMessage.builder()
                .type(MessageType.START_NEW_PROCESS)
                .processUuid(processUuid)
                .build().toJson();

        ServingClientInfo servingClientInfo = WebsocketSessionManager.getInstance().getServingClientInfo();

        if (servingClientInfo.getWebsocketSession().isOpen())
            servingClientInfo.getWebsocketSession().getRemote().sendString(message);

        return new RemoteProcessInfo(processUuid, servingClientInfo.getSessionUuid());
    }

    public void onMessageTerminalCommand(String processUuid, String message, ServingClientInfo servingClientInfo) throws Exception
    {
        WebsocketMessage websocketMessage = WebsocketMessage.fromJson(message);
        MessageType messageType = websocketMessage.getType();

        logger.info("onMessage() : sessionId = " + processUuid + " : " + message + " : " + messageType);

        websocketMessage.setProcessUuid(processUuid);

        WebsocketUtils.sendMsgToWebsocket(servingClientInfo.getWebsocketSession(), websocketMessage);
    }

    public void onMessageTerminalResize(String processUuid, String message, ServingClientInfo servingClientInfo) throws Exception
    {
        WebsocketMessage websocketMessage = WebsocketMessage.fromJson(message);
        MessageType messageType = websocketMessage.getType();

        logger.info("onMessage() : sessionId = " + processUuid + " : " + message + " : " + messageType);

        websocketMessage.setProcessUuid(processUuid);

        WebsocketUtils.sendMsgToWebsocket(servingClientInfo.getWebsocketSession(), websocketMessage);
    }

    public void onMessageTerminalOutput(String processUuid, String message) throws Exception
    {
        WebsocketMessage websocketMessage = WebsocketMessage.fromJson(message);
        MessageType messageType = websocketMessage.getType();

        logger.info("onMessage() : sessionId = " + processUuid + " : " + message + " : " + messageType);

        websocketMessage.setType(MessageType.TERMINAL_PRINT);

        Session session = WebsocketSessionManager.getInstance().getSession(websocketMessage.getReceivingSessionUuid());
        WebsocketUtils.sendMsgToWebsocket(session, websocketMessage);
    }

    @Override
    public Map<String, RemoteProcessInfo> getProcessInfoMap() {
        return processInfoMap;
    }

    @Override
    public void setProcessInfoMap(Map<String, RemoteProcessInfo> processInfoMap) {
        this.processInfoMap = processInfoMap;
    }

    @Override
    public RemoteProcessInfo findProcessInfo(String processUuid) {
        return processInfoMap.get(processUuid);
    }

    @Override
    public RemoteProcessInfo addProcessInfoIntoMap(String processUuid, RemoteProcessInfo processInfo) {
        return processInfoMap.put(processUuid, processInfo);
    }
}
