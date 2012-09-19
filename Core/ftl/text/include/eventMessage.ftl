<#--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#if evt.alarmLevel gt 0>

<@fmt key="ftl.note"/>: <#if evt.rtnApplicable><@fmt key="ftl.rtn"/><#else><@fmt key="ftl.manual"/></#if>

</#if>
