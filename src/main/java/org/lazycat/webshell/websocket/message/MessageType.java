package org.lazycat.webshell.websocket.message;

public enum MessageType
{
    TERMINAL_COMMAND, TERMINAL_RESIZE, TERMINAL_PRINT, TERMINAL_OUTPUT,
    TERMINAL_READY, // deprecated
    START_NEW_PROCESS;
}
