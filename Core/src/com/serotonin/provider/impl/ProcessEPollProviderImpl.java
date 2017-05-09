package com.serotonin.provider.impl;

import com.serotonin.epoll.ProcessEPoll;
import com.serotonin.provider.ProcessEPollProvider;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;

public class ProcessEPollProviderImpl implements ProcessEPollProvider {
    private ProcessEPoll processEPoll;

    @Override
    public void terminate(boolean waitForAll) {
        if (processEPoll != null) {
            if (waitForAll)
                processEPoll.waitForAll();
            processEPoll.terminate();
        }
    }

    @Override
    public ProcessEPoll getProcessEPoll() {
        if (processEPoll == null) {
            synchronized (this) {
                if (processEPoll == null) {
                    processEPoll = new ProcessEPoll();
                    Providers.get(TimerProvider.class).getTimer()
                            .execute(processEPoll, processEPoll.getClass().getSimpleName());
                    processEPoll.waitUntilStarted();
                }
            }
        }
        return processEPoll;
    }
}
