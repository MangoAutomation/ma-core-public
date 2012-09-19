/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.emport;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeDefinition;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.EmportDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.util.BackgroundContext;
import com.serotonin.m2m2.view.text.PlainRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.EventHandlerVO;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.permission.DataPointAccess;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.dwr.EmportDwr;
import com.serotonin.util.ProgressiveTask;

/**
 * @author Matthew Lohbihler
 */
public class ImportTask extends ProgressiveTask {
    private final ImportContext importContext;
    private final User user;
    private final UserDao userDao = new UserDao();
    private final DataSourceDao dataSourceDao = new DataSourceDao();
    private final DataPointDao dataPointDao = new DataPointDao();
    private final EventDao eventDao = new EventDao();
    private final MailingListDao mailingListDao = new MailingListDao();
    private final PublisherDao publisherDao = new PublisherDao();

    private final List<JsonValue> users;
    private int userIndexPass1;
    private final List<JsonValue> pass2users;
    private int userIndexPass2;
    private final List<JsonValue> dataSources;
    private int dataSourceIndex;
    private final List<JsonValue> dataPoints;
    private int dataPointIndex;
    private JsonArray pointHierarchy;
    private final List<JsonValue> eventHandlers;
    private int eventHandlerIndex;
    private final List<JsonValue> mailingLists;
    private int mailingListIndex;
    private final List<JsonValue> publishers;
    private int publisherIndex;

    private final List<ImportItem> importItems = new ArrayList<ImportItem>();

    //    private final List<Integer> disabledDataSources = new ArrayList<Integer>();

    public ImportTask(JsonObject root, Translations translations, User user) {
        JsonReader reader = new JsonReader(Common.JSON_CONTEXT, root);
        this.importContext = new ImportContext(reader, new ProcessResult(), translations);
        this.user = user;

        users = nonNullList(root, EmportDwr.USERS);
        pass2users = new ArrayList<JsonValue>();
        dataSources = nonNullList(root, EmportDwr.DATA_SOURCES);
        dataPoints = nonNullList(root, EmportDwr.DATA_POINTS);
        pointHierarchy = root.getJsonArray(EmportDwr.POINT_HIERARCHY);
        mailingLists = nonNullList(root, EmportDwr.MAILING_LISTS);
        publishers = nonNullList(root, EmportDwr.PUBLISHERS);
        eventHandlers = nonNullList(root, EmportDwr.EVENT_HANDLERS);

        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class)) {
            ImportItem importItem = new ImportItem(def, root.get(def.getElementId()));
            importItems.add(importItem);
        }

        Common.timer.execute(this);
    }

    private List<JsonValue> nonNullList(JsonObject root, String key) {
        JsonArray arr = root.getJsonArray(key);
        if (arr == null)
            arr = new JsonArray();
        return arr;
    }

    public ProcessResult getResponse() {
        return importContext.getResult();
    }

    @Override
    protected void runImpl() {
        try {
            BackgroundContext.set(user);

            if (userIndexPass1 < users.size()) {
                importUser(users.get(userIndexPass1++).toJsonObject());
                return;
            }

            if (dataSourceIndex < dataSources.size()) {
                importDataSource(dataSources.get(dataSourceIndex++).toJsonObject());
                return;
            }

            if (dataPointIndex < dataPoints.size()) {
                importDataPoint(dataPoints.get(dataPointIndex++).toJsonObject());
                return;
            }

            if (userIndexPass2 < pass2users.size()) {
                importUserPermissions(pass2users.get(userIndexPass2++).toJsonObject());
                return;
            }

            if (pointHierarchy != null) {
                try {
                    importPointHierarchy(pointHierarchy);
                }
                finally {
                    pointHierarchy = null;
                }
                return;
            }

            if (mailingListIndex < mailingLists.size()) {
                importMailingList(mailingLists.get(mailingListIndex++).toJsonObject());
                return;
            }

            if (publisherIndex < publishers.size()) {
                importPublisher(publishers.get(publisherIndex++).toJsonObject());
                return;
            }

            if (eventHandlerIndex < eventHandlers.size()) {
                importEventHandler(eventHandlers.get(eventHandlerIndex++).toJsonObject());
                return;
            }

            for (ImportItem importItem : importItems) {
                if (!importItem.isComplete()) {
                    importItem.importNext(importContext);
                    return;
                }
            }

            completed = true;

            // Restart any data sources that were disabled.
            //            for (Integer id : disabledDataSources) {
            //                DataSourceVO<?> ds = dataSourceDao.getDataSource(id);
            //                ds.setEnabled(true);
            //                Common.runtimeManager.saveDataSource(ds);
            //            }
        }
        catch (Exception e) {
            String msg = e.getMessage();
            Throwable t = e;
            while ((t = t.getCause()) != null)
                msg += ", " + importContext.getTranslations().translate("emport.causedBy") + " '" + t.getMessage()
                        + "'";
            importContext.getResult().addGenericMessage("common.default", msg);
        }
        finally {
            BackgroundContext.remove();
        }
    }

    private void importUser(JsonObject userJson) {
        String username = userJson.getString("username");
        if (StringUtils.isBlank(username))
            importContext.getResult().addGenericMessage("emport.user.username");
        else {
            User user = userDao.getUser(username);
            if (user == null) {
                user = new User();
                user.setUsername(username);
                user.setPassword(Common.encrypt(username));
                user.setDataSourcePermissions(new ArrayList<Integer>());
                user.setDataPointPermissions(new ArrayList<DataPointAccess>());
            }

            try {
                importContext.getReader().readInto(user, userJson);

                // Now validate it. Use a new response object so we can distinguish errors in this user from other
                // errors.
                ProcessResult userResponse = new ProcessResult();
                user.validate(userResponse);
                if (userResponse.getHasMessages())
                    // Too bad. Copy the errors into the actual response.
                    importContext.copyValidationMessages(userResponse, "emport.user.prefix", username);
                else {
                    // Sweet. Save it.
                    boolean isnew = user.getId() == Common.NEW_ID;
                    userDao.saveUser(user);
                    importContext.addSuccessMessage(isnew, "emport.user.prefix", username);

                    // Add the user to the second pass list.
                    pass2users.add(userJson);
                }
            }
            catch (TranslatableJsonException e) {
                importContext.getResult().addGenericMessage("emport.user.prefix", username, e.getMsg());
            }
            catch (JsonException e) {
                importContext.getResult().addGenericMessage("emport.user.prefix", username,
                        importContext.getJsonExceptionMessage(e));
            }
        }
    }

    private void importDataSource(JsonObject dataSource) {
        String xid = dataSource.getString("xid");

        if (StringUtils.isBlank(xid))
            xid = dataSourceDao.generateUniqueXid();

        DataSourceVO<?> vo = dataSourceDao.getDataSource(xid);
        if (vo == null) {
            String typeStr = dataSource.getString("type");
            if (StringUtils.isBlank(typeStr))
                importContext.getResult().addGenericMessage("emport.dataSource.missingType", xid,
                        ModuleRegistry.getDataSourceDefinitionTypes());
            else {
                DataSourceDefinition def = ModuleRegistry.getDataSourceDefinition(typeStr);
                if (def == null)
                    importContext.getResult().addGenericMessage("emport.dataSource.invalidType", xid, typeStr,
                            ModuleRegistry.getDataSourceDefinitionTypes());
                else {
                    vo = def.baseCreateDataSourceVO();
                    vo.setXid(xid);
                }
            }
        }

        if (vo != null) {
            try {
                // The VO was found or successfully created. Finish reading it in.
                importContext.getReader().readInto(vo, dataSource);

                // Now validate it. Use a new response object so we can distinguish errors in this vo from
                // other errors.
                ProcessResult voResponse = new ProcessResult();
                vo.validate(voResponse);
                if (voResponse.getHasMessages())
                    // Too bad. Copy the errors into the actual response.
                    importContext.copyValidationMessages(voResponse, "emport.dataSource.prefix", xid);
                else {
                    // Sweet. Save it.
                    boolean isnew = vo.isNew();
                    Common.runtimeManager.saveDataSource(vo);
                    importContext.addSuccessMessage(isnew, "emport.dataSource.prefix", xid);
                }
            }
            catch (TranslatableJsonException e) {
                importContext.getResult().addGenericMessage("emport.dataSource.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                importContext.getResult().addGenericMessage("emport.dataSource.prefix", xid,
                        importContext.getJsonExceptionMessage(e));
            }
        }
    }

    private void importDataPoint(JsonObject dataPoint) {
        String xid = dataPoint.getString("xid");

        if (StringUtils.isBlank(xid))
            xid = dataPointDao.generateUniqueXid();

        DataSourceVO<?> dsvo;
        DataPointVO vo = dataPointDao.getDataPoint(xid);
        if (vo == null) {
            // Locate the data source for the point.
            String dsxid = dataPoint.getString("dataSourceXid");
            dsvo = dataSourceDao.getDataSource(dsxid);
            if (dsvo == null)
                importContext.getResult().addGenericMessage("emport.dataPoint.badReference", xid);
            else {
                vo = new DataPointVO();
                vo.setXid(xid);
                vo.setDataSourceId(dsvo.getId());
                vo.setDataSourceXid(dsxid);
                vo.setPointLocator(dsvo.createPointLocator());
                vo.setEventDetectors(new ArrayList<PointEventDetectorVO>(0));
                vo.setTextRenderer(new PlainRenderer());
            }
        }
        else
            dsvo = dataSourceDao.getDataSource(vo.getDataSourceId());

        if (vo != null) {
            try {
                importContext.getReader().readInto(vo, dataPoint);

                // If the name is not provided, default to the XID
                if (StringUtils.isBlank(vo.getName()))
                    vo.setName(xid);

                // Now validate it. Use a new response object so we can distinguish errors in this vo from
                // other errors.
                ProcessResult voResponse = new ProcessResult();
                vo.validate(voResponse);
                if (voResponse.getHasMessages())
                    // Too bad. Copy the errors into the actual response.
                    importContext.copyValidationMessages(voResponse, "emport.dataPoint.prefix", xid);
                else {
                    // Sweet. Save it.
                    boolean isnew = vo.isNew();

                    //                        // Check if this data source is enabled. Because data sources do automatic stuff upon the
                    //                        // starting of a point, we need to shut it down. We restart again once all data points are
                    //                        // imported.
                    //                        if (dsvo.isEnabled() && !disabledDataSources.contains(dsvo.getId())) {
                    //                            disabledDataSources.add(dsvo.getId());
                    //                            dsvo.setEnabled(false);
                    //                            Common.runtimeManager.saveDataSource(dsvo);
                    //                        }

                    Common.runtimeManager.saveDataPoint(vo);
                    importContext.addSuccessMessage(isnew, "emport.dataPoint.prefix", xid);
                }
            }
            catch (TranslatableJsonException e) {
                importContext.getResult().addGenericMessage("emport.dataPoint.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                importContext.getResult().addGenericMessage("emport.dataPoint.prefix", xid,
                        importContext.getJsonExceptionMessage(e));
            }
        }
    }

    private void importUserPermissions(JsonObject userJson) {
        // This method uses user objects from the second pass list, which have already been validated.
        String username = userJson.getString("username");
        User user = userDao.getUser(username);

        try {
            user.jsonDeserializePermissions(importContext.getReader(), userJson);
            userDao.saveUser(user);
            importContext.addSuccessMessage(false, "emport.userPermission.prefix", username);
        }
        catch (TranslatableJsonException e) {
            importContext.getResult().addGenericMessage("emport.userPermission.prefix", username, e.getMsg());
        }
        catch (JsonException e) {
            importContext.getResult().addGenericMessage("emport.userPermission.prefix", username,
                    importContext.getJsonExceptionMessage(e));
        }
    }

    @SuppressWarnings("unchecked")
    private void importPointHierarchy(JsonArray pointHierarchyJson) {
        PointFolder root = new PointFolder(0, "Root");
        try {
            List<PointFolder> subfolders = (List<PointFolder>) importContext.getReader().read(
                    new TypeDefinition(List.class, PointFolder.class), pointHierarchyJson);
            root.setSubfolders(subfolders);

            // Save the new values.
            dataPointDao.savePointHierarchy(root);
            importContext.addSuccessMessage(false, "emport.pointHierarchy.prefix", "");
        }
        catch (TranslatableJsonException e) {
            importContext.getResult().addGenericMessage("emport.pointHierarchy.prefix", e.getMsg());
        }
        catch (JsonException e) {
            importContext.getResult().addGenericMessage("emport.pointHierarchy.prefix",
                    importContext.getJsonExceptionMessage(e));
        }
    }

    private void importMailingList(JsonObject mailingList) {
        String xid = mailingList.getString("xid");
        if (StringUtils.isBlank(xid))
            xid = mailingListDao.generateUniqueXid();

        MailingList vo = mailingListDao.getMailingList(xid);
        if (vo == null) {
            vo = new MailingList();
            vo.setXid(xid);
        }

        try {
            importContext.getReader().readInto(vo, mailingList);

            // Now validate it. Use a new response object so we can distinguish errors in this vo from other errors.
            ProcessResult voResponse = new ProcessResult();
            vo.validate(voResponse);
            if (voResponse.getHasMessages())
                // Too bad. Copy the errors into the actual response.
                importContext.copyValidationMessages(voResponse, "emport.mailingList.prefix", xid);
            else {
                // Sweet. Save it.
                boolean isnew = vo.getId() == Common.NEW_ID;
                mailingListDao.saveMailingList(vo);
                importContext.addSuccessMessage(isnew, "emport.mailingList.prefix", xid);
            }
        }
        catch (TranslatableJsonException e) {
            importContext.getResult().addGenericMessage("emport.mailingList.prefix", xid, e.getMsg());
        }
        catch (JsonException e) {
            importContext.getResult().addGenericMessage("emport.mailingList.prefix", xid,
                    importContext.getJsonExceptionMessage(e));
        }
    }

    private void importPublisher(JsonObject publisher) {
        String xid = publisher.getString("xid");

        if (StringUtils.isBlank(xid))
            xid = publisherDao.generateUniqueXid();

        PublisherVO<?> vo = publisherDao.getPublisher(xid);
        if (vo == null) {
            String typeStr = publisher.getString("type");
            if (StringUtils.isBlank(typeStr))
                importContext.getResult().addGenericMessage("emport.publisher.missingType", xid,
                        ModuleRegistry.getPublisherDefinitionTypes());
            else {
                PublisherDefinition def = ModuleRegistry.getPublisherDefinition(typeStr);
                if (def == null)
                    importContext.getResult().addGenericMessage("emport.publisher.invalidType", xid, typeStr,
                            ModuleRegistry.getPublisherDefinitionTypes());
                else {
                    vo = def.baseCreatePublisherVO();
                    vo.setXid(xid);
                }
            }
        }

        if (vo != null) {
            try {
                // The VO was found or successfully created. Finish reading it in.
                importContext.getReader().readInto(vo, publisher);

                // Now validate it. Use a new response object so we can distinguish errors in this vo from
                // other errors.
                ProcessResult voResponse = new ProcessResult();
                vo.validate(voResponse);
                if (voResponse.getHasMessages())
                    // Too bad. Copy the errors into the actual response.
                    importContext.copyValidationMessages(voResponse, "emport.publisher.prefix", xid);
                else {
                    // Sweet. Save it.
                    boolean isnew = vo.isNew();
                    Common.runtimeManager.savePublisher(vo);
                    importContext.addSuccessMessage(isnew, "emport.publisher.prefix", xid);
                }
            }
            catch (TranslatableJsonException e) {
                importContext.getResult().addGenericMessage("emport.publisher.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                importContext.getResult().addGenericMessage("emport.publisher.prefix", xid,
                        importContext.getJsonExceptionMessage(e));
            }
        }
    }

    private void importEventHandler(JsonObject eventHandler) {
        String xid = eventHandler.getString("xid");
        if (StringUtils.isBlank(xid))
            xid = eventDao.generateUniqueXid();

        EventHandlerVO handler = eventDao.getEventHandler(xid);
        if (handler == null) {
            handler = new EventHandlerVO();
            handler.setXid(xid);
        }

        try {
            // Find the event type.
            EventType eventType = importContext.getReader().read(EventType.class,
                    eventHandler.getJsonObject("eventType"));

            importContext.getReader().readInto(handler, eventHandler);

            // Now validate it. Use a new response object so we can distinguish errors in this vo from other errors.
            ProcessResult voResponse = new ProcessResult();
            handler.validate(voResponse);
            if (voResponse.getHasMessages())
                // Too bad. Copy the errors into the actual response.
                importContext.copyValidationMessages(voResponse, "emport.eventHandler.prefix", xid);
            else {
                // Sweet.
                boolean isnew = handler.getId() == Common.NEW_ID;

                if (!isnew) {
                    // Check if the event type has changed.
                    EventType oldEventType = eventDao.getEventHandlerType(handler.getId());
                    if (!oldEventType.equals(eventType)) {
                        // Event type has changed. Delete the old one.
                        eventDao.deleteEventHandler(handler.getId());

                        // Call it new
                        handler.setId(Common.NEW_ID);
                        isnew = true;
                    }
                }

                // Save it.
                eventDao.saveEventHandler(eventType, handler);
                importContext.addSuccessMessage(isnew, "emport.eventHandler.prefix", xid);
            }
        }
        catch (TranslatableJsonException e) {
            importContext.getResult().addGenericMessage("emport.eventHandler.prefix", xid, e.getMsg());
        }
        catch (JsonException e) {
            importContext.getResult().addGenericMessage("emport.eventHandler.prefix", xid,
                    importContext.getJsonExceptionMessage(e));
        }
    }
}
