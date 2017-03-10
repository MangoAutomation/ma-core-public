<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
    
   @Deprecated - Use jsp files in snippet/view/dataPoint/ instead
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common" %>
<div class="borderDiv marB marR">
  <table>
    <tr><td colspan="3">
      <span class="smallTitle"><fmt:message key="pointEdit.chart.props"/></span>
      <tag:help id="chartRenderers"/>
    </td></tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.chart.type"/></td>
      <td class="formField">
        <sst:select id="chartRendererSelect" onchange="chartRendererEditor.change()"
                value="${form.chartRenderer.typeName}">
          <c:forEach items="${chartRenderers}" var="crdef">
            <sst:option value="${crdef.name}"><fmt:message key="${crdef.nameKey}"/></sst:option>
          </c:forEach>
        </sst:select>
      </td>
    </tr>
    
    <tbody id="chartRendererNone" style="display:none;"></tbody>
    <tbody id="chartRendererTable" style="display:none;">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.chart.limit"/></td>
        <td class="formField"><input id="chartRendererTableLimit" type="text" class="formShort"/></td>
      </tr>
    </tbody>
    <tbody id="chartRendererImage" style="display:none;">
      <tr>
      	<td class="formLabelRequired"><fmt:message key="common.relativeDateType"/></td>
      	<td class="formField">
          <tag:exportCodesOptions id="chartRendererImageRelativeDateType" optionList="<%= Common.RELATIVE_DATE_TYPE_CODES.getIdKeys() %>"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.chart.timePeriod"/></td>
        <td class="formField">
          <input id="chartRendererImageNumberOfPeriods" type="text" class="formVeryShort"/>
          <tag:timePeriods id="chartRendererImageTimePeriod" min="true" h="true" d="true" w="true" mon="true"/>
        </td>
      </tr>
      <tr>
      	<td class="formLabelRequired"><fmt:message key="common.rollup"/></td>
      	<td class="formField">
          <tag:exportCodesOptions id="chartRendererImageRollupType" optionList="<%= Common.ROLLUP_CODES.getIdKeys() %>"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.rollupPeriod"/></td>
        <td class="formField">
          <input id="chartRendererImageRollupNumberOfPeriods" type="text" class="formVeryShort"/>
          <tag:timePeriods id="chartRendererImageRollupTimePeriod" s="true" min="true" h="true" d="true" w="true" mon="true"/>
        </td>
      </tr>
    </tbody>
    <tbody id="chartRendererStats" style="display:none;">
      <tr>
      	<td class="formLabelRequired"><fmt:message key="common.relativeDateType"/></td>
      	<td class="formField">
          <tag:exportCodesOptions id="chartRendererStatsRelativeDateType" optionList="<%= Common.RELATIVE_DATE_TYPE_CODES.getIdKeys() %>"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.chart.timePeriod"/></td>
        <td class="formField">
          <input id="chartRendererStatsNumberOfPeriods" type="text" class="formVeryShort"/>
          <tag:timePeriods id="chartRendererStatsTimePeriod" min="true" h="true" d="true" w="true" mon="true"/>
        </td>
      </tr>
      <tr>
      	<td class="formLabelRequired"><fmt:message key="common.rollup"/></td>
      	<td class="formField">
          <tag:exportCodesOptions id="chartRendererStatsRollupType" optionList="<%= Common.ROLLUP_CODES.getIdKeys() %>"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.rollupPeriod"/></td>
        <td class="formField">
          <input id="chartRendererStatsRollupNumberOfPeriods" type="text" class="formVeryShort"/>
          <tag:timePeriods id="chartRendererStatsRollupTimePeriod" s="true" min="true" h="true" d="true" w="true" mon="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.chart.includeSum"/></td>
        <td class="formField"><input id="chartRendererStatsIncludeSum" type="checkbox"/></td>
      </tr>
    </tbody>
    <tbody id="chartRendererImageFlipbook" style="display:none;">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.chart.limit"/></td>
        <td class="formField"><input id="chartRendererImageFlipbookLimit" type="text" class="formShort"/></td>
      </tr>
    </tbody>
    
    <tr>
      <td colspan="2"><fmt:message key="pointEdit.chart.note"/></td>
    </tr>
  </table>
</div>

<script type="text/javascript">
  function ChartRendererEditor() {
      var currentChartRenderer;
      
      this.init = function() {
          // Figure out which fields to populate with data.
          
          <c:choose>
            <c:when test='${empty form.chartRenderer}'>
            </c:when>
            <c:when test='${form.chartRenderer.typeName == "chartRendererTable"}'>
              $set("chartRendererTableLimit", "${form.chartRenderer.limit}");
            </c:when>
            <c:when test='${form.chartRenderer.typeName == "chartRendererImage"}'>
              $set("chartRendererImageNumberOfPeriods", "${form.chartRenderer.numberOfPeriods}");
              $set("chartRendererImageTimePeriod", "${form.chartRenderer.timePeriod}");
              $set("chartRendererImageRelativeDateType", "${form.chartRenderer.relativeDateType}");
              $set("chartRendererImageRollupType", "${form.chartRenderer.rollup}");
              $set("chartRendererImageRollupNumberOfPeriods", "${form.chartRenderer.rollupPeriods}");
              $set("chartRendererImageRollupTimePeriod", "${form.chartRenderer.rollupPeriodType}");
            </c:when>
            <c:when test='${form.chartRenderer.typeName == "chartRendererStats"}'>
              $set("chartRendererStatsNumberOfPeriods", "${form.chartRenderer.numberOfPeriods}");
              $set("chartRendererStatsTimePeriod", "${form.chartRenderer.timePeriod}");
              $set("chartRendererStatsRelativeDateType", "${form.chartRenderer.relativeDateType}");
              $set("chartRendererStatsIncludeSum", ${form.chartRenderer.includeSum});
              $set("chartRendererStatsRollupType", "${form.chartRenderer.rollup}");
              $set("chartRendererStatsRollupNumberOfPeriods", "${form.chartRenderer.rollupPeriods}");
              $set("chartRendererStatsRollupTimePeriod", "${form.chartRenderer.rollupPeriodType}");
            </c:when>
            <c:when test='${form.chartRenderer.typeName == "chartRendererImageFlipbook"}'>
              $set("chartRendererImageFlipbookLimit", "${form.chartRenderer.limit}");
            </c:when>
            <c:otherwise>
              dojo.debug("Unknown chart renderer: ${form.chartRenderer.typeName}");
            </c:otherwise>
          </c:choose>
          
          chartRendererEditor.change();
      };
  
      this.change = function() {
          if (currentChartRenderer)
              hide(currentChartRenderer);
          currentChartRenderer = $("chartRendererSelect").value
          show(currentChartRenderer);
      };
      
      this.save = function(callback) {
          var typeName = $get("chartRendererSelect");
          if (typeName == "chartRendererNone")
              DataPointEditDwr.setNoneChartRenderer(callback);
          else if (typeName == "chartRendererTable") {
              var limit = parseInt($get("chartRendererTableLimit"));
              if (isNaN(limit))
                  alert("<fmt:message key="pointEdit.chart.missingLimit"/>");
              else if (limit < 2 || limit > 50)
                  alert("<fmt:message key="pointEdit.chart.invalidLimit"/>");
              else
                  DataPointEditDwr.setTableChartRenderer(limit, callback);
          }
          else if (typeName == "chartRendererImage") {
              var numberOfPeriods = parseInt($get("chartRendererImageNumberOfPeriods"));
              var rollupNumberOfPeriods = parseInt($get("chartRendererImageRollupNumberOfPeriods"));
              if (isNaN(numberOfPeriods) || isNaN(rollupNumberOfPeriods))
                  alert("<fmt:message key="pointEdit.chart.missingPeriods"/>");
              else if (numberOfPeriods < 1 || rollupNumberOfPeriods < 1)
                  alert("<fmt:message key="pointEdit.chart.invalidPeriods"/>");
              else
                  DataPointEditDwr.setImageChartRenderer($get("chartRendererImageTimePeriod"),
                          numberOfPeriods, $get("chartRendererImageRollupType"),
                          $get("chartRendererImageRollupTimePeriod"), rollupNumberOfPeriods, $get("chartRendererImageRelativeDateType"), callback);
          }
          else if (typeName == "chartRendererStats") {
              var numberOfPeriods = parseInt($get("chartRendererStatsNumberOfPeriods"));
              var rollupNumberOfPeriods = parseInt($get("chartRendererStatsRollupNumberOfPeriods"));
              if (isNaN(numberOfPeriods) || isNaN(rollupNumberOfPeriods))
                  alert("<fmt:message key="pointEdit.chart.missingPeriods"/>");
              else if (numberOfPeriods < 1 || rollupNumberOfPeriods < 1)
                  alert("<fmt:message key="pointEdit.chart.invalidPeriods"/>");
              else
                  DataPointEditDwr.setStatisticsChartRenderer($get("chartRendererStatsTimePeriod"), 
                          numberOfPeriods, $get("chartRendererStatsIncludeSum"), $get("chartRendererStatsRollupType"),
                          $get("chartRendererStatsRollupTimePeriod"), rollupNumberOfPeriods, $get("chartRendererStatsRelativeDateType"), callback);
          }
          else if (typeName == "chartRendererImageFlipbook") {
              var limit = parseInt($get("chartRendererImageFlipbookLimit"));
              if (isNaN(limit))
                  alert("<fmt:message key="pointEdit.chart.missingLimit"/>");
              else if (limit < 2 || limit > 50)
                  alert("<fmt:message key="pointEdit.chart.invalidLimit"/>");
              else
                  DataPointEditDwr.setImageFlipbookRenderer(limit, callback);
          }
          else
              callback();
      };
  }
  var chartRendererEditor = new ChartRendererEditor();
  dojo.ready(chartRendererEditor, "init");
</script>