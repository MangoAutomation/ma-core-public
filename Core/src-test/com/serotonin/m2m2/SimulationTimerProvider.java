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
    
    /**
     * Create a new timer provider with a timer and set 
     *   Common.timer and Providers(TimerProvider) to use it.
     */
    public SimulationTimerProvider() {
        this(new SimulationTimer());
    }
    
    /**
     * Create a timer provider using the given timer and set 
     *   Common.timer and Providers(TimerProvider) to use it.
     * @param timer
     */
    public SimulationTimerProvider(SimulationTimer timer) {
        this.timer = timer;
        this.timer.init();
        Common.timer = timer;
    }
    
    public SimulationTimer reset() {
        this.timer.cancel();
        this.timer = new SimulationTimer();
        this.timer.init();
        Common.timer = this.timer;
        return this.timer;
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
