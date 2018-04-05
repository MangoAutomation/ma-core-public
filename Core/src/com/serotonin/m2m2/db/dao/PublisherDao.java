/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.ModuleNotLoadedException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.SerializationHelper;

/**
 * @author Matthew Lohbihler
 */
public class PublisherDao extends AbstractDao<PublisherVO<?>> {
	
	public static final PublisherDao instance = new PublisherDao();
	
    static final Log LOG = LogFactory.getLog(PublisherDao.class);

    private PublisherDao(){
    	super(ModuleRegistry.getWebSocketHandlerDefinition(EventType.EventTypeNames.PUBLISHER), AuditEventType.TYPE_PUBLISHER, new TranslatableMessage("internal.monitor.PUBLISHER_COUNT"));
    }
    
    
    public String generateUniqueXid() {
        return generateUniqueXid(PublisherVO.XID_PREFIX, SchemaDefinition.PUBLISHERS_TABLE);
    }

    public boolean isXidUnique(String xid, int excludeId) {
        return isXidUnique(xid, excludeId, SchemaDefinition.PUBLISHERS_TABLE);
    }

    private static final String PUBLISHER_SELECT = "select id, xid, publisherType, data from publishers ";

    public List<PublisherVO<? extends PublishedPointVO>> getPublishers() {
        return query(PUBLISHER_SELECT, new PublisherExtractor());
    }

    public List<PublisherVO<? extends PublishedPointVO>> getPublishers(Comparator<PublisherVO<?>> comparator) {
        List<PublisherVO<? extends PublishedPointVO>> result = getPublishers();
        Collections.sort(result, comparator);
        return result;
    }

    public static class PublisherNameComparator implements Comparator<PublisherVO<?>> {
        @Override
        public int compare(PublisherVO<?> p1, PublisherVO<?> p2) {
            if (StringUtils.isBlank(p1.getName()))
                return -1;
            return p1.getName().compareTo(p2.getName());
        }
    }

    public PublisherVO<? extends PublishedPointVO> getPublisher(int id) {
        return queryForObject(PUBLISHER_SELECT + " where id=?", new Object[] { id }, new PublisherRowMapper(), null);
    }

    public PublisherVO<? extends PublishedPointVO> getPublisher(String xid) {
        return queryForObject(PUBLISHER_SELECT + " where xid=?", new Object[] { xid }, new PublisherRowMapper(), null);
    }

    class PublisherExtractor implements ResultSetExtractor<List<PublisherVO<? extends PublishedPointVO>>> {
        @Override
        public List<PublisherVO<? extends PublishedPointVO>> extractData(ResultSet rs) throws SQLException,
                DataAccessException {
            PublisherRowMapper rowMapper = new PublisherRowMapper();
            List<PublisherVO<? extends PublishedPointVO>> results = new ArrayList<PublisherVO<? extends PublishedPointVO>>();
            int rowNum = 0;
            while (rs.next()) {
                try {
                    results.add(rowMapper.mapRow(rs, rowNum++));
                }
                catch (ShouldNeverHappenException e) {
                    // If the module was removed but there are still records in the database, this exception will be
                    // thrown. Check the inner exception to confirm.
                    if (e.getCause() instanceof ModuleNotLoadedException) {
                        // Yep. Log the occurrence and continue.
                        LOG.error(
                                "Publisher with type '" + rs.getString("publisherType") + "' and xid '"
                                        + rs.getString("xid") + "' could not be loaded. Is its module missing?", e);
                    }else {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
            return results;
        }
    }

    class PublisherRowMapper implements RowMapper<PublisherVO<? extends PublishedPointVO>> {
        @Override
        @SuppressWarnings("unchecked")
        public PublisherVO<? extends PublishedPointVO> mapRow(ResultSet rs, int rowNum) throws SQLException {
            PublisherVO<? extends PublishedPointVO> p = (PublisherVO<? extends PublishedPointVO>) SerializationHelper
                    .readObjectInContext(rs.getBinaryStream(4));
            p.setId(rs.getInt(1));
            p.setXid(rs.getString(2));
            p.setDefinition(ModuleRegistry.getPublisherDefinition(rs.getString(3)));
            return p;
        }
    }

    public void savePublisher(final PublisherVO<? extends PublishedPointVO> vo) {
        // Decide whether to insert or update.
        if (vo.getId() == Common.NEW_ID){
            vo.setId(ejt.doInsert(
                    "insert into publishers (xid, publisherType, data) values (?,?,?)",
                    new Object[] { vo.getXid(), vo.getDefinition().getPublisherTypeName(),
                            SerializationHelper.writeObject(vo) }, new int[] { Types.VARCHAR, Types.VARCHAR,
                    		Types.BINARY})); //TP Edit Nov 2013 had to change from BINARY to BLOB... Did we upgrade Derby version since this code was last touched?
        	AuditEventType.raiseAddedEvent(AuditEventType.TYPE_PUBLISHER, vo);
        	this.countMonitor.increment();
        }else{
            PublisherVO<?> old = getPublisher(vo.getId());
            ejt.update("update publishers set xid=?, data=? where id=?", new Object[] { vo.getXid(),
                    SerializationHelper.writeObject(vo), vo.getId() }, new int[] { Types.VARCHAR, Types.BINARY,
                    Types.INTEGER });
        	AuditEventType.raiseChangedEvent(AuditEventType.TYPE_PUBLISHER, old, vo);
        }
                    
    }

    public void deletePublisher(final int publisherId) {
    	PublisherVO<?> vo = getPublisher(publisherId);
        final ExtendedJdbcTemplate ejt2 = ejt;
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                ejt2.update("delete from eventHandlersMapping where eventTypeName=? and eventTypeRef1=?", new Object[] {
                        EventType.EventTypeNames.PUBLISHER, publisherId });
                ejt2.update("delete from publishers where id=?", new Object[] { publisherId });
                AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_PUBLISHER, vo);
                countMonitor.decrement();
            }
        });
        
    }

    public void deletePublisherType(final String publisherType) {
        List<Integer> pubIds = queryForList("SELECT id FROM publishers WHERE publisherType=?",
                new Object[] { publisherType }, Integer.class);
        for (Integer pubId : pubIds)
            deletePublisher(pubId);
    }

    public Object getPersistentData(int id) {
        return query("select rtdata from publishers where id=?", new Object[] { id },
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

    public void savePersistentData(int id, Object data) {
        ejt.update("update publishers set rtdata=? where id=?", new Object[] { SerializationHelper.writeObject(data),
                id }, new int[] { Types.BINARY, Types.INTEGER });
    }

    public int countPointsForPublisherType(String publisherType, int excludeId) {
        List<PublisherVO<? extends PublishedPointVO>> publishers = query(PUBLISHER_SELECT + " WHERE publisherType=?",
                new Object[] { publisherType }, new PublisherExtractor());
        int count = 0;
        for (PublisherVO<? extends PublishedPointVO> publisher : publishers) {
            if (publisher.getId() != excludeId)
                count += publisher.getPoints().size();
        }
        return count;
    }


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
	 */
	@Override
	protected String getXidPrefix() {
		return PublisherVO.XID_PREFIX;
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
	 */
	@Override
	public PublisherVO<?> getNewVo() {
        throw new ShouldNeverHappenException("Unable to create generic publisher, must supply a type");
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getTableName()
	 */
	@Override
	protected String getTableName() {
		return SchemaDefinition.PUBLISHERS_TABLE;
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#voToObjectArray(com.serotonin.m2m2.vo.AbstractBasicVO)
	 */
	@Override
	protected Object[] voToObjectArray(PublisherVO<?> vo) {
		return new Object[] { 
				vo.getXid(), 
				vo.getDefinition().getPublisherTypeName(),
                SerializationHelper.writeObjectToArray(vo)};
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
	@Override
	protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
    	LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
    	map.put("id", Types.INTEGER);
    	map.put("xid", Types.VARCHAR);
    	map.put("publisherType", Types.VARCHAR);
    	map.put("data", Types.BINARY);
    	return map;
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, IntStringPair> getPropertiesMap() {
		return new HashMap<>();
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
	 */
	@Override
	public RowMapper<PublisherVO<?>> getRowMapper() {
		return new PublisherRowMapper();
	}
}
