/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final LazyInitSupplier<RoleDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(RoleDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (RoleDao)o;
    });
    
    private RoleDao() {
        super(AuditEventType.TYPE_ROLE, "u",
                new String[0], false,
                new TranslatableMessage("internal.monitor.ROLE_COUNT"));
    }
    
    /**
     * Get the roles for a given permission type regardless of the VOs if any 
     *  that are linked to it.
     * @param permissionType
     * @return
     */
    public List<RoleVO> getRoles(String permissionType) {
        return query("", new Object[] {permissionType}, getRowMapper());
    }
    
    /**
     * Get the roles for a given VO
     * @param vo
     * @return
     */
    public List<RoleVO> getRoles(AbstractVO<?> vo, String permissionType) {
        return query("", new Object[] {vo.getId(), vo.getClass().getSimpleName(), permissionType}, getRowMapper());
    }

    /**
     * Add a role to the given vo's permission type
     * @param role
     * @param vo
     * @param permissionType
     */
    public void addRoleToVoPermission(RoleVO role, AbstractVO<?> vo, String permissionType) {
        doInsert("INSERT INTO roleMappings (roleId, voId, voType, permissionType) VALUES (?,?,?,?)", 
                new Object[]{
                        role.getId(),
                        vo.getId(),
                        vo.getClass().getSimpleName(),
                        permissionType,
                });
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
}
