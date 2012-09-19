<#ftl strip_whitespace=false><#--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
-->
${instanceDescription}
${evt.prettyRtnTimestamp} <@fmt key="ftl.eventInactive"/>: <@fmt message=evt.rtnMessage/>


<@fmt key="ftl.originalInformation"/>

*******************************************************
<#include "include/eventData.ftl">

*******************************************************

<#include "include/footer.ftl">