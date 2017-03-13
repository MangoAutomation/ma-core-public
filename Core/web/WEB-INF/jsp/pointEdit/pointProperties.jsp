<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
    
    @Deprecated - Use jsp files in snippet/view/dataPoint/ instead
--%>
<%@page import="com.serotonin.m2m2.vo.DataPointVO"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.DataTypes"%>
<%@page import="com.serotonin.m2m2.Common"%>

<script type="text/javascript">
  dojo.ready(function() {
      if (dataTypeId == <%= DataTypes.NUMERIC %>)
          show("engineeringUnitsSection");
      else {
          $("plotType").disabled = true;
          $set("plotType", <%= DataPointVO.PlotTypes.STEP %>);
      }
  });
  
  function changeExtremeSet() {
      var prevent = $get("preventSetExtremeValues");
      if (prevent) {
          $("setExtremeLowLimit").disabled = false;
          $("setExtremeHighLimit").disabled = false;
      }
      else {
          $("setExtremeLowLimit").disabled = true;
          $("setExtremeHighLimit").disabled = true;
      }
  }
</script>

<div class="borderDiv marB marR">
  <table>
    <tr>
      <td colspan="3">
        <img src="images/icon_comp_edit.png"/>
        <span class="smallTitle"><fmt:message key="pointEdit.props.props"/></span>
        <tag:help id="dataPointEditing"/>
        <a href="data_point_details.shtm?dpid=${form.id}"><tag:img png="icon_comp" title="pointEdit.props.details"/></a>
      </td>
    </tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.props.ds"/></td>
      <td colspan="2" class="formField">
        ${dataSource.name}
        <a href="data_source_edit.shtm?dsid=${dataSource.id}&pid=${form.id}"><tag:img png="icon_ds_edit"
                title="pointEdit.props.editDs"/></a>
      </td>
    </tr>
      
    <spring:bind path="form.name">
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.pointName"/></td>
        <td class="formField"><input type="text" name="name" value="${status.value}"/></td>
        <td class="formError">${status.errorMessage}</td>
      </tr>
    </spring:bind>
    
    <spring:bind path="form.deviceName">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.props.deviceName"/></td>
        <td class="formField"><input type="text" name="deviceName" value="${status.value}"/></td>
        <td class="formError">${status.errorMessage}</td>
      </tr>
    </spring:bind>
    
    <tbody id="engineeringUnitsSection" style="display:none;">
      <spring:bind path="form.engineeringUnits">
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.props.engineeringUnits"/></td>
<%--           <td class="formField"><tag:engineeringUnits name="engineeringUnits" value="${status.value}"/></td> --%>
          <td class="formError">${status.errorMessage}</td>
        </tr>
      </spring:bind>
    </tbody>
    
    <spring:bind path="form.chartColour">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.props.chartColour"/></td>
        <td class="formField"><input type="text" name="chartColour" value="${status.value}"/></td>
        <td class="formError">${status.errorMessage}</td>
      </tr>
    </spring:bind>
    
    <spring:bind path="form.plotType">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.plotType"/></td>
        <td class="formField">
          <sst:select id="plotType" name="plotType" value="${form.plotType}">
            <sst:option value="<%= Integer.toString(DataPointVO.PlotTypes.STEP) %>"><fmt:message key="pointEdit.plotType.step"/></sst:option>
            <sst:option value="<%= Integer.toString(DataPointVO.PlotTypes.LINE) %>"><fmt:message key="pointEdit.plotType.line"/></sst:option>
            <sst:option value="<%= Integer.toString(DataPointVO.PlotTypes.SPLINE) %>"><fmt:message key="pointEdit.plotType.spline"/></sst:option>
          </sst:select>
        </td>
        <td class="formError">${status.errorMessage}</td>
      </tr>
    </spring:bind>
    
    <spring:bind path="form.rollup">
    	<tr>
    		<td class="formLabelRequired"><fmt:message key="common.rollup"></fmt:message>
    		<td class="formField">
         	  <tag:exportCodesOptions id="rollup" name="rollup" optionList="<%= Common.ROLLUP_CODES.getIdKeys() %>"/>
            </td>
 		</tr>
    </spring:bind>
    
    <spring:bind path="form.preventSetExtremeValues">
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.props.preventSetExtremeValues"/></td>
          <td class="formField">
            <sst:checkbox id="preventSetExtremeValues" name="preventSetExtremeValues" selectedValue="${status.value}"
                    onclick="changeExtremeSet()"/>
          </td>
          <td class="formError">${status.errorMessage}</td>
        </tr>
      </spring:bind>
      <spring:bind path="form.setExtremeLowLimit">
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.props.setExtremeLowLimit"/></td>
          <td class="formField">
            <input id="setExtremeLowLimit" type="text" name="setExtremeLowLimit" value="${status.value}" class="formShort"/>
          </td>
          <td class="formError">${status.errorMessage}</td>
        </tr>
      </spring:bind>
      <spring:bind path="form.setExtremeHighLimit">
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.props.setExtremeHighLimit"/></td>
          <td class="formField">
            <input id="setExtremeHighLimit" type="text" name="setExtremeHighLimit" value="${status.value}" class="formShort"/>
          </td>
          <td class="formError">${status.errorMessage}</td>
        </tr>
      </spring:bind>
  </table>
</div>