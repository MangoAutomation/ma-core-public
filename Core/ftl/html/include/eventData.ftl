<#--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<tr>
  <td><#include "alarmLevel.ftl"></td>
  <td colspan="2">${evt.prettyActiveTimestamp} - <b><@fmt message=evt.message/></b></td>
</tr>
<#if evt.eventComments??>
  <#list evt.eventComments as comment>
    <tr>
      <td width="10"></td>
      <td valign="top" width="16"><img src="cid:<@img src="comment.png"/>" title="<@fmt key="notes.note"/>" alt="<@fmt key="notes.note"/>"/></td>
      <td valign="top">
        <span class="copyTitle">
          ${comment.prettyTime} <@fmt key="notes.by"/>
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
<tr><td colspan="3"></td></tr>