<#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Matthew Lohbihler
-->
<#if evt.alarmLevel.value()==0>
  <img src="cid:<@img src="flag_grey.png"/>" alt="<@fmt key="common.alarmLevel.none"/>" title="<@fmt key="common.alarmLevel.none"/>"/>
<#elseif evt.alarmLevel.value()==1>
  <img src="cid:<@img src="flag_blue.png"/>" alt="<@fmt key="common.alarmLevel.info"/>" title="<@fmt key="common.alarmLevel.info"/>"/>
<#elseif evt.alarmLevel.value()==2>
  <img src="cid:<@img src="flag_aqua.png"/>" alt="<@fmt key="common.alarmLevel.important"/>" title="<@fmt key="common.alarmLevel.important"/>"/>
<#elseif evt.alarmLevel.value()==3>
  <img src="cid:<@img src="flag_green.png"/>" alt="<@fmt key="common.alarmLevel.warning"/>" title="<@fmt key="common.alarmLevel.warning"/>"/>
<#elseif evt.alarmLevel.value()==4>
  <img src="cid:<@img src="flag_yellow.png"/>" alt="<@fmt key="common.alarmLevel.urgent"/>" title="<@fmt key="common.alarmLevel.urgent"/>"/>
<#elseif evt.alarmLevel.value()==5>
  <img src="cid:<@img src="flag_orange.png"/>" alt="<@fmt key="common.alarmLevel.critical"/>" title="<@fmt key="common.alarmLevel.critical"/>"/>
<#elseif evt.alarmLevel.value()==6>
  <img src="cid:<@img src="flag_red.png"/>" alt="<@fmt key="common.alarmLevel.lifeSafety"/>" title="<@fmt key="common.alarmLevel.lifeSafety"/>"/>
<#elseif evt.alarmLevel.value()==-2>
  <img src="cid:<@img src="cancel.png"/>" alt="<@fmt key="common.alarmLevel.doNotLog"/>" title="<@fmt key="common.alarmLevel.doNotLog"/>"/>
</#if>