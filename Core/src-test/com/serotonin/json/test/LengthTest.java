package com.serotonin.json.test;

import java.io.StringReader;

import com.serotonin.json.type.JsonTypeReader;

public class LengthTest {
    public static void main(String[] args) throws Exception {
        String data = "{\"bigDecimal\":1.1,\"bigInteger\":2,\"boolean1\":false,\"byte1\":-4,\"double1\":5,\"float1\":6.1,\"int1\":7,\"long1\":-8,\"short1\":9,\"string1\":\"i'm a read string\"}";
        System.out.println(data.length());

        new JsonTypeReader(new StringReader(data), -1).read();

        new JsonTypeReader(new StringReader(data), 150).read();
        new JsonTypeReader(new StringReader(data), 149).read();
        new JsonTypeReader(new StringReader(data), 148).read();
        new JsonTypeReader(new StringReader(data), 147).read();
        new JsonTypeReader(new StringReader(data), 146).read();
        new JsonTypeReader(new StringReader(data), 100).read();
    }
}
