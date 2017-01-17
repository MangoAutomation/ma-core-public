package com.serotonin.m2m2;

public interface UpgradeVersionState {
    int DEVELOPMENT = 0;
    int ALPHA = 1;
    int BETA = 2;
    int RELEASE_CANDIDATE = 3;
    int PRODUCTION = 4;
}