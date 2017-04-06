package com.serotonin.json;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

public class JsonStreamWriter {
    private final Writer out;
    private final JsonWriter writer;
    private final Deque<Boolean> firstElementStack = new ArrayDeque<Boolean>();

    public JsonStreamWriter(Writer out) {
        this.out = out;
        this.writer = new JsonWriter(out);
    }

    public Writer getOut() {
        return out;
    }

    public void startObject() throws IOException {
        writer.append('{');
        firstElementStack.push(true);
    }

    public void startObjectElement(String name) throws IOException {
        startElement();
        writer.quote(name);
        writer.append(':');
    }

    public void writeObjectNull(String name) throws IOException {
        startObjectElement(name);
        writeNull();
    }

    public void writeObjectBoolean(String name, boolean b) throws IOException {
        startObjectElement(name);
        writeBoolean(b);
    }

    public void writeObjectNumber(String name, Number n) throws IOException {
        startObjectElement(name);
        writeNumber(n);
    }

    public void writeObjectString(String name, String s) throws IOException {
        startObjectElement(name);
        writeString(s);
    }

    public void startObjectArray(String name) throws IOException {
        startObjectElement(name);
        startArray();
    }

    public void startObjectObject(String name) throws IOException {
        startObjectElement(name);
        startObject();
    }

    public void writeObjectObject(String name, Object o) throws IOException, JsonException {
        startObjectElement(name);
        writeObject(o);
    }

    public void endObject() throws IOException {
        writer.append('}');
        firstElementStack.pop();
    }

    public void startArray() throws IOException {
        writer.append('[');
        firstElementStack.push(true);
    }

    public void writeArrayNull() throws IOException {
        startElement();
        writeNull();
    }

    public void writeArrayBoolean(boolean b) throws IOException {
        startElement();
        writeBoolean(b);
    }

    public void writeArrayNumber(Number n) throws IOException {
        startElement();
        writeNumber(n);
    }

    public void writeArrayString(String s) throws IOException {
        startElement();
        writeString(s);
    }

    public void writeArrayObject(Object o) throws IOException, JsonException {
        startElement();
        writeObject(o);
    }

    public void startArrayArray() throws IOException {
        startElement();
        startArray();
    }

    public void startArrayObject() throws IOException {
        startElement();
        startObject();
    }

    public void endArray() throws IOException {
        writer.append(']');
        firstElementStack.pop();
    }

    public void startElement() throws IOException {
        if (firstElementStack.peek()) {
            firstElementStack.pop();
            firstElementStack.push(false);
        }
        else
            writer.append(',');
    }

    public void writeNull() throws IOException {
        writer.append("null");
    }

    public void writeBoolean(boolean b) throws IOException {
        writer.append(b ? "true" : "false");
    }

    public void writeNumber(Number n) throws IOException {
        if (n == null)
            writeNull();
        else
            writer.append(n.toString());
    }

    public void writeString(String s) throws IOException {
        if (s == null)
            writeNull();
        else
            writer.quote(s);
    }

    public void writeObject(Object o) throws IOException, JsonException {
        writer.writeObject(o);
    }
}
