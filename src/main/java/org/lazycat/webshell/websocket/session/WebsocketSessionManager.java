package org.lazycat.webshell.websocket.session;

import io.javalin.websocket.WsSession;
import org.eclipse.jetty.websocket.api.Session;
import org.lazycat.webshell.server.ServingClientInfo;
import org.lazycat.webshell.utils.WebsocketUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebsocketSessionManager
{
    private Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private ServingClientInfo servingClientInfo = null;

    private static WebsocketSessionManager websocketSessionManager;

    private WebsocketSessionManager() { }

    public static WebsocketSessionManager getInstance()
    {
        if(websocketSessionManager == null)
            websocketSessionManager = new WebsocketSessionManager();

        return websocketSessionManager;
    }

    public Session getSession(String sessionUuid)
    {
        return sessionMap.get(sessionUuid);
    }

    public void addSession(String sessionUuid, Session session)
    {
        if(WebsocketUtils.isServingClient(session))
            servingClientInfo = new ServingClientInfo((WsSession) session);

        sessionMap.put(sessionUuid, session);
    }

    public Session removeSession(String sessionUuid)
    {
        return sessionMap.remove(sessionUuid);
    }

    public ServingClientInfo getServingClientInfo() {
        return servingClientInfo;
    }
}
