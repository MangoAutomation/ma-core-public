package com.serotonin.m2m2.rt.script;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

public class RuntimeManagerScriptTestUtility extends RuntimeManagerScriptUtility {

    @Autowired
    public RuntimeManagerScriptTestUtility(MangoJavaScriptService service, DataPointService dataPointService, DataSourceService dataSourceService) {
        super(service, dataPointService, dataSourceService);
    }

    /**
     * Mock refresh a data point with the given XID.
     *
     * @param xid
     * @return status of operation
     * 0 - Point not enabled
     * -1 - Point does not exist
     * 1 - Mock refresh performed
     */
    @Override
    public int refreshDataPoint(String xid){

        DataPointVO vo = DataPointDao.getInstance().getByXid(xid);

        if(vo != null){

            if(!vo.isEnabled())
                return OPERATION_NO_CHANGE;

            DataSourceRT<?> dsRt = Common.runtimeManager.getRunningDataSource(vo.getDataSourceId());
            if(dsRt == null || !permissionService.hasDataSourceEditPermission(permissions, dsRt.getVo()))
                return OPERATION_NO_CHANGE;

            //forcePointRead
            return OPERATION_SUCCESSFUL;

        }else
            return DOES_NOT_EXIST;

    }

    /**
     * Mock refresh a data source with the given XID.
     *
     * @param xid
     * @return status of operation
     * 0 - Source not enabled
     * -1 - Source does not exist
     * 1 - Refresh performed
     */
    @Override
    public int refreshDataSource(String xid){

        DataSourceVO vo = DataSourceDao.getInstance().getByXid(xid);

        if(vo != null){

            if(!vo.isEnabled())
                return OPERATION_NO_CHANGE;

            DataSourceRT<?> dsRt = Common.runtimeManager.getRunningDataSource(vo.getId());
            if(dsRt == null || !permissionService.hasDataSourceEditPermission(permissions, dsRt.getVo()))
                return OPERATION_NO_CHANGE;

            //Common.runtimeManager.forceDataSourcePoll(vo.getId());
            return OPERATION_SUCCESSFUL;

        }else
            return DOES_NOT_EXIST;

    }

    /**
     * Would start a Data Source via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already enabled, 1 if it would be sent to RuntimeManager
     */
    @Override
    public int enableDataSource(String xid){
        DataSourceVO vo = DataSourceDao.getInstance().getByXid(xid);
        if(vo == null || !permissionService.hasDataSourceEditPermission(permissions, vo))
            return DOES_NOT_EXIST;
        else if(!vo.isEnabled())
            return OPERATION_SUCCESSFUL;
        else
            return OPERATION_NO_CHANGE;
    }

    /**
     * Would stop a Data Source via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already disabled, 1 if it would be sent to RuntimeManager
     */
    @Override
    public int disableDataSource(String xid){
        DataSourceVO vo = DataSourceDao.getInstance().getByXid(xid);
        if(vo == null || !permissionService.hasDataSourceEditPermission(permissions, vo))
            return DOES_NOT_EXIST;
        else if(vo.isEnabled())
            return OPERATION_SUCCESSFUL;
        else
            return OPERATION_NO_CHANGE;
    }

    /**
     * Would start a Data Point via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already enabled, 1 if it would be sent to RuntimeManager
     */
    @Override
    public int enableDataPoint(String xid){
        DataPointVO vo = DataPointDao.getInstance().getByXid(xid);
        if(vo == null || !permissionService.hasDataPointSetPermission(permissions, vo))
            return DOES_NOT_EXIST;
        else if(!vo.isEnabled())
            return OPERATION_SUCCESSFUL;
        else
            return OPERATION_NO_CHANGE;
    }

    /**
     * Would stop a Data Point via its XID
     * @param xid
     * @return -1 if DS DNE, 0 if it was already disabled, 1 if it would be sent to RuntimeManager
     */
    @Override
    public int disableDataPoint(String xid){
        DataPointVO vo = DataPointDao.getInstance().getByXid(xid);
        if(vo == null || !permissionService.hasDataPointSetPermission(permissions, vo))
            return DOES_NOT_EXIST;
        else if(vo.isEnabled())
            return OPERATION_SUCCESSFUL;
        else
            return OPERATION_NO_CHANGE;
    }

    @Override
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
        builder.append("sleep(milliseconds): void, \n");
        builder.append(" }");
        return builder.toString();
    }
}
