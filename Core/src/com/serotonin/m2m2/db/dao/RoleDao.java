/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
@Repository
public class RoleDao extends AbstractDao<RoleVO, RoleTableDefinition> {

    private static final LazyInitSupplier<RoleDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(RoleDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (RoleDao)o;
    });

    @Autowired
    private RoleDao(RoleTableDefinition table,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(AuditEventType.TYPE_ROLE,
                table,
                new TranslatableMessage("internal.monitor.ROLE_COUNT"),
                mapper, publisher);
    }

    private final String SELECT_ROLE = "SELECT id,xid FROM roles AS r ";

    /**
     * Get the roles for a given permission type regardless of the VOs if any
     *  that are linked to it.
     * @param permissionType
     * @return
     */
    public Set<Role> getRoles(String permissionType) {
        return query(SELECT_ROLE + " JOIN roleMappings rm ON rm.roleId=r.id WHERE rm.permissionType=?",
                new Object[] {permissionType},
                new RoleSetResultSetExtractor());
    }

    /**
     * Get the roles for a given VO
     * @param vo
     * @return
     */
    public Set<Role> getRoles(AbstractBasicVO vo, String permissionType) {
        return getRoles(vo.getId(), vo.getClass().getSimpleName(), permissionType);
    }

    /**
     * Get the roles for a given vo based on the provided information
     * @param voId
     * @param voClassSimpleName
     * @param permissionType
     * @return
     */
    public Set<Role> getRoles(int voId, String voClassSimpleName, String permissionType) {
        return query(SELECT_ROLE + " JOIN roleMappings rm ON rm.roleId=r.id WHERE rm.voId=? AND rm.voType=? AND rm.permissionType=?",
                new Object[] {voId, voClassSimpleName, permissionType},
                new RoleSetResultSetExtractor());
    }

    /**
     * Get the roles for a given permission type regardless of the VOs if any
     *  that are linked to it.
     * @param permissionType
     * @return
     */
    public Set<RoleVO> getRoleVOs(String permissionType) {
        String selectAll = getJoinedSelectQuery().getSQL();
        return query(selectAll + " JOIN roleMappings rm ON rm.roleId=r.id WHERE rm.permissionType=?",
                new Object[] {permissionType},
                new RoleVoSetResultSetExtractor());
    }

    /**
     * Get the roles for a given VO
     * @param vo
     * @return
     */
    public Set<RoleVO> getRoleVOs(AbstractBasicVO vo, String permissionType) {
        return getRoleVOs(vo.getId(), vo.getClass().getSimpleName(), permissionType);
    }

    /**
     * Get the role VOs for a given vo based on the provided information
     * @param voId
     * @param voClassSimpleName
     * @param permissionType
     * @return
     */
    public Set<RoleVO> getRoleVOs(int voId, String voClassSimpleName, String permissionType) {
        return query(SELECT_ROLE + " JOIN roleMappings rm ON rm.roleId=r.id WHERE rm.voId=? AND rm.voType=? AND rm.permissionType=?",
                new Object[] {voId, voClassSimpleName, permissionType},
                new RoleVoSetResultSetExtractor());
    }

    @Override
    public boolean delete(RoleVO vo) {
        //First get all mappings so we can publish them in the event
        if(super.delete(vo)) {
            this.eventPublisher.publishEvent(new RoleDeletedDaoEvent(this, vo));
            return true;
        }else {
            return false;
        }
    }

    private static final String INSERT_VO_ROLE_MAPPING = "INSERT INTO roleMappings (roleId, voId, voType, permissionType) VALUES (?,?,?,?)";

    /**
     * Add a role to the given vo's permission type
     * @param role
     * @param vo
     * @param permissionType
     */
    public void addRoleToVoPermission(Role role, AbstractBasicVO vo, String permissionType) {
        doInsert(INSERT_VO_ROLE_MAPPING,
                new Object[]{
                        role.getId(),
                        vo.getId(),
                        vo.getClass().getSimpleName(),
                        permissionType,
        });
    }

    /**
     * Add a role to a system permission
     * @param role
     * @param permissionType
     */
    public void addRoleToPermission(Role role, String permissionType) {
        doInsert(INSERT_VO_ROLE_MAPPING,
                new Object[]{
                        role.getId(),
                        null,
                        null,
                        permissionType,
        });
    }

    /**
     * Remove a role from a system permission
     * @param role
     * @param permissionType
     */
    public void removeRoleFromPermission(Role role, String permissionType) {
        ejt.update("DELETE FROM roleMappings WHERE voId=null AND voType=null AND roleId=? AND permissionType=?",
                new Object[]{
                        role.getId(),
                        permissionType
        });
    }

    /**
     * Delete all existing and create all new mappings
     * @param roles
     * @param permissionType
     */
    public void replaceRolesOnPermission(Set<Role> roles, String permissionType) {
        doInTransaction((status) -> {
            ejt.update("DELETE FROM roleMappings WHERE voId=null AND voType=null AND permissionType=?",
                    new Object[]{permissionType});

            List<Role> rolesList = new ArrayList<>(roles);
            ejt.batchUpdate(INSERT_VO_ROLE_MAPPING, new BatchPreparedStatementSetter() {
                @Override
                public int getBatchSize() {
                    return roles.size();
                }

                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Role r = rolesList.get(i);
                    ps.setInt(1, r.getId());
                    ps.setString(2, null);
                    ps.setString(3, null);
                    ps.setString(4, permissionType);
                }
            });
        });
    }

    /**
     * Replace all roles for a vo's given permission type.
     *   NOTE this should be used in a transaction and the RoleVO ids are not set
     * @param roles
     * @param vo
     * @param permissionType
     * @param newVO - is this a new VO
     */
    public void replaceRolesOnVoPermission(Set<Role> roles, AbstractBasicVO vo, String permissionType, boolean newVO) {
        replaceRolesOnVoPermission(roles, vo.getId(), vo.getClass().getSimpleName(), permissionType, newVO);
    }

    /**
     * Replace all roles for a vo's given permission type.
     *   NOTE this should be used in a transaction and the RoleVO ids are not set
     * @param roles
     * @param voId
     * @param classSimpleName
     * @param permissionType
     * @param newVO - is this a new VO
     */
    public void replaceRolesOnVoPermission(Set<Role> roles, int voId, String classSimpleName, String permissionType, boolean newVO) {
        //Delete em all
        if(!newVO) {
            ejt.update("DELETE FROM roleMappings WHERE voId=? AND voType=? AND permissionType=?",
                    new Object[]{
                            voId,
                            classSimpleName,
                            permissionType,
            });
        }
        //Push the new ones in
        List<Role> entries = new ArrayList<>(roles);
        ejt.batchUpdate(INSERT_VO_ROLE_MAPPING, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return entries.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Role role = entries.get(i);
                ps.setInt(1, role.getId());
                ps.setInt(2, voId);
                ps.setString(3, classSimpleName);
                ps.setString(4, permissionType);
            }
        });
    }

    /**
     * Delete role mappings for a vo permission
     * @param vo
     * @param permissionType
     */
    public void deleteRolesForVoPermission(AbstractBasicVO vo, String permissionType) {
        deleteRolesForVoPermission(vo.getId(), vo.getClass().getSimpleName(), permissionType);
    }

    /**
     * Delete role mappings for a vo permission
     * @param voId
     * @param classSimpleName
     * @param permissionType
     */
    public void deleteRolesForVoPermission(int voId, String classSimpleName, String permissionType) {
        ejt.update("DELETE FROM roleMappings WHERE voId=? AND voType=? AND permissionType=?",
                new Object[]{
                        voId,
                        classSimpleName,
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
    protected Object[] voToObjectArray(RoleVO vo) {
        return new Object[] {
                vo.getXid(),
                vo.getName()
        };
    }

    @Override
    public RowMapper<RoleVO> getRowMapper() {
        return new RoleVORowMapper();
    }

    /**
     * Get a result set extractor for an unmodifiable set of role vos
     * @return
     */
    public RoleVoSetResultSetExtractor getRoleVoSetResultSetExtractor() {
        return new RoleVoSetResultSetExtractor();
    }

    /**
     * Get a result set extractor for an unmodifiable set of roles
     * @return
     */
    public RoleSetResultSetExtractor getRoleSetResultSetExtractor() {
        return new RoleSetResultSetExtractor();
    }

    class RoleVORowMapper implements RowMapper<RoleVO> {
        @Override
        public RoleVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            RoleVO vo = new RoleVO(rs.getInt(1), rs.getString(2), rs.getString(3));
            return vo;
        }
    }

    class RoleRowMapper implements RowMapper<Role> {
        @Override
        public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Role(rs.getInt(1), rs.getString(2));
        }
    }

    /**
     * Extract the roles into an un-modifiable set
     * @author Terry Packer
     *
     */
    private class RoleVoSetResultSetExtractor implements ResultSetExtractor<Set<RoleVO>> {

        private final RoleVORowMapper rowMapper;

        public RoleVoSetResultSetExtractor() {
            this.rowMapper = new RoleVORowMapper();
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

    private class RoleSetResultSetExtractor implements ResultSetExtractor<Set<Role>> {

        private final RoleRowMapper rowMapper;

        public RoleSetResultSetExtractor() {
            this.rowMapper = new RoleRowMapper();
        }

        @Override
        public Set<Role> extractData(ResultSet rs) throws SQLException, DataAccessException {
            Set<Role> results = new HashSet<>();
            int rowNum = 0;
            while (rs.next()) {
                results.add(this.rowMapper.mapRow(rs, rowNum++));
            }
            return Collections.unmodifiableSet(results);
        }
    }

    /**
     * Event to inform what mappings existed when a role was deleted
     * @author Terry Packer
     *
     */
    public static class RoleDeletedDaoEvent extends ApplicationEvent implements PropagatingEvent  {
        private static final long serialVersionUID = 1L;

        private final RoleVO role;

        /**
         *
         * @param dao
         * @param role - the role that was deleted
         * @param mappings - the mappings at the time of deletion
         */
        public RoleDeletedDaoEvent(RoleDao dao, RoleVO role) {
            super(dao);
            this.role = role;
        }

        public RoleVO getRole() {
            return role;
        }


    }
}
