--
--    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
--    @author Matthew Lohbihler
--
--
-- System settings
create table systemSettings (
  settingName nvarchar(64) not null,
  settingValue ntext,
  primary key (settingName)
);

--
-- Users
create table users (
  id int not null identity,
  username nvarchar(40) not null,
  password nvarchar(255) not null,
  email nvarchar(255) not null,
  phone nvarchar(40),
  disabled char(1) not null,
  lastLogin bigint,
  homeUrl nvarchar(255),
  receiveAlarmEmails int not null,
  receiveOwnAuditEvents char(1) not null,
  timezone nvarchar(50),
  muted char(1),
  name nvarchar(255),
  locale nvarchar(50),
  tokenVersion int not null,
  passwordVersion int not null,
  passwordChangeTimestamp bigint NOT NULL,
  sessionExpirationOverride char(1),
  sessionExpirationPeriods int,
  sessionExpirationPeriodType nvarchar(25),
  organization nvarchar(80),
  organizationalRole nvarchar(80),
  createdTs bigint NOT NULL,
  emailVerifiedTs bigint,
  data ntext,
  primary key (id)
);
alter table users add constraint username_unique unique (username);
alter table users add constraint email_unique unique (email);

create table userComments (
  id int not null identity,
  xid nvarchar(100) not null,
  userId int,
  commentType int not null,
  typeKey int not null,
  ts bigint not null,
  commentText nvarchar(1024) not null
);
alter table userComments add constraint userCommentsFk1 foreign key (userId) references users(id);
alter table userComments add constraint userCommentsUn1 unique (xid);
CREATE INDEX userComments_performance1 ON userComments (commentType ASC, typeKey ASC);

--
-- Mailing lists
create table mailingLists (
  id int not null identity,
  xid nvarchar(100) not null,
  name nvarchar(40) not null,
  receiveAlarmEmails int not null,
  primary key (id)
);
alter table mailingLists add constraint mailingListsUn1 unique (xid);

create table mailingListInactive (
  mailingListId int not null,
  inactiveInterval int not null
);
alter table mailingListInactive add constraint mailingListInactiveFk1 foreign key (mailingListId) 
  references mailingLists(id) on delete cascade;

create table mailingListMembers (
  mailingListId int not null,
  typeId int not null,
  userId int,
  address nvarchar(255)
);
alter table mailingListMembers add constraint mailingListMembersFk1 foreign key (mailingListId) 
  references mailingLists(id) on delete cascade;


--
--
-- Data Sources
--
create table dataSources (
  id int not null identity,
  xid nvarchar(100) not null,
  name nvarchar(40) not null,
  dataSourceType nvarchar(40) not null,
  data image not null,
  rtdata image,
  primary key (id)
);
alter table dataSources add constraint dataSourcesUn1 unique (xid);
CREATE INDEX nameIndex on dataSources (name ASC);

--
--
-- Data Points
--
create table dataPoints (
  id int not null identity,
  xid nvarchar(100) not null,
  dataSourceId int not null,
  name nvarchar(255),
  deviceName nvarchar(255),
  enabled char(1),
  loggingType int,
  intervalLoggingPeriodType int,
  intervalLoggingPeriod int,
  intervalLoggingType int,
  tolerance float,
  purgeOverride char(1),
  purgeType int,
  purgePeriod int,
  defaultCacheSize int,
  discardExtremeValues char(1),
  engineeringUnits int,
  data image not null,
  rollup int,
  dataTypeId int not null,
  settable char(1),
  primary key (id)
);
alter table dataPoints add constraint dataPointsUn1 unique (xid);
alter table dataPoints add constraint dataPointsFk1 foreign key (dataSourceId) references dataSources(id);
CREATE INDEX pointNameIndex on dataPoints (name ASC);
CREATE INDEX deviceNameIndex on dataPoints (deviceName ASC);
CREATE INDEX deviceNameNameIndex on dataPoints (deviceName ASC, name ASC);
CREATE INDEX enabledIndex on dataPoints (enabled ASC);
CREATE INDEX xidNameIndex on dataPoints (xid ASC, name ASC);

-- Data point tags
CREATE TABLE dataPointTags (
  dataPointId INT NOT NULL,
  tagKey VARCHAR(255) NOT NULL,
  tagValue VARCHAR(255) NOT NULL,
  PRIMARY KEY (dataPointId, tagKey)
);
ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;
CREATE INDEX dataPointTagsIndex1 ON dataPointTags (tagKey ASC, tagValue ASC);

--
--
-- Point Values (historical data)
--
create table pointValues (
  id bigint not null identity,
  dataPointId int not null,
  dataType int not null,
  pointValue float,
  ts bigint not null,
  primary key (id)
);
create index pointValuesIdx1 on pointValues (dataPointId, ts);

create table pointValueAnnotations (
  pointValueId bigint not null,
  textPointValueShort nvarchar(128),
  textPointValueLong ntext,
  sourceMessage ntext,
  primary key (pointValueId)
);
  
--
--
-- Event detectors
--
CREATE TABLE eventDetectors (
  id int NOT NULL identity,
  xid nvarchar(100) NOT NULL,
  sourceTypeName nvarchar(32) NOT NULL,
  typeName nvarchar(32) NOT NULL,
  dataPointId int,
  data ntext NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);
ALTER TABLE eventDetectors ADD CONSTRAINT dataPointIdFk FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);

--
--
-- Events
--
create table events (
  id int not null identity,
  typeName nvarchar(32) not null,
  subtypeName nvarchar(32),
  typeRef1 int not null,
  typeRef2 int not null,
  activeTs bigint not null,
  rtnApplicable char(1) not null,
  rtnTs bigint,
  rtnCause int,
  alarmLevel int not null,
  message ntext,
  ackTs bigint,
  ackUserId int,
  alternateAckSource ntext,
  primary key (id)
);
alter table events add constraint eventsFk1 foreign key (ackUserId) references users(id);
CREATE INDEX events_performance1 ON events (activeTs ASC);
CREATE INDEX events_performance2 ON events (rtnApplicable ASC, rtnTs ASC);
CREATE INDEX events_performance3 ON events (typeName ASC, subTypeName ASC, typeRef1 ASC);

--
--
-- Event handlers
--
create table eventHandlers (
  id int not null identity,
  xid nvarchar(100) not null,
  alias nvarchar(255) not null,
  eventHandlerType nvarchar(40) not null,
  
  -- Event type, see events
  eventTypeName nvarchar(32) not null,
  eventSubtypeName nvarchar(32),
  eventTypeRef1 int not null,
  eventTypeRef2 int not null,
  
  data image not null,
  primary key (id)
);
alter table eventHandlers add constraint eventHandlersUn1 unique (xid);

--
--
-- Audit Table
-- 
CREATE TABLE audit (
  id int NOT NULL identity,
  typeName nvarchar(32) NOT NULL,
  alarmLevel int NOT NULL,
  userId int NOT NULL,
  changeType int NOT NULL,
  objectId int NOT NULL,
  ts bigint NOT NULL,
  context ntext,
  message nvarchar(255),
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
create table publishers (
  id int not null identity,
  xid nvarchar(100) not null,
  publisherType nvarchar(40) not null,
  data image not null,
  rtdata image,
  primary key (id)
);
alter table publishers add constraint publishersUn1 unique (xid);

`
--
--
-- JsonData
--
CREATE TABLE jsonData (
  	id int not null identity,
	xid nvarchar(100) not null,
	name nvarchar(255) not null,
  	publicData char(1),
  	data ntext,
    primary key (id)
);
ALTER TABLE jsonData ADD CONSTRAINT jsonDataUn1 UNIQUE (xid);

--
--
-- InstalledModules
--  Thirty character restriction is from the store
CREATE TABLE installedModules (
	name nvarchar(30) not null,
	version nvarchar(255) not null
);
ALTER TABLE installedModules ADD CONSTRAINT installModulesUn1 UNIQUE (name);

--
--
-- FileStores
--
CREATE TABLE fileStores (
	id int not null auto_increment, 
	storeName nvarchar(100) not null, 
	PRIMARY KEY (id)
);
ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (storeName);

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
--
-- Role Mappings
--
CREATE TABLE roleMappings (
	roleId int not null,
	voId int,
	voType nvarchar(255),
	permissionType nvarchar(255) not null
);
ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);

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

--
--
-- Mango Default Data
--
-- Insert admin user
INSERT INTO users (id, name, username, password, email, phone, disabled, lastLogin, homeUrl, receiveAlarmEmails, receiveOwnAuditEvents, muted, locale, tokenVersion, passwordVersion, passwordChangeTimestamp, sessionExpirationOverride, createdTs) VALUES 
	(1, 'Administrator', 'admin', '{BCRYPT}$2a$10$L6Jea9zZ79Hc82trIesw0ekqH0Q8hTGOBqSGutoi17p2UZ.j3vzWm', 'admin@mango.example.com', '', 'N', 0, '/ui/administration/home', -3, 'N', 'Y', '', 1, 1, 0, 'N', 0);      
-- Insert default roles
INSERT INTO roles (id, xid, name) VALUES (1, 'superadmin', 'Superadmin role');
INSERT INTO roles (id, xid, name) VALUES (2, 'user', 'User role');
-- Add admin user role mappings
INSERT INTO userRoleMappings (roleId, userId) VALUES (1, 1);
INSERT INTO userRoleMappings (roleId, userId) VALUES (2, 1);