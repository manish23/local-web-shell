package org.lazycat.webshell;

public class Constants
{
    public static final String REGISTER_AS_SERVING_CLIENT = "REGISTER_AS_SERVING_CLIENT";
    public static final String REGISTER_AS_SERVING_FTP_CLIENT = "REGISTER_AS_SERVING_FTP_CLIENT";
    public static final String PTY_LIB_FOLDER = "PTY_LIB_FOLDER";
    public static final String shell = "shell";
    public static final String SHELL = "SHELL";

    public static final String UNIX_DEFAULT_SHELL = "/bin/bash";
    public static final String WINDOWS_DEFAULT_SHELL = "jshell.exe";

    public static final long WS_SESSION_IDLE_TIMEOUT = Integer.MAX_VALUE;

    public static final String serverHost = "serverHost";
    public static final String serverPort = "serverPort";
    public static final String protocol = "protocol";
    public static final String topic = "topic";
    public static final String ftpMode = "ftpMode";
    public static final String file = "file";
    public static final String runAsDaemon = "runAsDaemon";

    public static final int KB = 1024;
    public static final int MB = 1024 * KB;

    public static final int SEC = 1000;
    public static final int MIN = 60 * SEC;
    public static final int HOUR = 60 * MIN;

}
