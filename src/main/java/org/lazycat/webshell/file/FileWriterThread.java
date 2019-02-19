package org.lazycat.webshell.file;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.lazycat.webshell.websocket.message.WebsocketFileMessage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
public class FileWriterThread implements Callable
{
    private static final int CHUNK_SIZE = 64 * 1024;

    private String filesLookupPath;
    private String filesSuccessPath;
    private String filesFailurePath;
    private Session websocketSession;

    public FileWriterThread(String filesLookupPath, String filesSuccessPath, String filesFailurePath, Session websocketSession) {
        this.filesLookupPath = filesLookupPath;
        this.filesSuccessPath = filesSuccessPath;
        this.filesFailurePath = filesFailurePath;
        this.websocketSession = websocketSession;
    }

    @Override
    public Object call() throws Exception
    {
        FileUtils.forceMkdir(new File(filesLookupPath));
        FileUtils.forceMkdir(new File(filesSuccessPath));
        FileUtils.forceMkdir(new File(filesFailurePath));

        return null;
    }

    public void writeFile(WebsocketFileMessage websocketFileMessage) throws IOException
    {
        // /Users/manish/Downloads/android-studio-ide-181.5056338-mac.dmg

        filesLookupPath = "/tmp/received/";
        FileUtils.forceMkdir(new File(filesLookupPath));

        File file = new File(filesLookupPath + websocketFileMessage.getFileName());
        Files.write(Paths.get(file.getPath()), websocketFileMessage.getFileContent(), file.exists() ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
    }
}
