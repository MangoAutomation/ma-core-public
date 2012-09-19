<#--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#if evt.alarmLevel==1>
** <@fmt key="common.alarmLevel.info"/> **
<#elseif evt.alarmLevel==2>
** <@fmt key="common.alarmLevel.urgent"/> **
<#elseif evt.alarmLevel==3>
** <@fmt key="common.alarmLevel.critical"/> **
<#elseif evt.alarmLevel==4>
** <@fmt key="common.alarmLevel.lifeSafety"/> **
</#if>