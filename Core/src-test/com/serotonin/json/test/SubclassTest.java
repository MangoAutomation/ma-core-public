package com.serotonin.json.test;

import java.util.List;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeDefinition;

public class SubclassTest {
    public static void main(String[] args) throws Exception {
        JsonContext context = new JsonContext();

        context.addResolver(new TypeResolver() {
            @Override
            public Class<?> resolve(JsonValue jsonValue) throws JsonException {
                if (jsonValue.toJsonObject().containsKey("sub1Value"))
                    return Subclass1.class;
                if (jsonValue.toJsonObject().containsKey("sub2Value"))
                    return Subclass2.class;
                throw new JsonException("Unknown BaseClass: " + jsonValue);
            }
        }, BaseClass.class);

        //        context.addFactory(new ObjectFactory() {
        //            @Override
        //            public Object create(JsonValue jsonValue) throws JsonException {
        //                if (jsonValue.toJsonObject().hasProperty("sub1Value"))
        //                    return new Subclass1();
        //                if (jsonValue.toJsonObject().hasProperty("sub2Value"))
        //                    return new Subclass2();
        //                throw new JsonException("Unknown BaseClass: " + jsonValue);
        //            }
        //        }, BaseClass.class);

        //        List<BaseClass> list = new ArrayList<BaseClass>();
        //        list.add(new Subclass1());
        //        list.add(new Subclass2());
        //
        //        String json = JsonWriter.writeToString(context, list);
        //
        //        System.out.println(json);

        String json = "[{\"id\":\"Subclass1\",\"sub1Value\":\"a\",\"baseValue\":\"b\"},{\"myId\":\"Subclass2\",\"sub2Value\":\"c\",\"baseValue\":\"d\"}]";

        JsonReader reader = new JsonReader(context, json);
        TypeDefinition type = new TypeDefinition(List.class, BaseClass.class);
        Object read = reader.read(type);

        System.out.println(read);
    }
}
