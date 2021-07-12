/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.RQLOperation;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.db.query.RQLToCondition.RQLVisitException;
import com.infiniteautomation.mango.db.tables.RoleInheritance;
import com.infiniteautomation.mango.db.tables.Roles;
import com.infiniteautomation.mango.db.tables.records.RolesRecord;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
@Repository
public class RoleDao extends AbstractVoDao<RoleVO, RolesRecord, Roles> {

    private final PermissionDao permissionDao;
    private final LazyInitSupplier<PermissionService> permissionServiceSupplier;

    // cannot inject permission service in this DAO or it would introduce a circular dependency
    @Autowired
    private RoleDao(@Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME) ObjectMapper mapper,
                    ApplicationEventPublisher publisher,
                    PermissionDao permissionDao, BeanFactory beanFactory) {
        super(AuditEventType.TYPE_ROLE,
                Roles.ROLES,
                new TranslatableMessage("internal.monitor.ROLE_COUNT"),
                mapper, publisher, null);
        this.permissionDao = permissionDao;

        this.permissionServiceSupplier = new LazyInitSupplier<>(() -> beanFactory.getBean(PermissionService.class));
    }

    @Override
    protected Map<String, RQLSubSelectCondition> createSubSelectMap() {
        Map<String, RQLSubSelectCondition> subselects = super.createSubSelectMap();
        subselects.put("inherited", (operation, node) -> {
            if (operation != RQLOperation.CONTAINS) {
                throw new RQLVisitException(String.format("Unsupported node type '%s' for field '%s'", node.getName(), node.getArgument(0)));
            }

            PermissionService permissionService = permissionServiceSupplier.get();
            Set<Integer> roleIds = extractArrayArguments(node, o -> o == null ? null : o.toString()).stream()
                    .filter(Objects::nonNull)
                    .map(permissionService::getRole)
                    .filter(Objects::nonNull)
                    .map(Role::getId)
                    .collect(Collectors.toSet());

            SelectConditionStep<Record1<Integer>> afterWhere;
            if (!roleIds.isEmpty()) {
                SelectJoinStep<Record1<Integer>> select = create.select(RoleInheritance.ROLE_INHERITANCE.roleId)
                        .from(RoleInheritance.ROLE_INHERITANCE);
                afterWhere = select.where(RoleInheritance.ROLE_INHERITANCE.inheritedRoleId.in(roleIds));
            } else {
                //Find all roles with no inherited roles
                SelectJoinStep<Record1<Integer>> select = create.select(getIdField()).from(table);
                SelectOnConditionStep<Record1<Integer>> afterJoin = select.leftJoin(RoleInheritance.ROLE_INHERITANCE)
                        .on(RoleInheritance.ROLE_INHERITANCE.roleId.eq(getIdField()));
                afterWhere = afterJoin.where(RoleInheritance.ROLE_INHERITANCE.roleId.isNull());
            }
            return table.id.in(afterWhere.asField());
        });
        subselects.put("inheritedBy", (operation, node) -> {
            if (operation != RQLOperation.CONTAINS) {
                throw new RQLVisitException(String.format("Unsupported node type '%s' for field '%s'", node.getName(), node.getArgument(0)));
            }

            PermissionService permissionService = permissionServiceSupplier.get();
            Set<Integer> roleIds = extractArrayArguments(node, o -> o == null ? null : o.toString()).stream()
                    .filter(Objects::nonNull)
                    .map(permissionService::getRole)
                    .filter(Objects::nonNull)
                    .map(Role::getId)
                    .collect(Collectors.toSet());

            SelectConditionStep<Record1<Integer>> afterWhere;
            if (!roleIds.isEmpty()) {
                //Find all roles inherited by this role
                SelectJoinStep<Record1<Integer>> select = create.select(RoleInheritance.ROLE_INHERITANCE.inheritedRoleId)
                        .from(RoleInheritance.ROLE_INHERITANCE);
                afterWhere = select.where(RoleInheritance.ROLE_INHERITANCE.roleId.in(roleIds));
            } else {
                //Find all roles with that are not inherited by any role
                SelectJoinStep<Record1<Integer>> select = create.select(getIdField()).from(table);
                SelectOnConditionStep<Record1<Integer>> afterJoin = select.leftJoin(RoleInheritance.ROLE_INHERITANCE)
                        .on(RoleInheritance.ROLE_INHERITANCE.inheritedRoleId.eq(getIdField()));
                afterWhere = afterJoin.where(RoleInheritance.ROLE_INHERITANCE.inheritedRoleId.isNull());
            }
            return table.id.in(afterWhere.asField());
        });
        return subselects;
    }

    @Override
    public void saveRelationalData(RoleVO existing, RoleVO vo) {
        if(existing != null) {
            //Drop the mappings

            this.create.deleteFrom(RoleInheritance.ROLE_INHERITANCE).where(RoleInheritance.ROLE_INHERITANCE.roleId.eq(vo.getId())).execute();
        }

        if(vo.getInherited() != null && vo.getInherited().size() > 0) {
            List<Query> inserts = new ArrayList<>();
            for(Role role : vo.getInherited()) {
                inserts.add(DSL.insertInto(RoleInheritance.ROLE_INHERITANCE).columns(
                        RoleInheritance.ROLE_INHERITANCE.roleId, RoleInheritance.ROLE_INHERITANCE.inheritedRoleId)
                        .values(vo.getId(), role.getId()));
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
    protected Record toRecord(RoleVO vo) {
        Record record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.name, vo.getName());
        return record;
    }

    @Override
    public RoleVO mapRecord(Record record) {
        return new RoleVO(record.get(table.id), record.get(table.xid), record.get(table.name));
    }

    public Role mapRecordToRole(Record record) {
        return new Role(record.get(table.id), record.get(table.xid));
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
     * @param roleId
     * @return
     */
    private Set<Role> getRolesThatInherit(int roleId) {
        try (Stream<Record> stream = this.create.select(getSelectFields())
                .from(table)
                .join(RoleInheritance.ROLE_INHERITANCE)
                .on(getIdField().eq(RoleInheritance.ROLE_INHERITANCE.roleId))
                .where(RoleInheritance.ROLE_INHERITANCE.inheritedRoleId.eq(roleId))
                .stream()) {

            return Collections.unmodifiableSet(stream.map(this::mapRecordToRole).collect(Collectors.toSet()));
        }
    }

    /**
     * Recursively get a set of all inherited roles of this role
     * @param role
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
     * @param roleId
     * @return
     */
    private Set<Role> getInherited(int roleId) {
        try (Stream<Record> stream = this.create.select(getSelectFields())
                .from(table)
                .join(RoleInheritance.ROLE_INHERITANCE)
                .on(getIdField().eq(RoleInheritance.ROLE_INHERITANCE.inheritedRoleId))
                .where(RoleInheritance.ROLE_INHERITANCE.roleId.eq(roleId))
                .stream()) {

            return Collections.unmodifiableSet(stream.map(this::mapRecordToRole).collect(Collectors.toSet()));
        }
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, PermissionHolder user) {
        PermissionService permissionService = permissionServiceSupplier.get();
        Set<Role> heldRoles = permissionService.getAllInheritedRoles(user);
        if (heldRoles.contains(PermissionHolder.SUPERADMIN_ROLE))  {
            return select;
        }
        List<String> xids = heldRoles.stream().map(Role::getXid).collect(Collectors.toList());
        return select.innerJoin(DSL.selectOne()).on(table.xid.in(xids));
    }
}
