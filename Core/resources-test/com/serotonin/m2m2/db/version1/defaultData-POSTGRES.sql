--
-- System settings
--
INSERT INTO systemSettings (settingName, settingValue) VALUES ('databaseSchemaVersion', 47);

--
-- Roles
--
INSERT INTO roles (xid, name) VALUES ('superadmin', 'Superadmins');
INSERT INTO roles (xid, name) VALUES ('user', 'Users');
INSERT INTO roles (xid, name) VALUES ('anonymous', 'Anonymous');

--
-- Role Inheritance Mappings
--
INSERT INTO roleInheritance (roleId, inheritedRoleId) VALUES (1, 2);
INSERT INTO roleInheritance (roleId, inheritedRoleId) VALUES (2, 3);

--
-- Permissions
--
INSERT INTO permissions DEFAULT VALUES;
