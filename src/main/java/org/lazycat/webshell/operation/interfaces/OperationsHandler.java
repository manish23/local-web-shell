package org.lazycat.webshell.operation.interfaces;

import io.javalin.websocket.WsSession;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.lazycat.webshell.file.FileWriterThread;
import org.lazycat.webshell.process.impl.local.LocalProcessInfo;
import org.lazycat.webshell.process.interfaces.IProcessInfo;
import org.lazycat.webshell.websocket.message.WebsocketFileMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

public interface OperationsHandler<T extends IProcessInfo>
{
    Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

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

    default void onMessageFtp(String processUuid, String message) throws Exception
    {
        WebsocketFileMessage websocketFileMessage = WebsocketFileMessage.fromJson(message);

        logger.info("onMessage() : FILE_TRANSFER : sessionId = " + processUuid);

        FileWriterThread fileWriterThread = new FileWriterThread();

        if(websocketFileMessage.getFileContent() != null)
        {
            Try.run(() -> fileWriterThread.writeFile(websocketFileMessage))
                    .onFailure(ex -> logger.error("exception while writing file. File might get corrupted", ex));

            // TODO : If writeFile() gets error, Then next chunk will corrupt the entire file !!!
            // TODO : send TERMINAL_PRINT to show file transfer progress
        }

    }

}
