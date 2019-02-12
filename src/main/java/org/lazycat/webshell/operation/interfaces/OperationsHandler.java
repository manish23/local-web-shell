package org.lazycat.webshell.operation.interfaces;

import io.javalin.websocket.WsSession;
import org.lazycat.webshell.process.impl.local.LocalProcessInfo;
import org.lazycat.webshell.process.interfaces.IProcessInfo;

import java.util.Map;

public interface OperationsHandler<T extends IProcessInfo>
{
    Map<String, T> getProcessInfoMap();
    void setProcessInfoMap(Map<String, T> processInfoMap);

    default T findProcessInfo(String processUuid)
    {
        return getProcessInfoMap().get(processUuid);
    }

    default T addProcessInfoIntoMap(String processUuid, T processInfo)
    {
        return getProcessInfoMap().put(processUuid, processInfo);
    }

    T onConnect(String processUuid, WsSession currentWebsocketSesssion) throws Exception;


}
