/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.RoleVO;

/**
 * @author Terry Packer
 *
 */
@Repository
public class RoleDao extends AbstractDao<RoleVO> {

    public static final String SUPERADMIN_ROLE_NAME = "superadmin";
    public static final String USER_ROLE_NAME = "user";
    
    private static final LazyInitSupplier<RoleDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(RoleDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (RoleDao)o;
    });
    
    private final LazyInitSupplier<RoleVO> superadminRole = new LazyInitSupplier<>(() -> {
       return queryForObject(SELECT_ALL + " WHERE xid=?", 
               new Object[] {SUPERADMIN_ROLE_NAME}, 
               getRowMapper()); 
    });
    
    private final LazyInitSupplier<RoleVO> userRole = new LazyInitSupplier<>(() -> {
        return queryForObject(SELECT_ALL + " WHERE xid=?", 
                new Object[] {USER_ROLE_NAME}, 
                getRowMapper()); 
     });
    
    private RoleDao() {
        super(AuditEventType.TYPE_ROLE, "r",
                new String[0], false,
                new TranslatableMessage("internal.monitor.ROLE_COUNT"));
    }
    
    /**
     * Get the roles for a given permission type regardless of the VOs if any 
     *  that are linked to it.
     * @param permissionType
     * @return
     */
    public Set<RoleVO> getRoles(String permissionType) {
        return query(SELECT_ALL + " JOIN roleMappings rm ON rm.roleId=r.id WHERE rm.permissionType=?", 
                new Object[] {permissionType}, 
                new RoleVoSetResultSetExtractor());
    }
    
    /**
     * Get the roles for a given VO
     * @param vo
     * @return
     */
    public Set<RoleVO> getRoles(AbstractVO<?> vo, String permissionType) {
        return query(SELECT_ALL + " JOIN roleMappings rm ON rm.roleId=r.id WHERE rm.voId=? AND rm.voType=? AND rm.permissionType=?",
                new Object[] {vo.getId(), vo.getClass().getSimpleName(), permissionType}, 
                new RoleVoSetResultSetExtractor());
    }

    private static final String INSERT_VO_ROLE_MAPPING = "INSERT INTO roleMappings (roleId, voId, voType, permissionType) VALUES (?,?,?,?)";
    /**
     * Add a role to the given vo's permission type
     * @param role
     * @param vo
     * @param permissionType
     */
    public void addRoleToVoPermission(RoleVO role, AbstractVO<?> vo, String permissionType) {
        doInsert(INSERT_VO_ROLE_MAPPING, 
                new Object[]{
                        role.getId(),
                        vo.getId(),
                        vo.getClass().getSimpleName(),
                        permissionType,
                });
    }
    
    /**
     * Replace all roles for a vo's given permission type.
     *   NOTE this should be used in a transaction and the RoleVO ids are not set
     * @param roles - non null set of roles
     * @param vo
     * @param permissionType
     */
    public void replaceRolesOnVoPermission(Set<RoleVO> roles, AbstractVO<?> vo, String permissionType) {
        //Delete em all
        ejt.update("DELETE FROM roleMappings WHERE voId=? AND voType=? AND permissionType=?", 
                new Object[]{
                        vo.getId(),
                        vo.getClass().getSimpleName(),
                        permissionType,
                });
        //Push the new ones in
        List<RoleVO> entries = new ArrayList<>(roles);
        ejt.batchUpdate(INSERT_VO_ROLE_MAPPING, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return entries.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RoleVO role = entries.get(i);
                ps.setInt(1, role.getId());
                ps.setInt(2, vo.getId());
                ps.setString(3, vo.getClass().getSimpleName());
                ps.setString(4, permissionType);
            }
        });
    }

    /**
     * Get the superadmin role
     * @return
     */
    public RoleVO getSuperadminRole() {
        return superadminRole.get();
    }
    
    public RoleVO getUserRole() {
        return userRole.get();
    }
    
    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static RoleDao getInstance() {
        return springInstance.get();
    }

    @Override
    protected String getXidPrefix() {
        return RoleVO.XID_PREFIX;
    }

    @Override
    public RoleVO getNewVo() {
        return new RoleVO();
    }

    @Override
    protected String getTableName() {
        return SchemaDefinition.ROLES_TABLE;
    }

    @Override
    protected Object[] voToObjectArray(RoleVO vo) {
        return new Object[] {
                vo.getXid(),
                vo.getName()
        };
    }

    @Override
    protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("id", Types.INTEGER);
        map.put("xid", Types.VARCHAR);
        map.put("name", Types.VARCHAR);
        return map;
    }

    @Override
    protected Map<String, IntStringPair> getPropertiesMap() {
        HashMap<String, IntStringPair> map = new HashMap<String, IntStringPair>();
        return map;
    }

    @Override
    public RowMapper<RoleVO> getRowMapper() {
        return new RoleRowMapper();
    }

    class RoleRowMapper implements RowMapper<RoleVO> {
        @Override
        public RoleVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            RoleVO vo = new RoleVO();
            int i = 0;
            vo.setId(rs.getInt(++i));
            vo.setXid(rs.getString(++i));
            vo.setName(rs.getString(++i));
            return vo;
        }
    }

    /**
     * Extract the roles into an un-modifiable set
     * @author Terry Packer
     *
     */
    private class RoleVoSetResultSetExtractor implements ResultSetExtractor<Set<RoleVO>> {

        private final RoleRowMapper rowMapper;
        
        public RoleVoSetResultSetExtractor() {
            this.rowMapper = new RoleRowMapper();
        }
        
        @Override
        public Set<RoleVO> extractData(ResultSet rs) throws SQLException, DataAccessException {
            Set<RoleVO> results = new HashSet<>();
            int rowNum = 0;
            while (rs.next()) {
                results.add(this.rowMapper.mapRow(rs, rowNum++));
            }
            return Collections.unmodifiableSet(results);
        }
        
    }

}
