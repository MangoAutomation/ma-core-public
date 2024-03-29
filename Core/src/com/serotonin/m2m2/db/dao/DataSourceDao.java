/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.tables.DataSources;
import com.infiniteautomation.mango.db.tables.EventHandlersMapping;
import com.infiniteautomation.mango.db.tables.records.DataSourcesRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.DaoDependencies;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.spring.events.StateChangeEvent;
import com.infiniteautomation.mango.spring.events.audit.DeleteAuditEvent;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.usage.DataSourceUsageStatistics;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.util.ILifecycleState;
import com.serotonin.util.SerializationHelper;

@Repository()
public class DataSourceDao extends AbstractVoDao<DataSourceVO, DataSourcesRecord, DataSources> {

    static final Logger LOG = LoggerFactory.getLogger(DataSourceDao.class);

    private static final LazyInitSupplier<DataSourceDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(DataSourceDao.class);
    });

    private final EventHandlersMapping eventHandlersMapping = EventHandlersMapping.EVENT_HANDLERS_MAPPING;

    @Autowired
    private DataSourceDao(DaoDependencies dependencies) {
        super(dependencies, AuditEventType.TYPE_DATA_SOURCE, DataSources.DATA_SOURCES,
                new TranslatableMessage("internal.monitor.DATA_SOURCE_COUNT"));
    }

    /**
     * Get cached instance from Spring Context
     */
    public static DataSourceDao getInstance() {
        return springInstance.get();
    }

    /**
     * Get all data sources for a given type
     */
    @SuppressWarnings("unchecked")
    public <T extends DataSourceVO> List<T> getDataSourcesForType(String type) {
        return (List<T>) getJoinedSelectQuery()
                .where(table.dataSourceType.eq(type))
                .fetch(this::mapRecordLoadRelationalData);
    }

    @Override
    public boolean delete(DataSourceVO vo) {
        //Since we are going to delete all the points we will select them for update as well as the data source
        if (vo != null) {
            DataSourceDeletionResult result = new DataSourceDeletionResult();
            int tries = transactionRetries;
            while(tries > 0) {
                try {
                    withLockedRow(vo.getId(), (txStatus) -> {
                        DataPointDao.getInstance().deleteDataPoints(vo.getId());
                        deleteRelationalData(vo);
                        result.deleted = this.create.deleteFrom(table).where(table.id.eq(vo.getId())).execute();
                        deletePostRelationalData(vo);
                    });
                    break;
                }catch(org.jooq.exception.DataAccessException | ConcurrencyFailureException e) {
                    if(tries == 1) {
                        throw e;
                    }
                }
                tries--;
            }

            if(this.countMonitor != null) {
                this.countMonitor.addValue(-result.deleted);
            }

            if(result.deleted > 0) {
                this.publishEvent(createDaoEvent(DaoEventType.DELETE, vo, null));
                publishAuditEvent(new DeleteAuditEvent<DataSourceVO>(this.auditEventType, Common.getUser(), vo));
            }

            return result.deleted > 0;
        }
        return false;
    }

    private class DataSourceDeletionResult {
        private Integer deleted;
    }

    /**
     * Delete all data source for a given type
     *  used during module uninstall
     */
    public void deleteDataSourceType(final String dataSourceType) {
        List<DataSourceVO> dss = getDataSourcesForType(dataSourceType);
        for(DataSourceVO ds : dss) {
            delete(ds);
        }
    }

    /**
     * Get runtime data
     */
    public Object getPersistentData(int id) {
        return create.select(table.rtdata)
                .from(table)
                .where(table.id.eq(id))
                .fetchSingle(record -> {
                    byte[] rtData = record.get(table.rtdata);
                    return SerializationHelper.readObjectFromArray(rtData);
                });
    }

    /**
     * Save runtime data
     */
    public void savePersistentData(int id, Object data) {
        create.update(table)
                .set(table.rtdata, SerializationHelper.writeObjectToArray(data))
                .where(table.id.eq(id))
                .execute();
    }

    /**
     * Get the count of data sources per type
     */
    public List<DataSourceUsageStatistics> getUsage() {
        List<DataSourceUsageStatistics> result = new ArrayList<>();
        create.select(table.dataSourceType, DSL.count(table.dataSourceType))
                .from(table)
                .groupBy(table.dataSourceType)
                .fetch()
                .forEach(record -> {
                    DataSourceUsageStatistics usage = new DataSourceUsageStatistics();
                    usage.setDataSourceType(record.get(table.dataSourceType));
                    usage.setCount(record.get(DSL.count(table.dataSourceType)));
                    result.add(usage);
                });
        return result;
    }

    @Override
    protected void handleMappingException(Exception e, Record record) {
        if (e.getCause() instanceof ModuleNotLoadedException) {
            LOG.error("Data source with xid '" + record.get(table.xid) +
                    "' could not be loaded. Is its module missing?", e.getCause());
        } else {
            LOG.error("Error mapping data source with xid '" + record.get(table.xid) +
                    "' from SQL record", e.getCause());
        }
    }

    @Override
    protected String getXidPrefix() {
        return DataSourceVO.XID_PREFIX;
    }

    @Override
    protected Record toRecord(DataSourceVO vo) {
        Record record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.name, vo.getName());
        record.set(table.dataSourceType, vo.getDefinition().getDataSourceTypeName());
        record.set(table.data, SerializationHelper.writeObjectToArray(vo));
        record.set(table.jsonData, convertData(vo.getData()));
        record.set(table.readPermissionId, vo.getReadPermission().getId());
        record.set(table.editPermissionId, vo.getEditPermission().getId());
        return record;
    }

    @Override
    public DataSourceVO mapRecord(Record record) {
        DataSourceVO ds = (DataSourceVO) SerializationHelper.readObjectInContextFromArray(record.get(table.data));
        ds.setId(record.get(table.id));
        ds.setXid(record.get(table.xid));
        ds.setName(record.get(table.name));
        ds.setDefinition(ModuleRegistry.getDataSourceDefinition(record.get(table.dataSourceType)));
        ds.setData(extractDataFromObject(record.get(table.jsonData)));
        ds.setReadPermission(new MangoPermission(record.get(table.readPermissionId)));
        ds.setEditPermission(new MangoPermission(record.get(table.editPermissionId)));
        return ds;
    }

    @Override
    public void savePreRelationalData(DataSourceVO existing, DataSourceVO vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);

        MangoPermission editPermission = permissionService.findOrCreate(vo.getEditPermission());
        vo.setEditPermission(editPermission);

        vo.getDefinition().savePreRelationalData(existing, vo);
    }

    @Override
    public void saveRelationalData(DataSourceVO existing, DataSourceVO vo) {
        vo.getDefinition().saveRelationalData(existing, vo);
        if(existing != null) {
            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionService.deletePermissions(existing.getReadPermission());
            }
            if(!existing.getEditPermission().equals(vo.getEditPermission())) {
                permissionService.deletePermissions(existing.getEditPermission());
            }
        }
    }

    @Override
    public void loadRelationalData(DataSourceVO vo) {
        //Populate permissions
        vo.setReadPermission(permissionService.get(vo.getReadPermission().getId()));
        vo.setEditPermission(permissionService.get(vo.getEditPermission().getId()));
        vo.getDefinition().loadRelationalData(vo);
    }


    @Override
    public void deleteRelationalData(DataSourceVO vo) {
        create.deleteFrom(eventHandlersMapping)
        .where(eventHandlersMapping.eventTypeName.eq(EventTypeNames.DATA_SOURCE),
                eventHandlersMapping.eventTypeRef1.eq(vo.getId())).execute();

        vo.getDefinition().deleteRelationalData(vo);
    }

    @Override
    public void deletePostRelationalData(DataSourceVO vo) {
        //Clean permissions
        MangoPermission readPermission = vo.getReadPermission();
        MangoPermission editPermission = vo.getEditPermission();
        permissionService.deletePermissions(readPermission, editPermission);

        vo.getDefinition().deletePostRelationalData(vo);
    }

    /**
     * Get the read permission ID for in memory checks
     * @return permission id or null
     */
    public Integer getReadPermissionId(int dataSourceId) {
        return this.create.select(table.readPermissionId).from(table)
                .where(table.id.eq(dataSourceId))
                .fetchOneInto(Integer.class);
    }

    public void notifyStateChanged(DataSourceVO vo, ILifecycleState state) {
        eventPublisher.publishEvent(new StateChangeEvent<>(this, state, vo));
    }

}
