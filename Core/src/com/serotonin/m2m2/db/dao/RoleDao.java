/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.Query;
import org.jooq.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
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
        Set<Role> inherited = Collections.unmodifiableSet(getInherited(vo.getId()).stream().map(r -> r.getRole()).collect(Collectors.toSet()));
        vo.setInherited(inherited);
    }

    @Override
    public void saveRelationalData(RoleVO existing, RoleVO vo) {
        if(existing != null) {
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
        Set<Role> inherited = Collections.unmodifiableSet(getInherited(role.getId()).stream().map(r -> r.getRole()).collect(Collectors.toSet()));
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
    public Set<RoleVO> getInherited(Integer roleId) {
        Select<?> select = this.getSelectQuery(getSelectFields())
                .join(RoleTableDefinition.roleInheritanceTableAsAlias)
                .on(this.table.getIdAlias().eq(RoleTableDefinition.roleInheritanceTableInheritedRoleIdFieldAlias))
                .where(RoleTableDefinition.roleInheritanceTableRoleIdFieldAlias.eq(roleId));
        List<Object> args = select.getBindValues();
        return query(select.getSQL(), args.toArray(new Object[args.size()]), new RoleVoSetResultSetExtractor());
    }

    public Set<RoleVO> getRootRoles() {
        Select<?> select = this.getSelectQuery(getSelectFields())
                .leftJoin(RoleTableDefinition.roleInheritanceTableAsAlias)
                .on(this.table.getIdAlias().eq(RoleTableDefinition.roleInheritanceTableInheritedRoleIdFieldAlias))
                .where(RoleTableDefinition.roleInheritanceTableRoleIdFieldAlias.isNull());
        List<Object> args = select.getBindValues();
        return query(select.getSQL(), args.toArray(new Object[args.size()]), new RoleVoSetResultSetExtractor());
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

    private static class RoleVORowMapper implements RowMapper<RoleVO> {
        @Override
        public RoleVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            RoleVO vo = new RoleVO(rs.getInt(1), rs.getString(2), rs.getString(3));
            return vo;
        }
    }

    private static class RoleRowMapper implements RowMapper<Role> {
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
    private static class RoleVoSetResultSetExtractor implements ResultSetExtractor<Set<RoleVO>> {

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

    private static class RoleSetResultSetExtractor implements ResultSetExtractor<Set<Role>> {

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
