/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.SimulationTimer;

/**
 * Timer Provider to use in testing.  Just create timer and fast forward though
 * time.  Useful to add as provider before testing begins.
 *   
 *   SimulationTimerProvider provider = new SimulationTimerProvider();
 *   Providers.add(TimerProvider.class, provider);
 *   provider.getSimulationTimer().fastForwardTo(instant);
 *
 * @author Terry Packer
 */
public class SimulationTimerProvider  implements TimerProvider<AbstractTimer> {

    private SimulationTimer timer;
    
    public SimulationTimerProvider() {
        this.timer = new SimulationTimer();
    }
    
    public void reset() {
        this.timer.cancel();
        this.timer = new SimulationTimer();
        Common.timer = this.timer;
    }
    /* (non-Javadoc)
     * @see com.serotonin.provider.TimerProvider#getTimer()
     */
    @Override
    public AbstractTimer getTimer() {
        return timer;
    }
    
    public SimulationTimer getSimulationTimer() {
        return timer;
    }
    
}
