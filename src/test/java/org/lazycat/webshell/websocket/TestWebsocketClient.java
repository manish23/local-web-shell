package org.lazycat.webshell.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class TestWebsocketClient extends WebSocketAdapter
{
    private Session session;

    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
    }

    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        this.session = session;
    }

    public void onWebSocketError(Throwable cause) {
        cause.printStackTrace();
    }

    public void onWebSocketText(String message) {

    }

    public Session getSession() {
        return session;
    }
}
