package org.lazycat.webshell.process;

public class ProcessOutput
{
    private ProcessOutputType outputType;

    private String message;

    public ProcessOutput(ProcessOutputType outputType, String message) {
        this.outputType = outputType;
        this.message = message;
    }

    public ProcessOutputType getOutputType() {
        return outputType;
    }

    public void setOutputType(ProcessOutputType outputType) {
        this.outputType = outputType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
