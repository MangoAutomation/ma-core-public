<#--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#include "include/eventHeader.ftl">
<table>
  <tr><td colspan="3" class="smallTitle">${instanceDescription} - <@fmt key="ftl.eventActive"/></td></tr>
  <#include "include/eventData.ftl">
  <#include "include/eventMessage.ftl">
</table>
<#include "include/eventFooter.ftl">