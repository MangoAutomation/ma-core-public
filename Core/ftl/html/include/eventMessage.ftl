<#--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#if evt.alarmLevel gt 0>
  <tr>
    <td valign="top"><img src="cid:<@img src="exclamation.png"/>" alt="<@fmt key="ftl.note"/>" title="<@fmt key="ftl.note"/>"/></td>
    <td colspan="2">
      <#if evt.rtnApplicable>
        <@fmt key="ftl.rtn"/>
      <#else>
        <@fmt key="ftl.manual"/>
      </#if>
    </td>
  </tr>
</#if>
