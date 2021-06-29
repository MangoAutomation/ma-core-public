/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Jared Wiltshire
 */
public class NullOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
    }

}
