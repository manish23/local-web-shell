package org.lazycat.webshell.process.interfaces;

import org.lazycat.webshell.process.ProcessType;

public interface IProcessInfo
{
    String getProcessUuid();

    void setProcessUuid(String processUuid);

    ProcessType getProcessType();
}
