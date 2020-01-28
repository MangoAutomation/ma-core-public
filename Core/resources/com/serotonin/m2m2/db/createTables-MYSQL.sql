--
--    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
--    @author Matthew Lohbihler
--

-- Make sure that everything get created with utf8 as the charset.
alter database default character set utf8;

--
-- System settings
create table systemSettings (
  settingName varchar(64) not null,
  settingValue longtext,
  primary key (settingName)
) engine=InnoDB;

--
-- Users
create table users (
  id int not null auto_increment,
  username varchar(40) not null,
  password varchar(255) not null,
  email varchar(255) not null,
  phone varchar(40),
  disabled char(1) not null,
  lastLogin bigint,
  homeUrl varchar(255),
  receiveAlarmEmails int not null,
  receiveOwnAuditEvents char(1) not null,
  timezone varchar(50),
  muted char(1),
  name varchar(255),
  locale varchar(50),
  tokenVersion int not null,
  passwordVersion int not null,
  passwordChangeTimestamp bigint NOT NULL,
  sessionExpirationOverride char(1),
  sessionExpirationPeriods int,
  sessionExpirationPeriodType varchar(25),
  organization varchar(80),
  organizationalRole varchar(80),
  createdTs bigint NOT NULL,
  emailVerifiedTs bigint,
  data JSON,
  primary key (id)
) engine=InnoDB;
ALTER TABLE users ADD CONSTRAINT username_unique UNIQUE(username);
ALTER TABLE users ADD CONSTRAINT email_unique UNIQUE(email);

create table userComments (
  id int not null auto_increment,
  xid varchar(100) not null,
  userId int,
  commentType int not null,
  typeKey int not null,
  ts bigint not null,
  commentText varchar(1024) not null,
  primary key (id)
) engine=InnoDB;
alter table userComments add constraint userCommentsFk1 foreign key (userId) references users(id);
alter table userComments add constraint userCommentsUn1 unique (xid);
ALTER TABLE userComments ADD INDEX userComments_performance1 (`commentType` ASC, `typeKey` ASC);

--
-- Mailing lists
create table mailingLists (
  id int not null auto_increment,
  xid varchar(100) not null,
  name varchar(40) not null,
  receiveAlarmEmails int not null,
  primary key (id)
) engine=InnoDB;
alter table mailingLists add constraint mailingListsUn1 unique (xid);

create table mailingListInactive (
  mailingListId int not null,
  inactiveInterval int not null
) engine=InnoDB;
alter table mailingListInactive add constraint mailingListInactiveFk1 foreign key (mailingListId) 
  references mailingLists(id) on delete cascade;

create table mailingListMembers (
  mailingListId int not null,
  typeId int not null,
  userId int,
  address varchar(255)
) engine=InnoDB;
alter table mailingListMembers add constraint mailingListMembersFk1 foreign key (mailingListId) 
  references mailingLists(id) on delete cascade;

--
--
-- Data Sources
--
create table dataSources (
  id int not null auto_increment,
  xid varchar(100) not null,
  name varchar(40) not null,
  dataSourceType varchar(40) not null,
  data longblob not null,
  rtdata longblob,
  primary key (id)
) engine=InnoDB;
alter table dataSources add constraint dataSourcesUn1 unique (xid);
ALTER TABLE dataSources ADD INDEX nameIndex (name ASC);

--
--
-- Data Points
--
CREATE TABLE dataPoints (
  id int not null auto_increment,
  xid varchar(100) not null,
  dataSourceId int not null,
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
  data longblob not null,
  templateId int,
  rollup int,
  dataTypeId int not null,
  settable char(1),
  primary key (id)
) engine=InnoDB;
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsUn1 UNIQUE (xid);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk1 FOREIGN KEY (dataSourceId) REFERENCES dataSources(id);
CREATE INDEX pointNameIndex on dataPoints (name ASC);
CREATE INDEX deviceNameIndex on dataPoints (deviceName ASC);
CREATE INDEX deviceNameNameIndex on dataPoints (deviceName ASC, name ASC);
CREATE INDEX enabledIndex on dataPoints (enabled ASC);
CREATE INDEX xidNameIndex on dataPoints (xid ASC, name ASC);

-- Data point tags
CREATE TABLE dataPointTags (
  dataPointId INT NOT NULL,
  tagKey VARCHAR(255) NOT NULL,
  tagValue VARCHAR(255) NOT NULL
) engine=InnoDB;
ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsUn1 UNIQUE (dataPointId ASC, tagKey ASC);
ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;
CREATE INDEX dataPointTagsIndex1 ON dataPointTags (tagKey ASC, tagValue ASC);


--
--
-- Point Values (historical data)
--
create table pointValues (
  id bigint not null auto_increment,
  dataPointId int not null,
  dataType int not null,
  pointValue double,
  ts bigint not null,
  primary key (id)
) engine=InnoDB;
create index pointValuesIdx1 on pointValues (dataPointId, ts);

create table pointValueAnnotations (
  pointValueId bigint not null,
  textPointValueShort varchar(128),
  textPointValueLong longtext,
  sourceMessage longtext,
  primary key (pointValueId)
) engine=InnoDB;

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
  PRIMARY KEY (id)
)engine=InnoDB;
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);
ALTER TABLE eventDetectors ADD CONSTRAINT dataPointIdFk FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);

--
--
-- Events
--
create table events (
  id int not null auto_increment,
  typeName varchar(32) not null,
  subtypeName varchar(32),
  typeRef1 int not null,
  typeRef2 int not null,
  activeTs bigint not null,
  rtnApplicable char(1) not null,
  rtnTs bigint,
  rtnCause int,
  alarmLevel int not null,
  message longtext,
  ackTs bigint,
  ackUserId int,
  alternateAckSource longtext,
  primary key (id)
) engine=InnoDB;
alter table events add constraint eventsFk1 foreign key (ackUserId) references users(id);
alter table events add index performance1 (activeTs ASC);
ALTER TABLE events ADD INDEX events_performance2 (`rtnApplicable` ASC, `rtnTs` ASC);
ALTER TABLE events ADD INDEX events_performance3 (`typeName` ASC, `subTypeName` ASC, `typeRef1` ASC);

--
--
-- Event handlers
--
create table eventHandlers (
  id int not null auto_increment,
  xid varchar(100) not null,
  alias varchar(255) not null,
  eventHandlerType varchar(40) NOT NULL,
  data longblob not null,
  primary key (id)
) engine=InnoDB;
alter table eventHandlers add constraint eventHandlersUn1 unique (xid);

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
)engine=InnoDB;
CREATE INDEX tsIndex ON audit (ts ASC);
CREATE INDEX userIdIndex ON audit (userId ASC);
CREATE INDEX typeNameIndex ON audit (typeName ASC);
CREATE INDEX alarmLevelIndex ON audit (alarmLevel ASC);

--
--
-- Publishers
--
create table publishers (
  id int not null auto_increment,
  xid varchar(100) not null,
  publisherType varchar(40) not null,
  data longblob not null,
  rtdata longblob,
  primary key (id)
) engine=InnoDB;
alter table publishers add constraint publishersUn1 unique (xid);

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
    primary key (id)
) engine=InnoDB;
ALTER TABLE jsonData ADD CONSTRAINT jsonDataUn1 UNIQUE (xid);

--
--
-- InstalledModules
--  Thirty character restriction is from the store
CREATE TABLE installedModules (
	name varchar(30) not null,
	version varchar(255) not null
) engine=InnoDB;
ALTER TABLE installedModules ADD CONSTRAINT installModulesUn1 UNIQUE (name);

--
--
-- FileStores
--
CREATE TABLE fileStores (
	id int not null auto_increment, 
	storeName varchar(100) not null, 
	PRIMARY KEY (id)
) engine=InnoDB;
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
) engine=InnoDB;
ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);

--
--
-- Role Mappings
--
CREATE TABLE roleMappings (
	roleId int not null,
	voId int,
	voType varchar(255),
	permissionType varchar(255) not null
) engine=InnoDB;
ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);

--
--
-- User Role Mappings
--
CREATE TABLE userRoleMappings (
	roleId int not null,
	userId int not null
) engine=InnoDB;
ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsUn1 UNIQUE (roleId,userId);


