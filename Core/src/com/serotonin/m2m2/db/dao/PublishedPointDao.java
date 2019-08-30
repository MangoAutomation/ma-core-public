/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.infiniteautomation.mango.spring.db.AbstractDatabaseModel;
import com.infiniteautomation.mango.spring.db.DatabaseModelJacksonMapping;
import com.infiniteautomation.mango.spring.db.DatabaseModelMapper;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class PublishedPointDao extends AbstractDao<PublishedPointVO> {

    private static final LazyInitSupplier<PublishedPointDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(PublishedPointDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (PublishedPointDao)o;
    });
    
    public static PublishedPointDao getInstance(){
        return springInstance.get();
    }
    
    private final DatabaseModelMapper mapper;
    
    @Autowired
    private PublishedPointDao(DatabaseModelMapper mapper) {
        super(AuditEventType.TYPE_PUBLISHED_POINT, "pp",
                new String[] { }, //Extra Properties not in table
                false, new TranslatableMessage("internal.monitor.PUBLISHED_POINT_COUNT"));
        this.mapper = mapper;
    }

    @Override
    protected String getXidPrefix() {
        return PublishedPointVO.XID_PREFIX;
    }

    @Override
    public PublishedPointVO getNewVo() {
        throw new ShouldNeverHappenException("Unable to create generic published point, must supply a type");
    }

    @Override
    protected String getTableName() {
        return SchemaDefinition.PUBLISHED_POINTS_TABLE;
    }

    @Override
    protected Object[] voToObjectArray(PublishedPointVO vo) {

        String jsonData = null;
        try {
            PublishedPointDatabaseModel model = mapper.map(vo, PublishedPointDatabaseModel.class);
            jsonData =  getObjectWriter(PublishedPointDatabaseModel.class).writeValueAsString(model);
        }catch(JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
        }
        
        return new Object[] {
                jsonData,
                vo.getXid(),
                vo.getName(),
                boolToChar(vo.isEnabled()),
                vo.getPublisherId(),
                vo.getDataPointId()
        };
    }

    @Override
    protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("id", Types.INTEGER);
        map.put("data", Types.CLOB);
        map.put("xid", Types.VARCHAR);
        map.put("name", Types.VARCHAR);
        map.put("enabled", Types.CHAR);
        map.put("publisherId", Types.INTEGER);
        map.put("dataPointId", Types.INTEGER);
        return map;
    }

    @Override
    protected Map<String, IntStringPair> getPropertiesMap() {
        return null;
    }

    @Override
    public RowMapper<PublishedPointVO> getRowMapper() {
        return rowMapper;
    }
    
    private final PublishedPointRowMapper rowMapper = new PublishedPointRowMapper();
    
    class PublishedPointRowMapper implements RowMapper<PublishedPointVO> {

        @Override
        public PublishedPointVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i = 0;
            try{
                int id = rs.getInt(++i);
                Clob c = rs.getClob(++i);
                if(c != null) {
                    PublishedPointDatabaseModel model = getObjectReader(PublishedPointDatabaseModel.class).readValue(c.getCharacterStream());
                    PublishedPointVO vo = model.toVO();
                    vo.setId(id);
                    vo.setXid(rs.getString(++i));
                    vo.setName(rs.getString(++i));
                    vo.setEnabled(charToBool(rs.getString(++i)));
                    vo.setPublisherId(rs.getInt(++i));
                    vo.setDataPointId(rs.getInt(++i));
                    return vo;
                }else {
                    throw new SQLException("no data for published point");
                }
            }catch(Exception e){
                throw new SQLException(e);
            }
        }

    }
    
    /**
     * Base class for published point database models, the module's point settings 
     *  are stored in settings so that we can version both independently
     * @author Terry Packer
     *
     */
    public static abstract class PublishedPointDatabaseModel extends AbstractDatabaseModel {
        protected PublishedPointSettingsDatabaseModel settings;
        
        public PublishedPointDatabaseModel() { }
        
        public PublishedPointDatabaseModel(PublishedPointVO vo) {
            fromVO(vo);
        }
        
        public PublishedPointSettingsDatabaseModel getSettings() {
            return settings;
        }
        
        public void setSettings(PublishedPointSettingsDatabaseModel settings) {
            this.settings = settings;
        }
        
        /**
         * Convert to a VO
         * @return
         */
        public abstract PublishedPointVO toVO();
        
        /**
         * Convert from a VO
         * @param vo
         */
        public abstract void fromVO(PublishedPointVO vo);
        
    }

    public static class PublishedPointDatabaseModel1 extends PublishedPointDatabaseModel {
        
        private int publisherId;
        private int dataPointId;
        
        public PublishedPointDatabaseModel1() { }
        public PublishedPointDatabaseModel1(PublishedPointVO vo) {
            super(vo);
        }
        
        @Override
        public void fromVO(PublishedPointVO vo) {
            this.publisherId = vo.getPublisherId();
            this.dataPointId = vo.getDataPointId();
        }
        
        @Override
        public PublishedPointVO toVO() {
            PublishedPointVO vo = settings.toVO();
            //TODO set superclass VO settings (enabled, xid etc?)
            vo.setPublisherId(publisherId);
            vo.setDataPointId(dataPointId);
            
            return vo;
        }
        
        @Override
        public String getVersion() {
            return this.getClass().getName();
        }
    }
    
    public abstract static class PublishedPointSettingsDatabaseModel extends AbstractDatabaseModel {
        public PublishedPointSettingsDatabaseModel() { }
        public PublishedPointSettingsDatabaseModel(PublishedPointVO vo) {
            
        }
        /**
         * Return an instance of the published point using the settings in this model
         * @return
         */
        public abstract PublishedPointVO toVO();
    }
    
    @Component
    public static class PublishedPointDatabaseModelMapping1 implements DatabaseModelJacksonMapping<PublishedPointVO, PublishedPointDatabaseModel1> {

        @Override
        public Class<? extends PublishedPointVO> fromClass() {
            return PublishedPointVO.class;
        }

        @Override
        public Class<? extends PublishedPointDatabaseModel1> toClass() {
            return PublishedPointDatabaseModel1.class;
        }

        @Override
        public PublishedPointDatabaseModel1 map(Object from, DatabaseModelMapper mapper) {
            PublishedPointDatabaseModel1 model = new PublishedPointDatabaseModel1((PublishedPointVO)from);
            PublishedPointSettingsDatabaseModel settings = mapper.map(from, PublishedPointSettingsDatabaseModel.class);
            model.setSettings(settings);
            return model;
        }

        @Override
        public String getVersion() {
            return PublishedPointDatabaseModel1.class.getName();
        }
        
    }
    
}
