package org.lazycat.webshell;

import org.lazycat.webshell.process.ProcessInfo;
import org.lazycat.webshell.process.ProcessManager;
import org.lazycat.webshell.process.ProcessOutput;
import org.lazycat.webshell.process.ProcessOutputType;
import org.lazycat.webshell.session.SessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.websocket.WsSession;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import static j2html.TagCreator.*;

public class LocalWebShell {

    private static int nextUserNumber = 1; // Assign to username for next connecting user
    private static final long WS_SESSION_IDLE_TIMEOUT = 1000 * 60 * 60; // 60 mins
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private SessionManager sessionManager = SessionManager.getInstance();
    private ProcessManager processManager = ProcessManager.getInstance();

    public static void main(String[] args)
    {
        printHostname();
        new LocalWebShell().startServer();
    }

    public void startServer() {

//        IntStream.range(0, 10)
//                .map(it -> ProcessUtils.findAvailablePort(7070, 7100))
//                .forEach(it -> System.out.println("random port = " + it));

        Javalin.create()
            .port(7070)
//            .port(HerokuUtil.getHerokuAssignedPort())
            .enableStaticFiles("/public")
            .ws("/chat", ws ->
            {
                ws.onConnect(session ->
                {
                    session.setIdleTimeout(WS_SESSION_IDLE_TIMEOUT);

                    String username = "User" + nextUserNumber++;
                    sessionManager.addSession(session, username);
//                    broadcastMessage("Server", (username + " joined the chat"));
                });
                ws.onClose((session, status, message) ->
                {
                    String username = sessionManager.getUser(session).getUsername();
                    sessionManager.removeSession(session);
                    broadcastMessage("Server", (username + " left the chat"));
                });
                ws.onError((session, error) ->
                        logger.log(Level.SEVERE, error.getMessage(), error));
                ws.onMessage((session, message) -> {
//                    broadcastMessage(userUsernameMap.get(session), message);
                    processManager.runCommand(session, message);
                });
            })
            .start();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // TODO : task timeout after 1 hr ??? WHY !!!
        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(
                () -> sendProcessOutput(), 0, 1, TimeUnit.SECONDS);

    }

    public void sendProcessOutput()
    {
        for(WsSession session : sessionManager.getUserSessionMap().keySet())
        {
            if(! session.isOpen())
            {
                sessionManager.removeSession(session);
                continue;
            }

            if(sessionManager.getUserSessionMap().get(session) == null
                    || sessionManager.getUserSessionMap().get(session).getProcessInfo() == null
                    || sessionManager.getUserSessionMap().get(session).getProcessInfo().getInputStreamQueue() == null)
                continue;

            String username = sessionManager.getUserSessionMap().get(session).getUsername();
            ProcessInfo processInfo = sessionManager.getUserSessionMap().get(session).getProcessInfo();

            List<String> msgs = new ArrayList<>();
            sessionManager.getUserSessionMap().get(session).getProcessInfo().getInputStreamQueue().drainTo(msgs);

            if(msgs.isEmpty())
                continue;

            List<ProcessOutput> output = msgs.stream()
                    .map(msg -> new ProcessOutput(ProcessOutputType.INFO, msg))
                    .collect(Collectors.toList());

            try
            {
                session.send(new ObjectMapper().writeValueAsString(output));
            }
            catch (JsonProcessingException e)
            {
                logger.log(Level.SEVERE, username + " :: process " + processInfo.getPid()
                        + " :: exception while sending msg", e);
            }

            // clean completed process
            if(! sessionManager.getUserSessionMap().get(session).getProcessInfo().getProcess().isAlive())
            {
                logger.info(username + " :: process " + processInfo.getPid()
                        + " is not alive, so cleaning up, " + processInfo.getCommand());
                sessionManager.removeProcessFromSession(session);
            }
        }


//        if(ProcessManager.getInstance().getInputStreamQueue().isEmpty())
//            return;
//
//        List<String> msgs = new ArrayList<>();
//        ProcessManager.getInstance().getInputStreamQueue().drainTo(msgs);
//        List<ProcessOutput> output = msgs.stream()
//                .map(msg -> new ProcessOutput(ProcessOutput.OUTPUT_TYPE.INFO, msg))
//                .collect(Collectors.toList());
//
//
//        for(WsSession session : sessionManager.getUserSessionMap().keySet())
//        {
//            if(! session.isOpen())
//                continue;
//
//            try
//            {
//                session.send(new ObjectMapper().writeValueAsString(output));
//            }
//            catch (JsonProcessingException e)
//            {
//                logger.log(Level.SEVERE, "exception while sending msg", e);
//            }
//        }
//
////        sessionManager.getUserUsernameMap().entrySet().stream()
////                .filter(it -> it.getKey().isOpen())
//////                .peek(it -> System.out.println("sending output to " + it.getValue()))
////                .forEach(it -> it.getKey().send(new ObjectMapper().writeValueAsString(it)));
    }

    // Sends a message from one user to all users, along with a list of current usernames
    private void broadcastMessage(String sender, String message) {
        sessionManager.getUserSessionMap().keySet().stream().filter(Session::isOpen).forEach(session -> {
            session.send(
                new JSONObject()
                    .put("userMessage", createHtmlMessageFromSender(sender, message))
                    .put("userlist", sessionManager.getUserSessionMap().values())
                    .put("log", "log_text_" + System.currentTimeMillis())
                    .toString()
            );
        });
    }

    // Builds a HTML element with a sender-name, a message, and a timestamp
    private static String createHtmlMessageFromSender(String sender, String message) {
        String html = article(
            b(sender + " says:"),
            span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
            p(message)
        ).render();

        System.out.println(new Date() + " :: " + sender + " :: " + html);

        return html;
    }

    public static void printHostname()
    {
        InetAddress inetAddress;
        String hostname;

        try
        {
            inetAddress = InetAddress.getLocalHost();
            hostname = inetAddress.getHostName();
            System.out.println("IP address = " + inetAddress);
            System.out.println("Hostname = " + hostname);
        }
        catch (UnknownHostException e)
        {
            System.err.println("cannot get hostname, " + e.getMessage());
            e.printStackTrace();
        }


    }

}
