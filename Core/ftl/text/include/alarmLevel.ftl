<#--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#if evt.alarmLevel==1>
** <@fmt key="common.alarmLevel.info"/> **
<#elseif evt.alarmLevel==2>
** <@fmt key="common.alarmLevel.important"/> **
<#elseif evt.alarmLevel==3>
** <@fmt key="common.alarmLevel.warning"/> **
<#elseif evt.alarmLevel==4>
** <@fmt key="common.alarmLevel.urgent"/> **
<#elseif evt.alarmLevel==5>
** <@fmt key="common.alarmLevel.critical"/> **
<#elseif evt.alarmLevel==6>
** <@fmt key="common.alarmLevel.lifeSafety"/> **
</#if>