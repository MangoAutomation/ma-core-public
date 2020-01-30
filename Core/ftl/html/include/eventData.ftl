<#--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<tr>
  <td><#include "alarmLevel.ftl"></td>
  <td colspan="2">${evt.activeTimestamp?number_to_datetime?string["yyyy/MM/dd HH:mm:ss"]} - <b><@fmt message=evt.message/></b></td>
</tr>

<#if renderedHtmlPointValues??>
<#if renderedHtmlPointValues?has_content>
<tr>
  <td colspan="3"><hr></td></tr>
<tr>
  <td colspan="3" class="smallTitle"><b><@fmt key="ftl.recentPointValues"/></b></td>
</tr>
</#if>
  <#list renderedHtmlPointValues as renderedPvt>
<tr>
  <td width="10"></td>
  <td>${renderedPvt.value}</td>
  <td>${renderedPvt.time}</td>
</tr>
  </#list>
</#if>
<#if evt.eventComments??>
<#if eventComments?has_content>
<tr> <td colspan="3"><hr></td></tr>
</#if>
  <#list evt.eventComments as comment>
    <tr>
      <td width="10"></td>
      <td valign="top" width="16"><img src="cid:<@img src="comment.png"/>" title="<@fmt key="notes.note"/>" alt="<@fmt key="notes.note"/>"/></td>
      <td valign="top">
        <span class="copyTitle">
          ${comment.ts?number_to_datetime?string["yyyy/MM/dd HH:mm:ss"]} <@fmt key="notes.by"/>
          <#if comment.username??>
            ${comment.username}
          <#else>
            <@fmt key="common.deleted"/>
          </#if>
        </span><br/>
        ${comment.comment}
      </td>
    </tr>
  </#list>
</#if>