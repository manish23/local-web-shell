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
public class FileReaderThread implements Callable
{
    private static final int CHUNK_SIZE = 50 * MB;

    private String filesLookupPath;
    private String filesSuccessPath;
    private String filesFailurePath;
    private Session websocketSession;

    public FileReaderThread()
    { }

    public FileReaderThread(String filesLookupPath, String filesSuccessPath, String filesFailurePath, Session websocketSession) {
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

        List<File> filesToTransfer = Arrays.stream(new File(filesLookupPath).listFiles())
                .filter(File::exists)
                .filter(File::isFile)
                .collect(Collectors.toList());

        for(File fileToTransfer : filesToTransfer)
        {
            Try.run(() -> fileTransfer(websocketSession, fileToTransfer))
                    .onSuccess(it -> Try.run(() -> FileUtils.moveFileToDirectory(fileToTransfer, new File(filesSuccessPath), true))
                            .onFailure(ex -> log.error("cannot move file to " + filesSuccessPath, ex))
                    )
                    .onFailure(it -> Try.run(() -> FileUtils.moveFileToDirectory(fileToTransfer, new File(filesFailurePath), true))
                            .onFailure(ex -> log.error("cannot move file to " + filesFailurePath, ex))
                    );
        }

        return null;
    }

    public void fileTransfer(Session websocketSession, File file) throws IOException
    {
        // /Users/manish/Downloads/android-studio-ide-181.5056338-mac.dmg

        if(file == null || ! file.exists() || file.isDirectory())
            return;

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
