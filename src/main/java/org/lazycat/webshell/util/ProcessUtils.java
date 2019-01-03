package org.lazycat.webshell.util;

import org.lazycat.webshell.process.ProcessInfo;
import org.lazycat.webshell.session.SessionInfo;
import org.lazycat.webshell.session.SessionManager;
import io.javalin.websocket.WsSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// http://www.golesny.de/p/code/javagetpid
public class ProcessUtils
{
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    public static final String KILL = "kill";

    public static int getPid(Process process)
    {
        if(process.getClass().getName().equals("java.lang.UNIXProcess"))
        {
            /* get the PID on unix/linux systems */
            try
            {
                return (int) FieldUtils.readField(process, "pid", true);
            }
            catch (Throwable e)
            {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }

        return -1;
    }

    public static boolean isPortAvailable(int port) {
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

    public static int findAvailablePort(int minPort, int maxPort) {
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

    public static boolean isActive(ProcessInfo processInfo)
    {
        if(processInfo != null
                && processInfo.getProcess() != null
                && processInfo.getProcess().isAlive())
            return true;

        return false;
    }

    public static ProcessInfo startProcess(ProcessBuilder processBuilder, String command) throws IOException
    {
        List<String> commands = Arrays.stream(command.split(" "))
                .collect(Collectors.toList());

        processBuilder.command(commands);
//        processBuilder.redirectErrorStream(true);
//        processBuilder.inheritIO();

        logger.info("starting process...");

        Process process = processBuilder.start();
        int pid = ProcessUtils.getPid(process);

        logger.info("pid = " + pid + " :: " + command);

        ProcessInfo processInfo = new ProcessInfo(process, pid, command);

        return processInfo;
    }

    public static ProcessInfo startProcessAndReadStreams(ProcessBuilder processBuilder, String command, ExecutorService threadPool)
            throws IOException, InterruptedException
    {
        ProcessInfo processInfo = ProcessUtils.startProcess(processBuilder, command);
        processInfo.readStreams(threadPool);
        processInfo.getProcess().waitFor(300, TimeUnit.MILLISECONDS);

        return processInfo;
    }

    public static void killProcess(ProcessInfo processInfo)
    {
        if(processInfo == null || processInfo.getProcess() == null)
            return;

        logger.info("attempting to kill process " + processInfo.getPid()
                + " :: isAlive = " + processInfo.getProcess().isAlive());

        processInfo.getProcess().destroyForcibly();

        logger.info("process " + processInfo.getPid() + " killed" +
                " :: isAlive = " + processInfo.getProcess().isAlive());
    }

    public static boolean isKillCommand(String command)
    {
        return KILL.equalsIgnoreCase(StringUtils.trimToEmpty(command));
    }

    public static void cleanupDeadProcess(SessionManager sessionManager, WsSession session)
    {
        SessionInfo sessionInfo = sessionManager.getSessionInfo(session);
        ProcessInfo processInfo = sessionInfo.getProcessInfo();

        if(processInfo != null && processInfo.getProcess() != null && ! processInfo.getProcess().isAlive())
            sessionManager.removeProcessFromSession(session);
    }

}
