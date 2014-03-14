<#--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#include "include/eventHeader.ftl">
<table>
  <tr><td colspan="3" class="smallTitle">${instanceDescription} - <@fmt key="ftl.escalation"/></td></tr>
  <#include "include/eventData.ftl">
  <#include "include/eventMessage.ftl">
</table>
<#include "include/eventFooter.ftl">