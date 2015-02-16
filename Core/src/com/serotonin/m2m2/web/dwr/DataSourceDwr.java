/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.ResultsWithTotal;
import com.serotonin.m2m2.db.dao.SortOption;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.dataSource.DataSourceRTM;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.comparators.StringStringPairComparator;
import com.serotonin.m2m2.web.dwr.beans.EventInstanceBean;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.m2m2.web.taglib.Functions;

/**
 * 
 * DWR For Data Source Manipulation
 * 
 * @author Terry Packer
 *
 */
public class DataSourceDwr extends AbstractRTDwr<DataSourceVO<?>, DataSourceDao, DataSourceRT, DataSourceRTM> {

    /**
     * Default Constructor
     */
    public DataSourceDwr() {
        super(DataSourceDao.instance, "dataSources", DataSourceRTM.instance, "dataSources");
        LOG = LogFactory.getLog(DataSourceDwr.class);
    }

    /**
     * Init Data Source Types
     * 
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult initDataSourceTypes() {
        ProcessResult response = new ProcessResult();

        User user = Common.getUser();

        if (user.isDataSourcePermission()) {
            List<StringStringPair> translatedTypes = new ArrayList<>();
            for (String type : ModuleRegistry.getDataSourceDefinitionTypes()) {
                translatedTypes.add(new StringStringPair(type, translate(ModuleRegistry.getDataSourceDefinition(type)
                        .getDescriptionKey())));
            }
            StringStringPairComparator.sort(translatedTypes);
            response.addData("types", translatedTypes);

        }

        return response;
    }

    @DwrPermission(user = true)
    public ProcessResult getNew(String type) {
        ProcessResult response = new ProcessResult();
        DataSourceVO<?> vo = null;
        DataSourceDefinition def = ModuleRegistry.getDataSourceDefinition(type);
        if (def == null) {
            //TODO Add message to response about unknown type or invalid type
        }
        else {
            try {
                vo = def.baseCreateDataSourceVO();
                vo.setId(Common.NEW_ID);
                vo.setXid(new DataSourceDao().generateUniqueXid());
                User user = Common.getUser();
                if (!user.isAdmin())
                    // Default the permissions of the data source to that of the user so that 
                    // the user can access the thing.
                    vo.setEditPermission(Common.getUser().getPermissions());

                response.addData("vo", vo);

                //Setup the page info
                response.addData("editPagePath", def.getModule().getWebPath() + "/" + def.getEditPagePath());
                response.addData("statusPagePath", def.getModule().getWebPath() + "/" + def.getStatusPagePath());
            }
            catch (Exception e) {
                LOG.error(e.getMessage());
                response.addMessage(new TranslatableMessage("table.error.dwr", e.getMessage()));
            }
        }
        return response;
    }

    @DwrPermission(user = true)
    @Override
    public ProcessResult get(int id) {
        ProcessResult response;
        try {
            if (id > 0) {
                response = super.get(id);
                //Kludge for modules to be able to use a default edit point for some of their tools (Bacnet for example needs this for adding lots of points)
                //This is an issue for opening AllDataPoints Point because it opens the Datasource too.
                //TODO to fix this we need to fix DataSourceEditDwr to not save the editing DataPoint state in the User, this will propogate into existing modules...
                DataSourceVO<?> vo = (DataSourceVO<?>) response.getData().get("vo");

                //Quick fix to ensure we don't keep the edit point around if we have switched data sources
                if ((Common.getUser().getEditPoint() == null)
                        || (Common.getUser().getEditPoint().getDataSourceId() != vo.getId())
                        || (Common.getUser().getEditPoint().getDataSourceTypeName() != vo.getDefinition()
                                .getDataSourceTypeName())) {
                    DataPointVO dp = new DataPointVO();

                    dp.setXid(DataPointDao.instance.generateUniqueXid());
                    dp.setDataSourceId(vo.getId());
                    dp.setDataSourceTypeName(vo.getDefinition().getDataSourceTypeName());
                    dp.setDeviceName(vo.getName());
                    dp.setEventDetectors(new ArrayList<PointEventDetectorVO>(0));
                    dp.defaultTextRenderer();
                    dp.setXid(DataPointDao.instance.generateUniqueXid());
                    dp.setPointLocator(vo.createPointLocator());
                    Common.getUser().setEditPoint(dp);
                }

            }
            else {
                throw new ShouldNeverHappenException("Unable to get a new DataSource.");
            }
            //Setup the page info
            response.addData("editPagePath", ((DataSourceVO<?>) response.getData().get("vo")).getDefinition()
                    .getModule().getWebPath()
                    + "/" + ((DataSourceVO<?>) response.getData().get("vo")).getDefinition().getEditPagePath());
            response.addData("statusPagePath", ((DataSourceVO<?>) response.getData().get("vo")).getDefinition()
                    .getModule().getWebPath()
                    + "/" + ((DataSourceVO<?>) response.getData().get("vo")).getDefinition().getStatusPagePath());
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
            response = new ProcessResult();
            response.addMessage(new TranslatableMessage("table.error.dwr", e.getMessage()));
        }
        return response;
    }

    /**
     * Export Data Source and Points together
     */
    @DwrPermission(user = true)
    @Override
    public String jsonExport(int id) {

        Map<String, Object> data = new LinkedHashMap<>();
        List<DataSourceVO<?>> dss = new ArrayList<>();
        dss.add(new DataSourceDao().getDataSource(id));
        data.put(EmportDwr.DATA_SOURCES, dss);
        data.put(EmportDwr.DATA_POINTS, DataPointDao.instance.getDataPoints(id, null));
        return EmportDwr.export(data, 3);
    }

    @DwrPermission(user = true)
    @Override
    public ProcessResult getCopy(int id) {

        //Get a Full Copy
        DataSourceVO<?> vo = dao.getFull(id);
        ProcessResult response = new ProcessResult();

        String name = StringUtils.abbreviate(
                TranslatableMessage.translate(getTranslations(), "common.copyPrefix", vo.getName()), 40);

        //Setup the Copy
        DataSourceVO<?> copy = vo.copy();
        copy.setId(Common.NEW_ID);
        copy.setName(name);
        copy.setXid(dao.generateUniqueXid());
        response.addData("vo", copy);

        //Don't Validate it, that will be done on save

        return response;
    }

    @DwrPermission(user = true)
    public ProcessResult finishCopy(int copyFromId, int newId) {
        ProcessResult result = new ProcessResult();

        if (!result.getHasMessages()) {
            this.dao.copyDataSourcePoints(copyFromId, newId);
            result.addData("vo", dao.get(newId));
        }
        return result;
    }

    /**
     * Get the general status messages for a given data source
     * 
     * @param id
     * @return
     */
    @DwrPermission(user = true)
    public final ProcessResult getGeneralStatusMessages(int id) {
        ProcessResult result = new ProcessResult();

        DataSourceRT rt = Common.runtimeManager.getRunningDataSource(id);

        List<TranslatableMessage> messages = new ArrayList<>();
        result.addData("messages", messages);
        if (rt == null)
            messages.add(new TranslatableMessage("dsEdit.notEnabled"));
        else {
            rt.addStatusMessages(messages);
            if (messages.isEmpty())
                messages.add(new TranslatableMessage("dsEdit.noStatus"));
        }

        return result;
    }

    /**
     * Get the current alarms for a datasource
     * 
     * @param id
     * @return
     */
    @DwrPermission(user = true)
    public List<EventInstanceBean> getAlarms(int id) {
        DataSourceVO<?> ds = Common.runtimeManager.getDataSource(id);
        List<EventInstanceBean> beans = new ArrayList<>();

        if (ds != null) {
            List<EventInstance> events = new EventDao().getPendingEventsForDataSource(ds.getId(), Common.getUser()
                    .getId());
            if (events != null) {
                for (EventInstance event : events)
                    beans.add(new EventInstanceBean(event.isActive(), event.getAlarmLevel(), Functions.getTime(event
                            .getActiveTimestamp()), translate(event.getMessage())));
            }
        }
        return beans;
    }

    /**
     * Load a list of VOs
     * 
     * Overridden to provide security
     * 
     * @return
     */
    @Override
    @DwrPermission(user = true)
    public ProcessResult dojoQuery(Map<String, String> query, List<SortOption> sort, Integer start, Integer count,
            boolean or) {
        ProcessResult response = new ProcessResult();

        ResultsWithTotal results = dao.dojoQuery(query, sort, start, count, or);
        List<DataSourceVO<?>> vos = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<DataSourceVO<?>> filtered = (List<DataSourceVO<?>>) results.getResults();

        //Filter list on User Permissions
        User user = Common.getUser();
        for (DataSourceVO<?> vo : filtered) {
            if (Permissions.hasDataSourcePermission(user, vo))
                vos.add(vo);
        }

        //Since we have removed some, we need to review our totals here,,
        // this will be a bit buggy because we don't know how many of the remaining items 
        // are actually viewable by this user.
        int total = results.getTotal() - (filtered.size() - vos.size());
        response.addData("list", vos);
        response.addData("total", total);

        return response;
    }

    /**
     * Export VOs based on a filter
     * 
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    @DwrPermission(user = true)
    public String jsonExportUsingFilter(Map<String, String> query, List<SortOption> sort, Integer start, Integer count,
            boolean or) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<DataSourceVO<?>> vos = new ArrayList<>();

        ResultsWithTotal results = dao.dojoQuery(query, sort, start, count, or);
        List<DataSourceVO<?>> filtered = (List<DataSourceVO<?>>) results.getResults();

        //Filter list on User Permissions
        User user = Common.getUser();
        for (DataSourceVO<?> vo : filtered) {
            if (Permissions.hasDataSourcePermission(user, vo)) {
                vos.add(vo);
                //Not doing this yet, might look weird to user
                //data.put(EmportDwr.DATA_POINTS, DataPointDao.instance.getDataPoints(vo.getId(), null));
            }
        }

        //Get the Full VO for the export
        data.put(keyName, vos);

        return EmportDwr.export(data, 3);
    }

}
