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
-- Users
CREATE TABLE users (
  id int NOT NULL auto_increment,
  username varchar(40) NOT NULL,
  password varchar(30) NOT NULL,
  email varchar(255),
  phone varchar(40),
  admin char(1) NOT NULL,
  disabled char(1) NOT NULL,
  lastLogin bigint,
  homeUrl varchar(255),
  receiveAlarmEmails int NOT NULL,
  receiveOwnAuditEvents char(1) NOT NULL,
  timezone varchar(50),
  muted char(1),
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
  PRIMARY KEY (id)
);
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesUn1 UNIQUE (xid);


-- Data source permissions
CREATE TABLE dataSourceUsers (
  dataSourceId int NOT NULL,
  userId int NOT NULL
);
ALTER TABLE dataSourceUsers ADD CONSTRAINT dataSourceUsersFk1 FOREIGN KEY (dataSourceId) REFERENCES dataSources(id);
ALTER TABLE dataSourceUsers ADD CONSTRAINT dataSourceUsersFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;


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
  PRIMARY KEY (id)
);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsUn1 UNIQUE (xid);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk1 FOREIGN KEY (dataSourceId) REFERENCES dataSources(id);

-- Data point hierarchy
CREATE TABLE dataPointHierarchy (
  id int NOT NULL,
  parentId int,
  name varchar(100)
);
ALTER TABLE dataPointHierarchy ADD CONSTRAINT dataPointHierarchyPk PRIMARY KEY (id);

-- Data point permissions
CREATE TABLE dataPointUsers (
  dataPointId int NOT NULL,
  userId int NOT NULL,
  permission int NOT NULL
);
ALTER TABLE dataPointUsers ADD CONSTRAINT dataPointUsersFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);
ALTER TABLE dataPointUsers ADD CONSTRAINT dataPointUsersFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;


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
-- Point event detectors
--
CREATE TABLE pointEventDetectors (
  id int NOT NULL auto_increment,
  xid varchar(50) NOT NULL,
  alias varchar(255),
  dataPointId int NOT NULL,
  detectorType int NOT NULL,
  alarmLevel int NOT NULL,
  stateLimit double,
  duration int,
  durationType int,
  binaryState char(1),
  multistateState int,
  changeCount int,
  alphanumericState varchar(128),
  weight double,
  PRIMARY KEY (id)
);
ALTER TABLE pointEventDetectors ADD CONSTRAINT pointEventDetectorsUn1 UNIQUE (xid, dataPointId);
ALTER TABLE pointEventDetectors ADD CONSTRAINT pointEventDetectorsFk1 FOREIGN KEY (dataPointId) 
  REFERENCES dataPoints(id);


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
-- Publishers
--
CREATE TABLE publishers (
  id int NOT NULL auto_increment,
  xid varchar(50) NOT NULL,
  publisherType varchar(40) NOT NULL,
  data longblob NOT NULL,
  rtdata longblob,
  tags longtext,
  PRIMARY KEY (id)
);
ALTER TABLE publishers ADD CONSTRAINT publishersUn1 UNIQUE (xid);
