package com.serotonin.m2m2.rt.script;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.infiniteautomation.mango.db.query.BaseSqlQuery;
import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.EmportDwr;
import com.serotonin.m2m2.web.dwr.emport.ImportTask;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;

public class JsonEmportScriptUtility {
	public static String CONTEXT_KEY = "JsonEmport";
	
	private final boolean admin;
	private final RQLParser parser;
	private final List<JsonImportExclusion> importExclusions;
	
	public JsonEmportScriptUtility(ScriptPermissions permissions, List<JsonImportExclusion> importExclusions) {
		admin = Permissions.hasPermission(permissions.getDataSourcePermissions(), SuperadminPermissionDefinition.GROUP_NAME) &&
				Permissions.hasPermission(permissions.getDataPointSetPermissions(), SuperadminPermissionDefinition.GROUP_NAME);
		this.parser = new RQLParser();
		this.importExclusions = importExclusions;
	}
	
	public String getFullConfiguration() {
		return getFullConfiguration(0);
	}
	
	public String getFullConfiguration(int prettyIndent) {
		if(admin) {
	        return EmportDwr.export(ConfigurationExportData.createExportDataMap(null), prettyIndent);
		}
		return "{}";
	}
	
	public String getConfiguration(String type) {
		return getConfiguration(type, 0);
	}
	
	public String getConfiguration(String type, int prettyIndent) {
		Map<String, Object> data;
		if(admin) {
		    data = ConfigurationExportData.createExportDataMap(new String[] {type});
		}else {
		    data = new LinkedHashMap<>();
		}
		
		return EmportDwr.export(data, prettyIndent);
	}
	
	public String dataPointQuery(String query) {
		return dataPointQuery(query, 0);
	}
	
	public String dataPointQuery(String query, int prettyIndent) {
		Map<String, Object> data = new LinkedHashMap<>();
		if(admin) {
			ASTNode root = parser.parse(query);
			BaseSqlQuery<DataPointVO> sqlQuery = DataPointDao.instance.createQuery(root, true);
			
			List<DataPointVO> dataPoints = sqlQuery.immediateQuery();
			data.put(ConfigurationExportData.DATA_POINTS, dataPoints);
		}
		return EmportDwr.export(data, prettyIndent);
	}
	
	public String dataSourceQuery(String query) {
		return dataSourceQuery(query, 0);
	}
	
	public String dataSourceQuery(String query, int prettyIndent) {
		Map<String, Object> data = new LinkedHashMap<>();
		if(admin) {
			ASTNode root = parser.parse(query);
			BaseSqlQuery<DataSourceVO<?>> sqlQuery = DataSourceDao.instance.createQuery(root, true);
			
			List<DataSourceVO<?>> dataSources = sqlQuery.immediateQuery();
			data.put(ConfigurationExportData.DATA_SOURCES, dataSources);
		}
		return EmportDwr.export(data, prettyIndent);
	}
	
	public void doImport(String json) throws Exception {
		if(admin) {
			JsonTypeReader reader = new JsonTypeReader(json);
			JsonValue value = reader.read();
			JsonObject jo = value.toJsonObject();
			if(importExclusions != null)
				doExclusions(jo);
			ScriptImportTask sit = new ScriptImportTask(jo);
			sit.run(Common.timer.currentTimeMillis());
		}
	}
	
	public List<ProcessMessage> doImportGetStatus(String json) throws Exception {
		if(admin) {
			JsonTypeReader reader = new JsonTypeReader(json);
			JsonValue value = reader.read();
			JsonObject jo = value.toJsonObject();
			if(importExclusions != null)
				doExclusions(jo);
			ScriptImportTask sit = new ScriptImportTask(jo);
			sit.run(Common.timer.currentTimeMillis());
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
	
	class ScriptImportTask extends ImportTask {
		
		public ScriptImportTask(JsonObject jo) {
			super(jo, Common.getTranslations(), null, false);
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
		builder.append("}\n");
		return builder.toString();
	}
}
