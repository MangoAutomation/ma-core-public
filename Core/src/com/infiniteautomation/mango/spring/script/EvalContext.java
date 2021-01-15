/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.google.common.io.CharStreams;

/**
 * @author Jared Wiltshire
 */
public class EvalContext {
    Writer writer;
    Writer errorWriter;
    Reader reader;
    Map<String, Object> bindings;

    public EvalContext() {
        this(new HashMap<>());
    }

    public EvalContext(@NonNull Map<String, Object> bindings) {
        this.bindings = bindings;
        this.writer = CharStreams.nullWriter();
        this.errorWriter = CharStreams.nullWriter();
        this.reader = new StringReader("");
    }

    public Writer getWriter() {
        return writer;
    }
    public void setWriter(@NonNull Writer writer) {
        this.writer = writer;
    }
    public Writer getErrorWriter() {
        return errorWriter;
    }
    public void setErrorWriter(@NonNull Writer errorWriter) {
        this.errorWriter = errorWriter;
    }
    public Reader getReader() {
        return reader;
    }
    public void setReader(@NonNull Reader reader) {
        this.reader = reader;
    }
    public Map<String, Object> getBindings() {
        return bindings;
    }
    public void setBindings(@NonNull Map<String, Object> bindings) {
        this.bindings = bindings;
    }
    public void addBinding(String key, Object value) {
        this.bindings.put(key, value);
    }
}
