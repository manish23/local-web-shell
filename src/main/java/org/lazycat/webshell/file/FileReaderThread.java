package org.lazycat.webshell.file;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.lazycat.webshell.websocket.message.MessageType;
import org.lazycat.webshell.websocket.message.WebsocketFileMessage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.lazycat.webshell.Constants.MB;

@Slf4j
public class FileReaderThread //implements Callable
{
    private static final int CHUNK_SIZE = 50 * MB;

    private String filesLookupPath;
    private String filesSuccessPath;
    private String filesFailurePath;
    private Session websocketSession;

    public void fileTransfer(Session websocketSession, File file) throws IOException
    {
        // /Users/manish/Downloads/android-studio-ide-181.5056338-mac.dmg

        if(file == null || ! file.exists() || file.isDirectory())
        {
            log.error("file doesnt exist or its a directory, so exiting, " + file);
            return;
        }

        if(websocketSession == null || ! websocketSession.isOpen())
        {
            log.error("websocketSession is closed, so exiting, " + websocketSession);
            return;
        }

        try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(file)))
        {
            int msgCount = 1;
            byte[] buffer = new byte[CHUNK_SIZE];
            int totalBytesRead;
            while ((totalBytesRead = in.read(buffer)) != -1)
            {
                byte[] data = buffer;

                if(totalBytesRead != buffer.length)
                {
                    data = new byte[totalBytesRead];
                    System.arraycopy(buffer, 0, data, 0, totalBytesRead);
                }

                String websocketFileMessage = WebsocketFileMessage.builder()
                        .type(MessageType.FILE_TRANSFER)
                        .fileName(file.getName())
                        .fileContent(data)
                        .build().toJson();

                log.info(msgCount++ + " : data chunk size = " + data.length + " : msg.len = " + websocketFileMessage.length());

                websocketSession.getRemote().sendString(websocketFileMessage);
            }

        }

//        FileChannel fc = new FileInputStream(file).getChannel();
//        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, (int) fc.size());
    }
}
