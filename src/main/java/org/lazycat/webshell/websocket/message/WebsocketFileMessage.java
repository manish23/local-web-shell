package org.lazycat.webshell.websocket.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebsocketFileMessage
{
    private String fileName;
    private String fileContentLine;
    private String processUuid;
    private String receivingSessionUuid;

    public Map<String, String> toMap()
    {
        Map<String, String> map = ((Map<String, String>) new ObjectMapper().convertValue(this, Map.class))
                .entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return map;
    }

    public static WebsocketFileMessage fromJson(String json) throws IOException
    {
        return new ObjectMapper().readValue(json, WebsocketFileMessage.class);
    }

    public String toJson() throws JsonProcessingException
    {
        Map<String, String> map = toMap();
        return new ObjectMapper().writeValueAsString(map);
    }
}
