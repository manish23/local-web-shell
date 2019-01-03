package org.lazycat.webshell.process;

import org.lazycat.webshell.process.reader.ProcessStreamReaderThread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ProcessInfo
{
    private Process process;
    private int pid;
    private String command;

    private BlockingQueue<String> inputStreamQueue;
//    private BlockingQueue<String> errorStreamQueue;
    private ProcessStreamReaderThread inputStreamReader;
    private ProcessStreamReaderThread errorStreamReader;
    private Future<String> inputStreamReaderFut;
    private Future<String> errorStreamReaderFut;

    public ProcessInfo(Process process, int pid, String command) {
        this.process = process;
        this.pid = pid;
        this.command = command;

        inputStreamQueue = new ArrayBlockingQueue<>(10000);
//        errorStreamQueue = new ArrayBlockingQueue<>(10000);
    }

    public void readStreams(ExecutorService threadPool)
    {
        inputStreamReader = new ProcessStreamReaderThread(
                this, this.getProcess().getInputStream(), inputStreamQueue);
        errorStreamReader = new ProcessStreamReaderThread(
                this, this.getProcess().getErrorStream(), inputStreamQueue);

        // start input-stream-reader thread
        inputStreamReaderFut = threadPool.submit(inputStreamReader);

        // start input-stream-reader thread
        errorStreamReaderFut = threadPool.submit(errorStreamReader);
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public BlockingQueue<String> getInputStreamQueue() {
        return inputStreamQueue;
    }

    public void setInputStreamQueue(BlockingQueue<String> inputStreamQueue) {
        this.inputStreamQueue = inputStreamQueue;
    }

//    public BlockingQueue<String> getErrorStreamQueue() {
//        return errorStreamQueue;
//    }
//
//    public void setErrorStreamQueue(BlockingQueue<String> errorStreamQueue) {
//        this.errorStreamQueue = errorStreamQueue;
//    }
//
    public ProcessStreamReaderThread getInputStreamReader() {
        return inputStreamReader;
    }

    public void setInputStreamReader(ProcessStreamReaderThread inputStreamReader) {
        this.inputStreamReader = inputStreamReader;
    }

    public ProcessStreamReaderThread getErrorStreamReader() {
        return errorStreamReader;
    }

    public void setErrorStreamReader(ProcessStreamReaderThread errorStreamReader) {
        this.errorStreamReader = errorStreamReader;
    }

    public Future<String> getInputStreamReaderFut() {
        return inputStreamReaderFut;
    }

    public void setInputStreamReaderFut(Future<String> inputStreamReaderFut) {
        this.inputStreamReaderFut = inputStreamReaderFut;
    }

    public Future<String> getErrorStreamReaderFut() {
        return errorStreamReaderFut;
    }

    public void setErrorStreamReaderFut(Future<String> errorStreamReaderFut) {
        this.errorStreamReaderFut = errorStreamReaderFut;
    }
}
