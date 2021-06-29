/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.usage;

import java.util.List;

/**
 * Container to use until we break apart publisher points and publishers
 * @author Terry Packer
 *
 */
public class AggregatePublisherUsageStatistics {

    List<PublisherUsageStatistics> publisherUsageStatistics;
    List<PublisherPointsUsageStatistics> publisherPointsUsageStatistics;
    /**
     * @return the publisherUsageStatistics
     */
    public List<PublisherUsageStatistics> getPublisherUsageStatistics() {
        return publisherUsageStatistics;
    }
    /**
     * @param publisherUsageStatistics the publisherUsageStatistics to set
     */
    public void setPublisherUsageStatistics(List<PublisherUsageStatistics> publisherUsageStatistics) {
        this.publisherUsageStatistics = publisherUsageStatistics;
    }
    /**
     * @return the publisherPointsUsageStatistics
     */
    public List<PublisherPointsUsageStatistics> getPublisherPointsUsageStatistics() {
        return publisherPointsUsageStatistics;
    }
    /**
     * @param publisherPointsUsageStatistics the publisherPointsUsageStatistics to set
     */
    public void setPublisherPointsUsageStatistics(
            List<PublisherPointsUsageStatistics> publisherPointsUsageStatistics) {
        this.publisherPointsUsageStatistics = publisherPointsUsageStatistics;
    }
}
