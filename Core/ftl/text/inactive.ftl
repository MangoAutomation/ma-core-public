<#ftl strip_whitespace=false><#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Matthew Lohbihler
-->
${instanceDescription}
${evt.rtnTimestamp?number_to_datetime?string["yyyy/MM/dd HH:mm:ss"]} <@fmt key="ftl.eventInactive"/>: <@fmt message=evt.rtnMessage/>


<@fmt key="ftl.originalInformation"/>

*******************************************************
<#include "include/eventData.ftl">

*******************************************************
<#include "include/systemInfo.ftl">

*******************************************************

<#include "include/footer.ftl">