/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.tables.DataSources;
import com.infiniteautomation.mango.db.tables.EventHandlersMapping;
import com.infiniteautomation.mango.db.tables.records.DataSourcesRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.spring.events.StateChangeEvent;
import com.infiniteautomation.mango.spring.events.audit.DeleteAuditEvent;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.usage.DataSourceUsageStatistics;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.util.ILifecycleState;
import com.serotonin.util.SerializationHelper;

@Repository()
public class DataSourceDao extends AbstractVoDao<DataSourceVO, DataSourcesRecord, DataSources> {

    static final Log LOG = LogFactory.getLog(DataSourceDao.class);

    private static final LazyInitSupplier<DataSourceDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(DataSourceDao.class);
    });

    private final EventHandlersMapping eventHandlersMapping = EventHandlersMapping.EVENT_HANDLERS_MAPPING;

    @Autowired
    private DataSourceDao(
            PermissionService permissionService,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME) ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(AuditEventType.TYPE_DATA_SOURCE, DataSources.DATA_SOURCES,
                new TranslatableMessage("internal.monitor.DATA_SOURCE_COUNT"),
                mapper, publisher, permissionService);
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static DataSourceDao getInstance() {
        return springInstance.get();
    }

    /**
     * Get all data sources for a given type
     * @param type
     * @return
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
     * @param dataSourceType
     */
    public void deleteDataSourceType(final String dataSourceType) {
        List<DataSourceVO> dss = getDataSourcesForType(dataSourceType);
        for(DataSourceVO ds : dss) {
            delete(ds);
        }
    }

    /**
     * Get runtime data
     * @param id
     * @return
     */
    public Object getPersistentData(int id) {
        return query("select rtdata from dataSources where id=?", new Object[] { id },
                new ResultSetExtractor<Serializable>() {
            @Override
            public Serializable extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (!rs.next())
                    return null;

                InputStream in = rs.getBinaryStream(1);
                if (in == null)
                    return null;

                return (Serializable) SerializationHelper.readObjectInContext(in);
            }
        });
    }

    /**
     * Save runtime data
     * @param id
     * @param data
     */
    public void savePersistentData(int id, Object data) {
        ejt.update("UPDATE dataSources SET rtdata=? WHERE id=?", new Object[] { SerializationHelper.writeObject(data),
                id }, new int[] { Types.BINARY, Types.INTEGER });
    }

    /**
     * Get the count of data sources per type
     * @return
     */
    public List<DataSourceUsageStatistics> getUsage() {
        return ejt.query("SELECT dataSourceType, COUNT(dataSourceType) FROM dataSources GROUP BY dataSourceType", new RowMapper<DataSourceUsageStatistics>() {
            @Override
            public DataSourceUsageStatistics mapRow(ResultSet rs, int rowNum) throws SQLException {
                DataSourceUsageStatistics usage = new DataSourceUsageStatistics();
                usage.setDataSourceType(rs.getString(1));
                usage.setCount(rs.getInt(2));
                return usage;
            }
        });
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
        .where(eventHandlersMapping.eventTypeName.eq(EventType.EventTypeNames.DATA_SOURCE),
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
     * @param dataSourceId
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
