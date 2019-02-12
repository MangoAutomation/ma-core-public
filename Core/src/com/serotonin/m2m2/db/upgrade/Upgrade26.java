/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.RowCallbackHandler;

import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * 3.6.0 Schema Update
 * - Add permissions to mailing lists
 * - Upgrade data source event type mappings to use subtype
 * 
 *
 * @author Terry Packer
 */
public class Upgrade26 extends DBUpgrade {
    
    @Override
    protected void upgrade() throws Exception {
        upgradeEventTypes();
        
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), sql);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), sql);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sql);
        runScript(scripts);

    }

    @Override
    protected String getNewSchemaVersion() {
        return "27";
    }
    
    private String[] sql = new String[]{
            "ALTER TABLE mailingLists ADD COLUMN readPermission varchar(255);",
            "ALTER TABLE mailingLists ADD COLUMN editPermission varchar(255);"
    };
    
    private String[] mssql = new String[]{
            "ALTER TABLE mailingLists ADD COLUMN readPermission nvarchar(255);",
            "ALTER TABLE mailingLists ADD COLUMN editPermission nvarchar(255);"
    };
    
    private void upgradeEventTypes() {
        
        String selectEventMappings = "SELECT eventTypeRef1, eventTypeRef2 FROM eventHandlersMapping WHERE eventTypeName=?";
        String updateMapping = "UPDATE eventHandlersMapping SET eventSubtypeName=? WHERE eventTypeName=? AND eventTypeRef1=? AND eventTypeRef2=?"; 
        String selectDsType = "SELECT dataSourceType FROM dataSources WHERE id=?";
        ejt.query(selectEventMappings, new Object[] {EventType.EventTypeNames.DATA_SOURCE}, new RowCallbackHandler() {

            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int dataSourceId = rs.getInt(1);
                int eventTypeId = rs.getInt(2);
                String type = ejt.queryForObject(selectDsType, new Object[] {dataSourceId}, String.class, null);
                if(type != null) {
                    DataSourceDefinition def = ModuleRegistry.getDataSourceDefinition(type);
                    if(def != null) {
                        DataSourceVO<?> vo = def.baseCreateDataSourceVO();
                        for(EventTypeVO et : vo.getEventTypes()){
                            if(et.getEventType().getReferenceId2() == eventTypeId) {
                                //Update the row
                                ejt.update(updateMapping, new Object[] {et.getEventType().getEventSubtype(), EventType.EventTypeNames.DATA_SOURCE, dataSourceId, eventTypeId});
                                break;
                            }
                        }
                    }
                }else {
                    //This data source is missing should we delete this mapping?
                }
            }
        });
    }
}
