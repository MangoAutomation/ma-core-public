/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import org.springframework.core.Ordered;

/**
 * Define filter orders in one place
 *
 * @author Jared Wiltshire
 */
public interface FilterOrder {
    public static final int SPRING_SECURITY = Ordered.HIGHEST_PRECEDENCE + 1000;
    public static final int FORWARDED_HEADER = SPRING_SECURITY - 100;
    public static final int QOS = SPRING_SECURITY + 100;
    public static final int DOS = SPRING_SECURITY + 200;
    public static final int URL_SECURITY = SPRING_SECURITY + 300;

}
