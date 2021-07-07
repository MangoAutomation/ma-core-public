--
--    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
--    @author Matthew Lohbihler
--
alter table events add (typeName varchar(32) not null);
alter table events add (subtypeName varchar(32));

update events set typeName='DATA_POINT' where typeId=1;
update events set typeName='DATA_SOURCE' where typeId=3;
update events set typeName='SYSTEM' where typeId=4;
update events set typeName='COMPOUND' where typeId=5;
update events set typeName='SCHEDULED' where typeId=6;
update events set typeName='PUBLISHER' where typeId=7;
update events set typeName='AUDIT' where typeId=8;
update events set typeName='MAINTENANCE' where typeId=9;

update events set subtypeName='SYSTEM_STARTUP' where typeName='SYSTEM' and typeRef1=1;
update events set subtypeName='SYSTEM_SHUTDOWN' where typeName='SYSTEM' and typeRef1=2;
update events set subtypeName='MAX_ALARM_LEVEL_CHANGED' where typeName='SYSTEM' and typeRef1=3;
update events set subtypeName='USER_LOGIN' where typeName='SYSTEM' and typeRef1=4;
update events set subtypeName='VERSION_CHECK' where typeName='SYSTEM' and typeRef1=5;
update events set subtypeName='COMPOUND_DETECTOR_FAILURE' where typeName='SYSTEM' and typeRef1=6;
update events set subtypeName='SET_POINT_HANDLER_FAILURE' where typeName='SYSTEM' and typeRef1=7;
update events set subtypeName='EMAIL_SEND_FAILURE' where typeName='SYSTEM' and typeRef1=8;
update events set subtypeName='POINT_LINK_FAILURE' where typeName='SYSTEM' and typeRef1=9;
update events set subtypeName='PROCESS_FAILURE' where typeName='SYSTEM' and typeRef1=10;
update events set subtypeName='LICENSE_CHECK' where typeName='SYSTEM' and typeRef1=11;
update events set typeRef1=typeRef2 where typeName='SYSTEM';
update events set typeRef2=0 where typeName='SYSTEM';

update events set subtypeName='DATA_SOURCE' where typeName='AUDIT' and typeRef1=1;
update events set subtypeName='DATA_POINT' where typeName='AUDIT' and typeRef1=2;
update events set subtypeName='POINT_EVENT_DETECTOR' where typeName='AUDIT' and typeRef1=3;
update events set subtypeName='COMPOUND_EVENT_DETECTOR' where typeName='AUDIT' and typeRef1=4;
update events set subtypeName='SCHEDULED_EVENT' where typeName='AUDIT' and typeRef1=5;
update events set subtypeName='EVENT_HANDLER' where typeName='AUDIT' and typeRef1=6;
update events set subtypeName='POINT_LINK' where typeName='AUDIT' and typeRef1=7;
update events set subtypeName='MAINTENANCE_EVENT' where typeName='AUDIT' and typeRef1=8;
update events set typeRef1=typeRef2 where typeName='AUDIT';
update events set typeRef2=0 where typeName='AUDIT';

alter table events drop typeId;
