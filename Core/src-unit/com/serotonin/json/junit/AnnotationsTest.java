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
import com.serotonin.json.junit.vo.SomeAnnotations;

public class AnnotationsTest extends TestCase {
    public void testSample() throws Exception {
        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(out);
        writer.writeObject(new SomeAnnotations());
        String json = out.toString();
        assertEquals(json, "{\"id\":0}");
    }
}
