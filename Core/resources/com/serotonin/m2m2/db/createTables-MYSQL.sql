--
--    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
--    @author Matthew Lohbihler
--

-- Make sure that everything get created with utf8mb4 as the charset.
-- Collation is full unicode comparison, case-insensitive
ALTER DATABASE DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SET default_storage_engine=innodb;

--
-- System settings
--
CREATE TABLE systemSettings (
    settingName varchar(64) NOT NULL,
    settingValue longtext,
    PRIMARY KEY (settingName)
);

--
-- Roles
--
CREATE TABLE roles (
    id int NOT NULL AUTO_INCREMENT,
    xid varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);

--
-- Role Inheritance Mappings
--
CREATE TABLE roleInheritance (
	roleId int NOT NULL,
	inheritedRoleId int NOT NULL
);
ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceUn1 UNIQUE (roleId,inheritedRoleId);
ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk2 FOREIGN KEY (inheritedRoleId) REFERENCES roles(id) ON DELETE CASCADE;

CREATE TABLE minterms (
    id int NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id)
);

CREATE TABLE mintermsRoles (
    mintermId int NOT NULL,
    roleId int NOT NULL
);
ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesIdx1 UNIQUE (mintermId,roleId);
ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk1 FOREIGN KEY (mintermId) REFERENCES minterms(id) ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk2 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE ON UPDATE NO ACTION;

CREATE INDEX mintermsRolesFk1Idx ON mintermsRoles (mintermId);
CREATE INDEX mintermsRolesFk2Idx ON mintermsRoles (roleId);

CREATE TABLE permissions (
    id int NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id)
);

CREATE TABLE permissionsMinterms (
    permissionId int NOT NULL,
    mintermId int NOT NULL
);
ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsIdx1 UNIQUE (permissionId,mintermId);
ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk1 FOREIGN KEY (permissionId) REFERENCES permissions(id) ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk2 FOREIGN KEY (mintermId) REFERENCES minterms(id) ON DELETE CASCADE ON UPDATE NO ACTION;

CREATE INDEX permissionsMintermsFk1Idx ON permissionsMinterms (permissionId);
CREATE INDEX permissionsMintermsFk2Idx ON permissionsMinterms (mintermId);

--
-- System wide permissions
--
CREATE TABLE systemPermissions (
    permissionType varchar(255),
    permissionId int NOT NULL
);
ALTER TABLE systemPermissions ADD CONSTRAINT systemPermissionsFk1 FOREIGN KEY (permissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE systemPermissions ADD CONSTRAINT permissionTypeUn1 UNIQUE(permissionType);

--
-- Users
--
CREATE TABLE users (
    id int NOT NULL AUTO_INCREMENT,
    username varchar(40) NOT NULL,
    password varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
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
    data json,
    readPermissionId int NOT NULL,
    editPermissionId int NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE users ADD CONSTRAINT username_unique UNIQUE(username);
ALTER TABLE users ADD CONSTRAINT email_unique UNIQUE(email);
ALTER TABLE users ADD CONSTRAINT usersFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions (id) ON DELETE RESTRICT;
ALTER TABLE users ADD CONSTRAINT usersFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions (id) ON DELETE RESTRICT;

--
-- Links OAuth2 users to a Mango user
--
CREATE TABLE oAuth2Users(
    id int NOT NULL AUTO_INCREMENT,
    issuer varchar (255) NOT NULL,
    subject varchar(255) NOT NULL,
    userId int NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE oAuth2Users ADD CONSTRAINT oAuth2UsersFk1 FOREIGN KEY (userId) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE oAuth2Users ADD CONSTRAINT oAuth2UsersUn1 UNIQUE (issuer, subject);

--
-- User Role Mappings
--
CREATE TABLE userRoleMappings (
	roleId int NOT NULL,
	userId int NOT NULL
);
ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsUn1 UNIQUE (roleId,userId);

CREATE TABLE userComments (
    id int NOT NULL AUTO_INCREMENT,
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

CREATE INDEX userComments_performance1 ON userComments (commentType ASC, typeKey ASC);

--
-- Mailing lists
--
CREATE TABLE mailingLists (
    id int NOT NULL AUTO_INCREMENT,
    xid varchar(100) NOT NULL,
    name varchar(40) NOT NULL,
    receiveAlarmEmails int NOT NULL,
    readPermissionId int NOT NULL,
    editPermissionId int NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE mailingLists ADD CONSTRAINT mailingListsUn1 UNIQUE (xid);
ALTER TABLE mailingLists ADD CONSTRAINT mailingListsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE mailingLists ADD CONSTRAINT mailingListsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

CREATE TABLE mailingListInactive (
    mailingListId int NOT NULL,
    inactiveInterval int NOT NULL
);
ALTER TABLE mailingListInactive ADD CONSTRAINT mailingListInactiveFk1 FOREIGN KEY (mailingListId) REFERENCES mailingLists(id) ON DELETE CASCADE;

CREATE TABLE mailingListMembers (
    mailingListId int NOT NULL,
    typeId int NOT NULL,
    userId int,
    address varchar(255)
);
ALTER TABLE mailingListMembers ADD CONSTRAINT mailingListMembersFk1 FOREIGN KEY (mailingListId) REFERENCES mailingLists(id) ON DELETE CASCADE;

--
-- Data Sources
--
CREATE TABLE dataSources (
    id int NOT NULL AUTO_INCREMENT,
    xid varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    dataSourceType varchar(40) NOT NULL,
    data longblob NOT NULL,
    jsonData json,
    rtdata longblob,
    readPermissionId int NOT NULL,
    editPermissionId int NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesUn1 UNIQUE (xid);
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

CREATE INDEX nameIndex ON dataSources (name ASC);
CREATE INDEX dataSourcesIdNameTypeXidIndex ON dataSources (id ASC, name ASC, dataSourceType ASC, xid ASC);

-- Time series table
CREATE TABLE timeSeries (
	id int NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (id)
);

CREATE TABLE timeSeriesMigrationProgress (
    seriesId int NOT NULL,
    status varchar(100) NOT NULL,
    timestamp bigint NOT NULL
);
ALTER TABLE timeSeriesMigrationProgress ADD CONSTRAINT timeSeriesMigrationProgressUn1 UNIQUE (seriesId);
ALTER TABLE timeSeriesMigrationProgress ADD CONSTRAINT timeSeriesMigrationProgressFk1 FOREIGN KEY (seriesId) REFERENCES timeSeries (id) ON DELETE CASCADE;

--
-- Data Points
--
CREATE TABLE dataPoints (
    id int NOT NULL AUTO_INCREMENT,
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
    dataTypeId int NOT NULL,
    settable char(1),
    jsonData json,
    seriesId int NOT NULL,
    readPermissionId int NOT NULL,
    editPermissionId int NOT NULL,
    setPermissionId int NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsUn1 UNIQUE (xid);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk1 FOREIGN KEY (dataSourceId) REFERENCES dataSources(id);
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk4 FOREIGN KEY (setPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk5 FOREIGN KEY (seriesId) REFERENCES timeSeries(id);

CREATE INDEX pointNameIndex ON dataPoints (name ASC);
CREATE INDEX deviceNameNameIdIndex ON dataPoints (deviceName ASC, name ASC, id ASC);
CREATE INDEX enabledIndex ON dataPoints (enabled ASC);
CREATE INDEX xidNameIndex ON dataPoints (xid ASC, name ASC);

-- Data point tags
CREATE TABLE dataPointTags (
    dataPointId int NOT NULL,
    tagKey varchar(255) NOT NULL,
    tagValue varchar(255) NOT NULL
);
ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX dataPointTagsUn1 ON dataPointTags (dataPointId ASC, tagKey ASC);
CREATE INDEX dataPointTagsIndex1 ON dataPointTags (tagKey ASC, tagValue ASC);

--
-- Point Values (historical data)
--  the dataPointId column refers to the series id of the data point, 
--  which is not necessarily the data point id
--
CREATE TABLE pointValues (
    id bigint NOT NULL AUTO_INCREMENT,
    dataPointId int NOT NULL,
    dataType int NOT NULL,
    pointValue double,
    ts bigint NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX pointValuesIdx1 on pointValues (dataPointId, ts);

CREATE TABLE pointValueAnnotations (
    pointValueId bigint NOT NULL,
    textPointValueShort varchar(128),
    textPointValueLong longtext,
    sourceMessage longtext,
    PRIMARY KEY (pointValueId)
);

--
-- Event detectors
--
CREATE TABLE eventDetectors (
    id int NOT NULL AUTO_INCREMENT,
    xid varchar(100) NOT NULL,
    sourceTypeName varchar(32) NOT NULL,
    typeName varchar(32) NOT NULL,
    dataPointId int,
    data longtext NOT NULL,
    jsonData json,
    readPermissionId int NOT NULL,
    editPermissionId int NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);
ALTER TABLE eventDetectors ADD CONSTRAINT dataPointIdFk FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

--
-- Events
--
CREATE TABLE events (
    id int NOT NULL AUTO_INCREMENT,
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
    readPermissionId int NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE events ADD CONSTRAINT eventsFk1 FOREIGN KEY (ackUserId) REFERENCES users(id);
ALTER TABLE events ADD CONSTRAINT eventsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

CREATE INDEX performance1 ON events (activeTs ASC);
CREATE INDEX events_performance2 ON events (rtnApplicable ASC, rtnTs ASC);
CREATE INDEX events_performance3 ON events (typeName ASC, subTypeName ASC, typeRef1 ASC);
CREATE INDEX events_performance4 ON events (typeName ASC, typeRef1 ASC);

--
-- Event handlers
--
CREATE TABLE eventHandlers (
    id int NOT NULL AUTO_INCREMENT,
    xid varchar(100) NOT NULL,
    alias varchar(255) NOT NULL,
    eventHandlerType varchar(40) NOT NULL,
    readPermissionId int NOT NULL,
    editPermissionId int NOT NULL,
    data longblob NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersUn1 UNIQUE (xid);
ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

CREATE TABLE eventHandlersMapping (
    eventHandlerId int NOT NULL,

    -- Event type, see events
    eventTypeName varchar(32) NOT NULL,
    eventSubtypeName varchar(32) NOT NULL DEFAULT '',
    eventTypeRef1 int NOT NULL,
    eventTypeRef2 int NOT NULL
);
ALTER TABLE eventHandlersMapping ADD CONSTRAINT eventHandlersFk1 FOREIGN KEY (eventHandlerId) REFERENCES eventHandlers(id) ON DELETE CASCADE;
ALTER TABLE eventHandlersMapping ADD CONSTRAINT handlerMappingUniqueness UNIQUE(eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2);

--
-- Audit Table
--
CREATE TABLE audit (
    id int NOT NULL AUTO_INCREMENT,
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
-- Publishers
--
CREATE TABLE publishers (
    id int NOT NULL AUTO_INCREMENT,
    xid varchar(100) NOT NULL,
    publisherType varchar(40) NOT NULL,
    data longblob NOT NULL,
    rtdata longblob,
    PRIMARY KEY (id)
);
ALTER TABLE publishers ADD CONSTRAINT publishersUn1 UNIQUE (xid);

CREATE TABLE publishedPoints (
    id int NOT NULL AUTO_INCREMENT,
    xid varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    enabled char(1),
    publisherId int NOT NULL,
    dataPointId int NOT NULL,
    data longtext,
    jsonData longtext,
    PRIMARY KEY (id)
 );
 ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsUn1 UNIQUE (xid);
 ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsFk1 FOREIGN KEY (publisherId) REFERENCES publishers(id) ON DELETE CASCADE;
 ALTER TABLE publishedPoints ADD CONSTRAINT publishedPointsFk2 FOREIGN KEY (dataPointId) REFERENCES dataPoints(id) ON DELETE CASCADE;

 CREATE INDEX publishedPointNameIndex ON publishedPoints (name ASC);
 CREATE INDEX publishedPointEnabledIndex ON publishedPoints (enabled ASC);
 CREATE INDEX publishedPointXidNameIndex ON publishedPoints (xid ASC, name ASC);

--
-- JsonData
--
CREATE TABLE jsonData (
    id int NOT NULL AUTO_INCREMENT,
    xid varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    data longtext,
    readPermissionId int NOT NULL,
    editPermissionId int NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE jsonData ADD CONSTRAINT jsonDataUn1 UNIQUE (xid);
ALTER TABLE jsonData ADD CONSTRAINT jsonDataFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE jsonData ADD CONSTRAINT jsonDataFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

--
-- InstalledModules
--  Thirty character restriction is from the store
--
CREATE TABLE installedModules (
    name varchar(30) NOT NULL,
    version varchar(255) NOT NULL,
    upgradedTimestamp bigint NOT NULL,
    buildTimestamp bigint NOT NULL
);
ALTER TABLE installedModules ADD CONSTRAINT installModulesUn1 UNIQUE (name);

--
-- FileStores
--
CREATE TABLE fileStores (
    id int NOT NULL AUTO_INCREMENT,
    xid varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    readPermissionId int NOT NULL,
    writePermissionId int NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (xid);
ALTER TABLE fileStores ADD CONSTRAINT fileStoresFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;
ALTER TABLE fileStores ADD CONSTRAINT fileStoresFk2 FOREIGN KEY (writePermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;

--
-- Persistent session data
--
CREATE TABLE mangoSessionData (
    sessionId varchar(120),
    contextPath varchar(60),
    virtualHost varchar(60),
    lastNode varchar(60),
    accessTime bigint,
    lastAccessTime bigint,
    createTime bigint,
    cookieTime bigint,
    lastSavedTime bigint,
    expiryTime bigint,
    maxInterval bigint,
    userId int,
    PRIMARY KEY (sessionId, contextPath, virtualHost)
);
CREATE INDEX mangoSessionDataExpiryIndex ON mangoSessionData (expiryTime);
CREATE INDEX mangoSessionDataSessionIndex ON mangoSessionData (sessionId, contextPath);
