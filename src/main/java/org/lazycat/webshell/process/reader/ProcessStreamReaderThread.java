package org.lazycat.webshell.process.reader;

import org.apache.commons.io.IOUtils;
import org.lazycat.webshell.process.ProcessInfo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessStreamReaderThread implements Callable<String>
{
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private ProcessInfo processInfo;
    private InputStream processStream;
    private BlockingQueue<String> streamQueue;

    public ProcessStreamReaderThread(ProcessInfo processInfo, InputStream processStream, BlockingQueue<String> streamQueue)
    {
        this.processInfo = processInfo;
        this.processStream = processStream;
        this.streamQueue = streamQueue;
    }

    @Override
    public String call() throws Exception
    {
        if(processStream == null)
            return null;

        if(processInfo == null || processInfo.getProcess() == null) {
            logger.log(Level.WARNING, "process should not be null, " + processInfo);
            return null;
        }

        BufferedReader reader = null;

        try
        {
            reader = new BufferedReader(new InputStreamReader(processStream));

            String line = null;

            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
                streamQueue.put(line);
            }

            logger.info("process " + processInfo.getPid() + " has ended, " + processInfo.getCommand());
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "exception while reading process stream "
                    + " :: pid = " + processInfo.getPid() + " :: command = " + processInfo.getCommand()
                    + " :: Process.isAlive = " + processInfo.getProcess().isAlive(), ex);
        }
        finally
        {
            IOUtils.closeQuietly(reader);
        }

        return null;
    }
}
