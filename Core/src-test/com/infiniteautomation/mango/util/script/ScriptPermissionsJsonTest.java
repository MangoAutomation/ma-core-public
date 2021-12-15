/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util.script;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Terry Packer
 */
public class ScriptPermissionsJsonTest extends MangoTestBase {

    /**
     * Test import from the old version with 4 possible sets of roles
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testV1() {

        Set<Role> dataSourceRoles = createRoles(2).stream().map(r->r.getRole()).collect(Collectors.toSet());
        Set<Role> dataPointReadRoles = createRoles(2).stream().map(r->r.getRole()).collect(Collectors.toSet());
        Set<Role> dataPointSetRoles = createRoles(2).stream().map(r->r.getRole()).collect(Collectors.toSet());
        Set<Role> customRoles = createRoles(2).stream().map(r->r.getRole()).collect(Collectors.toSet());

        com.serotonin.m2m2.rt.script.ScriptPermissions toSerialize = new com.serotonin.m2m2.rt.script.ScriptPermissions();
        toSerialize.setDataSourcePermissions(PermissionService.implodeRoles(dataSourceRoles));
        toSerialize.setDataPointReadPermissions(PermissionService.implodeRoles(dataPointReadRoles));
        toSerialize.setDataPointSetPermissions(PermissionService.implodeRoles(dataPointSetRoles));
        toSerialize.setCustomPermissions(PermissionService.implodeRoles(customRoles));

        ScriptPermissions permissions = convert(toSerialize);

        Set<Role> all = new HashSet<>();
        all.addAll(dataSourceRoles);
        all.addAll(dataPointReadRoles);
        all.addAll(dataPointSetRoles);
        all.addAll(customRoles);

        assertRoles(all, permissions.getRoles());

    }

    /**
     * Test import from the old version with 4 possible sets of roles
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testV1nullPermissions() {
        com.serotonin.m2m2.rt.script.ScriptPermissions toSerialize = new com.serotonin.m2m2.rt.script.ScriptPermissions();
        toSerialize.setDataSourcePermissions(null);
        toSerialize.setDataPointReadPermissions(null);
        toSerialize.setDataPointSetPermissions(null);
        toSerialize.setCustomPermissions(null);

        ScriptPermissions permissions = convert(toSerialize);
        assertTrue(permissions.getRoles().isEmpty());
    }

    /**
     * Test import from the old version with 4 possible sets of roles
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testV1null() {
        com.serotonin.m2m2.rt.script.ScriptPermissions toSerialize = null;
        ScriptPermissions permissions = convert(toSerialize);
        assertTrue(permissions == null);
    }

    /**
     * Test import as latest version
     */
    @Test
    public void testV2() {
        Set<Role> all = createRoles(2).stream().map(r->r.getRole()).collect(Collectors.toSet());
        ScriptPermissions permissions = convert(new ScriptPermissions(all));
        assertRoles(all, permissions.getRoles());
    }

    /**
     */
    private ScriptPermissions convert(Object toSerialize) {
        try {
            String json = convertToSeroJson(toSerialize);
            return (ScriptPermissions) readSeroJson(ScriptPermissions.class, json);
        } catch (IOException e) {
            fail(e.getMessage());
            return null;
        }

    }

    private void assertRoles(Set<Role> expected, Set<Role> actual) {
        for(Role e : expected) {
            assertTrue(actual.contains(e));
        }
    }

}
