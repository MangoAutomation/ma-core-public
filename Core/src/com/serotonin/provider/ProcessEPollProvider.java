package com.serotonin.provider;

import com.serotonin.epoll.ProcessEPoll;

public interface ProcessEPollProvider extends Provider {
    ProcessEPoll getProcessEPoll();

    void terminate(boolean waitForAll);
}
