/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.web.dwr.longPoll.LongPollHandler;

/**
 * m2m2 uses AJAX extensively to implement both an effective and simple user interface, and rapid and automatic page
 * updating for system monitoring. The long poll mechanism allows services in a page to request to be included in a
 * single long poll request, such that updates directed to any of the requested services will be returned. By coalescing
 * all services into a single request, m2m2 prevents conditions where an arbitrary number of AJAX requests are being
 * made, potentially exceeding the maximum number of requests allowed by a single page. Long poll requests also utilize
 * resources better than polling, and also result in better response times.
 * 
 * @author Matthew Lohbihler
 */
abstract public class LongPollDefinition extends ModuleElementDefinition {
    /**
     * @return an instance of the long poll handler. Instances may be cached and reused.
     */
    abstract public LongPollHandler getHandler();
}
