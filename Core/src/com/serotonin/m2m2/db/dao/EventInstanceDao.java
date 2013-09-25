/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DeltamationCommon;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.UserComment;
import com.serotonin.m2m2.vo.event.EventInstanceVO;

/**
 * @author Terry Packer
 *
 */
public class EventInstanceDao extends AbstractDao<EventInstanceVO> {

	public static final EventInstanceDao instance = new EventInstanceDao();
	
	/**
	 * @param typeName
	 */
	private EventInstanceDao() {
		super(null,"evt",
				new String[]{
					"u.username",
					"(select count(1) from userComments where commentType=" + UserComment.TYPE_EVENT +" and typeKey=evt.id) as cnt ",
					"ue.silenced"},
				"left join users u on evt.ackUserId=u.id left join userEvents ue on evt.id=ue.eventId ");
		LOG = LogFactory.getLog(EventInstanceDao.class);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getTableName()
	 */
	@Override
	protected String getTableName() {
		return SchemaDefinition.EVENTS_TABLE;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
	 */
	@Override
	protected String getXidPrefix() {
		return null; //No XIDs
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#voToObjectArray(com.serotonin.m2m2.vo.AbstractVO)
	 */
	@Override
	protected Object[] voToObjectArray(EventInstanceVO vo) {
		return new Object[]{
				vo.getId(),
				
		};
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
	 */
	@Override
	public EventInstanceVO getNewVo() {
		return new EventInstanceVO();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getProperties()
	 */
	@Override
	protected List<String> getProperties() {
		
		return Arrays.asList(
				"id",
				"typeName",
				"subtypeName",
				"typeRef1",
				"typeRef2",
				"activeTs",
				"rtnApplicable",
				"rtnTs",
				"rtnCause",
				"alarmLevel",
				"message",
				"ackTs",
				"ackUserId",
				"alternateAckSource"
		);
		
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, String> getPropertiesMap() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("activeTimestamp", "evt.activeTs");
		map.put("activeTimestampString", "evt.activeTs");
		map.put("rtnTimestampString", "evt.rtnTs");
		map.put("totalTimeString", "IF(evt.rtnTs is null,IF(evt.rtnApplicable='Y',(? - evt.activeTs),-1),IF(evt.rtnApplicable='Y',(evt.rtnTs - evt.activeTs),-1))");
		map.put("messageString", "evt.message");
		map.put("rtnTimestampString", "evt.rtnTs");
		map.put("userNotified", "ue.silenced");
		return map;
	}

	protected Map<String, PropertyArguments> getPropertyArgumentsMap(){
		Map<String,PropertyArguments> map = new HashMap<String,PropertyArguments>();
		map.put("totalTimeString", new PropertyArguments(){
			public Object[] getArguments(){
				return new Object[]{new Date().getTime()};
			}
			});
		
		return map;
	}
	
	@Override
	protected Map<String, Comparator<EventInstanceVO>> getComparatorMap() {
		HashMap<String,Comparator<EventInstanceVO>> comparatorMap = new HashMap<String,Comparator<EventInstanceVO>>();
		
//		comparatorMap.put("messageString", new Comparator<EventInstanceVO>(){
//			public int compare(EventInstanceVO lhs, EventInstanceVO rhs){
//				return lhs.getMessageString().compareTo(rhs.getMessageString());
//			}
//		});

//		comparatorMap.put("totalTimeString", new Comparator<EventInstanceVO>(){
//			public int compare(EventInstanceVO lhs, EventInstanceVO rhs){
//				return lhs.getTotalTime().compareTo(rhs.getTotalTime());
//			}
//		});

		
		return comparatorMap;
	}

	@Override
	protected Map<String, IFilter<EventInstanceVO>> getFilterMap(){
		HashMap<String, IFilter<EventInstanceVO>> filterMap = new HashMap<String,IFilter<EventInstanceVO>>();
		
		filterMap.put("messageString", new IFilter<EventInstanceVO>(){
			
			private String regex;
			@Override
			public boolean filter(EventInstanceVO vo) {
				return !vo.getMessageString().matches(regex);
			}

			@Override
			public void setFilter(Object matches) {
				this.regex = "(?i)"+(String)matches;
				
			}
			
		});
		
		filterMap.put("rtnTimestampString", new IFilter<EventInstanceVO>(){
			
			private String regex;
			@Override
			public boolean filter(EventInstanceVO vo) {
				String rtnTimestampString;
				if(vo.isActive())
					rtnTimestampString = Common.translate("common.active");
				else if(!vo.isRtnApplicable())
					rtnTimestampString = Common.translate("common.nortn");
				else
					rtnTimestampString = vo.getRtnTimestampString() + " - " + vo.getRtnMessageString();
				
				return !rtnTimestampString.matches(regex);
			}

			@Override
			public void setFilter(Object matches) {
				this.regex = "(?i)"+(String)matches;
			}
			
		});
		
		
		filterMap.put("totalTimeString", new IFilter<EventInstanceVO>(){
			
			private Long duration;
			private int operator;
			
			@Override
			public boolean filter(EventInstanceVO vo) {
				//Remember filter means to remove from list if true
				if(operator == 1){
					return vo.getTotalTime() < duration;
				}else if (operator == 2){
					return vo.getTotalTime() > duration; 
				}else{
					return !vo.getTotalTime().equals(duration);
				}
			}

			@Override
			public void setFilter(Object matches) {
				String condition = (String)matches;
		    	//Parse the value as Duration:operatorvalue - Duration:>1:00:00
            	String durationString = condition.substring(10,condition.length());
            	String compare = condition.substring(9, 10);
            	this.duration = DeltamationCommon.unformatDuration(durationString);
            	
            	if(compare.equals(">")){
            		operator = 1;
            	}else if(compare.equals("<")){
            		operator = 2;
            	}else if(compare.equals("=")){
            		operator = 3; 
            	}
			}
			
		});
		
		
		
		return filterMap;
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
	 */
	@Override
	public RowMapper<EventInstanceVO> getRowMapper() {
		return new UserEventInstanceVORowMapper();
	}
	
    public static class EventInstanceVORowMapper implements RowMapper<EventInstanceVO> {
        @Override
        public EventInstanceVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventInstanceVO event = new EventInstanceVO();
            event.setId(rs.getInt(1));

            EventType type = createEventType(rs, 2);
            event.setEventType(type);
            event.setActiveTimestamp(rs.getLong(6));
            event.setRtnApplicable(charToBool(rs.getString(7)));
            event.setAlarmLevel(rs.getInt(10));
            TranslatableMessage message = BaseDao.readTranslatableMessage(rs, 11);
            if(message == null)
            	event.setMessage(new TranslatableMessage("common.noMessage"));
            else
            	event.setMessage(message);
            /*
             * IF(evt.rtnTs=null,
             * 		IF(evt.rtnApplicable='Y',
             * 			(NOW() - evt.activeTs),
             * 			-1),
             * 		IF(evt.rtnApplicable='Y',
             * 			(evt.rtnTs - evt.activeTs),
             * 			-1)
             *  )
             */
            //Set the Return to normal
            long rtnTs = rs.getLong(8);
            if (!rs.wasNull()){
            	//if(event.isActive()){ Probably don't need this
            		event.setRtnTimestamp(rtnTs);
            		event.setRtnCause(rs.getInt(9));
            	//}
            	if(event.isRtnApplicable()){
            		event.setTotalTime(rtnTs - event.getActiveTimestamp());
            	}else{
            		event.setTotalTime(-1L);
            	}            		
            }else{
            	if(event.isRtnApplicable()){
            		//Has not been acknowledged yet
            		Date now = new Date();
            		event.setTotalTime(now.getTime() -event.getActiveTimestamp());
            	}else{
            		//Won't ever be
            		event.setTotalTime(-1L);
            	}
            }
            
            long ackTs = rs.getLong(12);
            if (!rs.wasNull()) {
            	//Compute total time
                event.setAcknowledgedTimestamp(ackTs);
                event.setAcknowledgedByUserId(rs.getInt(13));
                if (!rs.wasNull())
                    event.setAcknowledgedByUsername(rs.getString(15));
                event.setAlternateAckSource(BaseDao.readTranslatableMessage(rs, 14));
            }
            
            event.setHasComments(rs.getInt(16) > 0);         
            
            //This makes another query!
            this.attachRelationalInfo(event);
            
            
            return event;
        }
        
        private static final String EVENT_COMMENT_SELECT = UserCommentRowMapper.USER_COMMENT_SELECT //
                + "where uc.commentType= " + UserComment.TYPE_EVENT //
                + " and uc.typeKey=? " //
                + "order by uc.ts";

        void attachRelationalInfo(EventInstanceVO event) {
            if (event.isHasComments())
                event.setEventComments(EventInstanceDao.instance.query(EVENT_COMMENT_SELECT, new Object[] { event.getId() },
                        new UserCommentRowMapper()));
        }

        
    }
	
    class UserEventInstanceVORowMapper extends EventInstanceVORowMapper {
        @Override
        public EventInstanceVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventInstanceVO event = super.mapRow(rs, rowNum);
            event.setSilenced(charToBool(rs.getString(17)));
            if (!rs.wasNull())
                event.setUserNotified(true);
            return event;
        }
    }
    

    
    
    
	/**
	 * @param userId
	 * @param level 
	 * @return
	 */
	public EventInstanceVO getHighestUnsilencedEvent(int userId, int level) {
//        return ejt.queryForObject(SELECT_ALL
//                + "where ue.silenced=? and ue.userId=? and evt.alarmLevel=? ORDER BY evt.activeTs DESC LIMIT 1", new Object[] { boolToChar(false), userId, level },getRowMapper(), null);
        return ejt.queryForObject(SELECT_ALL
                + "where ue.silenced=? and ue.userId=? and evt.alarmLevel=? ORDER BY evt.activeTs DESC", new Object[] { boolToChar(false), userId, level },getRowMapper(), null);

	}

	/**
	 * @param userId
	 * @return
	 */
	public List<EventInstanceVO> getUnsilencedEvents(int userId) {
        return ejt.query(SELECT_ALL
                + "where ue.silenced=? and ue.userId=?", new Object[] { boolToChar(false), userId },getRowMapper());

	}
	
	
	
    static EventType createEventType(ResultSet rs, int offset) throws SQLException {
        String typeName = rs.getString(offset);
        String subtypeName = rs.getString(offset + 1);
        EventType type;
        if (typeName.equals(EventType.EventTypeNames.DATA_POINT))
            type = new DataPointEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventType.EventTypeNames.DATA_SOURCE))
            type = new DataSourceEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventType.EventTypeNames.SYSTEM))
            type = new SystemEventType(subtypeName, rs.getInt(offset + 2));
        else if (typeName.equals(EventType.EventTypeNames.PUBLISHER))
            type = new PublisherEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventType.EventTypeNames.AUDIT))
            type = new AuditEventType(subtypeName, rs.getInt(offset + 2));
        else {
            EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(typeName);
            if (def == null)
                throw new ShouldNeverHappenException("Unknown event type: " + typeName);
            type = def.createEventType(subtypeName, rs.getInt(offset + 2), rs.getInt(offset + 3));
            if (type == null)
                throw new ShouldNeverHappenException("Unknown event type: " + typeName);
        }
        return type;
    }

    
}
