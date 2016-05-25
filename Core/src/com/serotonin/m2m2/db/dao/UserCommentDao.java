/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.module.definitions.websocket.UserCommentWebSocketDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.UserComment;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.web.taglib.Functions;

/**
 * This class should Extend a class that 
 * doesn't require VO use.
 * 
 * This means we need 
 * 
 * @author Terry Packer
 *
 */
public class UserCommentDao  extends AbstractDao<UserCommentVO>{
	
	public static final UserCommentDao instance = new UserCommentDao();

	private UserCommentDao(){
		super(UserCommentWebSocketDefinition.handler, AuditEventType.TYPE_USER_COMMENT, "uc", 
				new String[]{ "u.username" },
				"left join users u on uc.userId = u.id"
			);
		
		
		LOG = LogFactory.getLog(UserCommentDao.class);
	}
    
    private static final String POINT_COMMENT_SELECT = UserCommentRowMapper.USER_COMMENT_SELECT
            + "where uc.commentType= " + UserComment.TYPE_POINT + " and uc.typeKey=? " + "order by uc.ts";
    
    private static final String EVENT_COMMENT_SELECT = UserCommentRowMapper.USER_COMMENT_SELECT //
            + "where uc.commentType= " + UserComment.TYPE_EVENT //
            + " and uc.typeKey=? " //
            + "order by uc.ts";

    /**
     * Return all comments for a given event
     * @param id
     * @return
     */
    public void getEventComments(int id, MappedRowCallback<UserCommentVO> callback) {
    	query(EVENT_COMMENT_SELECT, new Object[] { id }, new UserCommentVORowMapper(), callback);
    }
    
    private static final String USER_COMMENT_INSERT = //
    "INSERT INTO userComments (userId, commentType, typeKey, ts, commentText) VALUES (?,?,?,?,?)";

    public void insertUserComment(int typeId, int referenceId, UserComment comment) {
        comment.setComment(Functions.truncate(comment.getComment(), 1024));
        ejt.update(USER_COMMENT_INSERT, new Object[] { comment.getUserId(), typeId, referenceId, comment.getTs(),
                comment.getComment() });
    }

    
    @Override 
    public void save(UserCommentVO vo){
    	//We can't use the standard INSERT until we clean up 
    	// the class heirarchy because it expects an ID AUTO INCREMENT Column
        ejt.update(USER_COMMENT_INSERT, new Object[] { 
        		vo.getUserId(), vo.getCommentType(),
        		vo.getReferenceId(), vo.getTs(),
                vo.getComment() });
    	
    	//Not ready yet, need type key and ChangeComparable etc. AuditEventType.raiseAddedEvent(this.typeName, vo);

    }
    
    /**
     * Return all comments for a given point
     * @param dpId
     * @return
     */
    public void getPointComments(int dpId, MappedRowCallback<UserCommentVO> callback){
    	query(POINT_COMMENT_SELECT, new Object[] { dpId }, new UserCommentVORowMapper(), callback);
    }
    
    
    public  class UserCommentVORowMapper implements RowMapper<UserCommentVO> {
        public static final String USER_COMMENT_SELECT = "select uc.userId, u.username, uc.ts, uc.commentText "
                + "from userComments uc left join users u on uc.userId = u.id ";

        public UserCommentVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserCommentVO c = new UserCommentVO();
            c.setUserId(rs.getInt(1));
            c.setTs(rs.getLong(2));
            c.setComment(rs.getString(3));
            c.setCommentType(rs.getInt(4));
            c.setReferenceId(rs.getInt(5));
            c.setUsername(rs.getString(6));
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
		return "";  //N/A so far
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#voToObjectArray(com.serotonin.m2m2.vo.AbstractVO)
	 */
	@Override
	protected Object[] voToObjectArray(UserCommentVO vo) {
		return new Object[]{
				vo.getUserId(),
				vo.getUsername(),
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
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
	@Override
	protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("userId", Types.INTEGER);
		map.put("ts", Types.BIGINT);
		map.put("commentText", Types.VARCHAR);
		map.put("commentType", Types.INTEGER);
		map.put("typeKey", Types.INTEGER);
		return map;
	}
}
