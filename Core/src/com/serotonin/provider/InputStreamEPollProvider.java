package com.serotonin.provider;

import com.serotonin.epoll.InputStreamEPoll;

public interface InputStreamEPollProvider extends Provider {
    void initialize();

    void terminate();

    InputStreamEPoll getInputStreamEPoll();
}
