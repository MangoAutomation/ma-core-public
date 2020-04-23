/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public EvalContext(Map<String, Object> bindings) {
        this.bindings = bindings;
        this.writer = CharStreams.nullWriter();
        this.errorWriter = CharStreams.nullWriter();
        this.reader = new StringReader("");
    }

    public Writer getWriter() {
        return writer;
    }
    public void setWriter(Writer writer) {
        this.writer = Objects.requireNonNull(writer);
    }
    public Writer getErrorWriter() {
        return errorWriter;
    }
    public void setErrorWriter(Writer errorWriter) {
        this.errorWriter = Objects.requireNonNull(errorWriter);
    }
    public Reader getReader() {
        return reader;
    }
    public void setReader(Reader reader) {
        this.reader = Objects.requireNonNull(reader);
    }
    public Map<String, Object> getBindings() {
        return bindings;
    }
    public void setBindings(Map<String, Object> bindings) {
        this.bindings = Objects.requireNonNull(bindings);
    }
    public void addBinding(String key, Object value) {
        this.bindings.put(key, value);
    }
}
