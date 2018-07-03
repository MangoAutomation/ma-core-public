<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.vo.comment.UserCommentVO"%>

<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="DataPointDetailsDwr" js="/resources/view.js">
  <c:if test="${!empty point}">
    <script type="text/javascript">
    require(["dojo","dojo/store/Memory","dijit/form/FilteringSelect","dijit/form/Select"], 
    		function(dojo,Memory,FilteringSelect,Select){
	      
	      mango.view.initPointDetails();
	
	      dojo.ready(function() {
	          getHistoryTableData();
	          <c:if test="${!empty periodType}">
	            DataPointDetailsDwr.getDateRangeDefaults(${periodType}, ${periodCount}, function(data) {
	                setDateRange(data);
	                getImageChart();
	            });
	          </c:if>
	          <c:if test="${!empty flipbookLimit}">getFlipbookChart();</c:if>
	          getStatsChart();
	          
	          // Point lookup
	          new dijit.form.FilteringSelect({
	              store: new dojo.store.Memory({ data: pointList }),
	              autoComplete: false,
	              style: "width: 250px;",
	              queryExpr: "*\${0}*",
	              highlightMatch: "all",
	              required: false,
	              onChange: function(point) {
	                  if (this.item)
	                      window.location='data_point_details.shtm?dpid='+ this.item.id;
	              }
	          }, "picker");        
	      
	          
	          //Setup the File Download Selector
	          var uploadTypeChoice = new Select({
	              name: "downloadTypeSelect",
	              options: [
	                  { label: "Excel", value: ".xlsx", selected: true},
	                  { label: "Comma Separated Value (CSV)", value: ".csv", },
	              ]
	          },"downloadTypeSelect");
	       
	      
	      
	      });
	      
	      
	      
	      
	      
    });
      //
      // History
      //
      function getHistoryTableData() {
          var limit = parseInt($get("historyLimit"));
          if (isNaN(limit))
              alert("<fmt:message key="pointDetails.recordCountError"/>");
          else {
              startImageFader($("historyLimitImg"));
              DataPointDetailsDwr.getHistoryTableData(limit, $get("usePointCache"), function(response) {
                  var data = response.data.history;
                  dwr.util.removeAllRows("historyTableData");
                  if (!data || data.length == 0)
                      dwr.util.addRows("historyTableData", ["<fmt:message key="common.noData"/>"], [function(data) { return data; }]);
                  else {
                      dwr.util.addRows("historyTableData", data,
                          [
                              function(data) { return data.value; },
                              function(data) { return data.time; },
                              function(data) { return data.annotation; }
                          ],
                          {
                              rowCreator:function(options) {
                                  var tr = document.createElement("tr");
                                  tr.className = "row"+ (options.rowIndex % 2 == 0 ? "" : "Alt");
                                  return tr;
                              }
                          });
                  }
                  $("historyTableAsof").innerHTML = response.data.asof;
                  stopImageFader($("historyLimitImg"));
              });
          }
      }
      
      //
      // Image chart
      //
      function getImageChart() {
          var width = dojo.contentBox($("imageChartDiv")).w - 20;
          DataPointDetailsDwr.getImageChartData($get("fromYear"), $get("fromMonth"), $get("fromDay"), $get("fromHour"),
                $get("fromMinute"), $get("fromSecond"), $get("fromNone"), $get("toYear"), $get("toMonth"), 
                $get("toDay"), $get("toHour"), $get("toMinute"), $get("toSecond"), $get("toNone"), width, 350, false, function(response) {
              $("imageChartDiv").innerHTML = response.data.chart;
              $("imageChartAsof").innerHTML = response.data.asof;
              stopImageFader($("imageChartImg"));
          });
          startImageFader($("imageChartImg"));
      }
      
      function getChartData() {
          startImageFader($("chartDataImg"));
          DataPointDetailsDwr.getChartData($get("fromYear"), $get("fromMonth"), $get("fromDay"), $get("fromHour"),
                $get("fromMinute"), $get("fromSecond"), $get("fromNone"), $get("toYear"), $get("toMonth"),
                $get("toDay"), $get("toHour"), $get("toMinute"), $get("toSecond"), $get("toNone"), function(data) {
              stopImageFader($("chartDataImg"));
              var downloadTypeSelect = dijit.byId("downloadTypeSelect");
              var downloadUrl = "chartExport/pointData" + downloadTypeSelect.get('value');
              window.location = downloadUrl;
          });          
      }
      
      //
      // Stats chart
      //
      function getStatsChart() {
          var period = parseInt($get("statsChartDuration"));
          var periodType = parseInt($get("statsChartDurationType"));
          
          if (isNaN(period))
              alert("<fmt:message key="pointDetails.timePeriodError"/>");
          else {
              startImageFader($("statsChartImg"));
              DataPointDetailsDwr.getStatsChartData(periodType, period, true, function(response) {
                  $("statsChartData").innerHTML = response.data.stats;
                  $("statsAsof").innerHTML = response.data.asof;
                  stopImageFader($("statsChartImg"));
              });
          }
      }
      
      function togglePanel(img, panelId) {
          if (!img.minimized) {
              img.src = "images/arrow_out.png";
              img.title = "<fmt:message key="common.maximize"/>";
              hide(panelId);
              img.minimized = true;
          }
          else {
              img.src = "images/arrow_in.png";
              img.title = "<fmt:message key="common.minimize"/>";
              show(panelId);
              img.minimized = false;
          }
      }
      
      // Flipbook
      function getFlipbookChart() {
          var limit = parseInt($get("flipbookLimit"));
          if (isNaN(limit))
              alert("<fmt:message key="pointDetails.imageCountError"/>");
          else {
              startImageFader($("flipbookChartImg"));
              DataPointDetailsDwr.getFlipbookData(limit, function(response) {
                  var data = response.data.images;
                  var thumbContent = "";
                  for (var i=0; i<data.length; i++)
                      thumbContent += '<img src="'+ data[i].uri +'?w=40&h=40" onmouseover="swapFlipbookImage(\''+ data[i].uri +'\')"/>';
                  $set("flipbookThumbsDiv", thumbContent);
                  if (data.length > 0)
                      swapFlipbookImage(data[0].uri);
                  
                  $("flipbookAsof").innerHTML = response.data.asof;
                  stopImageFader($("flipbookChartImg"));
              });
          }
      }
      
      function swapFlipbookImage(uri) {
          $("flipbookImage").src = uri;
      }
      
      // Force read
      function forceRead() {
          <c:if test="${!empty point}">
            startImageFader($("forceReadImg"));
            DataPointDetailsDwr.forcePointRead(${point.id}, function() {
                stopImageFader($("forceReadImg"));
            });
          </c:if>
      }
    </script>
  </c:if>
      
  <table class="wide">
    <tr>
      <td valign="top" align="right">
        <fmt:message key="pointDetails.goto"/>:
        <div style="display:inline; padding-left:5px;"><div id="picker"></div></div>
        
        <c:if test="${!empty prevId}">
          <tag:img png="bullet_go_left" title="pagination.previous"
                  onclick="window.location='data_point_details.shtm?dpid=${prevId}'"/>
        </c:if>
        
        <c:if test="${!empty nextId}">
          <tag:img png="bullet_go" title="pagination.next"
                  onclick="window.location='data_point_details.shtm?dpid=${nextId}'"/>
        </c:if>
        |
        <fmt:message key="pointDetails.findXid"/>:&nbsp;
        <input type="text" value="${currentXid}" onkeypress="if (event.keyCode==13) window.location='data_point_details.shtm?dpxid='+ this.value"/>
      </td>
    </tr>
  </table>
  
  <c:choose>
    <c:when test="${empty point && !empty currentXid}"><m2m2:translate key="pointDetails.pointNotFound"/></c:when>
    <c:when test="${empty point}"><m2m2:translate key="pointDetails.noPoints"/></c:when>
    <c:otherwise>
      <table class="wide">
        <tr>
          <td valign="top">
            <div class="borderDiv marB marR">
              <table>
                <tr>
                  <td class="smallTitle" colspan="2">
                    <tag:img png="icon_comp" title="common.point"/>
                    ${fn:escapeXml(point.extendedName)}
                    <c:if test="${pointEditor}">
                      <a href="data_point_edit.shtm?dpid=${point.id}"><tag:img png="icon_comp_edit" title="pointDetails.editPoint"/></a>
                      <a href="data_source_edit.shtm?dsid=${point.dataSourceId}&pid=${point.id}"><tag:img png="icon_ds_edit"
                              title="pointDetails.editDataSource"/></a>
                    </c:if>
                  </td>
                </tr>
                <tr>
                  <td class="formLabelRequired">
                    <tag:img id="forceReadImg" png="arrow_refresh" title="common.refresh" onclick="forceRead()"/>
                    <fmt:message key="common.value"/>
                  </td>
                  <td id="pointValue" class="formField"></td>
                </tr>
                <tr>
                  <td class="formLabelRequired"><fmt:message key="common.time"/></td>
                  <td id="pointValueTime" class="formField"></td>
                </tr>
                <tr id="pointChangeNode" style="display:none">
                  <td class="formLabelRequired">
                    <tag:img id="pointChanging" png="icon_edit" title="common.set"/>
                    <fmt:message key="common.set"/>
                  </td>
                  <td id="pointChange" class="formField"></td>
                </tr>
                <tr>
                  <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
                  <td class="formField">${point.xid}</td>
                </tr>
                <c:if test="${!empty hierPath}">
                  <tr>
                    <td class="formLabelRequired"><fmt:message key="common.path"/></td>
                    <td class="formField">${hierPath}</td>
                  </tr>
                </c:if>
                <tr>
                  <td colspan="2">
                    <div style='max-height:100px; overflow-y: scroll'>
                      <table><tr><td id="pointMessages"></td></tr>
                      </table>
                    </div>
                  </td>
                </tr>
              </table>
            </div>
            
            <div class="borderDiv marB marR">
              <table class="wide">
                <tr>
                  <td class="smallTitle"><fmt:message key="pointDetails.statistics"/></td>
                  <td id="statsAsof"></td>
                  <td align="right">
                    <fmt:message key="pointDetails.timePeriod"/>:
                    <input id="statsChartDuration" style="text-align:right;" type="text" class="formVeryShort"
                            value='${empty periodCount ? "1" : periodCount}'/>
                    <tag:timePeriods id="statsChartDurationType" value="${periodType}" min="true" h="true" d="true" w="true" mon="true"/>
                    <tag:img id="statsChartImg" png="control_play_blue" title="pointDetails.getStatistics" onclick="getStatsChart()"/>
                  </td>
                </tr>
              </table>
              <div id="statsChartData" style="padding:0px 0px 5px 5px;"></div>
            </div>
          </td>
          
          <td valign="top">
            <div class="borderDiv marB marR">
              <table class="wide">
                <tr>
                  <td class="smallTitle"><fmt:message key="pointDetails.history"/></td>
                  <td id="historyTableAsof"></td>
                  <td><sst:checkbox id="usePointCache" selectedValue="true"/>&nbsp;<fmt:message key="pointDetails.useCache"/></td>
                  <td align="right">
                    <fmt:message key="pointDetails.show"/>
                    <input id="historyLimit" type="text" style="text-align:right;" value="${historyLimit}"
                            class="formVeryShort"/>
                    <fmt:message key="pointDetails.mostRecentRecords"/>
                    <tag:img id="historyLimitImg" png="control_play_blue" title="pointDetails.getData" onclick="getHistoryTableData()"/>
                  </td>
                </tr>
              </table>
              <table cellspacing="1">
                <tr class="rowHeader">
                  <td><fmt:message key="common.value"/></td>
                  <td><fmt:message key="common.time"/></td>
                  <td><fmt:message key="common.annotation"/></td>
                </tr>
                <tbody id="historyTableData"></tbody>
              </table>
            </div>
          </td>
          
          <td valign="top">
            <div class="borderDiv marB">
              <table class="wide">
                <tr>
                  <td class="smallTitle"><fmt:message key="notes.userNotes"/></td>
                  <td align="right">
                    <tag:img png="comment_add" title="notes.addNote"
                            onclick="openCommentDialog(${applicationScope['constants.UserComment.TYPE_POINT']}, ${point.id})"/>
                  </td>
                </tr>
              </table>
              <table id="pointComments${point.id}"><tag:comments comments="${point.comments}"/></table>
            </div>
          </td>
        </tr>
        
        <c:if test="${!empty periodType}">
          <tr>
            <td colspan="3">
              <!-- chart with editable properties and annotations -->
              <div class="borderDiv marB">
                <table class="wide">
                  <tr>
                    <td class="smallTitle"><fmt:message key="pointDetails.chart"/> &nbsp; <tag:help id="chartServlet"/></td>
                    <td id="imageChartAsof"></td>
                    <td align="right"><tag:dateRange/></td>
                    <td>
                      <tag:img id="imageChartImg" png="control_play_blue" title="pointDetails.imageChartButton"
                              onclick="getImageChart()"/>
                      <!-- TODO Add selectable type here xslx or csv, Maybe Create Tag... -->
                      <input id="downloadTypeSelect"></input>
                      <tag:img id="chartDataImg" png="bullet_down" title="pointDetails.chartDataButton"
                              onclick="getChartData()"/>
                       
                              
                    </td>
                  </tr>
                  <tr><td colspan="4" id="imageChartDiv"></td></tr>
                </table>
              </div>
            </td>
          </tr>
        </c:if>
        
        <c:if test="${!empty flipbookLimit}">
          <tr>
            <td colspan="3">
              <div class="borderDiv marB">
                <table class="wide">
                  <tr>
                    <td class="smallTitle"><fmt:message key="pointDetails.flipbook"/></td>
                    <td id="flipbookAsof"></td>
                    <td align="right">
                      <fmt:message key="pointDetails.images"/>:
                      <input id="flipbookLimit" style="text-align:right;" type="text" value="${flipbookLimit}"
                              class="formVeryShort"/>
                      <tag:img id="flipbookChartImg" png="control_play_blue" title="pointDetails.getImages" 
                              onclick="getFlipbookChart()"/>
                    </td>
                  </tr>
                  <tr><td colspan="2" id="flipbookThumbsDiv"></td></tr>
                  <tr><td colspan="2"><img id="flipbookImage" src=""/></td></tr>
                </table>
              </div>
            </td>
          </tr>
        </c:if>
      
        <tr>
          <td colspan="3">
            <div class="borderDiv marB">
              <table class="wide">
                <tr>
                  <td class="smallTitle"><fmt:message key="pointDetails.events"/></td>
                  <td align="right">
                    <tag:img png="arrow_in" title="common.maximize" onclick="togglePanel(this, 'eventsTable');"/>
                    <a href=""><tag:img png="control_repeat_blue" title="common.refresh"/></a>
                  </td>
                </tr>
              </table>
              <table id="eventsTable" cellspacing="1" cellpadding="0" class="wide">
                <tr class="rowHeader">
                  <td><fmt:message key="pointDetails.id"/></td>
                  <td><fmt:message key="common.alarmLevel"/></td>
                  <td><fmt:message key="common.activeTime"/></td>
                  <td><fmt:message key="pointDetails.message"/></td>
                  <td><fmt:message key="common.status"/></td>
                  <td><fmt:message key="events.acknowledged"/></td>
                </tr>
                
                <c:forEach items="${events}" var="event" varStatus="status" end="19">
                  <tr class="row<c:if test="${status.index % 2 == 1}">Alt</c:if>">
                    <td align="center">${event.id}</td>
                    <td align="center"><tag:eventIcon event="${event}"/></td>
                    <td>${m2m2:time(event.activeTimestamp)}</td>
                    <td>
                      <table cellspacing="0" cellpadding="0" class="wide">
                        <tr>
                          <td><b><m2m2:translate message="${event.message}"/></b></td>
                          <td align="right"><tag:img png="comment_add" title="notes.addNote"
                                  onclick="openCommentDialog(${applicationScope['constants.UserComment.TYPE_EVENT']}, ${event.id})"/></td>
                        </tr>
                      </table>
                      <table cellspacing="0" cellpadding="0" id="eventComments${event.id}">
                        <c:forEach items="${event.eventComments}" var="comment">
                          <tr>
                            <td valign="top" width="16"><tag:img png="comment" title="notes.note"/></td>
                            <td valign="top">
                              <span class="copyTitle">
                                ${comment.prettyTime} <fmt:message key="notes.by"/>
                                <c:choose>
                                  <c:when test="${empty comment.username}"><fmt:message key="common.deleted"/></c:when>
                                  <c:otherwise>${fn:escapeXml(comment.username)}</c:otherwise>
                                </c:choose>
                              </span><br/>
                              ${fn:escapeXml(comment.comment)}
                            </td>
                          </tr>
                        </c:forEach>
                      </table>
                    </td>
                    <td>
                      <c:choose>
                        <c:when test="${event.active}">
                          <fmt:message key="common.active"/>
                          <a href="events.shtm"><tag:img png="flag_white" title="common.active"/></a>
                        </c:when>
                        <c:when test="${!event.rtnApplicable}"><fmt:message key="common.nortn"/></c:when>
                        <c:otherwise>
                          ${m2m2:time(event.rtnTimestamp)} - <m2m2:translate message="${event.rtnMessage}"/>
                        </c:otherwise>
                      </c:choose>
                    </td>
                    <td>
                      <c:if test="${event.acknowledged}">
                        ${m2m2:time(event.acknowledgedTimestamp)}
                        <m2m2:translate message="${event.ackMessage}" />
                      </c:if>
                    </td>
                  </tr>
                </c:forEach>
                <c:if test="${sst:size(events) > 20}">
                  <tr class="row"><td align="center" colspan="6"><fmt:message key="pointDetails.maxEvents"/> ${sst:size(events)}</td></tr>
                </c:if>
                <c:if test="${empty events}">
                  <tr class="row"><td colspan="6"><fmt:message key="events.emptyList"/></td></tr>
                </c:if>
              </table>
            </div>
          </td>
        </tr>
        
        <tr>
          <td colspan="3" valign="top">
            <div class="borderDiv">
              <span class="smallTitle" style="margin:3px;"><fmt:message key="pointDetails.userAccess"/></span>
              <table class="wide" cellspacing="1">
                <tr class="rowHeader">
                  <td width="16"></td>
                  <td><fmt:message key="pointDetails.username"/></td>
                  <td><fmt:message key="pointDetails.accessType"/></td>
                </tr>
                <c:forEach items="${users}" var="userData" varStatus="status">
                  <tr class="row<c:if test="${status.index % 2 == 1}">Alt</c:if>">
                    <c:set var="user" value="${userData.user}"/>
                    <td><%@ include file="/WEB-INF/snippet/userIcon.jsp" %></td>
                    <td>${fn:escapeXml(user.username)}</td>
                    <td>
                      <c:choose>
                        <c:when test="${userData.accessType == applicationScope['constants.Permissions.DataPointAccessTypes.READ']}"><fmt:message key="common.access.read"/></c:when>
                        <c:when test="${userData.accessType == applicationScope['constants.Permissions.DataPointAccessTypes.SET']}"><fmt:message key="common.access.set"/></c:when>
                        <c:when test="${userData.accessType == applicationScope['constants.Permissions.DataPointAccessTypes.DATA_SOURCE']}"><fmt:message key="common.access.dataSource"/></c:when>
                        <c:when test="${userData.accessType == applicationScope['constants.Permissions.DataPointAccessTypes.ADMIN']}"><fmt:message key="common.access.admin"/></c:when>
                        <c:otherwise><fmt:message key="common.unknown"/> (${userData.accessType})</c:otherwise>
                      </c:choose>
                    </td>
                  </tr>
                </c:forEach>
              </table>
            </div>
          </td>
        </tr>
      </table>
      
      <%@ include file="/WEB-INF/jsp/include/userComment.jsp" %>
    </c:otherwise>
  </c:choose>
</tag:page>

<script type="text/javascript">
  var pointList = [
     <c:forEach items="${userPoints}" var="point" varStatus="status">
      {id:${point.id},name:"${sst:escapeLessThan(sst:dquotEncode(point.extendedName))}".replace(/&lt;/g, "<")}${status.last ? '' : ','}</c:forEach>
  ];
</script>