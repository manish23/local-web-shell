package org.lazycat.webshell.utils;

import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.lazycat.webshell.Constants;
import org.lazycat.webshell.process.impl.local.LocalProcessInfo;
import org.lazycat.webshell.websocket.message.MessageType;
import org.lazycat.webshell.websocket.message.WebsocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;

import static org.lazycat.webshell.utils.WebsocketUtils.sendMsgToWebsocket;

// http://www.golesny.de/p/code/javagetpid
public class ProcessUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    public static boolean isPortAvailable(int port)
    {
        try {
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
                    port, 1, InetAddress.getByName("localhost"));
            serverSocket.close();
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    public static int findAvailablePort(int minPort, int maxPort)
    {
//            Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
//            Assert.isTrue(maxPort >= minPort, "'maxPort' must be greater than or equal to 'minPort'");
//            Assert.isTrue(maxPort <= PORT_RANGE_MAX, "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);

        int candidatePort = minPort;

        while (!isPortAvailable(candidatePort))
        {
            if (candidatePort > maxPort) {
                throw new IllegalStateException(String.format(
                        "Could not find an available TCP port in the range [%d, %d] ",
                        minPort, maxPort));
            }
            candidatePort++;
        }

        return candidatePort;
    }

    public static void writeToProcess(BufferedWriter outputWriter, String command) throws IOException
    {
        if (StringUtils.isEmpty(command))
            return;

        outputWriter.write(command);
        outputWriter.flush();
    }

    public static String getShellStartCommand()
    {
        String shellStarter = System.getenv(Constants.shell);

        if (StringUtils.isEmpty(shellStarter))
            shellStarter = System.getenv(Constants.SHELL);

        if (StringUtils.isEmpty(shellStarter)) {
            shellStarter = "jshell.exe";
        }

        return shellStarter;
    }

    public static LocalProcessInfo startShellProcess(String processUuid) throws IOException
    {
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path dataDir = Paths.get(tmpDir).resolve(".terminalfx");
        copyLibPty(dataDir);

//        Path systemRoot = Files.createTempDirectory(Paths.get(tmpDir), "systemRoot");
//        Files.createDirectories(systemRoot);
//        Path prefsFile = Files.createTempFile(systemRoot, ".userPrefs", null);
//        System.setProperty("java.util.prefs.systemRoot", systemRoot.normalize().toString());
//        System.setProperty("java.util.prefs.userRoot", prefsFile.normalize().toString());

        String[] termCommand = getShellStartCommand().split("\\s+");

        Map<String, String> envs = new HashMap<>(System.getenv());
        envs.put("TERM", "xterm");

        System.setProperty(Constants.PTY_LIB_FOLDER, dataDir.resolve("libpty").toString());

        PtyProcess process = PtyProcess.exec(termCommand, envs, System.getProperty("user.home"));
//        process.setWinSize(new WinSize(50, 20));
        process.setWinSize(new WinSize(100, 20));

        return new LocalProcessInfo(process, processUuid);
    }

    public static void readFromProcessAndSendToWebsocket(BufferedReader bufferedReader, Session websocketSession
            , MessageType websocketMsgType, String receivingSessionUuid) throws Exception
    {
        int nRead;
        char[] data = new char[1 * 1024];

        while ((nRead = bufferedReader.read(data, 0, data.length)) != -1)
        {
            StringBuilder builder = new StringBuilder(nRead);
            builder.append(data, 0, nRead);

            logger.info("send -> " + builder.toString());

            WebsocketMessage websocketMessage = WebsocketMessage.builder()
                    .type(websocketMsgType)
                    .commandOutput(builder.toString())
                    .receivingSessionUuid(receivingSessionUuid)
                    .build();


            sendMsgToWebsocket(websocketSession, websocketMessage);
        }
    }

    public static void printHostname()
    {
        InetAddress inetAddress;
        String hostname;

        try
        {
            inetAddress = InetAddress.getLocalHost();
            hostname = inetAddress.getHostName();
            logger.info("IP address = " + inetAddress + " : Hostname = " + hostname);
        }
        catch (UnknownHostException ex)
        {
            logger.error("cannot get hostname", ex);
        }
    }

    public static void startReadingFromProcess(LocalProcessInfo processInfo, Session websocketSession
            , MessageType websocketMsgType, String receivingSessionUuid) throws Exception
    {
        Executors.newSingleThreadExecutor()//(new BasicThreadFactory.Builder().namingPattern("input-thread").build())
                .submit(() ->
                {
                    Try.run(() -> readFromProcessAndSendToWebsocket(processInfo.getInputReader(), websocketSession, websocketMsgType, receivingSessionUuid))
                            .onSuccess(a -> logger.info("process input-reader thread ended"))
                            .onFailure(ex -> logger.error("process input-reader thread failed", ex))
                            .andFinally(() -> IOUtils.closeQuietly(processInfo.getInputReader()));
                });

        Executors.newSingleThreadExecutor()//(new BasicThreadFactory.Builder().namingPattern("error-thread").build())
                .submit(() ->
                {
                    Try.run(() -> readFromProcessAndSendToWebsocket(processInfo.getErrorReader(), websocketSession, websocketMsgType, receivingSessionUuid))
                            .onSuccess(a -> logger.info("process error-reader thread ended"))
                            .onFailure(ex -> logger.error("process error-reader thread failed", ex))
                            .andFinally(() -> IOUtils.closeQuietly(processInfo.getErrorReader()));
                });

        processInfo.getProcess().waitFor();

        processInfo.getProcess().destroyForcibly();

        IOUtils.closeQuietly(processInfo.getOutputWriter());
    }

    public static void resizeProcessWindow(PtyProcess process, Integer cols, Integer rows)
    {
        if (cols != null && cols > 0
                && rows != null && rows > 0
                && process != null)
            process.setWinSize(new WinSize(cols, rows));
    }

    public static synchronized void copyLibPty(Path dataDir) throws IOException {

        Path donePath = dataDir.resolve(".DONE");

        if (Files.exists(donePath))
            return;

        Set<String> nativeFiles = getNativeFiles();

        for (String nativeFile : nativeFiles)
        {
            Path nativePath = dataDir.resolve(nativeFile);

            if (Files.notExists(nativePath))
            {
                Files.createDirectories(nativePath.getParent());

                try(InputStream inputStream = ProcessUtils.class.getResourceAsStream("/" + nativeFile);)
                {
                    Files.copy(inputStream, nativePath);
                }
            }

        }

        Files.createFile(donePath);
    }

    private static Set<String> getNativeFiles() {

        final Set<String> nativeFiles = new HashSet<>();

        List<String> freebsd = Arrays.asList("libpty/freebsd/x86/libpty.so", "libpty/freebsd/x86_64/libpty.so");
        List<String> linux = Arrays.asList("libpty/linux/x86/libpty.so", "libpty/linux/x86_64/libpty.so");
        List<String> macosx = Arrays.asList("libpty/macosx/x86/libpty.dylib", "libpty/macosx/x86_64/libpty.dylib");
        List<String> win_x86 = Arrays.asList("libpty/win/x86/winpty.dll", "libpty/win/x86/winpty-agent.exe");
        List<String> win_x86_64 = Arrays.asList("libpty/win/x86_64/winpty.dll", "libpty/win/x86_64/winpty-agent.exe", "libpty/win/x86_64/cyglaunch.exe");
        List<String> win_xp = Arrays.asList("libpty/win/xp/winpty.dll", "libpty/win/xp/winpty-agent.exe");

        nativeFiles.addAll(freebsd);
        nativeFiles.addAll(linux);
        nativeFiles.addAll(macosx);
        nativeFiles.addAll(win_x86);
        nativeFiles.addAll(win_x86_64);
        nativeFiles.addAll(win_xp);

        return nativeFiles;
    }

}
