<#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Matthew Lohbihler
-->
<#if evt.alarmLevel.value()==1>
** <@fmt key="common.alarmLevel.info"/> **
<#elseif evt.alarmLevel.value()==2>
** <@fmt key="common.alarmLevel.important"/> **
<#elseif evt.alarmLevel.value()==3>
** <@fmt key="common.alarmLevel.warning"/> **
<#elseif evt.alarmLevel.value()==4>
** <@fmt key="common.alarmLevel.urgent"/> **
<#elseif evt.alarmLevel.value()==5>
** <@fmt key="common.alarmLevel.critical"/> **
<#elseif evt.alarmLevel.value()==6>
** <@fmt key="common.alarmLevel.lifeSafety"/> **
</#if>