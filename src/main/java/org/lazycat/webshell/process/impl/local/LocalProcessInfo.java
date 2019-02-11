package org.lazycat.webshell.process.impl.local;

import com.pty4j.PtyProcess;
import lombok.Getter;
import lombok.Setter;
import org.lazycat.webshell.process.ProcessType;
import org.lazycat.webshell.process.interfaces.IProcessInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

@Getter
@Setter
public class LocalProcessInfo implements IProcessInfo
{
    private String processUuid;
    private PtyProcess process;
    private BufferedWriter outputWriter;
    private BufferedReader inputReader;
    private BufferedReader errorReader;

    public LocalProcessInfo(PtyProcess process, String processUuid)
    {
        this.processUuid = processUuid;
        this.process = process;

        outputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    }

    @Override
    public ProcessType getProcessType() {
        return ProcessType.LOCAL;
    }

}
