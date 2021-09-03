/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.script.ping;

import java.io.IOException;

public interface PingUtility {
    /**
     * Executes ping command in external process.
     *
     * @param hostname host/ip to ping
     * @return round trip time in ms
     * @throws IOException if error occurs pinging host
     * @throws InterruptedException if interrupted while waiting for ping process
     */
    default PingStats ping(String hostname) throws IOException, InterruptedException {
        return ping(hostname, 1);
    }

    /**
     * Executes ping command in external process, gets all stats (avg, min, max, packet loss)
     *
     * @param hostname host/ip to ping
     * @param count number of ICMP echo requests to send
     * @return ping stats
     * @throws IOException if error occurs pinging host
     * @throws InterruptedException if interrupted while waiting for ping process
     */
    default PingStats ping(String hostname, int count) throws IOException, InterruptedException {
        return ping(hostname, count, 5000);
    }

    /**
     * Executes ping command in external process, gets all stats (avg, min, max, packet loss)
     *
     * @param hostname host/ip to ping
     * @param count number of ICMP echo requests to send
     * @param timeout time to wait for each reply in ms
     * @return ping stats
     * @throws IOException if error occurs pinging host
     * @throws InterruptedException if interrupted while waiting for ping process
     */
    PingStats ping(String hostname, int count, int timeout) throws IOException, InterruptedException;

    /**
     * Checks if host is reachable using Java API.
     *
     * @param hostname host/ip to ping
     * @param timeout time to wait for response in ms
     * @return true if host is reachable
     * @throws IOException if error occurs pinging host
     */
    boolean isReachable(String hostname, int timeout) throws IOException;

    /**
     * Checks if host is reachable using Java API and returns time taken.
     * May be affected by JVM pauses such as garbage collection.
     *
     * @param hostname host/ip to ping
     * @param timeout time to wait for response in ms
     * @return average round trip time in ms
     * @throws IOException if error occurs pinging host
     */
    float isReachablePing(String hostname, int timeout) throws IOException;
}
