package com.serotonin.m2m2.util;

import java.io.IOException;
import java.util.List;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonString;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.DataPointVO;

public class VarNames {
    public static boolean validateVarName(String varName) {
        char ch = varName.charAt(0);
        if (!Character.isLetter(ch) && ch != '_')
            return false;
        for (int i = 1; i < varName.length(); i++) {
            ch = varName.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch != '_')
                return false;
        }
        return true;
    }

    public static String contextToString(List<IntStringPair> context) {
        DataPointDao dataPointDao = DataPointDao.getInstance();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (IntStringPair ivp : context) {
            DataPointVO dp = dataPointDao.getDataPoint(ivp.getKey(), false);
            if (first)
                first = false;
            else
                sb.append(", ");

            if (dp == null)
                sb.append("?=");
            else
                sb.append(dp.getExtendedName()).append("=");
            sb.append(ivp.getValue());
        }
        return sb.toString();
    }

    public static void jsonWriteVarContext(ObjectWriter writer, List<IntStringPair> context) throws IOException,
            JsonException {
        DataPointDao dataPointDao = DataPointDao.getInstance();
        JsonArray pointList = new JsonArray();
        for (IntStringPair p : context) {
            JsonObject point = new JsonObject();
            pointList.add(point);
            point.put("varName", new JsonString(p.getValue()));
            point.put("dataPointXid", new JsonString(dataPointDao.getXidById(p.getKey())));
        }
        writer.writeEntry("context", pointList);
    }

    public static void jsonReadVarContext(JsonObject json, List<IntStringPair> context) throws JsonException {
        JsonArray jsonContext = json.getJsonArray("context");
        if (jsonContext != null) {
            context.clear();
            DataPointDao dataPointDao = DataPointDao.getInstance();

            for (JsonValue jv : jsonContext) {
                JsonObject jo = jv.toJsonObject();
                String xid = jo.getString("dataPointXid");
                if (xid == null)
                    throw new TranslatableJsonException("emport.error.meta.missing", "dataPointXid");

                Integer dpid = dataPointDao.getIdByXid(xid);
                if (dpid == null)
                    throw new TranslatableJsonException("emport.error.missingPoint", xid);

                String var = jo.getString("varName");
                if (var == null)
                    throw new TranslatableJsonException("emport.error.meta.missing", "varName");

                context.add(new IntStringPair(dpid, var));
            }
        }
    }
}
