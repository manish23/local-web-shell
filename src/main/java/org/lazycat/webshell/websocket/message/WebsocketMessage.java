package org.lazycat.webshell.websocket.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import lombok.*;

import java.io.IOException;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebsocketMessage
{
    private MessageType type;
    private Integer cols;
    private Integer rows;
    private String command;
    private String commandOutput;
    private String processUuid;
    private String receivingSessionUuid;

    public Map<String, String> toMap()
    {
        Map<String, String> map = new ObjectMapper().convertValue(this, Map.class);

        Iterables.removeIf(map.values(), Predicates.isNull());

        return map;
    }

    public static WebsocketMessage fromJson(String json) throws IOException
    {
        return new ObjectMapper().readValue(json, WebsocketMessage.class);
    }

    public String toJson() throws JsonProcessingException
    {
        Map<String, String> map = toMap();
        return new ObjectMapper().writeValueAsString(map);
    }
}
