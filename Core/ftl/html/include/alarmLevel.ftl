<#--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#if evt.alarmLevel==1>
  <img src="cid:<@img src="flag_blue.png"/>" alt="<@fmt key="common.alarmLevel.info"/>" title="<@fmt key="common.alarmLevel.info"/>"/>
<#elseif evt.alarmLevel==2>
  <img src="cid:<@img src="flag_yellow.png"/>" alt="<@fmt key="common.alarmLevel.urgent"/>" title="<@fmt key="common.alarmLevel.urgent"/>"/>
<#elseif evt.alarmLevel==3>
  <img src="cid:<@img src="flag_orange.png"/>" alt="<@fmt key="common.alarmLevel.critical"/>" title="<@fmt key="common.alarmLevel.critical"/>"/>
<#elseif evt.alarmLevel==4>
  <img src="cid:<@img src="flag_red.png"/>" alt="<@fmt key="common.alarmLevel.lifeSafety"/>" title="<@fmt key="common.alarmLevel.lifeSafety"/>"/>
</#if>