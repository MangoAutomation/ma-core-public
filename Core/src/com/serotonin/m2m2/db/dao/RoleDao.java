/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.db.query.RQLToCondition.RQLVisitException;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
@Repository
public class RoleDao extends AbstractVoDao<RoleVO, RoleTableDefinition> {

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
    protected Map<String, RQLSubSelectCondition> createSubSelectMap() {
        Map<String, RQLSubSelectCondition> subselects = super.createSubSelectMap();
        Map<String, RQLSubSelectCondition> mySubselects = new HashMap<>();
        mySubselects.put("inherited", createInheritedRoleCondition());
        return combine(subselects, mySubselects);
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

    @Override
    public void loadRelationalData(RoleVO vo) {
        Set<Role> inherited = getInherited(vo.getId());
        vo.setInherited(inherited);
    }

    @Override
    public void deletePostRelationalData(RoleVO vo) {
        permissionDao.roleUnlinked();
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

    @Override
    public SelectJoinStep<Record> getSelectQuery(List<Field<?>> fields) {
        // use select distinct as the join below results in multiple rows per role
        return this.create.selectDistinct(fields)
                .from(this.table.getTableAsAlias());
    }

    /**
     * Get all roles that inherit this role
     * @param role
     * @return
     */
    public Set<Role> getRolesThatInherit(Role role) {
        return this.doInTransaction((tx) -> {
            Set<Role> all = new HashSet<Role>();
            addRolesThatInherit(role, all);
            return all;
        });
    }

    /**
     * Recursively add all roles that inherit this role
     * @param role
     * @param all
     */
    private void addRolesThatInherit(Role role, Set<Role> all) {
        Set<Role> inherited = getRolesThatInherit(role.getId());
        for(Role inheritedRole : inherited) {
            all.add(inheritedRole);
            addRolesThatInherit(inheritedRole, all);
        }
    }

    /**
     * Get the roles that inherit this role, one level deep.
     * @param vo
     * @return
     */
    private Set<Role> getRolesThatInherit(int roleId) {
        Select<?> select = this.create.select(getSelectFields())
                .from(this.table.getTableAsAlias())
                .join(RoleTableDefinition.roleInheritanceTableAsAlias)
                .on(this.table.getIdAlias().eq(RoleTableDefinition.roleInheritanceTableRoleIdFieldAlias))
                .where(RoleTableDefinition.roleInheritanceTableInheritedRoleIdFieldAlias.eq(roleId));
        List<Object> args = select.getBindValues();
        return query(select.getSQL(), args.toArray(new Object[args.size()]), new RoleSetResultSetExtractor());
    }

    /**
     * Recursively get a set of all inherited roles of this role
     * @param vo
     * @return
     */
    public Set<Role> getFlatInheritance(Role role) {
        return this.doInTransaction((tx) -> {
            Set<Role> all = new HashSet<Role>();
            addInheritance(role, all);
            return all;
        });
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
        Select<?> select = this.create.select(getSelectFields())
                .from(this.table.getTableAsAlias())
                .join(RoleTableDefinition.roleInheritanceTableAsAlias)
                .on(this.table.getIdAlias().eq(RoleTableDefinition.roleInheritanceTableInheritedRoleIdFieldAlias))
                .where(RoleTableDefinition.roleInheritanceTableRoleIdFieldAlias.eq(roleId));
        List<Object> args = select.getBindValues();
        return query(select.getSQL(), args.toArray(new Object[args.size()]), new RoleSetResultSetExtractor());
    }

    public RQLSubSelectCondition createInheritedRoleCondition() {
        return (operation, node) -> {
            List<Object> arguments = node.getArguments();

            //Check the role Xid input
            if (arguments.size() > 2) {
                throw new RQLVisitException(String.format("Only single arguments supported for node type '%s'", node.getName()));
            }

            Object roleXid = arguments.get(1);
            Integer roleId = null;

            //TODO Should really used role cache in PermissionService but there is a
            // circular dependency if injected due to our use of the RoleDao
            RoleVO role = getByXid((String)roleXid);
            if(role != null) {
                roleId = role.getId();
            }

            SelectJoinStep<Record1<Integer>> select = create.select(RoleTableDefinition.roleInheritanceTableInheritedRoleIdFieldAlias).from(RoleTableDefinition.roleInheritanceTableAsAlias);
            SelectConditionStep<Record1<Integer>> afterWhere = select.where(RoleTableDefinition.roleInheritanceTableRoleIdFieldAlias.eq(roleId));
            switch(operation) {
                case CONTAINS:
                    return table.getIdAlias().in(afterWhere.asField());
                default:
                    throw new RQLVisitException(String.format("Unsupported node type '%s' for property '%s'", node.getName(), arguments.get(0)));
            }
        };
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

    public static class RoleSetResultSetExtractor implements ResultSetExtractor<Set<Role>> {

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
}
