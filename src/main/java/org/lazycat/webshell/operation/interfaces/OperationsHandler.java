package org.lazycat.webshell.operation.interfaces;

import io.javalin.websocket.WsSession;
import org.lazycat.webshell.process.impl.local.LocalProcessInfo;
import org.lazycat.webshell.process.interfaces.IProcessInfo;

import java.util.Map;

public interface OperationsHandler<T extends IProcessInfo>
{
    Map<String, T> getProcessInfoMap();
    void setProcessInfoMap(Map<String, T> processInfoMap);

    T findProcessInfo(String processUuid);
    T addProcessInfoIntoMap(String processUuid, T processInfo);

    T onConnect(String processUuid, WsSession currentWebsocketSesssion) throws Exception;

//    // Local
//    void startNewProcessAndSendOutputToWebsocket(String processUuid, String websocketSessionUuid) throws Exception;
//    void writeToProcess(String processUuid, String command) throws Exception;
//
//
//    void onStartNewProcess(String processUuid, String websocketSessionUuid) throws Exception;
//
//    void onTerminalCommand(String processUuid, String message) throws Exception;
//
//    void resizeProcessWindow(String processUuid, String message) throws Exception;
//
//    FIXME : void onTerminalOutput(String processUuid, String message) throws Exception;


//    // Serving Client
//    void onTerminalCommand(String processUuid, String message, WsSession websocketSession) throws Exception;
//
//    void resizeProcessWindow(String processUuid, String message, WsSession websocketSession) throws Exception;
//
//    void onTerminalOutput(String processUuid, String message) throws Exception;



}
