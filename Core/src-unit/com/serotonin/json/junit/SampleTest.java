/*
 * ----------------------------------------------------------------------
 * Copyright (C) 2011, Numenta Inc. All rights reserved.
 *
 * The information and source code contained herein is the
 * exclusive property of Numenta Inc. No part of this software
 * may be used, reproduced, stored or distributed in any form,
 * without explicit written authorization from Numenta Inc.
 * ----------------------------------------------------------------------
 */

package com.serotonin.json.junit;

import java.io.StringWriter;

import junit.framework.TestCase;

import com.serotonin.json.JsonWriter;

public class SampleTest extends TestCase {
    public void testSample() throws Exception {
        assertEquals(true, true);
        new JsonWriter(new StringWriter()).writeObject(12);
    }
}
