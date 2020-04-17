/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.permission;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 *
 * A -> B (A inherits B)
 *
 * Inheritance
 * @author Terry Packer
 */
public class RoleInheritanceTest extends MangoTestBase {

    /**
     * Test a straight hierarchy with only single inheritance
     *
     * admin -> supervisor -> edit -> read
     */
    @Test
    public void test1() {

        PermissionService service = Common.getBean(PermissionService.class);

        //Lowest level
        RoleVOHierarchy read = createRoleVOHierarchy("read", "read");
        MangoPermission readPermission = MangoPermission.createOrSet(read.getRole());

        RoleVOHierarchy edit = createRoleVOHierarchy("edit", "edit", read);
        MangoPermission editPermission = MangoPermission.createOrSet(edit.getRole());

        RoleVOHierarchy supervisor = createRoleVOHierarchy("supervisor", "supervisor", edit);
        MangoPermission supervisorPermission = MangoPermission.createOrSet(supervisor.getRole());

        RoleVOHierarchy admin = createRoleVOHierarchy("admin", "admin", supervisor);
        MangoPermission adminPermission = MangoPermission.createOrSet(admin.getRole());

        //What admin can do
        assertTrue(service.hasPermission(createPermissionHolder(admin), readPermission));
        assertTrue(service.hasPermission(createPermissionHolder(admin), editPermission));
        assertTrue(service.hasPermission(createPermissionHolder(admin), supervisorPermission));
        assertTrue(service.hasPermission(createPermissionHolder(admin), adminPermission));

        //What supervisor can do
        assertTrue(service.hasPermission(createPermissionHolder(supervisor), readPermission));
        assertTrue(service.hasPermission(createPermissionHolder(supervisor), editPermission));
        assertTrue(service.hasPermission(createPermissionHolder(supervisor), supervisorPermission));
        assertFalse(service.hasPermission(createPermissionHolder(supervisor), adminPermission));

        //What edit can do
        assertTrue(service.hasPermission(createPermissionHolder(edit), readPermission));
        assertTrue(service.hasPermission(createPermissionHolder(edit), editPermission));
        assertFalse(service.hasPermission(createPermissionHolder(edit), supervisorPermission));
        assertFalse(service.hasPermission(createPermissionHolder(edit), adminPermission));

        //What read can do
        assertTrue(service.hasPermission(createPermissionHolder(read), readPermission));
        assertFalse(service.hasPermission(createPermissionHolder(read), editPermission));
        assertFalse(service.hasPermission(createPermissionHolder(read), supervisorPermission));
        assertFalse(service.hasPermission(createPermissionHolder(read), adminPermission));


    }

    /**
     * Test a tree hierarchy with multiple inheritance
     *
     *
     *
     *                -> gasTechnician -> gasEdit -> gasRead
     * technician ->  -> solidTechnician -> solidEdit -> solidRead
     *                -> fluidTechnician -> fluidEdit -> fluidRead
     *
     *
     */
    @Test
    public void test2() {

        PermissionService service = Common.getBean(PermissionService.class);

        RoleVOHierarchy fluidRead = createRoleVOHierarchy("fluidRead", "fluidRead");
        MangoPermission fluidReadPermission = MangoPermission.createOrSet(fluidRead.getRole());

        RoleVOHierarchy fluidEdit = createRoleVOHierarchy("fluidEdit", "fluidEdit", fluidRead);
        MangoPermission fluidEditPermission = MangoPermission.createOrSet(fluidEdit.getRole());

        RoleVOHierarchy solidRead = createRoleVOHierarchy("solidRead", "solidRead");
        MangoPermission solidReadPermission = MangoPermission.createOrSet(solidRead.getRole());

        RoleVOHierarchy solidEdit = createRoleVOHierarchy("solidEdit", "solidEdit", solidRead);
        MangoPermission solidEditPermission = MangoPermission.createOrSet(solidEdit.getRole());

        RoleVOHierarchy gasRead = createRoleVOHierarchy("gasRead", "gasRead");
        MangoPermission gasReadPermission = MangoPermission.createOrSet(gasRead.getRole());

        RoleVOHierarchy gasEdit = createRoleVOHierarchy("gasEdit", "gasEdit", gasRead);
        MangoPermission gasEditPermission = MangoPermission.createOrSet(gasEdit.getRole());

        RoleVOHierarchy fluidTechnician = createRoleVOHierarchy("fluidTechnician", "fluidTechnician", fluidEdit);
        RoleVOHierarchy solidTechnician = createRoleVOHierarchy("solidTechnician", "solidTechnician", solidEdit);
        RoleVOHierarchy gasTechnician = createRoleVOHierarchy("gasTechnician", "gasTechnician", gasEdit);

        RoleVOHierarchy technician = createRoleVOHierarchy("technician", "technician", fluidTechnician, solidTechnician, gasTechnician);


        //Who can read fluid
        assertTrue(service.hasPermission(createPermissionHolder(fluidRead), fluidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(fluidEdit), fluidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(fluidTechnician), fluidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(technician), fluidReadPermission));

        //Who can edit fluid
        assertTrue(service.hasPermission(createPermissionHolder(fluidEdit), fluidEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(fluidTechnician), fluidEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(technician), fluidEditPermission));

        //Who cannot read fluid
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasEdit), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasTechnician), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidEdit), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidTechnician), fluidReadPermission));

        //Who cannot edit fluid
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasEdit), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasTechnician), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidEdit), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidTechnician), fluidEditPermission));

        //Who can read solid
        assertTrue(service.hasPermission(createPermissionHolder(solidRead), solidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(solidEdit), solidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(solidTechnician), solidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(technician), solidReadPermission));

        //Who can edit solid
        assertTrue(service.hasPermission(createPermissionHolder(solidEdit), solidEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(solidTechnician), solidEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(technician), solidEditPermission));

        //Who cannot read solid
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasEdit), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasTechnician), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidEdit), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidTechnician), solidReadPermission));

        //Who cannot edit solid
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasEdit), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasTechnician), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidEdit), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidTechnician), solidEditPermission));

        //Who can read gas
        assertTrue(service.hasPermission(createPermissionHolder(gasRead), gasReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(gasEdit), gasReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(gasTechnician), gasReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(technician), gasReadPermission));

        //Who can edit gas
        assertTrue(service.hasPermission(createPermissionHolder(gasEdit), gasEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(gasTechnician), gasEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(technician), gasEditPermission));

        //Who cannot read gas
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidEdit), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidTechnician), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidEdit), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidTechnician), gasReadPermission));

        //Who cannot edit gas
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidEdit), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidTechnician), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidEdit), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidTechnician), gasEditPermission));

    }

    /**
     * Test a slightly more complex tree hierarchy with multiple inheritance
     *
     *
     *                 gasTechnician  ->  gasRead  <-  gasEdit  <-  gasAdmin
     * technician ->  solidTechnician -> solidRead <- solidEdit <- solidAdmin  <- admin
     *                fluidTechnician -> fluidRead <- fluidEdit <- fluidAdmin
     *
     *
     */
    @Test
    public void test3() {

        PermissionService service = Common.getBean(PermissionService.class);

        //Fluid roles
        RoleVOHierarchy fluidRead = createRoleVOHierarchy("fluidRead", "fluidRead");
        MangoPermission fluidReadPermission = MangoPermission.createOrSet(fluidRead.getRole());

        RoleVOHierarchy fluidEdit = createRoleVOHierarchy("fluidEdit", "fluidEdit", fluidRead);
        MangoPermission fluidEditPermission = MangoPermission.createOrSet(fluidEdit.getRole());

        RoleVOHierarchy fluidTechnician = createRoleVOHierarchy("fluidTechnician", "fluidTechnician", fluidRead);
        RoleVOHierarchy fluidAdmin = createRoleVOHierarchy("fluidAdmin", "fluidAdmin", fluidEdit);

        //Solid roles
        RoleVOHierarchy solidRead = createRoleVOHierarchy("solidRead", "solidRead");
        MangoPermission solidReadPermission = MangoPermission.createOrSet(solidRead.getRole());

        RoleVOHierarchy solidEdit = createRoleVOHierarchy("solidEdit", "solidEdit", solidRead);
        MangoPermission solidEditPermission = MangoPermission.createOrSet(solidEdit.getRole());

        RoleVOHierarchy solidTechnician = createRoleVOHierarchy("solidTechnician", "solidTechnician", solidRead);
        RoleVOHierarchy solidAdmin = createRoleVOHierarchy("solidAdmin", "solidAdmin", solidEdit);

        //Gas roles
        RoleVOHierarchy gasRead = createRoleVOHierarchy("gasRead", "gasRead");
        MangoPermission gasReadPermission = MangoPermission.createOrSet(gasRead.getRole());

        RoleVOHierarchy gasEdit = createRoleVOHierarchy("gasEdit", "gasEdit", gasRead);
        MangoPermission gasEditPermission = MangoPermission.createOrSet(gasEdit.getRole());

        RoleVOHierarchy gasTechnician = createRoleVOHierarchy("gasTechnician", "gasTechnician", gasRead);
        RoleVOHierarchy gasAdmin = createRoleVOHierarchy("gasAdmin", "gasAdmin", gasEdit);

        RoleVOHierarchy technician = createRoleVOHierarchy("technician", "technician", fluidTechnician, solidTechnician, gasTechnician);
        RoleVOHierarchy admin = createRoleVOHierarchy("admin", "admin", fluidAdmin, solidAdmin, gasAdmin);

        //Who can read fluid
        assertTrue(service.hasPermission(createPermissionHolder(fluidRead), fluidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(fluidEdit), fluidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(fluidTechnician), fluidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(fluidAdmin), fluidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(technician), fluidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(admin), fluidReadPermission));

        //Who cannot read fluid
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasEdit), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasTechnician), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasAdmin), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidEdit), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidTechnician), fluidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidAdmin), fluidReadPermission));

        //Who can edit fluid
        assertTrue(service.hasPermission(createPermissionHolder(fluidEdit), fluidEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(fluidAdmin), fluidEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(admin), fluidEditPermission));

        //Who cannot edit fluid
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasEdit), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasTechnician), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidEdit), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidTechnician), fluidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(technician), fluidEditPermission));

        //Who can read solid
        assertTrue(service.hasPermission(createPermissionHolder(solidRead), solidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(solidEdit), solidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(solidTechnician), solidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(solidAdmin), solidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(technician), solidReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(admin), solidReadPermission));

        //Who cannot read solid
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasEdit), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasTechnician), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasAdmin), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidEdit), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidTechnician), solidReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidAdmin), solidReadPermission));

        //Who can edit solid
        assertTrue(service.hasPermission(createPermissionHolder(solidEdit), solidEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(solidAdmin), solidEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(admin), solidEditPermission));

        //Who cannot edit solid
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasEdit), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(gasTechnician), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidEdit), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidTechnician), solidEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(technician), solidEditPermission));

        //Who can read gas
        assertTrue(service.hasPermission(createPermissionHolder(gasRead), gasReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(gasEdit), gasReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(gasTechnician), gasReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(gasAdmin), gasReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(technician), gasReadPermission));
        assertTrue(service.hasPermission(createPermissionHolder(admin), gasReadPermission));

        //Who cannot read gas
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidEdit), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidTechnician), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidAdmin), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidEdit), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidTechnician), gasReadPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidAdmin), gasReadPermission));

        //Who can edit gas
        assertTrue(service.hasPermission(createPermissionHolder(gasEdit), gasEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(gasAdmin), gasEditPermission));
        assertTrue(service.hasPermission(createPermissionHolder(admin), gasEditPermission));

        //Who cannot edit gas
        assertFalse(service.hasPermission(createPermissionHolder(gasRead), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidRead), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidEdit), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(solidTechnician), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidRead), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidEdit), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(fluidTechnician), gasEditPermission));
        assertFalse(service.hasPermission(createPermissionHolder(technician), gasEditPermission));

    }

    /**
     * Create a role with a parent (
     * @param xid
     * @param name
     * @param parent - can be null to signify no parent
     * @return
     */
    protected RoleVOHierarchy createRoleVOHierarchy(String xid, String name, RoleVOHierarchy... inherited) {
        return new RoleVOHierarchy(Common.NEW_ID, xid, name, new HashSet<>(Arrays.asList(inherited)));
    }

    private PermissionHolder createPermissionHolder(RoleVOHierarchy... roles) {
        Set<Role> holderRoles = Arrays.asList(roles).stream().map(r -> r.getRoles()).collect(Collectors.toSet()).stream().flatMap(Collection::stream).collect(Collectors.toSet());
        return new PermissionHolder() {

            @Override
            public String getPermissionHolderName() {
                return "Inheritance";
            }

            @Override
            public boolean isPermissionHolderDisabled() {
                return false;
            }

            @Override
            public Set<Role> getAllInheritedRoles() {
                return holderRoles;
            }

        };
    }

    class RoleVOHierarchy extends RoleVO {

        private static final long serialVersionUID = 1L;

        Set<RoleVOHierarchy> inheritedRoles;

        public RoleVOHierarchy(int id, String xid, String name, Set<RoleVOHierarchy> inheritedRoles) {
            super(id, xid, name);
            this.inheritedRoles = inheritedRoles;
        }

        /**
         * Get a set of this role and all the inherited roles
         * @return
         */
        public Set<Role> getRoles() {
            Set<Role> roles = new HashSet<>();
            roles.add(getRole());
            for(RoleVOHierarchy inherited : this.inheritedRoles) {
                roles.add(inherited.getRole());
                followLineage(inherited, roles);
            }
            return roles;
        }

        /**
         * Trace the lineage of this role
         *
         * @param role
         * @param roles
         */
        private void followLineage(RoleVOHierarchy role, Set<Role> roles) {
            for(RoleVOHierarchy inherited : role.inheritedRoles) {
                roles.add(inherited.getRole());
                followLineage(inherited, roles);
            }
        }
    }



}
