package org.lazycat.webshell.server;

import io.javalin.websocket.WsSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServingClientInfo
{
    private String sessionUuid;
    private WsSession websocketSession;

    public ServingClientInfo(WsSession websocketSession)
    {
        this.websocketSession = websocketSession;
        this.sessionUuid = websocketSession.getId();
    }
}
