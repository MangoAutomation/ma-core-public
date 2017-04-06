package com.serotonin.json.test;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonWriter;

public class WriteTest {
    static JsonContext context = new JsonContext();

    public static void main(String[] args) throws Exception {
        write(true);
        write(10);
        write(10.1);
        write(new BigInteger("1234567890123456789012345678901234567890"));
        write(new BigDecimal("1234567890123456.7890123456789"));
        write(Enums.SECOND);

        List<String> list = new ArrayList<String>();
        list.add("qwer");
        list.add("asdf");
        list.add("zxcv");
        write(list);

        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put(34, "34");
        map.put("34", 34);
        map.put("list", list);
        write(map);

        write(new String[] { "qwer", "asdf", "zxcv" });

        write(new ImSerializable());

        write(new Primitives());
        context.addSerializer(new PrimitiveSerializer(), Primitives.class);
        write(new Primitives());

        Subclass2 sc2 = new Subclass2();
        sc2.setBaseValue("base");
        sc2.setSub2Value("sub2");
        write(sc2);

        write(new Compound());

        write(new SerTest());

        Mutable mutable = new Mutable();
        mutable.setS1("asdf");
        mutable.setS2("zxcv");
        write(mutable);
    }

    static void write(Object o) throws Exception {
        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(context, out);
        writer.writeObject(o);
        System.out.println(out);
    }
}
