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
-- Templates
CREATE TABLE templates (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  name varchar(255),
  templateType varchar(50),
  readPermission varchar(255),
  setPermission varchar(255),
  data longblob NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE templates ADD CONSTRAINT templatesUn1 UNIQUE (xid);

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
  permissions varchar(255),
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
  readPermission varchar(255),
  editPermission varchar(255),
  PRIMARY KEY (id)
);
ALTER TABLE mailingLists ADD CONSTRAINT mailingListsUn1 UNIQUE (xid);

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
  rtdata longblob,
  editPermission varchar(255),
  PRIMARY KEY (id)
);
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesUn1 UNIQUE (xid);
CREATE INDEX nameIndex ON dataSources (name ASC);
CREATE INDEX dataSourcesPermissionIndex on dataSources (editPermission ASC);

--
-- Data Points
CREATE TABLE dataPoints (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  dataSourceId int NOT NULL,
  name varchar(255),
  deviceName varchar(255),
  enabled char(1),
  pointFolderId int,
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
  readPermission varchar(255),
  setPermission varchar(255),
  templateId int,
  rollup int,
  dataTypeId int not null,
  settable char(1),
  PRIMARY KEY (id)
);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsUn1 UNIQUE (xid);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk1 FOREIGN KEY (dataSourceId) REFERENCES dataSources(id);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (templateId) REFERENCES templates(id);
CREATE INDEX pointNameIndex on dataPoints (name ASC);
CREATE INDEX deviceNameIndex on dataPoints (deviceName ASC);
CREATE INDEX pointFolderIdIndex on dataPoints (pointFolderId ASC);
CREATE INDEX deviceNameNameIndex on dataPoints (deviceName ASC, name ASC);
CREATE INDEX enabledIndex on dataPoints (enabled ASC);
CREATE INDEX xidNameIndex on dataPoints (xid ASC, name ASC);
CREATE INDEX dataPointsPermissionIndex on dataPoints (dataSourceId ASC, readPermission ASC, setPermission ASC);

-- Data point tags
CREATE TABLE dataPointTags (
  dataPointId INT NOT NULL,
  tagKey VARCHAR(255) NOT NULL,
  tagValue VARCHAR(255) NOT NULL
);
ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsUn1 UNIQUE (dataPointId ASC, tagKey ASC);
ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;
CREATE INDEX dataPointTagsIndex1 ON dataPointTags (tagKey ASC, tagValue ASC);

-- Data point hierarchy
CREATE TABLE dataPointHierarchy (
  id int NOT NULL,
  parentId int,
  name varchar(100)
);
ALTER TABLE dataPointHierarchy ADD CONSTRAINT dataPointHierarchyPk PRIMARY KEY (id);


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
  PRIMARY KEY (id)
);
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);
ALTER TABLE eventDetectors ADD CONSTRAINT dataPointIdFk FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);

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


CREATE TABLE userEvents (
  eventId int NOT NULL,
  userId int NOT NULL,
  silenced char(1) NOT NULL,
  PRIMARY KEY (eventId, userId)
);
ALTER TABLE userEvents ADD CONSTRAINT userEventsFk1 FOREIGN KEY (eventId) REFERENCES events(id) ON DELETE CASCADE;
ALTER TABLE userEvents ADD CONSTRAINT userEventsFk2 FOREIGN KEY (userId) REFERENCES users(id);
CREATE INDEX userEvents_performance1 ON userEvents (`userId` ASC, `silenced` ASC);

--
--
-- Event handlers
--
CREATE TABLE eventHandlers (
  id int NOT NULL auto_increment,
  xid varchar(100) NOT NULL,
  alias varchar(255) NOT NULL,
  eventHandlerType varchar(40) NOT NULL,
  data longblob NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersUn1 UNIQUE (xid);

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
	readPermission varchar(255),
  	editPermission varchar(255),
  	publicData char(1),
  	data longtext,
  	primary key (id)
);
ALTER TABLE jsonData ADD CONSTRAINT jsonDataUn1 UNIQUE (xid);

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
	readPermission varchar(255), 
	writePermission varchar(255),
	PRIMARY KEY (id)
);
ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (storeName);

--
--
-- Compound events detectors
--
-- create table compoundEventDetectors (
--   id int not null auto_increment,
--   xid varchar(50) not null,
--   name varchar(100),
--   alarmLevel int not null,
--   returnToNormal char(1) not null,
--   disabled char(1) not null,
--   conditionText varchar(256) not null,
--   primary key (id)
-- );
-- alter table compoundEventDetectors add constraint compoundEventDetectorsUn1 unique (xid);

