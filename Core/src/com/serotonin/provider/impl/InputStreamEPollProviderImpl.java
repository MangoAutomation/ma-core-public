package com.serotonin.provider.impl;

import com.serotonin.epoll.InputStreamEPoll;
import com.serotonin.provider.InputStreamEPollProvider;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;

public class InputStreamEPollProviderImpl implements InputStreamEPollProvider {
    private InputStreamEPoll inputStreamEPoll;

    @Override
    public void initialize() {
        // no op
    }

    @Override
    public void terminate() {
        if (inputStreamEPoll != null)
            inputStreamEPoll.terminate();
    }

    @Override
    public InputStreamEPoll getInputStreamEPoll() {
        if (inputStreamEPoll == null) {
            synchronized (this) {
                if (inputStreamEPoll == null) {
                    inputStreamEPoll = new InputStreamEPoll();
                    Providers.get(TimerProvider.class).getTimer()
                            .execute(inputStreamEPoll, inputStreamEPoll.getClass().getSimpleName());
                }
            }
        }

        return inputStreamEPoll;
    }
}
