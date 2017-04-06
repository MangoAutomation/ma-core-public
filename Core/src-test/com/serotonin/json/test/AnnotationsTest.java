package com.serotonin.json.test;

import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;

public class AnnotationsTest {
    public static void main(String[] args) throws Exception {
        AnnotatedClass ac = new AnnotatedClass();
        ac.setId("qwer");
        ac.setName("asdf");

        System.out.println(JsonWriter.writeToString(null, ac));

        String s = "{\"id\":\"qwer\", \"name\":\"asdf\"}";
        AnnotatedClass ac2 = new JsonReader(s).read(AnnotatedClass.class);
        System.out.println(ac2.getId());
        System.out.println(ac2.getName());
    }
}
