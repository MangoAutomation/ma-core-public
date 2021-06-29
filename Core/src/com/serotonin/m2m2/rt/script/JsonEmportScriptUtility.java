/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.emport.ImportTask;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.EmportService;
import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.infiniteautomation.mango.spring.service.EventHandlerService;
import com.infiniteautomation.mango.spring.service.JsonDataService;
import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.PublisherService;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.spring.service.SystemPermissionService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;

public class JsonEmportScriptUtility extends ScriptUtility {
    public static String CONTEXT_KEY = "JsonEmport";

    private final RQLParser parser = new RQLParser();;
    private List<JsonImportExclusion> importExclusions;
    protected boolean importDuringValidation = false;
    private final DataSourceService dataSourceService;
    private final DataPointDao dataPointDao;
    private final RunAs runAs;

    @Autowired
    public JsonEmportScriptUtility(MangoJavaScriptService service, PermissionService permissionService,
                                   DataSourceService dataSourceService, DataPointDao dataPointDao, RunAs runAs) {
        super(service, permissionService);
        this.dataSourceService = dataSourceService;
        this.dataPointDao = dataPointDao;
        this.runAs = runAs;
    }

    @Override
    public String getContextKey() {
        return CONTEXT_KEY;
    }

    @Override
    public void takeContext(ScriptEngine engine, Bindings engineScope,
            ScriptPointValueSetter setter, List<JsonImportExclusion> importExclusions, boolean testRun) {
        this.importExclusions = importExclusions;
    }

    public String getFullConfiguration() {
        return getFullConfiguration(0);
    }

    public String getFullConfiguration(int prettyIndent) {
        EmportService service = Common.getBean(EmportService.class);
        return runAs.runAs(permissions, () -> {
            try{
                StringWriter stringWriter = new StringWriter();
                service.export(ConfigurationExportData.createExportDataMap(null), stringWriter, prettyIndent);
                return stringWriter.toString();
            }catch(PermissionException e) {
                return "{}";
            }
        });
    }

    public String getConfiguration(String type) {
        return getConfiguration(type, 0);
    }

    public String getConfiguration(String type, int prettyIndent) {
        return runAs.runAs(permissions, () -> {
            try {
                Map<String, Object> data = ConfigurationExportData.createExportDataMap(new String[] {type});
                StringWriter stringWriter = new StringWriter();
                EmportService service = Common.getBean(EmportService.class);
                service.export(data, stringWriter, prettyIndent);
                return stringWriter.toString();
            }catch(PermissionException e) {
                return "{}";
            }
        });
    }

    public String dataPointQuery(String query) {
        return dataPointQuery(query, 0);
    }

    public String dataPointQuery(String query, int prettyIndent) {
        Map<String, Object> data = new LinkedHashMap<>();
        if(permissionService.hasAdminRole(permissions)) {
            ASTNode root = parser.parse(query);
            List<DataPointVO> dataPoints = new ArrayList<>();
            ConditionSortLimitWithTagKeys conditions = (ConditionSortLimitWithTagKeys) dataPointDao.rqlToCondition(root, null, null, null);
            DataPointDao.getInstance().customizedQuery(conditions, permissions, dataPoints::add);

            data.put(ConfigurationExportData.DATA_POINTS, dataPoints);
        }
        return runAs.runAs(permissions, () -> {
            try{
                StringWriter stringWriter = new StringWriter();
                EmportService service = Common.getBean(EmportService.class);
                service.export(data, stringWriter, prettyIndent);
                return stringWriter.toString();
            }catch(PermissionException e) {
                return "{}";
            }
        });
    }

    public String dataSourceQuery(String query) {
        return dataSourceQuery(query, 0);
    }

    public String dataSourceQuery(String query, int prettyIndent) {
        Map<String, Object> data = new LinkedHashMap<>();
        if(permissionService.hasAdminRole(permissions)) {
            List<DataSourceVO> dataSources = new ArrayList<>();
            ASTNode root = parser.parse(query);
            runAs.runAs(permissions, () -> {
                dataSourceService.customizedQuery(root, (ds) -> {
                    dataSources.add(ds);
                });
            });

            data.put(ConfigurationExportData.DATA_SOURCES, dataSources);
        }
        return runAs.runAs(permissions, () -> {
            try{
                StringWriter stringWriter = new StringWriter();
                EmportService service = Common.getBean(EmportService.class);
                service.export(data, stringWriter, prettyIndent);
                return stringWriter.toString();
            }catch(PermissionException e) {
                return "{}";
            }
        });
    }

    public void doImport(String json) throws Exception {
        if(permissionService.hasAdminRole(permissions)) {
            JsonTypeReader reader = new JsonTypeReader(json);
            JsonValue value = reader.read();
            JsonObject jo = value.toJsonObject();
            if(importExclusions != null)
                doExclusions(jo);
            ScriptImportTask sit = new ScriptImportTask(jo);
            runAs.runAs(permissions, () -> {
                sit.run(Common.timer.currentTimeMillis());
            });
        }
    }

    public List<ProcessMessage> doImportGetStatus(String json) throws Exception {
        if(permissionService.hasAdminRole(permissions)) {
            JsonTypeReader reader = new JsonTypeReader(json);
            JsonValue value = reader.read();
            JsonObject jo = value.toJsonObject();
            if(importExclusions != null)
                doExclusions(jo);
            ScriptImportTask sit = new ScriptImportTask(jo);
            runAs.runAs(permissions, () -> {
                sit.run(Common.timer.currentTimeMillis());
            });
            return sit.getMessages();
        }
        return null;
    }

    private void doExclusions(JsonObject jo) {
        for(JsonImportExclusion exclusion : importExclusions) {
            if(jo.containsKey(exclusion.getImporterType())) {
                JsonArray ja = jo.getJsonArray(exclusion.getImporterType());
                int size = ja.size();
                for(int k = 0; k < size; k+=1) {
                    JsonObject obj = ja.getJsonObject(k);
                    if(obj.containsKey(exclusion.getKey()) && obj.getString(exclusion.getKey()).equals(exclusion.getValue())) {
                        ja.remove(k);
                        k -= 1;
                        size -= 1;
                    }
                }
            }
        }
    }

    public void setImportDuringValidation(boolean importDuringValidation) {
        this.importDuringValidation = importDuringValidation;
    }

    class ScriptImportTask extends ImportTask {

        public ScriptImportTask(JsonObject jo) {
            super(jo, Common.getTranslations(),
                    Common.getBean(RoleService.class),
                    Common.getBean(UsersService.class),
                    Common.getBean(MailingListService.class),
                    Common.getBean(DataSourceService.class),
                    Common.getBean(DataPointService.class),
                    Common.getBean(PublisherService.class),
                    Common.getBean(EventHandlerService.class),
                    Common.getBean(JsonDataService.class),
                    Common.getBean(EventDetectorsService.class),
                    Common.getBean(SystemPermissionService.class),
                    null, false);
        }

        public List<ProcessMessage> getMessages() {
            return importContext.getResult().getMessages();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ \n");
        builder.append("getFullConfiguration(): String, \n");
        builder.append("getConfiguration(String): String, \n");
        builder.append("dataPointQuery(rql): String, \n");
        builder.append("dataSourceQuery(rql): String, \n");
        builder.append("doImport(String): void, \n");
        builder.append("doImportGetStatus(String): List<ProcessMessage> \n");
        builder.append("setImportDuringValidation(boolean): void");
        builder.append("}\n");
        return builder.toString();
    }
}
