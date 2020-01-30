<#--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#include "include/eventHeader.ftl">
    <table width="500" cellPadding="0" cellSpacing="0" border="0">
      <tr><td>
        <table cellPadding="0" cellSpacing="1" border="0" width="100%" style="background-color:#F07800;">
          <tr>
           <td style="background-color:#FFFFFF;padding:5px;">
			<table>
			  <tr><td colspan="3" class="smallTitle">${instanceDescription} - <@fmt key="ftl.eventInactive"/></td></tr>
			  <tr><td colspan="3">${evt.rtnTimestamp?number_to_datetime?string["yyyy/MM/dd HH:mm:ss"]} - <b><@fmt message=evt.rtnMessage/></b></td></tr>
			  <tr><td colspan="3"><hr></td></tr>
			  <tr><td colspan="3" class="smallTitle"><@fmt key="ftl.originalInformation"/></td></tr>
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