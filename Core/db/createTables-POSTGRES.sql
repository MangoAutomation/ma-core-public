--
--    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
--    @author Matthew Lohbihler
--

--
-- System settings
CREATE TABLE systemSettings (
  settingName varchar(64) NOT NULL,
  settingValue text,
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
  id SERIAL,
  username varchar(40) NOT NULL,
  password varchar(255) NOT NULL,
  email varchar(255) NOT NULL,
  phone varchar(40),
  disabled character(1) NOT NULL,
  lastLogin bigint,
  homeUrl varchar(255),
  receiveAlarmEmails integer NOT NULL,
  receiveOwnAuditEvents character(1) NOT NULL,
  timezone varchar(50),
  muted character(1),
  permissions varchar(255),
  name varchar(255),
  locale varchar(50),
  tokenVersion integer NOT NULL,
  passwordVersion integer NOT NULL,
  passwordChangeTimestamp bigint NOT NULL,
  sessionExpirationOverride char(1),
  sessionExpirationPeriods int,
  sessionExpirationPeriodType varchar(25),
  organization varchar(80),
  organizationalRole varchar(80),
  createdTs bigint NOT NULL,
  emailVerifiedTs bigint,
  data JSON,
  PRIMARY KEY (id)
);
ALTER TABLE users ADD CONSTRAINT username_unique UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT email_unique UNIQUE (email);

CREATE TABLE userComments (
  id int not null auto_increment,
  xid varchar(100) NOT NULL,
  userId integer,
  commentType integer NOT NULL,
  typeKey integer NOT NULL,
  ts bigint NOT NULL,
  commentText varchar(1024) NOT NULL
);
ALTER TABLE userComments ADD CONSTRAINT userCommentsFk1 FOREIGN KEY (userId) REFERENCES users(id);
ALTER TABLE userComments ADD CONSTRAINT userCommentsUn1 UNIQUE (xid);
ALTER TABLE userComments ADD INDEX userComments_performance1 (`commentType` ASC, `typeKey` ASC);

--
-- Mailing lists
CREATE TABLE mailingLists (
  id SERIAL,
  xid varchar(100) NOT NULL,
  name varchar(40) NOT NULL,
  receiveAlarmEmails INT NOT NULL,
  readPermission varchar(255),
  editPermission varchar(255),
  PRIMARY KEY (id)
);
ALTER TABLE mailingLists ADD CONSTRAINT mailingListsUn1 UNIQUE (xid);

CREATE TABLE mailingListInactive (
  mailingListId integer NOT NULL,
  inactiveInterval integer NOT NULL
);
ALTER TABLE mailingListInactive ADD CONSTRAINT mailingListInactiveFk1 FOREIGN KEY (mailingListId) 
  REFERENCES mailingLists(id) ON DELETE CASCADE;

CREATE TABLE mailingListMembers (
  mailingListId integer NOT NULL,
  typeId integer NOT NULL,
  userId integer,
  address varchar(255)
);
ALTER TABLE mailingListMembers ADD CONSTRAINT mailingListMembersFk1 FOREIGN KEY (mailingListId) 
  REFERENCES mailingLists(id) ON DELETE CASCADE;




--
--
-- Data Sources
--
CREATE TABLE dataSources (
  id SERIAL,
  xid varchar(100) NOT NULL,
  name varchar(40) NOT NULL,
  dataSourceType varchar(40) NOT NULL,
  data bytea NOT NULL,
  rtdata bytea,
  editPermission varchar(255),
  PRIMARY KEY (id)
);
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesUn1 UNIQUE (xid);
CREATE INDEX nameIndex on dataSources (name ASC);
CREATE INDEX dataSourcesPermissionIndex on dataSources (editPermission ASC);

--
--
-- Data Points
--
CREATE TABLE dataPoints (
  id SERIAL,
  xid varchar(100) NOT NULL,
  dataSourceId integer NOT NULL,
  name varchar(255),
  deviceName varchar(255),
  enabled character(1),
  pointFolderId integer,
  loggingType integer,
  intervalLoggingPeriodType integer,
  intervalLoggingPeriod integer,
  intervalLoggingType integer,
  tolerance double precision,
  purgeOverride character(1),
  purgeType integer,
  purgePeriod integer,
  defaultCacheSize integer,
  discardExtremeValues character(1),
  engineeringUnits integer,
  data bytea NOT NULL,
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
  tagValue VARCHAR(255) NOT NULL,
  PRIMARY KEY (dataPointId, tagKey)
);
ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;
CREATE INDEX dataPointTagsIndex1 ON dataPointTags (tagKey ASC, tagValue ASC);

-- Data point hierarchy
CREATE TABLE dataPointHierarchy (
  id SERIAL,
  parentId integer,
  name varchar(100),
  PRIMARY KEY (id)
);


--
--
-- Point Values (historical data)
--
CREATE TABLE pointValues (
  id BIGSERIAL,
  dataPointId integer NOT NULL,
  dataType integer NOT NULL,
  pointValue double precision,
  ts bigint NOT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX pointValuesIdx1 ON pointValues (dataPointId, ts);

CREATE TABLE pointValueAnnotations (
  pointValueId bigint NOT NULL,
  textPointValueShort varchar(128),
  textPointValueLong text,
  sourceMessage text,
  PRIMARY KEY (pointValueId)
);

--
--
-- Event detectors
--
CREATE TABLE eventDetectors (
  id SERIAL,
  xid varchar(100) NOT NULL,
  sourceTypeName varchar(32) NOT NULL,
  typeName varchar(32) NOT NULL,
  dataPointId int NOT NULL,
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
  id SERIAL,
  typeName varchar(32) NOT NULL,
  subtypeName varchar(32),
  typeRef1 integer NOT NULL,
  typeRef2 integer NOT NULL,
  activeTs bigint NOT NULL,
  rtnApplicable character(1) NOT NULL,
  rtnTs bigint,
  rtnCause integer,
  alarmLevel integer NOT NULL,
  message text,
  ackTs bigint,
  ackUserId integer,
  alternateAckSource text,
  PRIMARY KEY (id)
);
ALTER TABLE events ADD CONSTRAINT eventsFk1 FOREIGN KEY (ackUserId) REFERENCES users(id);
alter table events add index performance1 (activeTs ASC);
ALTER TABLE events ADD INDEX events_performance2 (`rtnApplicable` ASC, `rtnTs` ASC);
ALTER TABLE events ADD INDEX events_performance3 (`typeName` ASC, `subTypeName` ASC, `typeRef1` ASC);

CREATE TABLE userEvents (
  eventId integer NOT NULL,
  userId integer NOT NULL,
  silenced character(1) NOT NULL,
  PRIMARY KEY (eventId, userId)
);
ALTER TABLE userEvents ADD CONSTRAINT userEventsFk1 FOREIGN KEY (eventId) REFERENCES events(id) ON DELETE CASCADE;
ALTER TABLE userEvents ADD CONSTRAINT userEventsFk2 FOREIGN KEY (userId) REFERENCES users(id);
alter table userEvents add index performance1 (userId ASC, silenced ASC);

--
--
-- Event handlers
--
CREATE TABLE eventHandlers (
  id SERIAL,
  xid varchar(100) NOT NULL,
  alias varchar(255) NOT NULL,
  eventHandlerType varchar(40) NOT NULL,
  
  -- Event type, see events
  eventTypeName varchar(32) NOT NULL,
  eventSubtypeName varchar(32),
  eventTypeRef1 integer NOT NULL,
  eventTypeRef2 integer NOT NULL,
  
  data bytea NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersUn1 UNIQUE (xid);

--
--
-- Audit Table
-- 
CREATE TABLE audit (
  id SERIAL,
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
  id SERIAL,
  xid varchar(100) NOT NULL,
  publisherType varchar(40) NOT NULL,
  data bytea NOT NULL,
  rtdata bytea,
  PRIMARY KEY (id)
);
ALTER TABLE publishers ADD CONSTRAINT publishersUn1 UNIQUE (xid);

CREATE TABLE jsonData (
  	id SERIAL,
	xid varchar(100) not null,
	name varchar(255) not null,
	readPermission varchar(255),
  	editPermission varchar(255),
  	data clob,
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
-- CREATE TABLE compoundEventDetectors (
--   id SERIAL,
--   xid varchar(50) NOT NULL,
--   name varchar(100),
--   alarmLevel int not null,
--   returnToNormal char(1) not null,
--   disabled char(1) not null,
--   conditionText varchar(256) not null,
--   primary key (id)
-- );
-- alter table compoundEventDetectors add constraint compoundEventDetectorsUn1 unique (xid);

