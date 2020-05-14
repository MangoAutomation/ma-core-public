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

import org.jooq.Query;
import org.jooq.Select;
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
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.permission.MangoPermission.MangoPermissionEncoded;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
import com.infiniteautomation.mango.util.LazyInitSupplier;
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
        return Common.getRuntimeContext().getBean(RoleDao.class);
    });

    private final PermissionDao permissionDao;

    @Autowired
    private RoleDao(RoleTableDefinition table,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher,
            PermissionDao permissionDao) {
        super(AuditEventType.TYPE_ROLE,
                table,
                new TranslatableMessage("internal.monitor.ROLE_COUNT"),
                mapper, publisher);
        this.permissionDao = permissionDao;
    }

    private final String SELECT_ROLE_MAPPING = "SELECT r.id,r.xid,rm.mask FROM roles AS r JOIN roleMappings rm ON rm.roleId=r.id ";

    /**
     * Get the MangoPermission regardless of the VOs if any
     *  that are linked to it.
     * @param permissionType
     * @return
     */
    public MangoPermission getPermission(String permissionType) {
        List<MangoPermissionEncoded> encoded = query(SELECT_ROLE_MAPPING + "WHERE rm.permissionType=?",
                new Object[] {permissionType},
                new MangoPermissionEncodedRowMapper());
        return MangoPermission.decode(encoded);
    }

    public MangoPermission getPermission(AbstractBasicVO vo, String permissionType) {
        return getPermission(vo.getId(), vo.getClass().getSimpleName(), permissionType);
    }

    /**
     * Get the Permission for a given vo based on the provided information
     * @param voId
     * @param voClassSimpleName
     * @param permissionType
     * @return
     */
    public MangoPermission getPermission(int voId, String voClassSimpleName, String permissionType) {
        List<MangoPermissionEncoded> encoded = query(SELECT_ROLE_MAPPING + "WHERE rm.voId=? AND rm.voType=? AND rm.permissionType=?",
                new Object[] {voId, voClassSimpleName, permissionType},
                new MangoPermissionEncodedRowMapper());
        return MangoPermission.decode(encoded);
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

    @Override
    public void deletePostRelationalData(RoleVO vo) {
        permissionDao.roleUnlinked();
    }

    @Override
    public void loadRelationalData(RoleVO vo) {
        vo.setInherited(getInherited(vo.getId()));
    }

    @Override
    public void saveRelationalData(RoleVO vo, boolean insert) {
        if(!insert) {
            //Drop the mappings
            this.create.deleteFrom(RoleTableDefinition.roleInheritanceTable).where(RoleTableDefinition.roleIdField.eq(vo.getId())).execute();
        }

        if(vo.getInherited() != null && vo.getInherited().size() > 0) {
            List<Query> inserts = new ArrayList<>();
            for(Role role : vo.getInherited()) {
                inserts.add(this.create.insertInto(RoleTableDefinition.roleInheritanceTable).columns(
                        RoleTableDefinition.roleIdField,
                        RoleTableDefinition.roleInheritanceTableInheritedRoleIdField).values(vo.getId(), role.getId()));
            }
            create.batch(inserts).execute();
        }
    }

    private static final String INSERT_VO_ROLE_MAPPING = "INSERT INTO roleMappings (roleId, voId, voType, permissionType, mask) VALUES (?,?,?,?,?)";

    /**
     * Delete all existing and create all new mappings
     * @param roles
     * @param permissionType
     */
    public MangoPermission replaceRolesOnPermission(Set<Set<Role>> roles, String permissionType) {
        MangoPermission newPermission = new MangoPermission(roles);
        doInTransaction((status) -> {
            ejt.update("DELETE FROM roleMappings WHERE voId IS null AND voType IS null AND permissionType=?",
                    new Object[]{permissionType});

            List<MangoPermissionEncoded> encoded = newPermission.encode();
            ejt.batchUpdate(INSERT_VO_ROLE_MAPPING, new BatchPreparedStatementSetter() {
                @Override
                public int getBatchSize() {
                    return encoded.size();
                }

                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MangoPermissionEncoded r = encoded.get(i);
                    ps.setInt(1, r.getRole().getId());
                    ps.setNull(2, java.sql.Types.INTEGER);
                    ps.setString(3, null);
                    ps.setString(4, permissionType);
                    ps.setLong(5, r.getMask());
                }
            });
        });
        return newPermission;
    }

    /**
     * Replace all roles for a vo's given permission type.
     *   NOTE this should be used in a transaction and the RoleVO ids are not set
     * @param permission
     * @param vo
     * @param permissionType
     * @param newVO - is this a new VO
     */
    public void replaceRolesOnVoPermission(MangoPermission permission, AbstractBasicVO vo, String permissionType, boolean newVO) {
        replaceRolesOnVoPermission(permission, vo.getId(), vo.getClass().getSimpleName(), permissionType, newVO);
    }

    /**
     * Replace all roles for a vo's given permission type.
     *   NOTE this should be used in a transaction and the RoleVO ids are not set
     * @param permission
     * @param voId
     * @param classSimpleName
     * @param permissionType
     * @param newVO - is this a new VO
     */
    public void replaceRolesOnVoPermission(MangoPermission permission, int voId, String classSimpleName, String permissionType, boolean newVO) {
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
        List<MangoPermissionEncoded> encoded = permission.encode();
        ejt.batchUpdate(INSERT_VO_ROLE_MAPPING, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return encoded.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MangoPermissionEncoded e = encoded.get(i);
                ps.setInt(1, e.getRole().getId());
                ps.setInt(2, voId);
                ps.setString(3, classSimpleName);
                ps.setString(4, permissionType);
                ps.setLong(5, e.getMask());
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
     * Recursively get a set of all inherited roles of this role
     * @param vo
     * @return
     */
    public Set<Role> getFlatInheritance(Role role){
        Set<Role> all = new HashSet<Role>();
        addInheritance(role, all);
        return all;
    }

    /**
     * Recursively add all inherited roles
     * @param role
     * @param all
     */
    private void addInheritance(Role role, Set<Role> all) {
        Set<Role> inherited = getInherited(role.getId());
        for(Role inheritedRole : inherited) {
            all.add(inheritedRole);
            addInheritance(inheritedRole, all);
        }
    }


    /**
     * Get the inherited roles of this role from the database,
     *  one level deep only.
     * @param vo
     * @return
     */
    private Set<Role> getInherited(int roleId) {
        Select<?> select = this.getSelectQuery(getSelectFields())
                .join(RoleTableDefinition.roleInheritanceTableAsAlias)
                .on(this.table.getIdAlias().eq(RoleTableDefinition.roleInheritanceTableInheritedRoleIdField))
                .where(RoleTableDefinition.roleInheritanceTableRoleIdFieldAlias.eq(roleId));
        List<Object> args = select.getBindValues();
        return query(select.getSQL(), args.toArray(new Object[args.size()]), new RoleSetResultSetExtractor());
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

    class MangoPermissionEncodedRowMapper implements RowMapper<MangoPermissionEncoded> {
        @Override
        public MangoPermissionEncoded mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MangoPermissionEncoded(new Role(rs.getInt(1), rs.getString(2)), rs.getLong(3));
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
