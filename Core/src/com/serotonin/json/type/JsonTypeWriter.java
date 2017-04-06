package com.serotonin.json.type;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;

public class JsonTypeWriter {
    /**
     * The writer's context.
     */
    private final JsonContext context;

    /**
     * The hint to use.
     */
    private String includeHint;

    public JsonTypeWriter() {
        this(new JsonContext());
    }

    public JsonTypeWriter(JsonContext context) {
        this.context = context;
        includeHint = context.getDefaultIncludeHint();
    }

    public String getIncludeHint() {
        return includeHint;
    }

    public void setIncludeHint(String includeHint) {
        this.includeHint = includeHint;
    }

    public JsonValue writeObject(Object value) throws JsonException {
        if (value == null)
            return null;

        try {
            return context.getConverter(value.getClass()).jsonWrite(this, value);
        }
        catch (RuntimeException e) {
            // Let runtime exceptions through
            throw e;
        }
        catch (Exception e) {
            throw new JsonException("Could not write object " + value + " of class " + value.getClass(), e);
        }
    }
}
