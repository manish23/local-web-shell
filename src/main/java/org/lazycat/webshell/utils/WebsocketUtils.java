package org.lazycat.webshell.utils;

import com.jayway.jsonpath.JsonPath;
import io.javalin.websocket.WsSession;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.lazycat.webshell.Constants;
import org.lazycat.webshell.operation.impl.OperationsHandlerForServerUsingServerMode;
import org.lazycat.webshell.operation.impl.OperationsHandlerForServerUsingServingClientMode;
import org.lazycat.webshell.operation.impl.OperationsHandlerForServingClient;
import org.lazycat.webshell.server.ServingClientInfo;
import org.lazycat.webshell.websocket.message.MessageType;
import org.lazycat.webshell.websocket.message.WebsocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class WebsocketUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    public static void sendMsgToWebsocket(Session websocketSession, WebsocketMessage websocketMessage) throws Exception
    {
        logger.info("sendMsgToWebsocket : Enter : " + websocketMessage);

        String message = websocketMessage.toJson();

        if (websocketSession.isOpen())
            websocketSession.getRemote().sendString(message);
        else
            logger.error("cannot send msg to closed session, " + websocketMessage);

        logger.info("sendMsgToWebsocket : Exit");
    }

    public static void processMsgByServingClient(OperationsHandlerForServingClient operationsHandler
            , Session currentWebsocketSession, String message) throws Exception
    {
        MessageType messageType = MessageType.valueOf(JsonPath.read(message, "$.type"));

        switch (messageType)
        {
            case START_NEW_PROCESS:
                operationsHandler.onMessageStartNewProcess(message, currentWebsocketSession);
                break;

            case TERMINAL_COMMAND:
                operationsHandler.onMessageTerminalCommand(message);
                break;

            case TERMINAL_RESIZE:
                operationsHandler.onMessageTerminalResize(message);
                break;

            default:
                logger.warn("Unrecognized action, " + message);
        }
    }

    public static void processMsgByServerLocally(OperationsHandlerForServerUsingServerMode operationsHandler
            , String processUuid, String message) throws Exception
    {
        MessageType messageType = MessageType.valueOf(JsonPath.read(message, "$.type"));

        switch (messageType)
        {
            case TERMINAL_COMMAND:
                operationsHandler.onMessageTerminalCommand(processUuid, message);
                break;

            case TERMINAL_RESIZE:
                operationsHandler.onMessageTerminalResize(processUuid, message);
                break;

            default:
                logger.warn("Unrecognized action, " + message);
        }
    }

    public static void processMsgByServerUsingServingClient(OperationsHandlerForServerUsingServingClientMode operationsHandler,
            String processUuid, String message, ServingClientInfo servingClientInfo) throws Exception
    {
        MessageType messageType = MessageType.valueOf(JsonPath.read(message, "$.type"));

        switch (messageType)
        {
            case TERMINAL_COMMAND:
                operationsHandler.onMessageTerminalCommand(processUuid, message, servingClientInfo);
                break;

            case TERMINAL_RESIZE:
                operationsHandler.onMessageTerminalResize(processUuid, message, servingClientInfo);
                break;

            case TERMINAL_OUTPUT:
                operationsHandler.onMessageTerminalOutput(processUuid, message);
                break;

            default:
                logger.warn("Unrecognized action, " + message);
        }
    }

    public static boolean isValidClient(WsSession session, boolean serverNeedsServingClient)
    {
        if(session == null)
            return false;

        boolean isServingClient = BooleanUtils.toBoolean(session.getUpgradeRequest().getHeader("REGISTER_AS_SERVING_CLIENT"));

        if(! serverNeedsServingClient && isServingClient)
        {
            session.close(StatusCode.ABNORMAL,
                    "serving-client is not supposed to have a web-shell. So closing the session");
            return false;
        }

        return true;
    }

    public static boolean isServingClient(Session session)
    {
        return BooleanUtils.toBoolean(
                session.getUpgradeRequest().getHeader(Constants.REGISTER_AS_SERVING_CLIENT));
    }

}
