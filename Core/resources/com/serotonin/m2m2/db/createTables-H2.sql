--
--    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
--    @author Matthew Lohbihler
--
--

--
-- System settings
CREATE TABLE systemSettings (
  settingName varchar(64) NOT NULL,
  settingValue longtext,
  PRIMARY KEY (settingName)
);

--
--
-- Roles
--
CREATE TABLE roles (
	id int not null auto_increment,
	xid varchar(100) not null,
	name varchar(255) not null,
  	primary key (id)
);
ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);

--
-- Role Inheritance Mappings
-- 
CREATE TABLE roleInheritance (
	roleId INT NOT NULL,
	inheritedRoleId INT NOT NULL
);
ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceUn1 UNIQUE (roleId,inheritedRoleId);
ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk2 FOREIGN KEY (inheritedRoleId) REFERENCES roles(id) ON DELETE CASCADE;

--
-- Permissions
CREATE TABLE minterms (
	id int(11) NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (id)
);

CREATE TABLE mintermsRoles (
	mintermId int(11) NOT NULL,
	roleId int(11) NOT NULL
);
ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesIdx1 UNIQUE (mintermId,roleId);
ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk1Idx KEY (mintermId);
ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk2_idx KEY (roleId);
ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk1 FOREIGN KEY (mintermId) REFERENCES minterms (id) ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk2 FOREIGN KEY (roleId) REFERENCES roles (id) ON DELETE CASCADE ON UPDATE NO ACTION;

CREATE TABLE permissions (
	id int(11) NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (id)
);

CREATE TABLE permissionsMinterms (
	permissionId int(11) NOT NULL,
	mintermId int(11) NOT NULL
);
ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsIdx1 UNIQUE KEY (permissionId, mintermId);
ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk1Idx KEY (permissionId);
ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk2Idx KEY(mintermId);
ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk1 FOREIGN KEY (permissionId) REFERENCES permissions (id) ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk2 FOREIGN KEY (mintermId) REFERENCES minterms (id) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- System wide permissions
-- 
CREATE TABLE systemPermissions (
	permissionType VARCHAR(255),
	permissionId INT NOT NULL
);
ALTER TABLE systemPermissions ADD CONSTRAINT systemPermissionsFk1 FOREIGN KEY (permissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE systemPermissions ADD CONSTRAINT permissionTypeUn1 UNIQUE(permissionType);

--
-- Users
CREATE TABLE users (
  id int NOT NULL auto_increment,
  username varchar(40) NOT NULL,
  password varchar(255) NOT NULL,
  email varchar(255),
  phone varchar(40),
  disabled char(1) NOT NULL,
  lastLogin bigint,
  homeUrl varchar(255),
  receiveAlarmEmails int NOT NULL,
  receiveOwnAuditEvents char(1) NOT NULL,
  timezone varchar(50),
  muted char(1),
  name varchar(255),
  locale varchar(50),
  tokenVersion int NOT NULL,
  passwordVersion int NOT NULL,
  passwordChangeTimestamp bigint NOT NULL,
  sessionExpirationOverride char(1),
  sessionExpirationPeriods int,
  sessionExpirationPeriodType varchar(25),
  organization varchar(80),
  organizationalRole varchar(80),
  createdTs bigint NOT NULL,
  emailVerifiedTs bigint,
  data longtext,
  PRIMARY KEY (id)
);
ALTER TABLE users ADD CONSTRAINT username_unique UNIQUE(username);
ALTER TABLE users ADD CONSTRAINT email_unique UNIQUE(email);

--
--
-- User Role Mappings
--
CREATE TABLE userRoleMappings (
	roleId int not null,
	userId int not null
);
ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsUn1 UNIQUE (roleId,userId);


CREATE TABLE userComments (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  userId int,
  commentType int NOT NULL,
  typeKey int NOT NULL,
  ts bigint NOT NULL,
  commentText varchar(1024) NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE userComments ADD CONSTRAINT userCommentsFk1 FOREIGN KEY (userId) REFERENCES users(id);
ALTER TABLE userComments ADD CONSTRAINT userCommentsUn1 UNIQUE (xid);
CREATE INDEX userComments_performance1 ON userComments (`commentType` ASC, `typeKey` ASC);

--
-- Mailing lists
CREATE TABLE mailingLists (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  name varchar(255) NOT NULL,
  receiveAlarmEmails INT NOT NULL,
  readPermissionId INT NOT NULL,
  editPermissionId INT NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE mailingLists ADD CONSTRAINT mailingListsUn1 UNIQUE (xid);
ALTER TABLE mailingLists ADD CONSTRAINT mailingListsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE mailingLists ADD CONSTRAINT mailingListsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

CREATE TABLE mailingListInactive (
  mailingListId int NOT NULL,
  inactiveInterval int NOT NULL
);
ALTER TABLE mailingListInactive ADD CONSTRAINT mailingListInactiveFk1 FOREIGN KEY (mailingListId) 
  REFERENCES mailingLists(id) ON DELETE CASCADE;

CREATE TABLE mailingListMembers (
  mailingListId int NOT NULL,
  typeId int NOT NULL,
  userId int,
  address varchar(255)
);
ALTER TABLE mailingListMembers ADD CONSTRAINT mailingListMembersFk1 FOREIGN KEY (mailingListId) 
  REFERENCES mailingLists(id) ON DELETE CASCADE;


--
-- Data Sources
CREATE TABLE dataSources (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  name varchar(255) NOT NULL,
  dataSourceType varchar(40) NOT NULL,
  data longblob NOT NULL,
  jsonData longtext,
  rtdata longblob,
  readPermissionId INT NOT NULL,
  editPermissionId INT NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesUn1 UNIQUE (xid);
CREATE INDEX nameIndex ON dataSources (name ASC);
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

--
-- Data Points
CREATE TABLE dataPoints (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  dataSourceId int NOT NULL,
  name varchar(255),
  deviceName varchar(255),
  enabled char(1),
  loggingType int,
  intervalLoggingPeriodType int,
  intervalLoggingPeriod int,
  intervalLoggingType int,
  tolerance double,
  purgeOverride char(1),
  purgeType int,
  purgePeriod int,
  defaultCacheSize int,
  discardExtremeValues char(1),
  engineeringUnits int,
  data longblob NOT NULL,
  rollup int,
  dataTypeId int not null,
  settable char(1),
  jsonData longtext,
  readPermissionId INT NOT NULL,
  editPermissionId INT NOT NULL,
  setPermissionId INT NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsUn1 UNIQUE (xid);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk1 FOREIGN KEY (dataSourceId) REFERENCES dataSources(id);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk4 FOREIGN KEY (setPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
CREATE INDEX pointNameIndex on dataPoints (name ASC);
CREATE INDEX deviceNameNameIdIndex ON dataPoints (deviceName ASC, name ASC, id ASC);
CREATE INDEX enabledIndex on dataPoints (enabled ASC);
CREATE INDEX xidNameIndex on dataPoints (xid ASC, name ASC);
CREATE INDEX dataSourceIdFkIndex ON dataPoints (dataSourceId ASC);

-- Data point tags
CREATE TABLE dataPointTags (
  dataPointId INT NOT NULL,
  tagKey VARCHAR(255) NOT NULL,
  tagValue VARCHAR(255) NOT NULL
);
ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsUn1 UNIQUE (dataPointId ASC, tagKey ASC);
ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;
CREATE INDEX dataPointTagsIndex1 ON dataPointTags (tagKey ASC, tagValue ASC);

--
--
-- Point Values (historical data)
--
CREATE TABLE pointValues (
  id bigint NOT NULL auto_increment,
  dataPointId int NOT NULL,
  dataType int NOT NULL,
  pointValue double,
  ts bigint NOT NULL,
  PRIMARY KEY (id)
);
CREATE index pointValuesIdx1 on pointValues (dataPointId, ts);

CREATE TABLE pointValueAnnotations (
  pointValueId bigint NOT NULL,
  textPointValueShort varchar(128),
  textPointValueLong longtext,
  sourceMessage longtext,
  PRIMARY KEY (pointValueId)
);

--
--
-- Event detectors
--
CREATE TABLE eventDetectors (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  sourceTypeName varchar(32) NOT NULL,
  typeName varchar(32) NOT NULL,
  dataPointId int,
  data longtext NOT NULL,
  jsonData longtext,
  readPermissionId INT NOT NULL,
  editPermissionId INT NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);
ALTER TABLE eventDetectors ADD CONSTRAINT dataPointIdFk FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

--
--
-- Events
--
CREATE TABLE events (
  id int NOT NULL auto_increment,
  typeName varchar(32) NOT NULL,
  subtypeName varchar(32),
  typeRef1 int NOT NULL,
  typeRef2 int NOT NULL,
  activeTs bigint NOT NULL,
  rtnApplicable char(1) NOT NULL,
  rtnTs bigint,
  rtnCause int,
  alarmLevel int NOT NULL,
  message longtext,
  ackTs bigint,
  ackUserId int,
  alternateAckSource longtext,
  PRIMARY KEY (id)
);
ALTER TABLE events ADD CONSTRAINT eventsFk1 FOREIGN KEY (ackUserId) REFERENCES users(id);
CREATE INDEX events_performance1 ON events (`activeTs` ASC);
CREATE INDEX events_performance2 ON events (`rtnApplicable` ASC, `rtnTs` ASC);
CREATE INDEX events_performance3 ON events (`typeName` ASC, `subTypeName` ASC, `typeRef1` ASC);

--
--
-- Event handlers
--
CREATE TABLE eventHandlers (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  alias varchar(255) NOT NULL,
  eventHandlerType varchar(40) NOT NULL,
  readPermissionId INT NOT NULL,
  editPermissionId INT NOT NULL,
  data longblob NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersUn1 UNIQUE (xid);
ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;


CREATE TABLE eventHandlersMapping (
  eventHandlerId int not null,
  
  -- Event type, see events
  eventTypeName varchar(32) NOT NULL,
  eventSubtypeName varchar(32) NOT NULL DEFAULT '',
  eventTypeRef1 int NOT NULL,
  eventTypeRef2 int NOT NULL
);
ALTER TABLE eventHandlersMapping ADD CONSTRAINT eventHandlersFk1 FOREIGN KEY (eventHandlerId) REFERENCES eventHandlers(id) ON DELETE CASCADE;
ALTER TABLE eventHandlersMapping ADD CONSTRAINT handlerMappingUniqueness UNIQUE(eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2);

--
--
-- Audit Table
-- 
CREATE TABLE audit (
  id int NOT NULL auto_increment,
  typeName varchar(32) NOT NULL,
  alarmLevel int NOT NULL,
  userId int NOT NULL,
  changeType int NOT NULL,
  objectId int NOT NULL,
  ts bigint NOT NULL,
  context longtext,
  message varchar(255),
  PRIMARY KEY (id)
);
CREATE INDEX tsIndex ON audit (ts ASC);
CREATE INDEX userIdIndex ON audit (userId ASC);
CREATE INDEX typeNameIndex ON audit (typeName ASC);
CREATE INDEX alarmLevelIndex ON audit (alarmLevel ASC);
--
--
-- Publishers
--
CREATE TABLE publishers (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  publisherType varchar(40) NOT NULL,
  data longblob NOT NULL,
  rtdata longblob,
  PRIMARY KEY (id)
);
ALTER TABLE publishers ADD CONSTRAINT publishersUn1 UNIQUE (xid);

--
--
-- JsonData
--
CREATE TABLE jsonData (
	id int not null auto_increment,
	xid varchar(100) not null,
	name varchar(255) not null,
  	publicData char(1),
  	data longtext,
    readPermissionId INT NOT NULL,
    editPermissionId INT NOT NULL,
  	primary key (id)
);
ALTER TABLE jsonData ADD CONSTRAINT jsonDataUn1 UNIQUE (xid);
ALTER TABLE jsonData ADD CONSTRAINT jsonDataFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE jsonData ADD CONSTRAINT jsonDataFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

--
--
-- InstalledModules
--  Thirty character restriction is from the store
CREATE TABLE installedModules (
	name varchar(30) not null,
	version varchar(255) not null
);
ALTER TABLE installedModules ADD CONSTRAINT installModulesUn1 UNIQUE (name);

--
--
-- FileStores
--
CREATE TABLE fileStores (
	id int not null auto_increment, 
	storeName varchar(100) not null, 
	readPermissionId INT NOT NULL,
    writePermissionId INT NOT NULL,
	PRIMARY KEY (id)
);
ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (storeName);
ALTER TABLE fileStores ADD CONSTRAINT fileStoresFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE fileStores ADD CONSTRAINT fileStoresFk2 FOREIGN KEY (writePermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

--
--
-- Persistent session data
--
CREATE TABLE mangoSessionData (
	sessionId VARCHAR(120),
	contextPath VARCHAR(60),
	virtualHost VARCHAR(60),
	lastNode VARCHAR(60),
	accessTime BIGINT,
	lastAccessTime BIGINT,
	createTime BIGINT,
	cookieTime BIGINT,
	lastSavedTime BIGINT,
	expiryTime BIGINT,
	maxInterval BIGINT,
	userId INT,
	primary key (sessionId, contextPath, virtualHost)
);
CREATE INDEX mangoSessionDataExpiryIndex ON mangoSessionData (expiryTime);
CREATE INDEX mangoSessionDataSessionIndex ON mangoSessionData (sessionId, contextPath);

--
--
-- Mango Default Data
--
-- Insert admin user
INSERT INTO users (id, name, username, password, email, phone, disabled, lastLogin, homeUrl, receiveAlarmEmails, receiveOwnAuditEvents, muted, tokenVersion, passwordVersion, passwordChangeTimestamp, sessionExpirationOverride, createdTs) VALUES 
	(1, 'Administrator', 'admin', '{BCRYPT}$2a$10$L6Jea9zZ79Hc82trIesw0ekqH0Q8hTGOBqSGutoi17p2UZ.j3vzWm', 'admin@mango.example.com', '', 'N', 0, '/ui/administration/home', -3, 'N', 'Y', 1, 1, DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()) * 1000, 'N', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()) * 1000);      
-- Insert default roles
INSERT INTO roles (id, xid, name) VALUES (1, 'superadmin', 'Superadmins');
INSERT INTO roles (id, xid, name) VALUES (2, 'user', 'Users');
INSERT INTO roles (id, xid, name) VALUES (3, 'anonymous', 'Anonymous role');
-- Add admin user role mappings
INSERT INTO userRoleMappings (roleId, userId) VALUES (1, 1);
INSERT INTO userRoleMappings (roleId, userId) VALUES (2, 1);
