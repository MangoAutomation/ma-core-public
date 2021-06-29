/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.concurrent.TimeUnit;

import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

/**
 * @author Jared Wiltshire
 */
public class RateLimiter<T> {

    private final LoadingCache<T, TokenBucket> tokenBucketCache;

    private final long refillPeriod;
    private final TimeUnit refillPeriodUnit;
    private final long burstCapacity;
    private final long refillQuanitity;

    public RateLimiter(long burstCapacity, long refillQuanitity, long refillPeriod, TimeUnit refillPeriodUnit) {

        this.refillPeriod = refillPeriod;
        this.refillPeriodUnit = refillPeriodUnit;
        this.burstCapacity = burstCapacity;
        this.refillQuanitity = refillQuanitity;

        this.tokenBucketCache = Caffeine.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(this::loadRateLimiter);
    }

    public boolean checkRateExceeded(T id) {
        return this.checkRateExceeded(id, 1);
    }

    public boolean checkRateExceeded(T id, int numTokens) {
        return this.tokenBucketCache.get(id).getNumTokens() < numTokens;
    }

    public boolean hit(T id) {
        return this.hit(id, 1);
    }

    public boolean hit(T id, int numTokens) {
        return !this.tokenBucketCache.get(id).tryConsume(numTokens);
    }

    private TokenBucket loadRateLimiter(T identifier) {
        return TokenBuckets.builder()
                .withCapacity(burstCapacity)
                .withFixedIntervalRefillStrategy(refillQuanitity, refillPeriod, refillPeriodUnit)
                .build();
    }

    public long secondsTillRetry(T id) {
        return this.tokenBucketCache.get(id).getDurationUntilNextRefill(TimeUnit.SECONDS);
    }

    public long getRefillPeriod() {
        return refillPeriod;
    }

    public TimeUnit getRefillPeriodUnit() {
        return refillPeriodUnit;
    }

    public long getBurstCapacity() {
        return burstCapacity;
    }

    public long getRefillQuanitity() {
        return refillQuanitity;
    }

}
