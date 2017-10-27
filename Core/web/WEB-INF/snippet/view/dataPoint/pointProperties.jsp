<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.vo.DataPointVO"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp"%>
<%@page import="com.serotonin.m2m2.DataTypes"%>
<%@page import="com.serotonin.m2m2.Common"%>

<script type="text/javascript">
	//Create a filter select list
	var unitsMasterListStore;
	var unitPicker, renderedUnitPicker, integralUnitPicker;

	require([ "dojo", "dojo/store/Memory", "dijit/form/ComboBox" ], function(
			dojo, Memory, ComboBox) {

		//Go get the units list
		DataPointDwr.getUnitsList(function(response) {
			//Create the store
			unitsMasterListStore = new dojo.store.Memory({
				idProperty : "key",
				valueProperty : "value",
				data : response.data.units
			});

			//Create the base unit input
			unitPicker = new ComboBox({
				store : unitsMasterListStore,
				autoComplete : false,
				style : "width: 250px;",
				queryExpr : "*\${0}*",
				highlightMatch : "all",
				required : false,
				placeHolder : "select unit",
				onChange : function(unit) {
					validateUnit(unit, 'unitMessage');
				}
			}, "unit");

			//Create the base unit input
			renderedUnitPicker = new ComboBox({
				store : unitsMasterListStore,
				autoComplete : false,
				style : "width: 250px;",
				queryExpr : "*\${0}*",
				highlightMatch : "all",
				required : false,
				placeHolder : "select unit",
				onChange : function(unit) {
					validateUnit(unit, 'renderedUnitMessage');
				}
			}, "renderedUnit");

			//Create the base unit input
			integralUnitPicker = new ComboBox({
				store : unitsMasterListStore,
				autoComplete : false,
				style : "width: 250px;",
				queryExpr : "*\${0}*",
				highlightMatch : "all",
				required : false,
				placeHolder : "select unit",
				onChange : function(unit) {
					validateUnit(unit, 'integralUnitMessage');
				}
			}, "integralUnit");
		});

	});

	/**
	 * Set the input values on the page using this vo
	 */
	function setPointProperties(vo) {

		var useIntegralUnit = dijit.byId('useIntegralUnit');

		useIntegralUnit.watch('checked', function(value) {
			if (useIntegralUnit.checked) {
				show("integralUnitSection");
			} else {
				hide("integralUnitSection");
			}
		});

		var useRenderedUnit = dijit.byId('useRenderedUnit');
		var renderedUnit = dojo.byId('renderedUnit');
		useRenderedUnit.watch('checked', function(value) {
			if (useRenderedUnit.checked) {
				show("renderedUnitSection");
			} else {
				hide("renderedUnitSection");
			}
		});

		//Set all necessary values
		//dojo.byId("unit").value = vo.unitString;
		unitPicker.set('value', vo.unitString);
		//dojo.byId("renderedUnit").value = vo.renderedUnitString;
		renderedUnitPicker.set('value', vo.renderedUnitString);
		//dojo.byId("integralUnit").value = vo.integralUnitString;
		integralUnitPicker.set('value', vo.integralUnitString);

		//Not sure why the watch isn't working
		useRenderedUnit.set('checked', vo.useRenderedUnit);
		if (vo.useRenderedUnit)
			show("renderedUnitSection");
		else
			hide("renderedUnitSection");

		useIntegralUnit.set('checked', vo.useIntegralUnit);
		if (vo.useIntegralUnit)
			show("integralUnitSection");
		else
			hide("integralUnitSection");

		dojo.byId("chartColour").value = vo.chartColour;
		dojo.byId("plotType").value = vo.plotType;

		if (vo.pointLocator.dataTypeId == <%=DataTypes.NUMERIC%>) {
			show("unitSection");
		} else {
			$("plotType").disabled = true;
			$set("plotType",<%=DataPointVO.PlotTypes.STEP%>);
		}
		
		$set("rollup", vo.rollup);

		dijit.byId('preventSetExtremeValues').set('checked', vo.preventSetExtremeValues);
		dojo.byId("setExtremeLowLimit").value = vo.setExtremeLowLimit;
		dojo.byId("setExtremeHighLimit").value = vo.setExtremeHighLimit;
		changeExtremeSet();
	}

	/*
	 * Get the values and put into the vo
	 */
	function getPointProperties(vo) {
		vo.unitString = unitPicker.get('value'); //dojo.byId("unit").value;
		vo.renderedUnitString = renderedUnitPicker.get('value'); //dojo.byId("renderedUnit").value;
		vo.integralUnitString = integralUnitPicker.get('value'); //dojo.byId("integralUnit").value;
		vo.useRenderedUnit = dijit.byId("useRenderedUnit").get('checked');
		vo.useIntegralUnit = dijit.byId("useIntegralUnit").get('checked');
		vo.rollup = dojo.byId("rollup").value;
		vo.chartColour = dojo.byId("chartColour").value;
		vo.plotType = dojo.byId("plotType").value;
		vo.preventSetExtremeValues = dijit.byId("preventSetExtremeValues").get('checked');
		vo.setExtremeLowLimit = dojo.byId("setExtremeLowLimit").value;
		vo.setExtremeHighLimit = dojo.byId("setExtremeHighLimit").value;
	}

	/**
	 * Helper method to validate units on demand
	 */
	function validateUnit(unitString, messageDivId) {
		DataPointDwr.validateUnit(unitString, function(response) {
			if (!response.data.validUnit) {
				var div = $(messageDivId);
				div.style.color = "red";
				div.innerHTML = response.data.message;
			} else {
				var div = $(messageDivId);
				div.style.color = "green";
				div.innerHTML = response.data.message;
			}
		});
	}

	/**
	 * Reset the Point Properties Inputs depending on Data Type
	 */
	function resetPointProperties(dataTypeId) {
		var rollupNode = $("rollup");
		if (dataTypeId == <%=DataTypes.NUMERIC%>) {
			show("unitSection");
			$("plotType").disabled = false;
			rollupNode[<%=Common.Rollups.NONE%>].hidden = false;
			rollupNode[<%=Common.Rollups.AVERAGE%>].hidden = false;
			rollupNode[<%=Common.Rollups.DELTA%>].hidden = false;
			rollupNode[<%=Common.Rollups.MINIMUM%>].hidden = false;
			rollupNode[<%=Common.Rollups.MAXIMUM%>].hidden = false;
			rollupNode[<%=Common.Rollups.ACCUMULATOR%>].hidden = true;
			rollupNode[<%=Common.Rollups.SUM%>].hidden = false;
			rollupNode[<%=Common.Rollups.START%>].hidden = false;
			rollupNode[<%=Common.Rollups.FIRST%>].hidden = false;
			rollupNode[<%=Common.Rollups.LAST%>].hidden = false;
			rollupNode[<%=Common.Rollups.COUNT%>].hidden = false;
			rollupNode[<%=Common.Rollups.INTEGRAL%>].hidden = false;
			rollupNode[<%=Common.Rollups.ALL%>].hidden = true;
			if(rollupNode[$get("rollup")].hidden)
				$set("rollup", <%=Common.Rollups.NONE%>);
		} else {
			hide("unitSection");
			$("plotType").disabled = true;
			$set("plotType",<%=DataPointVO.PlotTypes.STEP%>);
			rollupNode[<%=Common.Rollups.NONE%>].hidden = false;
			rollupNode[<%=Common.Rollups.AVERAGE%>].hidden = true;
			rollupNode[<%=Common.Rollups.DELTA%>].hidden = true;
			rollupNode[<%=Common.Rollups.MINIMUM%>].hidden = true;
			rollupNode[<%=Common.Rollups.MAXIMUM%>].hidden = true;
			rollupNode[<%=Common.Rollups.ACCUMULATOR%>].hidden = true;
			rollupNode[<%=Common.Rollups.SUM%>].hidden = true;
			rollupNode[<%=Common.Rollups.START%>].hidden = false;
			rollupNode[<%=Common.Rollups.FIRST%>].hidden = false;
			rollupNode[<%=Common.Rollups.LAST%>].hidden = false;
			rollupNode[<%=Common.Rollups.COUNT%>].hidden = false;
			rollupNode[<%=Common.Rollups.INTEGRAL%>].hidden = true;
			rollupNode[<%=Common.Rollups.ALL%>].hidden = true;
			if(rollupNode[$get("rollup")].hidden)
				$set("rollup", <%=Common.Rollups.NONE%>);
		}
	}
	
	//Register for callbacks when the data type is changed
	dataTypeChangedCallbacks.push(resetPointProperties);

	function disablePointProperties(dataTypeId) {
		setDisabled('chartColour', true);
		setDisabled('plotType', true);
		setDisabled('preventSetExtremeValues', true);
		setDisabled('setExtremeLowLimit', true);
		setDisabled('setExtremeHighLimit', true);
		setDisabled('rollup', true);
	}
	
	function enablePointProperties(dataTypeId) {
		setDisabled('chartColour', false);
		setDisabled('plotType', false);
		setDisabled('preventSetExtremeValues', false);
		setDisabled('setExtremeLowLimit', false);
		setDisabled('setExtremeHighLimit', false);
		setDisabled('rollup', false);
		resetPointProperties(dataTypeId);
	}
	
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

<div>
  <table>
    <tr>
      <td colspan="3"><span class="smallTitle"><fmt:message
            key="pointEdit.props.props" /></span> <tag:help
          id="dataPointEditing" /></td>
    </tr>

    <tbody id="unitSection" style="display: none;">
      <tr>
        <td class="formLabel"><fmt:message
            key="pointEdit.props.unit" /></td>
        <td class="formField">
          <div id="unit"></div>
          <div id="unitMessage"></div>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message
            key="pointEdit.props.useRenderedUnit" /></td>
        <td class="formField"><input
          data-dojo-type="dijit.form.CheckBox" id="useRenderedUnit"
          name="useRenderedUnit" /></td>
      </tr>
      <tr id="renderedUnitSection">
        <td class="formLabelRequired"><fmt:message
            key="pointEdit.props.renderedUnit" /></td>
        <td class="formField">
          <div id="renderedUnit"></div>
          <div id="renderedUnitMessage"></div>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message
            key="pointEdit.props.useIntegralUnit" /></td>
        <td class="formField"><input
          data-dojo-type="dijit.form.CheckBox" id="useIntegralUnit"
          name="useIntegralUnit" /></td>
      </tr>
      <tr id="integralUnitSection">
        <td class="formLabelRequired"><fmt:message
            key="pointEdit.props.integralUnit" /></td>
        <td class="formField">
          <div id="integralUnit"></div>
          <div id="integralUnitMessage"></div>
        </td>
      </tr>
    </tbody>

    <tr>
      <td class="formLabelRequired"><fmt:message
          key="pointEdit.props.chartColour" /></td>
      <td class="formField"><input type="text" name="chartColour"
        id="chartColour" /></td>
    </tr>
    
    <tr>
  		<td class="formLabelRequired"><fmt:message key="common.rollup"></fmt:message>
  		<td class="formField">
       	  <tag:exportCodesOptions id="rollup" optionList="<%= Common.ROLLUP_CODES.getIdKeys() %>"/>
        </td>
	</tr>

    <tr>
      <td class="formLabelRequired"><fmt:message
          key="pointEdit.plotType" /></td>
      <td class="formField"><sst:select name="plotType"
          id="plotType">
          <sst:option
            value="<%=Integer.toString(DataPointVO.PlotTypes.STEP)%>">
            <fmt:message key="pointEdit.plotType.step" />
          </sst:option>
          <sst:option
            value="<%=Integer.toString(DataPointVO.PlotTypes.LINE)%>">
            <fmt:message key="pointEdit.plotType.line" />
          </sst:option>
          <sst:option
            value="<%=Integer.toString(DataPointVO.PlotTypes.SPLINE)%>">
            <fmt:message key="pointEdit.plotType.spline" />
          </sst:option>
          <sst:option
            value="<%=Integer.toString(DataPointVO.PlotTypes.BAR)%>">
            <fmt:message key="pointEdit.plotType.bar" />
          </sst:option>
        </sst:select></td>
    </tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.props.preventSetExtremeValues"/></td>
      <td class="formField">
        <input
          data-dojo-type="dijit.form.CheckBox" id="preventSetExtremeValues"
          name="preventSetExtremeValues"  onchange="changeExtremeSet();"/>
      </td>
      <td class="formError">${status.errorMessage}</td>
    </tr>
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.props.setExtremeLowLimit"/></td>
      <td class="formField">
        <input id="setExtremeLowLimit" type="text" name="setExtremeLowLimit" value="${status.value}" class="formShort"/>
      </td>
      <td class="formError">${status.errorMessage}</td>
    </tr>
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.props.setExtremeHighLimit"/></td>
      <td class="formField">
        <input id="setExtremeHighLimit" type="text" name="setExtremeHighLimit" value="${status.value}" class="formShort"/>
      </td>
      <td class="formError">${status.errorMessage}</td>
    </tr>
  </table>
</div>