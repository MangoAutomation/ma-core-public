<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<div >
  <table>
    <tr><td colspan="3">
      <span class="smallTitle"><fmt:message key="pointEdit.chart.props"/></span>
      <tag:help id="chartRenderers"/>
    </td>
    </tr>
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.chart.type"/></td>
      <td class="formField">
        <input id="chartRendererSelect"></input>
      </td>
    </tr>
    <tbody id="timePeriodRow" style="display:none">
       <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.chart.timePeriod"/></td>
        <td class="formField">
          <input id="numberOfPeriods" type="text" class="formVeryShort"/>
          <tag:timePeriods id="timePeriod" min="true" h="true" d="true" w="true" mon="true"/>
        </td>
      </tr>
    </tbody>  
    <tbody id="limitRow" style="display:none">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.chart.limit"/></td>
        <td class="formField"><input id="limit" type="text" class="formShort"/></td>
      </tr>
    </tbody>
    <tbody id="includeSumRow" style="display:none">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.chart.includeSum"/></td>
        <td class="formField"><input id="includeSum" data-dojo-type="dijit/form/CheckBox"/></td>
      </tr>
    </tbody>
    <tr>
      <td colspan="2"><fmt:message key="pointEdit.chart.note"/></td>
    </tr>
  </table>
</div>

<script type="text/javascript">

dojo.require("dijit.form.Select");
dojo.require("dijit.form.CheckBox");

var chartRendererSelect = new dijit.form.Select({
    name: 'chartRendererSelect',
},"chartRendererSelect");

  /*
   * Set the page values from the current data point VO
   */
  function setChartRenderer(vo){
	  
      //Clear and Setup the Chart Renderer Options
      DataPointDwr.getChartRendererOptions(vo.pointLocator.dataTypeId,function (response){
    	  var options = [];
    	  for(var i=0; i<response.data.options.length; i++){
    		  options.push({
    			  label: mangoMsg[response.data.options[i].nameKey],
    			  value: response.data.options[i].name,
    		  })
    	  }
    	  chartRendererSelect.options = [];
          chartRendererSelect.addOption(options);
          
          
          if(vo.chartRenderer != null){
              chartRendererSelect.set('value',vo.chartRenderer.typeName);

              if(vo.chartRenderer.typeName == "chartRendererTable"){
                  $set("limit", vo.chartRenderer.limit);
              }else if(vo.chartRenderer.typeName === "chartRendererImage"){
                  $set("numberOfPeriods", vo.chartRenderer.numberOfPeriods);
                  $set("timePeriod", vo.chartRenderer.timePeriod);
              }else if(vo.chartRenderer.typeName == "chartRendererStats"){
                  $set("numberOfPeriods", vo.chartRenderer.numberOfPeriods);
                  $set("timePeriod", vo.chartRenderer.timePeriod);
                  dijit.byId("includeSum").set("checked", vo.chartRenderer.includeSum);
              }else if(vo.chartRenderer.typeName == "chartRendererImageFlipbook"){
                  $set("limit", vo.chartRenderer.limit);
              }else if(vo.chartRenderer.typeName == "chartRendererNone"){
            	  //Do nothing
              }else{
                  dojo.debug("Unknown chart renderer: " + vo.chartRenderer.typeName);
              }
          }//Not null
      });
	  

  }

  
  /*
   * Get the values from the page and put into current data point VO
   */
  function getChartRenderer(vo){
      
      //Save all pertinent pieces
      var typeName = chartRendererSelect.get('value');
      var renderer;
      if (typeName == "chartRendererNone"){
    	  renderer = null;
      }else if (typeName == "chartRendererTable") {
          var limit = parseInt($get("limit"));
          if(isNaN(limit))
        	  limit=-1;
          renderer = new TableChartRenderer();
          renderer.limit = limit;
          renderer.typeName = "chartRendererTable";
      }
      else if (typeName == "chartRendererImage") {
          renderer = new ImageChartRenderer();
          renderer.typeName = "chartRendererImage";
          renderer.timePeriod = $get("timePeriod");
          renderer.numberOfPeriods = parseInt($get("numberOfPeriods"));
          if(isNaN(renderer.numberOfPeriods))
        	  renderer.numberOfPeriods=-1;
      }
      else if (typeName == "chartRendererStats") {
          renderer = new StatisticsChartRenderer();
          renderer.typeName = "chartRendererStats";
          renderer.timePeriod = $get("timePeriod");
    	  renderer.numberOfPeriods = parseInt($get("numberOfPeriods"));
    	  renderer.includeSum = dijit.byId("includeSum").get('checked');
          if(isNaN(renderer.numberOfPeriods))
              renderer.numberOfPeriods=-1;
      }
      else if (typeName == "chartRendererImageFlipbook") {
    	  renderer = new ImageFlipbookRenderer();
    	  renderer.typeName = "chartRendererImageFlipbook";
    	  renderer.limit = parseInt($get("limit"));
    	  if(isNaN(renderer.limit))
    		  renderer.limit=-1;
      }

      //Set the renderer
      vo.chartRenderer = renderer;
  }
  
  /**
   * Reset the Chart Renderer Input to the default for that data type
   */
  function resetChartRendererOptions(dataTypeId){

	  //Change the renderer to the default for this data type
        var vo = {
      		 chartRenderer: {
      			 typeName: 'chartRendererNone',
      			 numberOfPeriods: -1,
      			 timePeriod: 1,
      			 limit: 1
          		 },
      		 pointLocator: {dataTypeId: dataTypeId},
      		 
      		 
        };
        setChartRenderer(vo);
  }
  //Register for callbacks when the data type is changed
  dataTypeChangedCallbacks.push(resetChartRendererOptions);
  
  function ChartRendererEditor() {

	  this.disableInputs = function(){
		  chartRendererSelect.set('disabled', true);
		  setDisabled('limit', true);
		  setDisabled('numberOfPeriods', true);
		  setDisabled('timePeriod', true);
          dijit.byId("includeSum").set('disabled', true);
	  }
	  
	  this.enableInputs = function(){
		  chartRendererSelect.set('disabled', false);
		  setDisabled('limit', false);
		  setDisabled('numberOfPeriods', false);
		  setDisabled('timePeriod', false);
          dijit.byId("includeSum").set('disabled', false);
      }
  
      this.change = function(name,oldValue,value) {
    	  
    	  if(value != null){
              if(value == "chartRendererTable"){
            	  hide("timePeriodRow");
            	  show("limitRow");
            	  hide("includeSumRow");
              }else if(value === "chartRendererImage"){
                  show("timePeriodRow");
                  hide("limitRow");
                  hide("includeSumRow");
              }else if(value == "chartRendererStats"){
                  show("timePeriodRow");
                  hide("limitRow");
                  show("includeSumRow");
              }else if(value == "chartRendererImageFlipbook"){
                  hide("timePeriodRow");
                  show("limitRow");
                  hide("includeSumRow");
              }else if(value == "chartRendererNone"){
                  hide("timePeriodRow");
                  hide("limitRow");
                  hide("includeSumRow");            	  
              }else{
                  alert("Unknown chart renderer: " + value);
              }

    	  }
    	  
    	  
      };
      
      chartRendererSelect.watch("value",this.change);

  }
  var chartRendererEditor = new ChartRendererEditor();


</script>