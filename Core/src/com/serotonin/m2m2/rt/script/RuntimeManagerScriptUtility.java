/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * @author Terry Packer
 *
 */
public class RuntimeManagerScriptUtility extends ScriptUtility {

    public static final String CONTEXT_KEY = "RuntimeManager";

    private static final Log LOG = LogFactory.getLog(RuntimeManagerScriptUtility.class);

    protected static final int DOES_NOT_EXIST = -1;  //Point or Data Soure Does not exist
    protected static final int OPERATION_NO_CHANGE = 0; //Operation didn't have any effect, it was already in that state
    protected static final int OPERATION_SUCCESSFUL = 1; //Operation worked

    private final DataSourceService dataSourceService;
    private final DataPointService dataPointService;

    @Autowired
    public RuntimeManagerScriptUtility(MangoJavaScriptService service, DataPointService dataPointService, DataSourceService dataSourceService) {
        super(service, dataPointService.getPermissionService());
        this.dataSourceService = dataSourceService;
        this.dataPointService = dataPointService;
    }

    @Override
    public String getContextKey() {
        return CONTEXT_KEY;
    }

    /**
     * Refresh a data point with the given XID.
     *
     * @param xid
     * @return status of operation
     * 0 - Point not enabled
     * -1 - Point does not exist
     * 1 - Refresh performed
     */
    public int refreshDataPoint(String xid){

        DataPointVO vo = DataPointDao.getInstance().getByXid(xid);

        if(vo != null){

            if(!vo.isEnabled())
                return OPERATION_NO_CHANGE;

            DataSourceRT<?> dsRt = Common.runtimeManager.getRunningDataSource(vo.getDataSourceId());
            if(dsRt == null || !permissionService.hasDataSourceEditPermission(permissions, dsRt.getVo()))
                return OPERATION_NO_CHANGE;

            Common.runtimeManager.forcePointRead(vo.getId());
            return OPERATION_SUCCESSFUL;

        }else
            return DOES_NOT_EXIST;

    }

    /**
     * Refresh a data source with the given XID.
     *
     * @param xid
     * @return status of operation
     * 0 - Source not enabled
     * -1 - Source does not exist
     * 1 - Refresh performed
     */
    public int refreshDataSource(String xid){

        DataSourceVO vo = DataSourceDao.getInstance().getByXid(xid);

        if(vo != null){

            if(!vo.isEnabled())
                return OPERATION_NO_CHANGE;

            DataSourceRT<?> dsRt = Common.runtimeManager.getRunningDataSource(vo.getId());
            if(dsRt == null || !permissionService.hasDataSourceEditPermission(permissions, dsRt.getVo()))
                return OPERATION_NO_CHANGE;

            Common.runtimeManager.forceDataSourcePoll(vo.getId());
            return OPERATION_SUCCESSFUL;

        }else
            return DOES_NOT_EXIST;

    }

    /**
     * Is a data source enabled?
     * @param xid
     * @return true if it is, false if it is not
     */
    public boolean isDataSourceEnabled(String xid){

        DataSourceVO vo = DataSourceDao.getInstance().getByXid(xid);

        if(vo == null)
            return false;
        else{
            //This will throw an exception if there is no permission
            if(permissionService.hasDataSourceEditPermission(permissions, vo))
                return vo.isEnabled();
            else
                return false;
        }

    }

    /**
     * Start a Data Source via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already enabled, 1 if it was sent to RuntimeManager
     */
    public int enableDataSource(String xid) {
        return Common.getBean(PermissionService.class).runAs(permissions, () -> {
            try {
                DataSourceVO vo = dataSourceService.get(xid);
                if(!vo.isEnabled()) {
                    vo.setEnabled(true);
                    try{
                        dataSourceService.update(xid, vo);
                    }catch(Exception e){
                        LOG.error(e.getMessage(), e);
                        throw e;
                    }
                    return OPERATION_SUCCESSFUL;
                } else
                    return OPERATION_NO_CHANGE;
            }catch(Exception e) {
                return DOES_NOT_EXIST;
            }
        });
    }

    /**
     * Stop a Data Source via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already disabled, 1 if it was sent to RuntimeManager
     */
    public int disableDataSource(String xid){
        return Common.getBean(PermissionService.class).runAs(permissions, () -> {
            try {
                DataSourceVO vo = dataSourceService.get(xid);
                if(vo.isEnabled()) {
                    vo.setEnabled(false);
                    try{
                        dataSourceService.update(xid, vo);
                    }catch(Exception e){
                        LOG.error(e.getMessage(), e);
                        throw e;
                    }
                    return OPERATION_SUCCESSFUL;
                } else
                    return OPERATION_NO_CHANGE;
            }catch(Exception e) {
                return DOES_NOT_EXIST;
            }
        });
    }

    /**
     * Is a data point enabled?
     *
     * A point is enabled if both the data source and point are enabled.
     *
     * @param xid
     * @return true if it is, false if it is not
     */
    public boolean isDataPointEnabled(String xid){
        DataPointVO vo = DataPointDao.getInstance().getByXid(xid);
        if(vo == null)
            return false;
        else{
            if(permissionService.hasDataPointSetPermission(permissions, vo) || permissionService.hasDataPointReadPermission(permissions, vo)){
                DataSourceVO ds = DataSourceDao.getInstance().get(vo.getDataSourceId());
                if(ds == null)
                    return false;
                return (ds.isEnabled() && vo.isEnabled());
            }else
                return false;
        }
    }

    /**
     * Start a Data Point via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already enabled, 1 if it was sent to RuntimeManager
     */
    public int enableDataPoint(String xid){
        return Common.getBean(PermissionService.class).runAs(permissions, () -> {
            try {
                if(dataPointService.setDataPointState(xid, true, false)) {
                    return OPERATION_SUCCESSFUL;
                }else {
                    return OPERATION_NO_CHANGE;
                }
            }catch(Exception e) {
                return DOES_NOT_EXIST;
            }
        });
    }

    /**
     * Stop a Data Point via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already disabled, 1 if it was sent to RuntimeManager
     */
    public int disableDataPoint(String xid){
        return Common.getBean(PermissionService.class).runAs(permissions, () -> {
            try {
                if(dataPointService.setDataPointState(xid, false, false)) {
                    return OPERATION_SUCCESSFUL;
                }else {
                    return OPERATION_NO_CHANGE;
                }
            }catch(Exception e) {
                return DOES_NOT_EXIST;
            }
        });
    }

    /**
     * Is a publisher enabled?
     * @param xid
     * @return true if it is, false if it is not or no permission
     */
    public boolean isPublisherEnabled(String xid){
        PublisherVO<?> vo = PublisherDao.getInstance().getByXid(xid);

        if(vo == null)
            return false;
        else{
            //This will throw an exception if there is no permission
            if(permissionService.hasAdminRole(permissions))
                return vo.isEnabled();
            else
                return false;
        }

    }

    /**
     * Start a publisher via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already enabled, 1 if it was sent to RuntimeManager
     */
    public int enablePublisher(String xid){
        PublisherVO<?> vo = PublisherDao.getInstance().getByXid(xid);
        if(vo == null || !permissionService.hasAdminRole(permissions))
            return DOES_NOT_EXIST;
        else if(!vo.isEnabled()){
            vo.setEnabled(true);
            try{
                Common.runtimeManager.savePublisher(vo);
            }catch(Exception e){
                LOG.error(e.getMessage(), e);
                throw e;
            }
            return OPERATION_SUCCESSFUL;
        }else
            return OPERATION_NO_CHANGE;
    }

    /**
     * Stop a publisher via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already disabled, 1 if it was sent to RuntimeManager
     */
    public int disablePublisher(String xid){
        PublisherVO<?> vo = PublisherDao.getInstance().getByXid(xid);
        if(vo == null || !permissionService.hasAdminRole(permissions))
            return DOES_NOT_EXIST;
        else if(vo.isEnabled()){
            vo.setEnabled(false);
            try{
                Common.runtimeManager.savePublisher(vo);
            }catch(Exception e){
                LOG.error(e.getMessage(), e);
                throw e;
            }
            return OPERATION_SUCCESSFUL;
        }else
            return OPERATION_NO_CHANGE;
    }

    public void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch(Exception e) {
            //no-op
        }
    }

    @Override
    public String getHelp(){
        return toString();
    }
    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        builder.append("refreshDataPoint(xid): -1 0 1, \n");
        builder.append("refreshDataSource(xid): -1 0 1, \n");
        builder.append("isDataSourceEnabled(xid): boolean, \n");
        builder.append("enableDataSource(xid): -1 0 1, \n");
        builder.append("disableDataSource(xid): -1 0 1, \n");
        builder.append("isDataPointEnabled(xid): boolean, \n");
        builder.append("enableDataPoint(xid): -1 0 1, \n");
        builder.append("disableDataPoint(xid): -1 0 1, \n");
        builder.append("isPublisherEnabled(xid): boolean, \n");
        builder.append("enablePublisher(xid): -1 0 1, \n");
        builder.append("disablePublisher(xid): -1 0 1, \n");
        builder.append("sleep(milliseconds): void, \n");
        builder.append(" }");
        return builder.toString();
    }
}
