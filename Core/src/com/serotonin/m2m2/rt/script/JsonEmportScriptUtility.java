package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.infiniteautomation.mango.db.query.BaseSqlQuery;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigDao;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.module.EmportDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.EmportDwr;
import com.serotonin.m2m2.web.dwr.emport.ImportItem;
import com.serotonin.m2m2.web.dwr.emport.ImportTask;
import com.serotonin.m2m2.web.dwr.emport.Importer;

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
			Map<String, Object> data = new LinkedHashMap<>();

            data.put(EmportDwr.DATA_SOURCES, DataSourceDao.instance.getDataSources());
            data.put(EmportDwr.DATA_POINTS, DataPointDao.instance.getDataPoints(null, true));
            data.put(EmportDwr.USERS, UserDao.instance.getUsers());
            data.put(EmportDwr.MAILING_LISTS, MailingListDao.instance.getMailingLists());
            data.put(EmportDwr.PUBLISHERS, PublisherDao.instance.getPublishers());
            data.put(EmportDwr.EVENT_HANDLERS, EventHandlerDao.instance.getEventHandlers());
            data.put(EmportDwr.POINT_HIERARCHY, DataPointDao.instance.getPointHierarchy(true).getRoot().getSubfolders());
            data.put(EmportDwr.SYSTEM_SETTINGS, SystemSettingsDao.instance.getAllSystemSettingsAsCodes());
            data.put(EmportDwr.TEMPLATES, TemplateDao.instance.getAll());
            data.put(EmportDwr.VIRTUAL_SERIAL_PORTS, VirtualSerialPortConfigDao.instance.getAll());
            data.put(EmportDwr.JSON_DATA, JsonDataDao.instance.getAll());
	        
	        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class))
                data.put(def.getElementId(), def.getExportData());

	        return EmportDwr.export(data, prettyIndent);
		}
		return "{}";
	}
	
	public String getConfiguration(String type) {
		return getConfiguration(type, 0);
	}
	
	public String getConfiguration(String type, int prettyIndent) {
		Map<String, Object> data = new LinkedHashMap<>();
		if(admin) {
			if(EmportDwr.DATA_SOURCES.equals(type)) {
				data.put(EmportDwr.DATA_SOURCES, DataSourceDao.instance.getDataSources());
			} else if(EmportDwr.DATA_POINTS.equals(type)) {
				data.put(EmportDwr.DATA_POINTS, DataPointDao.instance.getDataPoints(null, true));
			} else if(EmportDwr.USERS.equals(type)) {
				data.put(EmportDwr.USERS, UserDao.instance.getUsers());
			} else if(EmportDwr.MAILING_LISTS.equals(type)) {
				data.put(EmportDwr.MAILING_LISTS, MailingListDao.instance.getMailingLists());
			} else if(EmportDwr.PUBLISHERS.equals(type)) {
				data.put(EmportDwr.PUBLISHERS, PublisherDao.instance.getPublishers());
			} else if(EmportDwr.EVENT_HANDLERS.equals(type)) {
				data.put(EmportDwr.EVENT_HANDLERS, EventHandlerDao.instance.getEventHandlers());
			} else if(EmportDwr.POINT_HIERARCHY.equals(type)) {
				data.put(EmportDwr.POINT_HIERARCHY, DataPointDao.instance.getPointHierarchy(true).getRoot().getSubfolders());
			} else if(EmportDwr.SYSTEM_SETTINGS.equals(type)) {
				data.put(EmportDwr.SYSTEM_SETTINGS, SystemSettingsDao.instance.getAllSystemSettingsAsCodes());
			} else if(EmportDwr.TEMPLATES.equals(type)) {
				data.put(EmportDwr.TEMPLATES, TemplateDao.instance.getAll());
			} else if(EmportDwr.VIRTUAL_SERIAL_PORTS.equals(type)) {
				data.put(EmportDwr.VIRTUAL_SERIAL_PORTS, VirtualSerialPortConfigDao.instance.getAll());
			} else if(EmportDwr.JSON_DATA.equals(type)) {
				data.put(EmportDwr.JSON_DATA, JsonDataDao.instance.getAll());
			} else
				for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class))
	                if(def.getElementId().equals(type)) {
	                	data.put(def.getElementId(), def.getExportData());
	                	break;
	                }
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
			data.put(EmportDwr.DATA_POINTS, dataPoints);
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
			data.put(EmportDwr.DATA_SOURCES, dataSources);
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
			ScriptImportTask sit = new ScriptImportTask(jo, false);
			while(!sit.isCompleted() && !sit.isCancelled())
				try { Thread.sleep(1000); } catch(InterruptedException e) {} //Take a break.
		}
	}
	
	public List<String> doImportGetStatus(String json) throws Exception {
		if(admin) {
			JsonTypeReader reader = new JsonTypeReader(json);
			JsonValue value = reader.read();
			JsonObject jo = value.toJsonObject();
			if(importExclusions != null)
				doExclusions(jo);
			ScriptImportTask sit = new ScriptImportTask(jo, true);
			while(!sit.isCompleted() && !sit.isCancelled())
				try { Thread.sleep(1000); } catch(InterruptedException e) {} //Take a break.
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

		private List<String> messages;
		
		public ScriptImportTask(JsonObject jo, boolean messages) {
			super(jo, Common.getTranslations(), null);
			if(messages)
				this.messages = new ArrayList<>();
			else
				this.messages = null;
		}
		
		public List<String> getMessages() {
			return messages;
		}
		
		@Override
		protected void runImpl() {
            if (!importers.isEmpty()) {
                if (importerIndex >= importers.size()) {
                    // A run through the importers has been completed.
                    if (importerSuccess) {
                        // If there were successes with the importers and there are still more to do, run through 
                        // them again.
                        importerIndex = 0;
                        importerSuccess = false;
                    } else if(!importedItems) {
        	            try {
                            for (ImportItem importItem : importItems) {
                                if (!importItem.isComplete()) {
                                    importItem.importNext(importContext);
                                    return;
                                }
                            }
                            importedItems = true;   // We may have imported a dependency in a module
                            importerIndex = 0;
                        }
                        catch (Exception e) {
                            addException(e);
                        }
                    } else {
                        // There are importers left in the list, but there were no successful imports in the last run
                        // of the set. So, all that is left is stuff that will always fail. Copy the validation 
                        // messages to the context for each.
                    	// Run the import items.
                        for (Importer importer : importers)
                            importer.copyMessages();
                        importers.clear();
                        completed = true;
                        return;
                    }
                }

                // Run the next importer
                Importer importer = importers.get(importerIndex);
                try {
                    importer.doImport();
                    if (importer.success()) {
                        // The import was successful. Note the success and remove the importer from the list.
                        importerSuccess = true;
                        importers.remove(importerIndex);
                    }
                    else{
                        // The import failed. Leave it in the list since the run of another importer
                        // may resolved the problem.
                        importerIndex++;
                    }
                }
                catch (Exception e) {
                    // Uh oh...
                    addException(e);
                    importers.remove(importerIndex);
                }

                return;
            }

            // Run the import items.
            try {
                for (ImportItem importItem : importItems) {
                    if (!importItem.isComplete()) {
                        importItem.importNext(importContext);
                        return;
                    }
                }

                completed = true;
            }
            catch (Exception e) {
                addException(e);
            }
		}
		
		private void addException(Exception e) {
	        String msg = e.getMessage();
	        Throwable t = e;
	        while ((t = t.getCause()) != null)
	            msg += ", " + importContext.getTranslations().translate("emport.causedBy") + " '" + t.getMessage() + "'";
	        //We were missing NPE and others without a msg
	        if(msg == null)
	        	msg = e.getClass().getCanonicalName();
	        messages.add(msg);
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
		builder.append("doImportGetStatus(String): List<String> \n");
		builder.append("}\n");
		return builder.toString();
	}
}
