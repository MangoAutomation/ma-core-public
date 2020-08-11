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
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
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
    public void deletePostRelationalData(RoleVO vo) {
        permissionDao.roleUnlinked();
    }

    @Override
    public void loadRelationalData(RoleVO vo) {
        Set<Role> inherited = getInherited(vo.getId());
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
    protected Map<String, Function<Object, Object>> createValueConverterMap() {
        Map<String, Function<Object, Object>> map = new HashMap<>(super.createValueConverterMap());
        map.put("roleId", value -> {
            if (value instanceof String) {
                return getByXid((String) value).getId();
            }
            return value;
        });
        return map;
    }

    @Override
    public SelectJoinStep<Record> getSelectQuery(List<Field<?>> fields) {
        // use select distinct as the join below results in multiple rows per role
        return this.create.selectDistinct(fields)
                .from(this.table.getTableAsAlias());
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {
        return select.leftJoin(RoleTableDefinition.roleInheritanceTableAsAlias)
                .on(this.table.getIdAlias().eq(RoleTableDefinition.roleInheritanceTableInheritedRoleIdFieldAlias));
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
    public Set<Role> getInherited(int roleId) {
        Select<?> select = this.create.select(getSelectFields())
                .from(this.table.getTableAsAlias())
                .join(RoleTableDefinition.roleInheritanceTableAsAlias)
                .on(this.table.getIdAlias().eq(RoleTableDefinition.roleInheritanceTableInheritedRoleIdFieldAlias))
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
}
