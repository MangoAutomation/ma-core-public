/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.script.ping;

public class PingStats {
    final int count;
    final Float packetLoss;
    final Float minimum;
    final Float average;
    final Float maximum;

    public PingStats(int count, Float minimum, Float maximum, Float average, Float packetLoss) {
        this.count = count;
        this.packetLoss = packetLoss;
        this.minimum = minimum;
        this.average = average;
        this.maximum = maximum;
    }

    public int getCount() {
        return count;
    }

    public Float getPacketLoss() {
        return packetLoss;
    }

    public Float getMinimum() {
        return minimum;
    }

    public Float getAverage() {
        return average;
    }

    public Float getMaximum() {
        return maximum;
    }
}
