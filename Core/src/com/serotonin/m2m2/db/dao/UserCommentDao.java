/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import com.infiniteautomation.mango.db.query.JoinClause;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;

/**
 * We don't use XIDs for comments yet.
 * 
 * @author Terry Packer
 *
 */
public class UserCommentDao  extends AbstractDao<UserCommentVO>{
	
	public static final UserCommentDao instance = new UserCommentDao();
	
	private UserCommentDao(){
		super(ModuleRegistry.getWebSocketHandlerDefinition("USER_COMMENT"), AuditEventType.TYPE_USER_COMMENT, "uc", 
				new String[]{ "u.username" });
		LOG = LogFactory.getLog(UserCommentDao.class);
	}

	public static final String USER_COMMENT_SELECT = "select uc.id, uc.xid, uc.userId, uc.ts, uc.commentText, uc.commentType, uc.typeKey, u.username "
            + "from userComments uc left join users u on uc.userId = u.id ";

    private static final String POINT_COMMENT_SELECT = USER_COMMENT_SELECT
            + "where uc.commentType= " + UserCommentVO.TYPE_POINT + " and uc.typeKey=? " + "order by uc.ts";
    
    private static final String EVENT_COMMENT_SELECT = USER_COMMENT_SELECT //
            + "where uc.commentType= " + UserCommentVO.TYPE_EVENT //
            + " and uc.typeKey=? " //
            + "order by uc.ts";

    private static final String JSON_DATA_COMMENT_SELECT = USER_COMMENT_SELECT
    		+ "where uc.commentType=" + UserCommentVO.TYPE_JSON_DATA 
    		+ " and uc.typeKey=?"
    		+ "order by uc.ts";
    /**
     * Return all comments for a given event
     * @param id
     * @return
     */
    public void getEventComments(int id, MappedRowCallback<UserCommentVO> callback) {
    	query(EVENT_COMMENT_SELECT, new Object[] { id }, new UserCommentVORowMapper(), callback);
    }
    
    /**
     * Return all comments for a given point
     * @param dpId
     * @return
     */
    public void getPointComments(int dpId, MappedRowCallback<UserCommentVO> callback){
    	query(POINT_COMMENT_SELECT, new Object[] { dpId }, new UserCommentVORowMapper(), callback);
    }
    
    /**
     * Return all comments for a given JsonData Store Entry
     * @param jsonDataId
     * @param callback
     */
    public void getJsonDataComments(int jsonDataId, MappedRowCallback<UserCommentVO> callback){
    	query(JSON_DATA_COMMENT_SELECT, new Object[] { jsonDataId }, new UserCommentVORowMapper(), callback);
    }
    
    public  class UserCommentVORowMapper implements RowMapper<UserCommentVO> {

        public UserCommentVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserCommentVO c = new UserCommentVO();
            int i=0;
            c.setId(rs.getInt(++i));
            c.setXid(rs.getString(++i));
            c.setUserId(rs.getInt(++i));
            c.setTs(rs.getLong(++i));
            c.setComment(rs.getString(++i));
            c.setCommentType(rs.getInt(++i));
            c.setReferenceId(rs.getInt(++i));
            c.setUsername(rs.getString(++i));
            return c;
        }
    }

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getTableName()
	 */
	@Override
	protected String getTableName() {
		return SchemaDefinition.USER_COMMENTS_TABLE;
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
	 */
	@Override
	protected String getXidPrefix() {
		return "UC_";
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#voToObjectArray(com.serotonin.m2m2.vo.AbstractVO)
	 */
	@Override
	protected Object[] voToObjectArray(UserCommentVO vo) {
		return new Object[]{
				vo.getXid(),
				vo.getUserId(),
				vo.getTs(),
				vo.getComment(),
				vo.getCommentType(),
				vo.getReferenceId()
		};
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
	 */
	@Override
	public UserCommentVO getNewVo() {
		return new UserCommentVO();
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, IntStringPair> getPropertiesMap() {
		Map<String,IntStringPair> map = new HashMap<String,IntStringPair>();
		map.put("username", new IntStringPair(Types.VARCHAR, "u.username"));
		map.put("referenceId", new IntStringPair(Types.INTEGER, "typeKey"));
		map.put("timestamp", new IntStringPair(Types.BIGINT, "ts"));
		return map;
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
	 */
	@Override
	public RowMapper<UserCommentVO> getRowMapper() {
		return new UserCommentVORowMapper();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getJoins()
	 */
	@Override
	protected List<JoinClause> getJoins() {
    	List<JoinClause> joins = new ArrayList<JoinClause>();
    	joins.add(new JoinClause(LEFT_JOIN, "users", "u", "uc.userId = u.id"));
    	return joins;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPkColumnName()
	 */
	@Override
	public String getPkColumnName() {
		return "id";
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
	@Override
	protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("id", Types.INTEGER);
		map.put("xid", Types.VARCHAR);
		map.put("userId", Types.INTEGER);
		map.put("ts", Types.BIGINT);
		map.put("commentText", Types.VARCHAR);
		map.put("commentType", Types.INTEGER);
		map.put("typeKey", Types.INTEGER);
		return map;
	}
}
