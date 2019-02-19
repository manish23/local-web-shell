package org.lazycat.webshell.server;

import io.javalin.Javalin;
import io.vavr.API;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.lazycat.webshell.WebShellLauncher;
import org.lazycat.webshell.operation.impl.OperationsHandlerForServerUsingServerMode;
import org.lazycat.webshell.process.ProcessType;
import org.lazycat.webshell.process.impl.local.LocalProcessInfo;
import org.lazycat.webshell.utils.ProcessUtils;
import org.lazycat.webshell.utils.WebsocketUtils;
import org.lazycat.webshell.websocket.TestWebsocketClient;
import org.lazycat.webshell.websocket.message.MessageType;
import org.lazycat.webshell.websocket.message.WebsocketMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
//@Execution(ExecutionMode.CONCURRENT)
public class TestServer
{
    CountDownLatch testEndLatch;
    boolean testPassed = true;
    Throwable testFailureException = null;

    String serverHost = "localhost";
    String protocol = "ws";
    String topic = "/terminal";
    boolean serverNeedsServingClient = false;
    int serverPort = ProcessUtils.findAvailablePort(8000, 8050);
    String uriPath = null;

    @BeforeEach
    public void setup()
    {
        uriPath = protocol + "://" + serverHost + ":" + serverPort + topic;
        testEndLatch = new CountDownLatch(1);
    }

    @AfterEach
    public void end()
    {
        if(! testPassed)
            fail("test failed, " + ExceptionUtils.getStackTrace(testFailureException));
    }

    @DisplayName("e2e - clientConnect Then SendCommand Then GetOutput")
    @Test
    public void clientConnectThenSendCommandThenGetOutput() throws Exception
    {
        OperationsHandlerForServerUsingServerMode operationsHandlerForServerUsingServerMode
                = new OperationsHandlerForServerUsingServerMode();

        Javalin server = Try.ofCallable(() -> new WebShellLauncher().startServer(serverPort, serverNeedsServingClient,
                operationsHandlerForServerUsingServerMode, null)).get();


        AtomicReference<LocalProcessInfo> localProcessInfo = new AtomicReference<>();
        WebSocketClient webSocketClient = new WebSocketClient();

        AtomicReference<CountDownLatch> waitForOnMessage = new AtomicReference<>(new CountDownLatch(1));
        AtomicReference<WebsocketMessage> websocketMessageWrapper = new AtomicReference<>();
        AtomicReference<String> sessionIdWrapper = new AtomicReference<>();
        AtomicInteger messageCount = new AtomicInteger(1);

        TestWebsocketClient testWebsocket = new TestWebsocketClient()
        {
            @Override
            public void onWebSocketText(String message)
            {
                verify(() -> assertTrue(WebsocketUtils.isValidJson(message)));

                WebsocketMessage websocketMessage = Try.of(() -> WebsocketMessage.fromJson(message)).get();
                websocketMessageWrapper.set(websocketMessage);

                verify(() -> assertThat(websocketMessage.getReceivingSessionUuid(), is(not(isEmptyOrNullString()))));
                verify(() -> assertThat(operationsHandlerForServerUsingServerMode.getProcessInfoMap(), aMapWithSize(1)));

                localProcessInfo.set(
                        operationsHandlerForServerUsingServerMode.getProcessInfoMap()
                                .get(websocketMessage.getReceivingSessionUuid()));

                verifyShellProcess(localProcessInfo.get(), true);

                if(sessionIdWrapper.get() == null)
                    sessionIdWrapper.set(websocketMessage.getReceivingSessionUuid());

                // verify messages
                if(messageCount.getAndIncrement() == 1) // onConnect
                {
                    verify(() -> assertThat(websocketMessageWrapper.get().getCommand(), isEmptyOrNullString()));
                    verify(() -> assertThat(websocketMessageWrapper.get().getProcessUuid(), isEmptyOrNullString()));
                    verify(() -> assertThat(websocketMessageWrapper.get().getType(), equalTo(MessageType.TERMINAL_PRINT)));
                    verify(() -> assertThat(websocketMessageWrapper.get().getReceivingSessionUuid(), equalTo(sessionIdWrapper.get())));
                    verify(() -> assertThat(websocketMessageWrapper.get().getCommandOutput(), not(isEmptyOrNullString())));
                }
                else if(messageCount.getAndIncrement() == 2) // send command
                {
                    verify(() -> assertThat(websocketMessageWrapper.get().getCommand(), isEmptyOrNullString()));
                    verify(() -> assertThat(websocketMessageWrapper.get().getProcessUuid(), isEmptyOrNullString()));
                    verify(() -> assertThat(websocketMessageWrapper.get().getType(), equalTo(MessageType.TERMINAL_PRINT)));
                    verify(() -> assertThat(websocketMessageWrapper.get().getReceivingSessionUuid(), equalTo(sessionIdWrapper.get())));
                    verify(() -> assertThat(websocketMessageWrapper.get().getCommandOutput(), equalTo("echo 12345\r\n")));
                }
                else if(messageCount.getAndIncrement() == 3) // command output
                {
                    verify(() -> assertThat(websocketMessageWrapper.get().getCommand(), isEmptyOrNullString()));
                    verify(() -> assertThat(websocketMessageWrapper.get().getProcessUuid(), isEmptyOrNullString()));
                    verify(() -> assertThat(websocketMessageWrapper.get().getType(), equalTo(MessageType.TERMINAL_PRINT)));
                    verify(() -> assertThat(websocketMessageWrapper.get().getReceivingSessionUuid(), equalTo(sessionIdWrapper.get())));
                    verify(() -> assertThat(websocketMessageWrapper.get().getCommandOutput(), startsWith("12345\r")));
                }

                waitForOnMessage.get().countDown();
            }
        };

//        Process process = new ProcessBuilder("/bin/bash", "-c", "echo 12345").start();
//        String expectedCommandOutput = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());

        webSocketClient.start();
        webSocketClient.connect(testWebsocket, new URI(uriPath)).get();
        waitForOnMessage.get().await(2, TimeUnit.SECONDS);

        waitForOnMessage.set(new CountDownLatch(1));
        testWebsocket.getSession().getRemote().sendString(
                WebsocketMessage.builder().command("echo 12345" + "\n").type(MessageType.TERMINAL_COMMAND).build().toJson());
        waitForOnMessage.get().await(2, TimeUnit.SECONDS);

        waitForOnMessage.set(new CountDownLatch(1));
        waitForOnMessage.get().await(2, TimeUnit.SECONDS);

    }


    @DisplayName("server shud ignore invalid json msg from client")
    @Test
    public void testClientSendingInvalidMessage() throws Throwable
    {
        OperationsHandlerForServerUsingServerMode operationsHandlerForServerUsingServerMode
                = new OperationsHandlerForServerUsingServerMode();

        Try.ofCallable(() -> new WebShellLauncher().startServer(serverPort, serverNeedsServingClient,
                operationsHandlerForServerUsingServerMode, null)).get();

        WebSocketClient webSocketClient = new WebSocketClient();

        TestWebsocketClient testWebsocket = new TestWebsocketClient()
        {
            @Override
            public void onWebSocketText(String message)
            {
                verify(() -> assertTrue(WebsocketUtils.isValidJson(message)));

                WebsocketMessage websocketMessage = Try.of(() -> WebsocketMessage.fromJson(message)).get();

                verify(() -> assertThat(websocketMessage.getReceivingSessionUuid(), is(not(isEmptyOrNullString()))));
                verify(() -> assertThat(operationsHandlerForServerUsingServerMode.getProcessInfoMap(), aMapWithSize(1)));

                LocalProcessInfo localProcessInfo = operationsHandlerForServerUsingServerMode.getProcessInfoMap()
                        .get(websocketMessage.getReceivingSessionUuid());

                verifyShellProcess(localProcessInfo, true);

                getSession().close();
                testEndLatch.countDown();
            }

        };

        webSocketClient.start();
        webSocketClient.connect(testWebsocket, new URI(uriPath)).get();
        testWebsocket.getSession().getRemote().sendString("Hello World");

        testEndLatch.await(5, TimeUnit.SECONDS);

    }

    @DisplayName("close-session should stop shell process on server")
    @Test
    public void testClientCloseSessionShouldStopShellProcess() throws Exception
    {
        OperationsHandlerForServerUsingServerMode operationsHandlerForServerUsingServerMode
                = new OperationsHandlerForServerUsingServerMode();

        Javalin server = Try.ofCallable(() -> new WebShellLauncher().startServer(serverPort, serverNeedsServingClient,
                operationsHandlerForServerUsingServerMode, null)).get();


        AtomicReference<LocalProcessInfo> localProcessInfo = new AtomicReference<>();
        WebSocketClient webSocketClient = new WebSocketClient();

        CountDownLatch waitForOnMessage = new CountDownLatch(1);

        TestWebsocketClient testWebsocket = new TestWebsocketClient()
        {
            @Override
            public void onWebSocketText(String message)
            {
                verify(() -> assertTrue(WebsocketUtils.isValidJson(message)));

                WebsocketMessage websocketMessage = Try.of(() -> WebsocketMessage.fromJson(message)).get();

                verify(() -> assertThat(websocketMessage.getReceivingSessionUuid(), is(not(isEmptyOrNullString()))));
                verify(() -> assertThat(operationsHandlerForServerUsingServerMode.getProcessInfoMap(), aMapWithSize(1)));

                localProcessInfo.set(
                        operationsHandlerForServerUsingServerMode.getProcessInfoMap()
                                .get(websocketMessage.getReceivingSessionUuid()));

                verifyShellProcess(localProcessInfo.get(), true);

                waitForOnMessage.countDown();
            }
        };

        webSocketClient.start();
        webSocketClient.connect(testWebsocket, new URI(uriPath)).get();

        testWebsocket.getSession().getRemote().sendString("Hello World");
        waitForOnMessage.await(5, TimeUnit.SECONDS);

        testWebsocket.getSession().close();
        Thread.sleep(1500);

        verifyShellProcess(localProcessInfo.get(), false);
        verify(() -> assertThat(operationsHandlerForServerUsingServerMode.getProcessInfoMap(), aMapWithSize(0)));

    }

    @DisplayName("client sends valid json msg")
    @Test
    public void testClientSendingValidMessage() throws Exception
    {
        OperationsHandlerForServerUsingServerMode operationsHandlerForServerUsingServerMode
                = new OperationsHandlerForServerUsingServerMode();

        Try.ofCallable(() -> new WebShellLauncher().startServer(serverPort, serverNeedsServingClient,
                operationsHandlerForServerUsingServerMode, null)).get();


        AtomicReference<LocalProcessInfo> localProcessInfo = new AtomicReference<>();
        WebSocketClient webSocketClient = new WebSocketClient();

        CountDownLatch waitForOnMessage = new CountDownLatch(1);

        TestWebsocketClient testWebsocket = new TestWebsocketClient()
        {
            @Override
            public void onWebSocketText(String message)
            {
                verify(() -> assertTrue(WebsocketUtils.isValidJson(message)));

                WebsocketMessage websocketMessage = Try.of(() -> WebsocketMessage.fromJson(message)).get();

                verify(() -> assertThat(websocketMessage.getReceivingSessionUuid(), is(not(isEmptyOrNullString()))));
                verify(() -> assertThat(operationsHandlerForServerUsingServerMode.getProcessInfoMap(), aMapWithSize(1)));

                localProcessInfo.set(
                        operationsHandlerForServerUsingServerMode.getProcessInfoMap()
                                .get(websocketMessage.getReceivingSessionUuid()));

                verifyShellProcess(localProcessInfo.get(), true);

                waitForOnMessage.countDown();
            }
        };

        webSocketClient.start();
        webSocketClient.connect(testWebsocket, new URI(uriPath)).get();

        testWebsocket.getSession().getRemote().sendString(
                WebsocketMessage.builder().type(MessageType.TERMINAL_READY).build().toJson());
        waitForOnMessage.await(5, TimeUnit.SECONDS);

        verifyShellProcess(localProcessInfo.get(), true);
        verify(() -> assertThat(operationsHandlerForServerUsingServerMode.getProcessInfoMap(), aMapWithSize(1)));

        testEndLatch.await(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("server start and stop")
    public void serverUpAndDown()
    {
        int serverPort = ProcessUtils.findAvailablePort(8000, 8050);

        Arrays.asList(true, false).stream().forEach(serverNeedsServingClient ->
        {
            Javalin javalin = Try.ofCallable(() -> new WebShellLauncher().startServer(serverPort, serverNeedsServingClient,
                    null, null)).get();

            assertThat("server not coming up on port " + serverPort,
                    false, equalTo(ProcessUtils.isPortAvailable(serverPort)));

            javalin.stop();

            assertThat("server not going down, and still running on port " + serverPort,
                    true, equalTo(ProcessUtils.isPortAvailable(serverPort)));
        });

    }

    public void verify(CheckedRunnable runnable)
    {
        Try.run(runnable)
                .onFailure(ex ->
                {
                    testPassed = false;
                    testFailureException = ex;
                    testEndLatch.countDown();
                });
    }

    public void verifyShellProcess(LocalProcessInfo localProcessInfo, boolean expectedProcessStatus)
    {
        verify(() -> assertNotNull(localProcessInfo, "localProcessInfo"));
        verify(() -> assertNotNull(localProcessInfo.getProcess(), "localProcessInfo"));

        verify(() -> assertThat("process type check",
                ProcessType.LOCAL, equalTo(localProcessInfo.getProcessType())));

        verify(() -> assertThat("process isRunning() check",
                expectedProcessStatus, equalTo(localProcessInfo.getProcess().isRunning())));

        verify(() -> assertThat("process isAlive() check",
                expectedProcessStatus, equalTo(localProcessInfo.getProcess().isAlive())));
    }
}
