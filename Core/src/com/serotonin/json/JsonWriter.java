package com.serotonin.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes given data as JSON to a stream. Instances should be created, used, and discarded. Reuse is generally unwise.
 * 
 * @author Matthew Lohbihler
 */
public class JsonWriter {
    /**
     * A convenience method for converting an object to JSON. By default this method will write non-optimized,
     * human-readable JSON, with line breaks and an indent of 2 spaces. This method should not be used in production
     * code (where human-readability is not required).
     * 
     * @param context
     *            the JSON context to use. If null, a new context will be created.
     * @param value
     *            the object to serialize
     * @return the resulting JSON string
     */
    public static String writeToString(JsonContext context, Object value) throws JsonException {
        if (context == null)
            context = new JsonContext();

        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(context, out);
        writer.setPrettyOutput(true);
        writer.setPrettyIndent(2);
        try {
            writer.writeObject(value);
        }
        catch (IOException e) {
            // This should never happen because we are writing to a StringWriter
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    /**
     * The writer's context.
     */
    private final JsonContext context;

    /**
     * The I/O writer to which the JSON content is written.
     */
    private final Writer writer;

    /**
     * Useful for preventing infinite loops in objects where there are cyclical relationships.
     */
    private boolean trackAlreadySerialized = false;

    /**
     * Whether to insert line breaks in the JSON output
     */
    private boolean prettyOutput = false;

    /**
     * Determines whether forward slashes ('/') in strings should be escaped (true) or not (false).
     */
    private boolean escapeForwardSlash = true;

    /**
     * The amount to indent pretty output. Has no effect if prettyOutput is false. Defaults to two spaces.
     */
    private String prettyIndent = "  ";

    private final List<Object> alreadySerialized = new ArrayList<>();
    private String currentIndent = "";

    /**
     * The hint to use.
     */
    private String includeHint;

    /**
     * Creates a JSON writer around the given I/O writer. A new JSON context is created.
     *
     */
    public JsonWriter(Writer writer) {
        this.context = new JsonContext();
        this.writer = writer;
        includeHint = null;
        escapeForwardSlash = false;
    }

    /**
     * Creates a JSON writer with the given context around the given I/O writer.
     * 
     * @param context
     *            the context to use.
     * @param writer
     *            the I/O writer
     */
    public JsonWriter(JsonContext context, Writer writer) {
        this.context = context;
        this.writer = writer;
        includeHint = context.getDefaultIncludeHint();
        escapeForwardSlash = context.isEscapeForwardSlash();
    }

    public boolean isTrackAlreadySerialized() {
        return trackAlreadySerialized;
    }

    public void setTrackAlreadySerialized(boolean trackAlreadySerialized) {
        this.trackAlreadySerialized = trackAlreadySerialized;
    }

    public boolean isPrettyOutput() {
        return prettyOutput;
    }

    public void setPrettyOutput(boolean prettyOutput) {
        this.prettyOutput = prettyOutput;
    }

    public int getPrettyIndent() {
        return prettyIndent.length();
    }

    public boolean isEscapeForwardSlash() {
        return escapeForwardSlash;
    }

    public void setEscapeForwardSlash(boolean escapeForwardSlash) {
        this.escapeForwardSlash = escapeForwardSlash;
    }

    public void setPrettyIndent(int prettyIndent) {
        if (prettyIndent <= 0)
            this.prettyIndent = "";
        else {
            this.prettyIndent = " ";
            while (this.prettyIndent.length() < prettyIndent)
                this.prettyIndent += this.prettyIndent;
            this.prettyIndent = this.prettyIndent.substring(0, prettyIndent);
        }
    }

    public String getIncludeHint() {
        return includeHint;
    }

    public void setIncludeHint(String includeHint) {
        this.includeHint = includeHint;
    }

    /**
     * Writes the given object as JSON to the I/O writer.
     * 
     * @param value
     *            the object to write. May be null.
     */
    public void writeObject(Object value) throws JsonException, IOException {
        if (value == null) {
            writer.append("null");
            return;
        }

        // Do not serialize the same object instance twice.
        if (trackAlreadySerialized) {
            for (Object obj : alreadySerialized) {
                if (obj == value) {
                    writer.append("null");
                    return;
                }
            }
            alreadySerialized.add(value);
        }

        try {
            context.getConverter(value.getClass()).jsonWrite(this, value);
        }
        catch (IOException e) {
            // Let io exceptions through
            throw e;
        }
        catch (RuntimeException e) {
            // Let runtime exceptions through
            throw e;
        }
        catch (Exception e) {
            throw new JsonException("Could not write object " + value + " of class " + value.getClass(), e);
        }
    }

    /**
     * Flush the underlying I/O writer.
     *
     */
    public void flush() throws IOException {
        writer.flush();
    }

    /**
     * Append the given character to the I/O writer. This method should not normally be used by client code.
     *
     */
    public void append(char c) throws IOException {
        writer.append(c);
    }

    /**
     * Append the given string literal to the I/O writer. This method should not normally be used by client code.
     *
     */
    public void append(String s) throws IOException {
        writer.append(s);
    }

    /**
     * Quote the given string literal and append the result to the I/O writer. This method should not normally be used
     * by client code.
     *
     */
    public void quote(String s) throws IOException {
        if (s == null) {
            writer.append("null");
            return;
        }

        int len = s.length();
        if (len == 0) {
            writer.append("\"\"");
            return;
        }

        writer.append('"');
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                writer.append('\\');
                writer.append(c);
                break;
            case '/':
                if (escapeForwardSlash)
                    writer.append('\\');
                writer.append(c);
                break;
            case '\b':
                writer.append("\\b");
                break;
            case '\t':
                writer.append("\\t");
                break;
            case '\n':
                writer.append("\\n");
                break;
            case '\f':
                writer.append("\\f");
                break;
            case '\r':
                writer.append("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                    String t = "000" + Integer.toHexString(c);
                    writer.append("\\u" + t.substring(t.length() - 4));
                }
                else {
                    writer.append(c);
                }
            }
        }
        writer.append('"');
    }

    /**
     * Increase the current indenting amount. This method should not normally be used by client code.
     */
    public void increaseIndent() {
        if (prettyOutput)
            currentIndent += prettyIndent;
    }

    /**
     * Decrease the current indenting amount. This method should not normally be used by client code.
     */
    public void decreaseIndent() {
        if (prettyOutput)
            currentIndent = currentIndent.substring(0, currentIndent.length() - prettyIndent.length());
    }

    /**
     * Add the current indenting amount to the I/O writer. This method should not normally be used by client code.
     */
    public void indent() throws IOException {
        if (prettyOutput)
            writer.append("\r\n").append(currentIndent);
    }
}
