<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
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
       
      var useIntegralUnit = dijit.byId('useIntegralUnit');
      var integralUnit = dojo.byId('integralUnit');
      useIntegralUnit.watch('checked',function(value) {
         if(useIntegralUnit.checked){
             show("integralUnitSection");
         }else{
            hide("integralUnitSection"); 
         }
      });
      
      
      var useRenderedUnit = dijit.byId('useRenderedUnit');
      var renderedUnit = dojo.byId('renderedUnit');
      useRenderedUnit.watch('checked',function(value) {
          if(useRenderedUnit.checked){
              show("renderedUnitSection");
          }else{
              hide("renderedUnitSection");
          }
      });
      
          
       //Set all necessary values
       dojo.byId("unit").value = vo.unitString;
       dojo.byId("renderedUnit").value = vo.renderedUnitString;
       dojo.byId("integralUnit").value = vo.integralUnitString;
       
       //Not sure why the watch isn't working
       useRenderedUnit.set('checked',vo.useRenderedUnit);
       if(vo.useRenderedUnit)
           show("renderedUnitSection");
       else
           hide("renderedUnitSection");
       
       useIntegralUnit.set('checked',vo.useIntegralUnit);
       if(vo.useIntegralUnit)
           show("integralUnitSection");
       else 
           hide("integralUnitSection");

       dojo.byId("chartColour").value = vo.chartColour;
       dojo.byId("plotType").value = vo.plotType;
       
      if (vo.pointLocator.dataTypeId == <%= DataTypes.NUMERIC %>){
          show("unitSection");
      }else {
          $("plotType").disabled = true;
          $set("plotType", <%= DataPointVO.PlotTypes.STEP %>);
      }
      
   }
   
   /*
    * Get the values and put into the vo
    */
   function getPointProperties(vo){
       
       vo.unitString = dojo.byId("unit").value;
       vo.renderedUnitString = dojo.byId("renderedUnit").value;
       vo.integralUnitString = dojo.byId("integralUnit").value;
       vo.useRenderedUnit = dijit.byId("useRenderedUnit").get('checked');
       vo.useIntegralUnit = dijit.byId("useIntegralUnit").get('checked');

       vo.chartColour = dojo.byId("chartColour").value;
       vo.plotType =  dojo.byId("plotType").value;
       
       //For now 
       
   }
   
   /**
    * Helper method to validate units on demand
    */
   function validateUnit(unitString,messageDivId){
	   DataPointDwr.validateUnit(unitString,function(response){
		   if(!response.data.validUnit){
			   var div = $(messageDivId);
			   div.style.color = "red";
			   div.innerHTML = response.data.message;
		   }else{
			   var div = $(messageDivId);
			   div.style.color = "green";
			   div.innerHTML = response.data.message;
		   }
	   });
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
          <td class="formLabel"><fmt:message key="pointEdit.props.unit"/></td>
          <td class="formField"><input type="text" name="unit" id="unit" 
                onkeyup="validateUnit(value,'unitMessage');" 
                oninput="validateUnit(value,'unitMessage');"
                onpaste="validateUnit(value,'unitMessage');"/><div id="unitMessage"></div></td>
        </tr>
       <tr>
        <td class="formLabel"><fmt:message key="pointEdit.props.useRenderedUnit"/></td>
        <td class="formField">
          <input data-dojo-type="dijit/form/CheckBox" id="useRenderedUnit" name="useRenderedUnit" />
        </td>
      </tr>
      <tr id="renderedUnitSection">
          <td class="formLabelRequired"><fmt:message key="pointEdit.props.renderedUnit"/></td>
          <td class="formField"><input type="text" name="renderedUnit" id="renderedUnit" 
                onkeyup="validateUnit(value,'renderedUnitMessage');" 
                oninput="validateUnit(value,'renderedUnitMessage');"
                onpaste="validateUnit(value,'renderedUnitMessage');"/><div id="renderedUnitMessage"></div></td>
        </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.props.useIntegralUnit"/></td>
        <td class="formField">
          <input data-dojo-type="dijit/form/CheckBox" id="useIntegralUnit" name="useIntegralUnit" />
        </td>
      </tr>
      <tr id="integralUnitSection">
          <td class="formLabelRequired"><fmt:message key="pointEdit.props.integralUnit"/></td>
          <td class="formField"><input type="text" name="integralUnit" id="integralUnit" 
                onkeyup="validateUnit(value,'integralUnitMessage');" 
                oninput="validateUnit(value,'integralUnitMessage');"
                onpaste="validateUnit(value,'integralUnitMessage');"/><div id="integralUnitMessage"></div></td>
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