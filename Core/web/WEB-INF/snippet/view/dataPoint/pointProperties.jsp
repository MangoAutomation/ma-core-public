<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.vo.DataPointVO"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.DataTypes"%>

<script type="text/javascript">

   /**
    * Set the input values on the page using this vo
    */
   function setPointProperties(vo) {
	   //Set all necessary values
        dojo.byId("engineeringUnits").value = vo.engineeringUnits;
        dojo.byId("chartColour").value = vo.chartColour;
        dojo.byId("plotType").value = vo.plotType;
	   
      if (vo.pointLocator.dataTypeId == <%= DataTypes.NUMERIC %>){
          show("engineeringUnitsSection");
      }else {
          $("plotType").disabled = true;
          $set("plotType", <%= DataPointVO.PlotTypes.STEP %>);
      }
   }
   
   /*
    * Get the values and put into the vo
    */
   function getPointProperties(vo){
	   vo.engineeringUnits = dojo.byId("engineeringUnits").value;
	   vo.chartColour = dojo.byId("chartColour").value;
       vo.plotType =  dojo.byId("plotType").value;
       
       //For now 
       
   }
   
   
   
</script>

<div >
  <table>
    <tr>
      <td colspan="3">
        <span class="smallTitle"><fmt:message key="pointEdit.props.props"/></span>
        <tag:help id="dataPointEditing"/>
      </td>
    </tr>
    
<!-- Could re-add if we want to be able to access DS Settings from here?    <tr> -->
<%--       <td class="formLabelRequired"><fmt:message key="pointEdit.props.ds"/></td> --%>
<!--       <td colspan="2" class="formField"> -->
<%--         ${dataSource.name} --%>
<%--         <a href="data_source_edit.shtm?dsid=${dataSource.id}&pid=${form.id}"><tag:img png="icon_ds_edit" --%>
<%--                 title="pointEdit.props.editDs"/></a> --%>
<!--       </td> -->
<!--     </tr> -->
      
<!--       <tr> -->
<%--         <td class="formLabelRequired"><fmt:message key="common.pointName"/></td> --%>
<!--         <td class="formField"><input type="text" id="pointName"name="name"/></td> -->
<%--         <td class="formError">${status.errorMessage}</td> --%>
<!--       </tr> -->
    
<%--     <spring:bind path="form.deviceName"> --%>
<!--       <tr> -->
<%--         <td class="formLabelRequired"><fmt:message key="pointEdit.props.deviceName"/></td> --%>
<%--         <td class="formField"><input type="text" name="deviceName" value="${status.value}"/></td> --%>
<%--         <td class="formError">${status.errorMessage}</td> --%>
<!--       </tr> -->
<%--     </spring:bind> --%>
    
    <tbody id="engineeringUnitsSection" style="display:none;">
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.props.engineeringUnits"/></td>
          <td class="formField"><tag:engineeringUnits name="engineeringUnits" id="engineeringUnits"/></td>
        </tr>
    </tbody>
    
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.props.chartColour"/></td>
        <td class="formField"><input type="text" name="chartColour" id="chartColour"/></td>
      </tr>

      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.plotType"/></td>
        <td class="formField">
          <sst:select name="plotType" id="plotType">
            <sst:option value="<%= Integer.toString(DataPointVO.PlotTypes.STEP) %>"><fmt:message key="pointEdit.plotType.step"/></sst:option>
            <sst:option value="<%= Integer.toString(DataPointVO.PlotTypes.LINE) %>"><fmt:message key="pointEdit.plotType.line"/></sst:option>
            <sst:option value="<%= Integer.toString(DataPointVO.PlotTypes.SPLINE) %>"><fmt:message key="pointEdit.plotType.spline"/></sst:option>
          </sst:select>
        </td>
      </tr>
  </table>
</div>