package com.serotonin.json.test;

import java.io.StringWriter;

import com.serotonin.json.JsonStreamWriter;
import com.serotonin.json.type.JsonTypeReader;

public class StreamTest {
    public static void main(String[] args) throws Exception {
        StringWriter out = new StringWriter();
        JsonStreamWriter w = new JsonStreamWriter(out);

        w.startObject();
        {
            w.writeObjectString("command", "echo");
            w.startObjectArray("parameters");
            {
                w.writeArrayNull();
                w.writeArrayNumber(123);
                w.writeArrayString("str");
                w.writeArrayBoolean(true);
                w.startArrayObject();
                {
                    w.writeObjectString("nullStr", null);
                    w.writeObjectString("quote", "\"'\t");
                }
                w.endObject();
            }
            w.endArray();
        }
        w.endObject();

        System.out.println(out);

        String s = "{\"id\":\"1234\",\"result\":{\"node\":{\"path\":\"/DGBox/Data Sources/Anders\",\"name\":\"Anders\",\"hasChildren\":true,\"icon\":\"../../../modules/modbus/web/modbusIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/bnip\",\"name\":\"bnip\",\"hasChildren\":true,\"icon\":\"../../../modules/BACnet/web/bacnetIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/bnmstp\",\"name\":\"bnmstp\",\"hasChildren\":false,\"icon\":\"../../../modules/BACnet/web/bacnetIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/Calculations\",\"name\":\"Calculations\",\"hasChildren\":true,\"icon\":\"../../../images/icon_ds.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/Cylon test\",\"name\":\"Cylon test\",\"hasChildren\":true,\"icon\":\"../../../modules/BACnet/web/bacnetIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/Discover test\",\"name\":\"Discover test\",\"hasChildren\":true,\"icon\":\"../../../modules/modbus/web/modbusIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/EnOcean 315\",\"name\":\"EnOcean 315\",\"hasChildren\":true,\"icon\":\"../../../modules/enocean/web/enoceanIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/EnOcean 902\",\"name\":\"EnOcean 902\",\"hasChildren\":true,\"icon\":\"../../../modules/enocean/web/enoceanIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/House lights\",\"name\":\"House lights\",\"hasChildren\":true,\"icon\":\"../../../images/icon_ds.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/http\",\"name\":\"http\",\"hasChildren\":true,\"icon\":\"../../../modules/http/web/httpIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/Insteon\",\"name\":\"Insteon\",\"hasChildren\":true,\"icon\":\"../../../modules/insteon/web/insteon_5.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/Internal\",\"name\":\"Internal\",\"hasChildren\":true,\"icon\":\"../../../images/icon_ds.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/KNX\",\"name\":\"KNX\",\"hasChildren\":true,\"icon\":\"../../../modules/knx/web/knx.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/MatthewWS\",\"name\":\"MatthewWS\",\"hasChildren\":true,\"icon\":\"../../../images/icon_ds.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/Maverick\",\"name\":\"Maverick\",\"hasChildren\":true,\"icon\":\"../../../modules/mamac/web/mamacIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/mbus test\",\"name\":\"mbus test\",\"hasChildren\":true,\"icon\":\"../../../modules/mbus/web/mbusIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/Modbus Serial\",\"name\":\"Modbus Serial\",\"hasChildren\":true,\"icon\":\"../../../modules/modbus/web/modbusIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/OPC\",\"name\":\"OPC\",\"hasChildren\":true,\"icon\":\"../../../modules/opcda/web/opcIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/Set test\",\"name\":\"Set test\",\"hasChildren\":true,\"icon\":\"../../../modules/BACnet/web/bacnetIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/SNMP\",\"name\":\"SNMP\",\"hasChildren\":true,\"icon\":\"../../../modules/snmp/web/snmpIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/SQL\",\"name\":\"SQL\",\"hasChildren\":true,\"icon\":\"../../../modules/sqlds/web/sqlIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/test\",\"name\":\"test\",\"hasChildren\":true,\"icon\":\"../../../modules/mamac/web/mamacIcon.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/virt test\",\"name\":\"virt test\",\"hasChildren\":true,\"icon\":\"../../../images/icon_ds.png\"},\"node\":{\"path\":\"/DGBox/Data Sources/Virtual\",\"name\":\"Virtual\",\"hasChildren\":true,\"icon\":\"../../../images/icon_ds.png\"}}}";
        System.out.println(new JsonTypeReader(s).read());
    }
}
