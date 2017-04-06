package com.serotonin.json.test;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonReader;
import com.serotonin.json.util.TypeDefinition;

public class ReadTest {
    static JsonContext context = new JsonContext();

    static String[] json = { "{\"id\":12,\"value\":\"my value\"}",
            "{\"myId\":\"Subclass2\",\"sub2Value\":\"sub2\",\"baseValue\":\"base\"}", };

    public static void main(String[] args) throws Exception {
        read("\"k\"", String.class);
        read("{\"bigi\":1234567890123456,\"bigd\":1234567890123456.7890123456789,}", Object.class);
        read("{\"bigi\":1234567890123456,\"bigd\":1234567890123456.7890123456789,}", new TypeDefinition(Map.class,
                String.class, BigDecimal.class));
        read("{\"bigi\":1234567890123456,\"bigd\":1234567890123456.7890123456789,}", new TypeDefinition(HashMap.class,
                String.class, BigDecimal.class));

        read("true", Boolean.TYPE);
        read("true", Boolean.class);

        read("10.1", Float.TYPE);
        read("10.1", Float.class);
        read("10.1", Double.TYPE);
        read("10.1", Double.class);

        read("1234567890123456789012345678901234567890", BigInteger.class);
        read("1234567890123456789012345678901234567890", BigDecimal.class);

        read("1234567890123456789012345678901234567890.1234567890123456789012345678901234567890", BigDecimal.class);

        read("[\"qwer\",\"asdf\",\"zxcv\",]", new TypeDefinition(List.class, String.class));
        read("[\"qwer\",\"asdf\",\"zxcv\",]", new TypeDefinition(ArrayList.class, String.class));

        // read("{\"34\":\"34\",\"list\":[\"qwer\",\"asdf\",\"zxcv\"],\"34\":34}", new TypeDefinition(Map.class, null,
        // String.class));

        read("[\"qwer\",\"asdf\",\"zxcv\"]", String[].class);

        read("\"SECOND\" \"SECOND\"", Enums.class);

        read("{\"bigDecimal\":1.1,\"bigInteger\":2,\"boolean1\":false,\"byte1\":-4,\"double1\":5,\"float1\":6.1,\"int1\":7,\"long1\":-8,\"short1\":9,\"string1\":\"i'm a read string\"}",
                Primitives.class);

        context.addSerializer(new PrimitiveSerializer(), Primitives.class);

        read("{\"bigi\":1234567890123456,\"bigd\":1234567890123456.7890123456789,}", Primitives.class);

        read("{\"myId\":\"Subclass2\",\"sub2Value\":\"sub2\",\"baseValue\":\"base\"}", Subclass2.class);

        read("{\"value\":[\"qwer\",\"asdf\",\"zxcv\"]}", new TypeDefinition(GenObject.class, new TypeDefinition(
                List.class, String.class)));

        read("[\"qwer\",\"asdf\",\"zxcv\"]", new ArrayList<String>(), new TypeDefinition(List.class, String.class));

        read("{\"imSerializable\":{\"id\":12,\"value\":\"my value\"},\"list1\":null,\"list2\":null,\"map\":null,\"primitives\":{\"bigi\":1234567890123456,\"bigd\":1234567890123456.7890123456789},\"uuid\":\"ad9557f1-5e88-4b74-ab51-5b64364e6ccf\"}",
                Compound.class);

        read("[[\"a1\",\"b1\",\"c1\"],[\"a2\",\"b2\",\"c2\"],[\"a3\",\"b3\",\"c3\"]]", new TypeDefinition(List.class,
                String[].class));

        read("{\"rows\":[[\"a1\",\"b1\",\"c1\"],[\"a2\",\"b2\",\"c2\"],[\"a3\",\"b3\",\"c3\"]]}", ListArray.class);

        read("{\"s1\":\"asdf\",\"s2\":\"zxcv\"}", new Mutable(), Mutable.class);
    }

    static void read(String data, Type type) throws Exception {
        JsonReader reader = new JsonReader(context, data);
        while (!reader.isDone()) {
            Object p = reader.read(type);
            System.out.println(p);
        }
    }

    static void read(String data, Object obj, Type type) throws Exception {
        JsonReader reader = new JsonReader(context, data);
        while (!reader.isDone()) {
            reader.readInto(type, obj);
            System.out.println(obj);
        }
    }
}
