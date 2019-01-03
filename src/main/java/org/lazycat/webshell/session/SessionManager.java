package org.lazycat.webshell.session;

import org.lazycat.webshell.process.ProcessInfo;
import org.lazycat.webshell.util.ProcessUtils;
import io.javalin.websocket.WsSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager
{
    private Map<WsSession, SessionInfo> userSessionMap = new ConcurrentHashMap<>();
    private static SessionManager sessionManager;

    private SessionManager()
    {

    }

    public static SessionManager getInstance()
    {
        if(sessionManager == null)
            sessionManager = new SessionManager();

        return sessionManager;
    }

    public boolean isSessionHasProcess(WsSession session)
    {
        return userSessionMap.containsKey(session) && userSessionMap.get(session).getProcessInfo() != null;
    }

    public void addSession(WsSession session, String username)
    {
        userSessionMap.put(session, new SessionInfo(username, session, null));
    }

    public SessionInfo getSessionInfo(WsSession session)
    {
        return userSessionMap.get(session);
    }

//    public void addSession(WsSession session, SessionInfo sessionInfo)
//    {
//        userSessionMap.put(session, sessionInfo);
//    }

    public SessionInfo removeSession(WsSession session)
    {
        removeProcessFromSession(session);
        return userSessionMap.remove(session);
    }

    public void removeProcessFromSession(WsSession session)
    {
        SessionInfo sessionInfo = userSessionMap.get(session);
        ProcessInfo processInfo = sessionInfo.getProcessInfo();
        if(processInfo != null)
        {
            ProcessUtils.killProcess(sessionInfo.getProcessInfo());
            processInfo.getErrorStreamReaderFut().cancel(true);
            processInfo.getInputStreamReaderFut().cancel(true);
        }

        sessionInfo.setProcessInfo(null);
    }

    public SessionInfo getUser(WsSession session)
    {
        return userSessionMap.get(session);
    }

    public Map<WsSession, SessionInfo> getUserSessionMap() {
        return userSessionMap;
    }

//    public void setUserSessionMap(Map<WsSession, SessionInfo> userSessionMap) {
//        this.userSessionMap = userSessionMap;
//    }
}
