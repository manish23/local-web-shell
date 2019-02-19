package org.lazycat.webshell.process.impl.remote;

import lombok.Getter;
import lombok.Setter;
import org.lazycat.webshell.process.ProcessType;
import org.lazycat.webshell.process.interfaces.IProcessInfo;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteProcessInfo that = (RemoteProcessInfo) o;
        return Objects.equals(remoteProcessUuid, that.remoteProcessUuid) &&
                Objects.equals(remoteWebSessionUuid, that.remoteWebSessionUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteProcessUuid, remoteWebSessionUuid);
    }
}
