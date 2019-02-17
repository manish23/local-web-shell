package org.lazycat.webshell.file;

import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.lazycat.webshell.websocket.message.WebsocketFileMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class FileThread implements Callable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private String filesLookupPath;
    private String filesSuccessPath;
    private String filesFailurePath;
    private Session websocketSession;

    public FileThread(String filesLookupPath, String filesSuccessPath, String filesFailurePath, Session websocketSession) {
        this.filesLookupPath = filesLookupPath;
        this.filesSuccessPath = filesSuccessPath;
        this.filesFailurePath = filesFailurePath;
        this.websocketSession = websocketSession;
    }

    @Override
    public Object call() throws Exception
    {

//        FileUtils.forceMkdir(new File(filesLookupPath));
//        FileUtils.forceMkdir(new File(filesSuccessPath));
//        FileUtils.forceMkdir(new File(filesFailurePath));
//
//        List<File> filesToTransfer = Arrays.stream(new File(filesLookupPath).listFiles())
//                .filter(File::exists)
//                .filter(File::isFile)
//                .collect(Collectors.toList());
//
//        for(File fileToTransfer : filesToTransfer)
//        {
//            Try.run(() -> fileTransfer(fileToTransfer))
//                    .onSuccess(it -> Try.run(() -> FileUtils.moveFileToDirectory(fileToTransfer, new File(filesSuccessPath), true))
//                            .onFailure(ex -> logger.error("cannot move file to " + filesSuccessPath, ex))
//                    )
//                    .onFailure(it -> Try.run(() -> FileUtils.moveFileToDirectory(fileToTransfer, new File(filesFailurePath), true))
//                            .onFailure(ex -> logger.error("cannot move file to " + filesFailurePath, ex))
//                    );
//        }

        return null;
    }

//    public void fileTransfer(File file) throws IOException
//    {
//        List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
//
//        for(int index = 0; index < lines.size(); index++)
//        {
//            boolean isLast = index < lines.size() ? false : true;
//
//            websocketSession.getRemote().sendPartialString(
//                    WebsocketFileMessage.builder()
//                            .fileName(file.getName())
//                            .fileContentLine(lines.get(index))
//                            .build().toJson(),
//                    isLast);
//        }
//
//    }
}
