package org.lazycat.webshell.session;

import org.lazycat.webshell.process.ProcessInfo;
import io.javalin.websocket.WsSession;

public class SessionInfo
{
    private String username;
    private WsSession wsSession;
    private ProcessInfo processInfo;

    public SessionInfo(String username, WsSession wsSession, ProcessInfo processInfo) {
        this.username = username;
        this.wsSession = wsSession;
        this.processInfo = processInfo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public WsSession getWsSession() {
        return wsSession;
    }

    public void setWsSession(WsSession wsSession) {
        this.wsSession = wsSession;
    }

    public ProcessInfo getProcessInfo() {
        return processInfo;
    }

    public void setProcessInfo(ProcessInfo processInfo) {
        this.processInfo = processInfo;
    }
}
