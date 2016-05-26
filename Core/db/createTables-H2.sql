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
  xid varchar(50) NOT NULL,
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
  PRIMARY KEY (id)
);

CREATE TABLE userComments (
  userId int,
  commentType int NOT NULL,
  typeKey int NOT NULL,
  ts bigint NOT NULL,
  commentText varchar(1024) NOT NULL
);
ALTER TABLE userComments ADD CONSTRAINT userCommentsFk1 FOREIGN KEY (userId) REFERENCES users(id);


--
-- Mailing lists
CREATE TABLE mailingLists (
  id int NOT NULL auto_increment,
  xid varchar(50) NOT NULL,
  name varchar(255) NOT NULL,
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
  xid varchar(50) NOT NULL,
  name varchar(255) NOT NULL,
  dataSourceType varchar(40) NOT NULL,
  data longblob NOT NULL,
  rtdata longblob,
  editPermission varchar(255),
  PRIMARY KEY (id)
);
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesUn1 UNIQUE (xid);


--
-- Data Points
CREATE TABLE dataPoints (
  id int NOT NULL auto_increment,
  xid varchar(50) NOT NULL,
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
  PRIMARY KEY (id)
);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsUn1 UNIQUE (xid);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk1 FOREIGN KEY (dataSourceId) REFERENCES dataSources(id);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (templateId) REFERENCES templates(id);

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
  xid varchar(50) NOT NULL,
  typeName varchar(32) NOT NULL,
  sourceId int NOT NULL,
  data longtext NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid, sourceId);

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
  xid varchar(50) NOT NULL,
  alias varchar(255),
  eventHandlerType varchar(40) NOT NULL,
  
  -- Event type, see events
  eventTypeName varchar(32) NOT NULL,
  eventSubtypeName varchar(32),
  eventTypeRef1 int NOT NULL,
  eventTypeRef2 int NOT NULL,
  
  data longblob NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersUn1 UNIQUE (xid);

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
CREATE INDEX audit_performance1 ON audit (`ts` ASC);

--
--
-- Publishers
--
CREATE TABLE publishers (
  id int NOT NULL auto_increment,
  xid varchar(50) NOT NULL,
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
	xid varchar(50) not null,
	name varchar(255) not null,
	readPermission varchar(255),
  	editPermission varchar(255),
  	data longtext,
  	primary key (id)
);
ALTER TABLE jsonData ADD CONSTRAINT jsonDataUn1 UNIQUE (xid);
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

