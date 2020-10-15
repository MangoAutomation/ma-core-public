/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.json.junit.vo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Supplier;

import org.junit.Test;

import com.infiniteautomation.mango.emport.ImportContext;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.util.LazyField;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeDefinition;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 *
 * @author Terry Packer
 */
public class LazyFieldJsonTest extends MangoTestBase {

    @Test
    public void testLazyPermissionFromJsonObject() {

        RoleService roleService = Common.getBean(RoleService.class);
        PermissionService permissionService = Common.getBean(PermissionService.class);

        Role role1 = permissionService.runAsSystemAdmin(() -> {
            return roleService.insert(new RoleVO(Common.NEW_ID, "XID-1", "Role 1")).getRole();
        });

        Role role2 = permissionService.runAsSystemAdmin(() -> {
            return roleService.insert(new RoleVO(Common.NEW_ID, "XID-2", "Role 2")).getRole();
        });

        LazyField<MangoPermission> permission = new LazyField<>(() -> MangoPermission.builder().minterm(role1, role2).build());

        try (StringWriter stringWriter = new StringWriter()) {
            JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
            JsonTypeWriter typeWriter = new JsonTypeWriter(Common.JSON_CONTEXT);
            JsonValue value = typeWriter.writeObject(permission);

            writer.setPrettyIndent(0);
            writer.setPrettyOutput(true);
            writer.writeObject(value);
            String json = stringWriter.toString();

            JsonTypeReader typeReader = new JsonTypeReader(json);
            JsonValue read = typeReader.read();
            JsonArray root = read.toJsonArray();
            JsonReader reader = new JsonReader(Common.JSON_CONTEXT, root);
            ImportContext context = new ImportContext(reader, new ProcessResult(), Common.getTranslations());

            LazyField<MangoPermission> readPermission = new LazyField<>();
            TypeDefinition lazyType = new TypeDefinition(LazyField.class, MangoPermission.class);
            context.getReader().readInto(lazyType, readPermission, root);

            assertEquals(permission.get(), readPermission.get());
        }catch (IOException | JsonException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testLazyPermissionInObject() {

        RoleService roleService = Common.getBean(RoleService.class);
        PermissionService permissionService = Common.getBean(PermissionService.class);

        Role role1 = permissionService.runAsSystemAdmin(() -> {
            return roleService.insert(new RoleVO(Common.NEW_ID, "XID-1", "Role 1")).getRole();
        });

        Role role2 = permissionService.runAsSystemAdmin(() -> {
            return roleService.insert(new RoleVO(Common.NEW_ID, "XID-2", "Role 2")).getRole();
        });

        LazyContainer container = new LazyContainer();
        container.supplyPermission(() -> MangoPermission.builder().minterm(role1, role2).build());

        try (StringWriter stringWriter = new StringWriter()) {
            JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
            JsonTypeWriter typeWriter = new JsonTypeWriter(Common.JSON_CONTEXT);
            JsonValue value = typeWriter.writeObject(container);

            writer.setPrettyIndent(0);
            writer.setPrettyOutput(true);
            writer.writeObject(value);
            String json = stringWriter.toString();

            JsonTypeReader typeReader = new JsonTypeReader(json);
            JsonValue read = typeReader.read();
            JsonObject root = read.toJsonObject();
            JsonReader reader = new JsonReader(Common.JSON_CONTEXT, root);
            ImportContext context = new ImportContext(reader, new ProcessResult(), Common.getTranslations());

            LazyContainer readContainer = new LazyContainer();
            context.getReader().readInto(readContainer, root);

            assertEquals(container.getPermission(), readContainer.getPermission());
        }catch (IOException | JsonException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }



    public class LazyContainer {
        @JsonProperty
        private LazyField<MangoPermission> permission = new LazyField<>();

        public MangoPermission getPermission() {
            return permission.get();
        }
        public void setPermission(MangoPermission permission) {
            this.permission.set(permission);
        }
        public void supplyPermission(Supplier<MangoPermission> supplier) {
            this.permission = new LazyField<MangoPermission>(supplier);
        }
    }

}
