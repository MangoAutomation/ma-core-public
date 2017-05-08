package com.serotonin.provider;

import com.serotonin.timer.AbstractTimer;

public interface TimerProvider<T extends AbstractTimer> extends Provider {
    T getTimer();
}
