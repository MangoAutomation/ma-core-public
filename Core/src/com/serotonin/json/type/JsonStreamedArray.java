/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.json.type;

import java.io.IOException;

import com.serotonin.json.JsonWriter;

/**
 * Marker for writing a stream of JSON
 * @author Terry Packer
 */
public interface JsonStreamedArray {

    void writeArrayValues(JsonWriter writer) throws IOException;

}
