/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.RQLUtils;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.vo.DataPointVO;

import net.jazdw.rql.parser.ASTNode;

/**
 *
 * Scripting Utility to query data points
 *
 * @author Terry Packer
 *
 */
public class DataPointQuery extends ScriptUtility {

    public static final String CONTEXT_KEY = "DataPointQuery";

    private ScriptEngine engine;
    private ScriptPointValueSetter setter;

    @Autowired
    public DataPointQuery(MangoJavaScriptService service, PermissionService permissionService) {
        super(service, permissionService);
    }

    @Override
    public String getContextKey() {
        return DataPointQuery.CONTEXT_KEY;
    }

    @Override
    public void takeContext(ScriptEngine engine, Bindings engineScope,
            ScriptPointValueSetter setter, List<JsonImportExclusion> importExclusions, boolean testRun) {
        this.engine = engine;
        this.setter = setter;
    }

    /**
     * Perform a query on the data points table
     * @param query
     * @return
     */
    public List<DataPointWrapper> query(String query){
        ASTNode root = RQLUtils.parseRQLtoAST(query);
        List<DataPointVO> dataPoints = new ArrayList<>();
        ConditionSortLimitWithTagKeys conditions = (ConditionSortLimitWithTagKeys) DataPointDao.getInstance().rqlToCondition(root, null, null);
        DataPointDao.getInstance().customizedQuery(conditions, permissions, new MappedRowCallback<DataPointVO>() {
            @Override
            public void row(DataPointVO item, int index) {
                dataPoints.add(item);
            }
        });

        List<DataPointWrapper> results = new ArrayList<DataPointWrapper>();

        //Filter on permissions
        DataPointRT rt = null;
        AbstractPointWrapper wrapper = null;

        for(DataPointVO dp : dataPoints){
            //Can we read or write to this point?
            if(permissionService.hasPermission(permissions, dp.getReadPermission())){
                rt = Common.runtimeManager.getDataPoint(dp.getId());
                if(rt != null)
                    wrapper = service.wrapPoint(engine, rt, setter);
                else
                    wrapper = null;
                results.add(new DataPointWrapper(dp, wrapper));
            }
        }
        return results;
    }

    public DataPointWrapper byXid(String xid) {
        DataPointVO dp = DataPointDao.getInstance().getByXid(xid);
        if(dp == null)
            return null;

        if(permissionService.hasPermission(permissions, dp.getReadPermission())) {
            DataPointRT rt = null;
            AbstractPointWrapper wrapper = null;
            rt = Common.runtimeManager.getDataPoint(dp.getId());
            if(rt != null)
                wrapper = service.wrapPoint(engine, rt, setter);
            else
                wrapper = null;
            return new DataPointWrapper(dp, wrapper);
        } else
            return null;

    }

    @Override
    public String getHelp(){
        return toString();
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        builder.append("query(rql): List<DataPointWrapper>()");
        builder.append(" }\n");
        return builder.toString();
    }

}
