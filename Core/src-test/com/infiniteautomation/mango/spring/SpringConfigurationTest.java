/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;

/**
 * @author Terry Packer
 *
 */
public class SpringConfigurationTest extends MangoTestBase {

    @Test
    public void testInjectedPropertyTypes() {
        
        SimplePropertyInjectionContainer container = Common.getBean(SimplePropertyInjectionContainer.class);
        
        assertNotNull(container);
        
        assertEquals(3, container.getInjectedStringArray().size());
        assertEquals("ONE", container.getInjectedStringArray().get(0));
        assertEquals("TWO", container.getInjectedStringArray().get(1));
        assertEquals("THREE", container.getInjectedStringArray().get(2));
        
        assertEquals(0, container.getInjectedEmptyStringArray().size());
        
        assertEquals(3, container.getInjectedIntegerArray().size());
        assertEquals(1, (int)container.getInjectedIntegerArray().get(0));
        assertEquals(2, (int)container.getInjectedIntegerArray().get(1));
        assertEquals(3, (int)container.getInjectedIntegerArray().get(2));
       
        assertEquals(true, container.isInjectedBoolean());
        
        assertEquals("Testing String", container.getInjectedString());
        
        assertEquals(1, (int)container.getInjectedInteger());
    }
    
}
