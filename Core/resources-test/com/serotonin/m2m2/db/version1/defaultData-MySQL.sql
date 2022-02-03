--    This is data for the original version 1 Mango Schema for use to test upgrades to the schema
--
--    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
--    @author Terry Packer
--

--
-- System settings
INSERT INTO systemSettings (settingName, settingValue) VALUES ('databaseSchemaVersion', 1);

--
-- Users
INSERT INTO users (id, username, password, email, phone, admin, disabled, 
	lastLogin, homeUrl, receiveAlarmEmails, receiveOwnAuditEvents)
	VALUES (1, 'admin', 'admin', 'admin@admin.com', '1-800-000-0000', 'Y', 'N', 0, '/home.html', '1', 'N');
INSERT INTO users (id, username, password, email, phone, admin, disabled, 
	lastLogin, homeUrl, receiveAlarmEmails, receiveOwnAuditEvents)
	VALUES (2, 'non-admin', 'non-admin', 'non-admin@admin.com', '1-800-000-0000', 'N', 'N', 0, '/home.html', '1', 'N');

-- User Comments
INSERT INTO userComments (userId, commentType, typeKey, ts, commentText) 
	VALUES (1, 1, 1, 0, 'Event comment');
	
-- Mailing Lists
INSERT INTO mailingLists (id, xid, name) VALUES (1, 'ML_LIST_TEST', 'Test List Mailing List');
INSERT INTO mailingLists (id, xid, name) VALUES (2, 'ML_USER_TEST', 'Test User Mailing List');
INSERT INTO mailingLists (id, xid, name) VALUES (3, 'ML_ADDRESS_TEST', 'Test Address Mailing List');

INSERT INTO mailingListMembers (mailingListId, typeId, userId, address)
	VALUES(1, 1, null, null);
INSERT INTO mailingListMembers (mailingListId, typeId, userId, address)
	VALUES(2, 2, 1, null);
INSERT INTO mailingListMembers (mailingListId, typeId, userId, address)
	VALUES(3, 3, null, 'admin@admin.com');
INSERT INTO mailingListInactive (mailingListId, inactiveInterval)
	VALUES(3, 1);
	
-- Data Sources
	INSERT INTO dataSources(id, xid, name, dataSourceType, data, rtData) VALUES
(1, 'DS_50924c4d-d03b-47ef-9dcd-cef4a87c5f93', 'permissions_test_datasource', 'MOCK', X'aced000573720036636f6d2e7365726f746f6e696e2e6d326d322e766f2e64617461536f757263652e6d6f636b2e4d6f636b44617461536f75726365564fffffffffffffffff0300007872002d636f6d2e7365726f746f6e696e2e6d326d322e766f2e64617461536f757263652e44617461536f75726365564fffffffffffffffff0300065a000d70757267654f7665727269646549000b7075726765506572696f644900097075726765547970654c000b616c61726d4c6576656c7374000f4c6a6176612f7574696c2f4d61703b4c000a646566696e6974696f6e7400304c636f6d2f7365726f746f6e696e2f6d326d322f6d6f64756c652f44617461536f75726365446566696e6974696f6e3b4c000e656469745065726d697373696f6e7400124c6a6176612f6c616e672f537472696e673b78720026636f6d2e7365726f746f6e696e2e6d326d322e766f2e4162737472616374416374696f6e564fffffffffffffffff0200015a0007656e61626c656478720020636f6d2e7365726f746f6e696e2e6d326d322e766f2e4162737472616374564fffffffffffffffff0200024c00046e616d6571007e00044c000378696471007e0004787074001b7065726d697373696f6e735f746573745f64617461736f7572636574002744535f65643832663466312d343134392d343633302d393539622d3038653865616334393031310077050000000300737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f40000000000000770800000010000000007877090000000007000000017877040000000178', NULL);       
	
-- Data Points
INSERT INTO dataPoints(id, xid, dataSourceId, data)
	VALUES (1, 'DP_5e410079-2000-4dee-aff1-2b81fe0cb647', 1, X'aced000573720021636f6d2e7365726f746f6e696e2e6d326d322e766f2e44617461506f696e74564fffffffffffffffff03003249000c64617461536f75726365496449001064656661756c74436163686553697a655a00146469736361726445787472656d6556616c75657344001064697363617264486967684c696d697444000f646973636172644c6f774c696d6974490010656e67696e656572696e67556e697473490015696e74657276616c4c6f6767696e67506572696f64490019696e74657276616c4c6f6767696e67506572696f645479706549001f696e74657276616c4c6f6767696e6753616d706c6557696e646f7753697a65490013696e74657276616c4c6f6767696e675479706549000b6c6f6767696e67547970655a001e6f76657272696465496e74657276616c4c6f6767696e6753616d706c6573490008706c6f745479706549000d706f696e74466f6c64657249645a001770726576656e7453657445787472656d6556616c7565735a000d70757267654f7665727269646549000b7075726765506572696f64490009707572676554797065490006726f6c6c757044001373657445787472656d65486967684c696d697444001273657445787472656d654c6f774c696d697449000e73696d706c69667954617267657444001173696d706c696679546f6c6572616e636549000c73696d706c69667954797065440009746f6c6572616e63655a000f757365496e74656772616c556e69745a000f75736552656e6465726564556e69744c000b6368617274436f6c6f75727400124c6a6176612f6c616e672f537472696e673b4c000d636861727452656e646572657274002d4c636f6d2f7365726f746f6e696e2f6d326d322f766965772f63686172742f436861727452656e64657265723b4c0008636f6d6d656e74737400104c6a6176612f7574696c2f4c6973743b4c000e64617461536f757263654e616d6571007e00014c001264617461536f75726365547970654e616d6571007e00014c000d64617461536f7572636558696471007e00014c000a6465766963654e616d6571007e00014c000e6576656e744465746563746f727371007e00034c000c696e74656772616c556e69747400194c6a617661782f6d6561737572652f756e69742f556e69743b4c0012696e74656772616c556e6974537472696e6771007e00014c00096c61737456616c75657400304c636f6d2f7365726f746f6e696e2f6d326d322f72742f64617461496d6167652f506f696e7456616c756554696d653b4c000c706f696e744c6f6361746f727400314c636f6d2f7365726f746f6e696e2f6d326d322f766f2f64617461536f757263652f506f696e744c6f6361746f72564f3b4c000e726561645065726d697373696f6e71007e00014c000c72656e6465726564556e697471007e00044c001272656e6465726564556e6974537472696e6771007e00014c000d7365745065726d697373696f6e71007e00014c00047461677374000f4c6a6176612f7574696c2f4d61703b4c000a74656d706c61746549647400134c6a6176612f6c616e672f496e74656765723b4c000c74656d706c6174654e616d6571007e00014c000b74656d706c61746558696471007e00014c000c7465787452656e646572657274002b4c636f6d2f7365726f746f6e696e2f6d326d322f766965772f746578742f5465787452656e64657265723b4c0004756e697471007e00044c000a756e6974537472696e6771007e000178720026636f6d2e7365726f746f6e696e2e6d326d322e766f2e4162737472616374416374696f6e564fffffffffffffffff0200015a0007656e61626c656478720020636f6d2e7365726f746f6e696e2e6d326d322e766f2e4162737472616374564fffffffffffffffff0200024c00046e616d6571007e00014c000378696471007e0001787074001b7065726d697373696f6e735f746573745f64617461736f7572636574002744505f65333166626663312d366566612d346263612d623134662d3764316634363336623036640077040000000d7372002a636f6d2e7365726f746f6e696e2e6d326d322e766965772e746578742e506c61696e52656e6465726572ffffffffffffffff0300014c000673756666697871007e00017872002f636f6d2e7365726f746f6e696e2e6d326d322e766965772e746578742e436f6e76657274696e6752656e6465726572ffffffffffffffff0300035a000f757365556e697441735375666669784c000c72656e6465726564556e697471007e00044c0004756e697471007e00047872002d636f6d2e7365726f746f6e696e2e6d326d322e766965772e746578742e426173655465787452656e6465726572ffffffffffffffff030000787077040000000178770b000000020101000001000078770700000005010000787073720032636f6d2e7365726f746f6e696e2e6d326d322e766f2e64617461506f696e742e4d6f636b506f696e744c6f6361746f72564fffffffffffffffff03000249000a646174615479706549645a00087365747461626c6578720037636f6d2e7365726f746f6e696e2e6d326d322e766f2e64617461536f757263652e4162737472616374506f696e744c6f6361746f72564fffffffffffffffff030000787077040000000178770400000001787749ffefffffffffffff7fefffffffffffff01000000000001010000010001730100000000000000000000ffefffffffffffff7fefffffffffffff0000000140240000000000000000138878');


-- Events of all types
INSERT INTO events (id,typename,subtypename,typeref1,typeref2,activets,rtnapplicable,rtnts,rtncause,alarmlevel,message,ackts,ackuserid,alternateacksource) VALUES
(1,'SYSTEM','SYSTEM_STARTUP',0,0,1449880069262,'N',0,0,1,'event.system.startup|',null,null,null),
(2,'SYSTEM','LICENSE_CHECK',0,0,1449880069281,'Y',1449880504905,4,2,'modules.event.freeMode|[modules.event.freeMode.reason.missingLicense|]',null,null,null),
(3,'SYSTEM','MAX_ALARM_LEVEL_CHANGED',0,0,1449880069281,'N',0,0,0,'event.alarmMaxIncreased|[common.alarmLevel.none|][common.alarmLevel.urgent|]',1449880069281,null,null),
(4,'SYSTEM','USER_LOGIN',1,0,1449880445234,'Y',1449880504929,1,1,'event.login|admin|192.168.1.20|',null,null,null),
(5,'SYSTEM','MAX_ALARM_LEVEL_CHANGED',0,0,1449880504905,'N',0,0,0,'event.alarmMaxDecreased|[common.alarmLevel.urgent|][common.alarmLevel.info|]',1449880504905,null,null),
(6,'SYSTEM','SYSTEM_SHUTDOWN',0,0,1449880504911,'N',0,0,1,'event.system.shutdown|',null,null,null),
(7,'SYSTEM','MAX_ALARM_LEVEL_CHANGED',0,0,1449880504929,'N',0,0,0,'event.alarmMaxDecreased|[common.alarmLevel.info|][common.alarmLevel.none|]',1449880504929,null,null),
(8,'SYSTEM','SYSTEM_STARTUP',0,0,1534878033198,'N',0,0,1,'event.system.startup|',null,null,null),
(9,'SYSTEM','LICENSE_CHECK',0,0,1534878033249,'Y',1534878243472,4,2,'modules.event.freeMode|[modules.event.freeMode.reason.missingLicense|]',null,null,null),
(10,'SYSTEM','MAX_ALARM_LEVEL_CHANGED',0,0,1534878033249,'N',0,0,0,'event.alarmMaxIncreased|[common.alarmLevel.none|][common.alarmLevel.urgent|]',1534878033249,null,null),
(11,'SYSTEM','USER_LOGIN',1,0,1534878069808,'Y',1534878243514,1,1,'event.login|admin|10.55.55.233|',null,null,null),
(12,'SYSTEM','USER_LOGIN',1,0,1534878119135,'Y',1534878243514,1,1,'event.login|admin|10.55.55.233|',null,null,null),
(13,'AUDIT','DATA_SOURCE',1,0,1534878136753,'N',0,0,1,'event.audit.added|admin(1)|[event.audit.dataSource|]1|[event.audit.propertyList.6|[event.audit.property|[common.xid|]DS_249200|][event.audit.property|[common.name|]asshoels|][event.audit.property|[common.enabled|][common.false|]][event.audit.property|[dsEdit.logging.purgeOverride|][common.true|]][event.audit.property|[dsEdit.logging.purge|][common.tp.description|1|[common.tp.years|]]][event.audit.property|[dsEdit.updatePeriod|][common.tp.description|5|[common.tp.minutes|]]]]',null,null,null),
(14,'AUDIT','DATA_POINT',1,0,1534878154732,'N',0,0,1,'event.audit.added|admin(1)|[event.audit.dataPoint|]1|[event.audit.propertyList.23|[event.audit.property|[common.xid|]DP_055067|][event.audit.property|[common.name|]asdf|][event.audit.property|[common.enabled|][common.false|]][event.audit.property|[pointEdit.logging.type|][pointEdit.logging.type.change|]][event.audit.property|[pointEdit.logging.period|][common.tp.description|15|[common.tp.minutes|]]][event.audit.property|[pointEdit.logging.valueType|][pointEdit.logging.valueType.instant|]][event.audit.property|[pointEdit.logging.tolerance|]0.0|][event.audit.property|[pointEdit.logging.purgeOverride|][common.true|]][event.audit.property|[pointEdit.logging.purge|][common.tp.description|1|[common.tp.years|]]][event.audit.property|[pointEdit.logging.defaultCache|]1|][event.audit.property|[pointEdit.logging.discard|][common.false|]][event.audit.property|[pointEdit.logging.discardLow|]0.0|][event.audit.property|[pointEdit.props.engineeringUnits|]95|][event.audit.property|[pointEdit.props.chartColour|]|][event.audit.property|[pointEdit.plotType|][pointEdit.plotType.step|]][event.audit.property|[pointEdit.props.overrideIntervalLoggingSamples|][common.false|]][event.audit.property|[pointEdit.props.intervalLoggingSampleWindowSize|]0|][event.audit.property|[dsEdit.settable|][common.false|]][event.audit.property|[dsEdit.pointDataType|][common.dataTypes.numeric|]][event.audit.property|[dsEdit.virtual.changeType|][dsEdit.virtual.changeType.random|]][event.audit.property|[dsEdit.virtual.startValue|]50|][event.audit.property|[dsEdit.virtual.min|]0.0|][event.audit.property|[dsEdit.virtual.max|]1000.0|]]',null,null,null),
(15,'AUDIT','DATA_POINT',1,0,1534878159426,'N',0,0,1,'event.audit.changed|admin(1)|[event.audit.dataPoint|]1|[event.audit.propertyList.1|[event.audit.changedProperty|[common.enabled|][common.false|][common.true|]]]',null,null,null),
(16,'AUDIT','DATA_SOURCE',1,0,1534878162316,'N',0,0,1,'event.audit.changed|admin(1)|[event.audit.dataSource|]1|[event.audit.propertyList.1|[event.audit.changedProperty|[common.enabled|][common.false|][common.true|]]]',null,null,null),
(17,'SYSTEM','MAX_ALARM_LEVEL_CHANGED',0,0,1534878243472,'N',0,0,0,'event.alarmMaxDecreased|[common.alarmLevel.urgent|][common.alarmLevel.info|]',1534878243472,null,null),
(18,'SYSTEM','SYSTEM_SHUTDOWN',0,0,1534878243483,'N',0,0,1,'event.system.shutdown|',null,null,null),
(19,'SYSTEM','MAX_ALARM_LEVEL_CHANGED',0,0,1534878243514,'N',0,0,0,'event.alarmMaxDecreased|[common.alarmLevel.info|][common.alarmLevel.none|]',1534878243514,null,null),
(20,'SYSTEM','SYSTEM_STARTUP',0,0,1534880772657,'N',0,0,1,'event.system.startup|',null,null,null),
(21,'SYSTEM','LICENSE_CHECK',0,0,1534880772713,'Y',0,2,1,'modules.event.freeMode|[modules.event.freeMode.reason.missingLicense|]',null,null,null),
(22,'SYSTEM','MAX_ALARM_LEVEL_CHANGED',0,0,1534880772713,'N',0,0,0,'event.alarmMaxIncreased|[common.alarmLevel.none|][common.alarmLevel.urgent|]',1534880772713,null,null),
(23,'SYSTEM','USER_LOGIN',1,0,1534880965239,'Y',0,1,1,'event.login|admin|10.55.55.233|',null,null,null),
(24,'SYSTEM','USER_LOGIN',1,0,1534881679005,'Y',0,1,1,'event.login|admin|10.55.55.233|',null,null,null),
(25,'AUDIT','DATA_POINT',1,0,1534881709116,'N',0,0,1,'event.audit.changed|admin(1)|[event.audit.dataPoint|]1|[event.audit.propertyList.1|[event.audit.changedProperty|[pointEdit.props.chartColour|]||]]',null,null,null),
(26,'AUDIT','POINT_EVENT_DETECTOR',1,0,1534881709125,'N',0,0,1,'event.audit.added|admin(1)|[event.audit.pointEventDetector|]1|[event.audit.propertyList.6|[event.audit.property|[common.xid|]PED_906913|][event.audit.property|[pointEdit.detectors.alias|]|][event.audit.property|[pointEdit.detectors.type|]pointEdit.detectors.change|][event.audit.property|[common.alarmLevel|][common.alarmLevel.critical|]][event.audit.property|[common.configuration|][event.detectorVo.change|]][event.audit.property|[pointEdit.detectors.weight|]0.0|]]',null,null,null),
(27,'DATA_POINT',null,1,1,1534881711512,'N',0,0,3,'event.detector.changeCount|asdf|792.4720020555209|953.6426159206546|',null,null,null),
(28,'AUDIT','DATA_SOURCE',1,0,1534881714788,'N',0,0,1,'event.audit.changed|admin(1)|[event.audit.dataSource|]1|[event.audit.propertyList.1|[event.audit.changedProperty|[common.enabled|][common.true|][common.false|]]]',null,null,null);

-- Non-audit events need userEvent entries for our admin user
INSERT INTO userEvents (eventId, userId, silenced) VALUES
(1,1,'N'),
(2,1,'N'),
(3,1,'Y'),
(4,1,'N'),
(5,1,'Y'),
(6,1,'N'),
(7,1,'Y'),
(8,1,'N'),
(9,1,'N'),
(10,1,'Y'),
(11,1,'N'),
(12,1,'N'),
(17,1,'Y'),
(18,1,'N'),
(19,1,'Y'),
(20,1,'N'),
(21,1,'N'),
(22,1,'Y'),
(23,1,'N'),
(24,1,'N'),
(27,1,'N');
