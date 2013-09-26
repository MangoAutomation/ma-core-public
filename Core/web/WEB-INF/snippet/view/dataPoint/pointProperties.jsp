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
       dojo.byId("unit").value = vo.unitString;
	   dojo.byId("renderedUnit").value = vo.renderedUnitString;
	   dojo.byId("integralUnit").value = vo.integralUnitString;
	   
	   dojo.byId("useRenderedUnit").value = vo.useRenderedUnit;
	   dojo.byId("useIntegralUnit").value = vo.useIntegralUnit;

       dojo.byId("chartColour").value = vo.chartColour;
       dojo.byId("plotType").value = vo.plotType;
	   
      if (vo.pointLocator.dataTypeId == <%= DataTypes.NUMERIC %>){
          show("unitSection");
      }else {
          $("plotType").disabled = true;
          $set("plotType", <%= DataPointVO.PlotTypes.STEP %>);
      }
      
      var useIntegralUnit = dojo.byId('useIntegralUnit');
      var integralUnit = dojo.byId('integralUnit');
      useIntegralUnit.onchange = function(value) {
          integralUnit.disabled = !useIntegralUnit.checked;
      }
      useIntegralUnit.onchange();
      
      var useRenderedUnit = dojo.byId('useRenderedUnit');
      var renderedUnit = dojo.byId('renderedUnit');
      useRenderedUnit.onchange = function(value) {
          renderedUnit.disabled = !useRenderedUnit.checked;
      }
      useRenderedUnit.onchange();
      
   }
   
   /*
    * Get the values and put into the vo
    */
   function getPointProperties(vo){
       
       vo.unitString = dojo.byId("unit").value;
       vo.renderedUnitString = dojo.byId("renderedUnit").value;
       vo.integralUnitString = dojo.byId("integralUnit").value;
	   vo.useRenderedUnit = dojo.byId("useRenderedUnit").value;
       vo.useIntegralUnit = dojo.byId("useIntegralUnit").value;

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
    
    <tbody id="unitSection" style="display:none;">
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.props.unit"/></td>
          <td class="formField"><input type="text" name="unit" id="unit" /></td>
        </tr>
       <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.props.renderedUnit"/></td>
        <td class="formField">
          <sst:checkbox id="useRenderedUnit" name="useRenderedUnit" />
          <label for="useRenderedUnit"><fmt:message key="pointEdit.props.useRenderedUnit"/></label>
        </td>
      </tr>
      <tr id="renderedUnitSection">
          <td></td>
          <td class="formField"><input type="text" name="renderedUnit" id="renderedUnit"/></td>
        </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.props.integralUnit"/></td>
        <td class="formField">
          <sst:checkbox id="useIntegralUnit" name="useIntegralUnit" />
          <label for="useIntegralUnit"><fmt:message key="pointEdit.props.useIntegralUnit"/></label>
        </td>
      </tr>
      <tr id="integralUnitSection">
          <td></td>
          <td class="formField"><input type="text" name="integralUnit" id="integralUnit"/></td>
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