<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf" %><%--
--%><%@attribute name="descriptionKey" required="true" rtexprvalue="true" %><%--
--%><%@attribute name="helpId" rtexprvalue="true" %><%--
--%><%@attribute name="extraPanels" fragment="true" %><%--
--%><%@tag import="com.serotonin.m2m2.Common"%><%--
--%>
<script type="text/javascript">
require(['jquery',
         'view/CronPicker'],
function($, CronPicker) {
  'use strict';
  var cronPicker = new CronPicker({
      $input: $('#cronPattern'),
      $picker: $('.cron-picker')
  });
  
  $('#quantize').on('change', function(event){
	  if(event.target.checked === true){
		 //Disable the Cron Pattern and enable the update periods
		 $('#cronPattern').val('');
		 $('#cronPattern').prop('disabled', true);
		 $('#cronPattern').css('background', '#dddddd');
		 $('#clearCronBtn').prop('disabled', true);
		 $('#setToPollPeriodBtn').prop('disabled', true);		 
		 
		 $('#updatePeriodType').prop('disabled', false);
		 $('#updatePeriods').prop('disabled', false);
		 $('#updatePeriodType').css('background', '');
		 $('#updatePeriods').css('background', '');		 
	 }else{
		 //Enable the Cron Pattern
		 $('#cronPattern').prop('disabled', false);
		 $('#cronPattern').css('background', '');
		 $('#clearCronBtn').prop('disabled', false);
		 $('#setToPollPeriodBtn').prop('disabled', false);
	 }
  });
  
  //Set to disable the time periods if there is a value in the picker
  $('#cronPattern').on('change', function(event){
	 if((event.target.value === '')||(event.target.value === null)){
		 $('#updatePeriodType').prop('disabled', false);
		 $('#updatePeriods').prop('disabled', false);
		 $('#updatePeriodType').css('background', '');
		 $('#updatePeriods').css('background', '');		 
	 }else{
		 $('#updatePeriodType').prop('disabled', true);
		 $('#updatePeriodType').css('background', '#dddddd');
		 $('#updatePeriods').prop('disabled', true);
		 $('#updatePeriods').css('background', '#dddddd');
	 }
  });
  //Clear the cron value
  $('#clearCronBtn').on('click', function(){
	  $('#cronPattern').val('').change();
  })
  //Create the cron value
  $('#setToPollPeriodBtn').on('click', function(){
	  var type = $('#updatePeriodType').val();
	  switch(type){
	  default:
		  alert('Not possible via cron patterns.');
	  break;
	  case '<%= Integer.toString(Common.TimePeriods.SECONDS) %>':
		  var pattern = '0/' +  $('#updatePeriods').val() + ' * * * * ?';
		  $('#cronPattern').val(pattern).change();
	  break;
	  case '<%= Integer.toString(Common.TimePeriods.MINUTES) %>':
		  var pattern = '* 0/' +  $('#updatePeriods').val() + ' * * * ?';
		  $('#cronPattern').val(pattern).change();
	  break;
	  case '<%= Integer.toString(Common.TimePeriods.HOURS) %>':
		  var pattern = '* * 0/' + $('#updatePeriods').val() + ' * * ?';
		  $('#cronPattern').val(pattern).change();
	  break;
	  case '<%= Integer.toString(Common.TimePeriods.DAYS) %>':
		  var pattern = '* * * 0/' +  $('#updatePeriods').val() + ' * ?';
		  $('#cronPattern').val(pattern).change();
	  break;
	  }
  });
  
  //Trigger changes for quantize and cron pattern if necessary
  var cronPattern = '${dataSource.cronPattern}';
  if(cronPattern !== '')
	  $('#cronPattern').change();
  var quantize = ${dataSource.quantize};
  if(quantize === true)
	  $('#quantize').change();
});
</script>
<div id="dataSourcePropertiesTab">
<input type="hidden" id="dataSource.enabled" value="${dataSource.enabled}"/>
<table>
  <tr>
    <td valign="top">
      <div class="borderDiv marB marR" id="dataSourceProperties">
        <table class="wide">
          <tr>
            <td class="smallTitle">
              <tag:img png="icon_ds" title="common.edit"/>
              <fmt:message key="${descriptionKey}"/>
              <c:if test="${!empty helpId}"><tag:help id="${helpId}"/></c:if>
            </td>
            <td align="right">
              <tag:img png="icon_ds" onclick="toggleDataSource()" id="dsStatusImg" style="display:none"/>
              <tag:img id="dsSaveImg" png="save" onclick="saveDataSource()" title="common.save"/>

                <tag:img png="emport" title="emport.export" onclick="exportDataSource()"/>
              <m2m2:moduleExists name="mangoApi">
                <tag:img png="csv" title="emport.exportDataPointsAsCsv" onclick="exportDataSourcePointsFromEditingSource();"/>
              </m2m2:moduleExists>
            </td>
          </tr>
        </table>
        <div id="dataSourceMessage" class="ctxmsg formError"></div>
        <table>
          <c:if test="${copy}">
            <tr id="copyDeviceName">
              <td class="formLabelRequired"><fmt:message key="dsEdit.deviceName"/></td>
              <td class="formField"><input type="text" id="dataSource.deviceName" value="${dataSource.name}"/></td>
            </tr>
          </c:if>
          <tr>
            <td class="formLabelRequired"><fmt:message key="dsEdit.head.name"/></td>
            <td class="formField"><input type="text" id="dataSource.name" value="${dataSource.name}"/></td>
          </tr>
          <tr>
            <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
            <td class="formField"><input type="text" id="dataSource.xid" value="${dataSource.xid}"/></td>
          </tr>
          <tr>
            <td class="formLabel"><fmt:message key="dsEdit.permission.edit"/></td>
            <td class="formField">
              <input type="text" id="dataSource.editPermission" value="${dataSource.editPermission}" class="formLong"/>
              <tag:img png="bullet_down" onclick="permissionUI.viewPermissions('dataSource.editPermission')"/>
              <tag:help id="permissions"/>
            </td>
          </tr>
          <tr>
            <td class="formLabel"><fmt:message key="dsEdit.logging.purge"/></td>
            <td class="formField">
              <div>
                <sst:checkbox id="dataSource.purgeOverride" selectedValue="${dataSource.purgeOverride}" onclick="changeDataSourcePurgeOverride()"/>
                <label for="dataSource.purgeOverride"><fmt:message key="dsEdit.logging.purgeOverride"/></label>
              </div>
              <div>
                <fmt:message key="pointEdit.logging.after"/>
                <input type="text" id="dataSource.purgePeriod" value="${dataSource.purgePeriod}" class="formShort"/>
                <tag:timePeriods id="dataSource.purgeType" value="${dataSource.purgeType}" d="true" w="true" mon="true" y="true"/>
              </div>
            </td>
          </tr>
          <tr>
            <td class="formLabelRequired"><fmt:message key="dsEdit.quantize"/></td>
            <td class="formField"><sst:checkbox id="quantize" selectedValue="${dataSource.quantize}"/></td>
          </tr>
          <tr>
            <td class="formLabelRequired"><fmt:message key="dsEdit.updatePeriod"/></td>
            <td class="formField">
              <input type="text" id="updatePeriods" value="${dataSource.updatePeriods}" class="formShort" />
              <tag:timePeriods id="updatePeriodType" value="${dataSource.updatePeriodType}" ms="true" s="true" min="true" h="true"/>
            </td>
          </tr>
          <tr>
            <td class="formLabelRequired"><fmt:message key="dsEdit.cronPattern"/></td>
            <td class="formField">
              <div class="label-clear">
                <input id="cronPattern" type="text" name="cronPattern" value="${dataSource.cronPattern}">
                <button id="clearCronBtn">Clear</button>
                <button id="setToPollPeriodBtn">Set to Poll Period</button>
                <jsp:include page="/WEB-INF/snippet/view/misc/cronPicker.jsp"/>
               </div>
            </td>
          </tr>
                    
          <jsp:doBody/>
          
        </table>
        <tag:dsEvents/>
        <tag:purge dwr="DataSourceEditDwr"/>
      </div>
    </td>
    
    <c:if test="${!empty extraPanels}">
      <jsp:invoke fragment="extraPanels"/>
    </c:if>
  </tr>
</table>
</div>