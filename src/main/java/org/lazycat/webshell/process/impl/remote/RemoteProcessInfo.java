package org.lazycat.webshell.process.impl.remote;

import lombok.Getter;
import lombok.Setter;
import org.lazycat.webshell.process.ProcessType;
import org.lazycat.webshell.process.interfaces.IProcessInfo;

@Getter
@Setter
public class RemoteProcessInfo implements IProcessInfo
{
    private String remoteProcessUuid;
    private String remoteWebSessionUuid;

    public RemoteProcessInfo(String remoteProcessUuid, String remoteWebSessionUuid) {
        this.remoteProcessUuid = remoteProcessUuid;
        this.remoteWebSessionUuid = remoteWebSessionUuid;
    }

    @Override
    public ProcessType getProcessType() {
        return ProcessType.REMOTE;
    }

    @Override
    public String getProcessUuid() {
        return remoteProcessUuid;
    }

    @Override
    public void setProcessUuid(String processUuid) {
        this.remoteProcessUuid = processUuid;
    }

}
