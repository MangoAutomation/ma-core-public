<#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Matthew Lohbihler
-->
<#include "include/eventHeader.ftl">
    <table width="500" cellPadding="0" cellSpacing="0" border="0">
      <tr><td>
        <table cellPadding="0" cellSpacing="1" border="0" width="100%" style="background-color:#F07800;">
          <tr>
           <td style="background-color:#FFFFFF;padding:5px;">
			<table>
			  <tr><td colspan="3" class="smallTitle">${instanceDescription} - <@fmt key="ftl.eventActive"/></td></tr>
			  <tr> <td colspan="3"><hr></td></tr>
			  <#include "include/eventData.ftl">
			  <tr> <td colspan="3"><hr></td></tr>
			  <#include "include/eventMessage.ftl">
			</table>
		  </td>
		  </tr>
        </table>
      </td></tr>
    </table>
<#include "include/systemInfo.ftl">
<#include "include/eventFooter.ftl">