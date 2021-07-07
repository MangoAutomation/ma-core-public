<#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Matthew Lohbihler
-->
<#if evt.alarmLevel.value() gt 0>

<@fmt key="ftl.note"/>: <#if evt.rtnApplicable><@fmt key="ftl.rtn"/><#else><@fmt key="ftl.manual"/></#if>

</#if>
