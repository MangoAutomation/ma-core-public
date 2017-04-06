package com.serotonin.json.test;

import java.io.StringWriter;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;

public class EscapeTest {
    public static void main(String[] args) throws Exception {
        JsonContext context = new JsonContext();
        context.setEscapeForwardSlash(true);

        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(context, out);
        writer.writeObject("stream://asdf");
        String json = out.toString();

        System.out.println(json);

        JsonReader reader = new JsonReader(context, json);
        String s = reader.read(String.class);
        System.out.println(s);
    }
}
