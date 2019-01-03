package org.lazycat.webshell.process;

import org.lazycat.webshell.session.SessionInfo;
import org.lazycat.webshell.session.SessionManager;
import org.lazycat.webshell.util.ProcessUtils;
import io.javalin.websocket.WsSession;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessManager
{
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private static ProcessManager processManager;
    private ProcessBuilder processBuilder;
    private ExecutorService threadPool;

    private ProcessManager()
    {
        threadPool = Executors.newFixedThreadPool(100);
        processBuilder = new ProcessBuilder();
    }

    public static ProcessManager getInstance()
    {
        if(processManager == null)
            processManager = new ProcessManager();

        return processManager;
    }

    public void runCommand(WsSession session, String command) throws Exception
    {
        SessionManager sessionManager = SessionManager.getInstance();
        SessionInfo sessionInfo = sessionManager.getSessionInfo(session);

        if(sessionManager.isSessionHasProcess(session))
        {
            ProcessInfo existingProcessInfo = sessionInfo.getProcessInfo();

            if(ProcessUtils.isKillCommand(command))
            {
                sessionManager.removeProcessFromSession(session);
            }
            else if(ProcessUtils.isActive(existingProcessInfo))
            {
                // 1. if any process is already running, then dont start new process
                // 2. first "kill" the existing process then start new process

                logger.log(Level.WARNING, "cannot run another command "
                        + "bcoz current process is still active with pid = " + existingProcessInfo.getPid()
                        + " , kill it first");
            }
            else
            {
                // its NOT a kill command AND current process is NOT alive
                // so start a new process
                sessionManager.removeProcessFromSession(session);

                ProcessInfo newProcessInfo = ProcessUtils.startProcessAndReadStreams(processBuilder, command, threadPool);
                sessionInfo.setProcessInfo(newProcessInfo);
            }
        }
        else
        {
            ProcessInfo processInfo = ProcessUtils.startProcessAndReadStreams(processBuilder, command, threadPool);
            sessionInfo.setProcessInfo(processInfo);
        }

//        ProcessUtils.cleanupDeadProcess(sessionManager, session);
    }


//    public void run(String command) throws Exception
//    {
////        processBuilder.environment().entrySet().stream()
////                .forEach(entry ->
////                        System.out.println(entry.getKey() + " - " + entry.getValue()));
////
////        processBuilder.command(Arrays.asList("ping", "www.google.com"));
////        processBuilder.command(Arrays.asList("sh", "-c", "ls", "-lart"));
////        processBuilder.command(Arrays.asList("ls", "-lart"));
//
//        // is it a kill command
//        if(ProcessUtils.isKillCommand(command))
//        {
//            ProcessUtils.killProcess(processInfo);
//            return;
//        }
//
//        // 1. if any process is already running, then dont start new process
//        // 2. first "kill" the existing process then start new process
//        if(ProcessUtils.isActive(processInfo))
//        {
//            logger.log(Level.WARNING, "cannot run another command "
//                    + "bcoz current process is still active with pid = " + processInfo.getPid()
//                    + " , kill it first");
//            return;
//        }
//
////        // stop existing input-stream-reader thread
////        if(inputStreamReader != null && inputStreamReaderFut != null && ! inputStreamReaderFut.isDone())
////            inputStreamReaderFut.cancel(true);
////
////        // stop existing error-stream-reader thread
////        if(errorStreamReader != null && errorStreamReaderFut != null && ! errorStreamReaderFut.isDone())
////            errorStreamReaderFut.cancel(true);
////
////        processInfo = ProcessUtils.startProcess(processBuilder, command);
////
////        inputStreamReader = new ProcessStreamReaderThread(
////                processInfo, processInfo.getProcess().getInputStream(), inputStreamQueue);
////        errorStreamReader = new ProcessStreamReaderThread(
////                processInfo, processInfo.getProcess().getErrorStream(), inputStreamQueue);
////
////        // start input-stream-reader thread
////        inputStreamReaderFut = threadPool.submit(inputStreamReader);
////
////        // start input-stream-reader thread
////        errorStreamReaderFut = threadPool.submit(errorStreamReader);
////
////        processInfo.getProcess().waitFor(1, TimeUnit.SECONDS);
//    }

}
