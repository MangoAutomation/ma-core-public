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
  PRIMARY KEY (id)
);

CREATE TABLE userComments (
  userId integer,
  commentType integer NOT NULL,
  typeKey integer NOT NULL,
  ts bigint NOT NULL,
  commentText varchar(1024) NOT NULL
);
ALTER TABLE userComments ADD CONSTRAINT userCommentsFk1 FOREIGN KEY (userId) REFERENCES users(id);


--
-- Mailing lists
CREATE TABLE mailingLists (
  id SERIAL,
  xid varchar(50) NOT NULL,
  name varchar(40) NOT NULL,
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
  xid varchar(50) NOT NULL,
  name varchar(40) NOT NULL,
  dataSourceType varchar(40) NOT NULL,
  data bytea NOT NULL,
  rtdata bytea,
  editPermission varchar(255),
  PRIMARY KEY (id)
);
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesUn1 UNIQUE (xid);


--
--
-- Data Points
--
CREATE TABLE dataPoints (
  id SERIAL,
  xid varchar(50) NOT NULL,
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
  PRIMARY KEY (id)
);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsUn1 UNIQUE (xid);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk1 FOREIGN KEY (dataSourceId) REFERENCES dataSources(id);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (templateId) REFERENCES templates(id);


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
-- Point event detectors
--
CREATE TABLE pointEventDetectors (
  id SERIAL,
  xid varchar(50) NOT NULL,
  alias varchar(255),
  dataPointId integer NOT NULL,
  detectorType integer NOT NULL,
  alarmLevel integer NOT NULL,
  stateLimit double precision,
  duration integer,
  durationType integer,
  binaryState character(1),
  multistateState integer,
  changeCount integer,
  alphanumericState varchar(128),
  weight double precision,
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

CREATE TABLE userEvents (
  eventId integer NOT NULL,
  userId integer NOT NULL,
  silenced character(1) NOT NULL,
  PRIMARY KEY (eventId, userId)
);
ALTER TABLE userEvents ADD CONSTRAINT userEventsFk1 FOREIGN KEY (eventId) REFERENCES events(id) ON DELETE CASCADE;
ALTER TABLE userEvents ADD CONSTRAINT userEventsFk2 FOREIGN KEY (userId) REFERENCES users(id);


--
--
-- Event handlers
--
CREATE TABLE eventHandlers (
  id SERIAL,
  xid varchar(50) NOT NULL,
  alias varchar(255),
  
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
-- Publishers
--
CREATE TABLE publishers (
  id SERIAL,
  xid varchar(50) NOT NULL,
  publisherType varchar(40) NOT NULL,
  data bytea NOT NULL,
  rtdata bytea,
  PRIMARY KEY (id)
);
ALTER TABLE publishers ADD CONSTRAINT publishersUn1 UNIQUE (xid);

CREATE TABLE jsonData (
  	id SERIAL,
	xid varchar(50) not null,
	name varchar(255) not null,
	readPermission varchar(255),
  	editPermission varchar(255),
  	data clob,
    primary key (id)
)engine=InnoDB;
ALTER TABLE jsonData ADD CONSTRAINT jsonDataUn1 UNIQUE (xid);

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

