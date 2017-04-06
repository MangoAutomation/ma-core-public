package com.serotonin.json.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.spi.ObjectFactory;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeDefinition;

public class PopulateTest {
    static JsonContext context;

    public static void main(String[] args) throws Exception {
        context = new JsonContext();
        context.addFactory(new ObjectFactory() {
            @Override
            public Object create(JsonValue jsonValue) throws JsonException {
                if (jsonValue.toJsonObject().containsKey("sub1Value"))
                    return new Subclass1();
                if (jsonValue.toJsonObject().containsKey("sub2Value"))
                    return new Subclass2();
                throw new JsonException("Unknown BaseClass: " + jsonValue);
            }
        }, BaseClass.class);

        test1();
        test2();

    }

    static void test1() throws JsonException, IOException {
        List<BaseClass> list = new ArrayList<BaseClass>();
        list.add(new Subclass1());
        list.add(new Subclass2());

        String json = JsonWriter.writeToString(context, list);
        System.out.println(json);

        JsonReader reader = new JsonReader(context, json);

        TypeDefinition type = new TypeDefinition(List.class, BaseClass.class);
        list.clear();
        reader.readInto(type, list);

        System.out.println(list);
    }

    static void test2() throws JsonException, IOException {
        Subclass2 subclass2 = new Subclass2();
        String json = JsonWriter.writeToString(context, subclass2);
        System.out.println(json);

        JsonReader reader = new JsonReader(context, json);
        reader.readInto(subclass2);

        System.out.println(subclass2);
    }
}
