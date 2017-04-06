package com.serotonin.json.test;

import com.serotonin.json.type.JsonTypeReader;

public class ReadTypeTest {
    static String[] json = {
            "true",
            "10.1",
            "1234567890123456789012345678901234567890",
            "1234567890123456789012345678901234567890.1234567890123456789012345678901234567890",
            "1234567890123456.7890123456789",
            "[\"qwer\",\"asdf\",\"zxcv\"]",
            "{\"34\":\"34\",\"list\":[\"qwer\",\"asdf\",\"zxcv\"],\"34\":34}",
            "[\"qwer\",\"asdf\",\"zxcv\"]",
            "{\"id\":12,\"value\":\"my value\"}",
            "{\"bigDecimal\":1234567890123456.7890123456789,\"bigInteger\":1234567890123456,\"boolean1\":true,\"byte1\":-3,\"double1\":123.456,\"float1\":0.1,\"int1\":2147483647,\"long1\":-9223372036854775808,\"short1\":15,\"string1\":\"i'm a string\"}",
            "{\"myId\":\"Subclass2\",\"sub2Value\":\"sub2\",\"baseValue\":\"base\"}", };

    public static void main(String[] args) throws Exception {
        for (String data : json) {
            try {
                read(data);
            }
            catch (Exception e) {
                throw new Exception("Error reading '" + data + "'", e);
            }
        }
    }

    static void read(String data) throws Exception {
        JsonTypeReader reader = new JsonTypeReader(data);
        while (!reader.isEos())
            System.out.println(reader.read());
    }
}
