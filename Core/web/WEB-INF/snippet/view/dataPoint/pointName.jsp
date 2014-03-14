<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<script type="text/javascript">
  var dataTypeId = ${form.pointLocator.dataTypeId};
</script>
<script type="text/javascript">
  dojo.require("dojo.store.Memory");
  dojo.require("dijit.form.FilteringSelect");
  
  dojo.ready(function() {
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
                  window.location='data_point_edit.shtm?dpid='+ this.item.id;
          }
      }, "picker");        
  });
  
  function doSave(taskName) {
      $("taskName").name = taskName;
      textRendererEditor.save(doSaveChartRenderer);
      return false;
  }
  function doSaveChartRenderer() {
      chartRendererEditor.save(doSavePointEventDetectors);
  }
  function doSavePointEventDetectors() {
      pointEventDetectorEditor.save(doSaveForm);
  }
  function doSaveForm() {
      document.forms[0].submit();
  }
</script>

<table width="100%">
  <tr>
    <td valign="top">
      <table width="100%" cellspacing="0" cellpadding="0" border="0">
        <spring:bind path="form">
          <c:if test="${status.error}">
            <tr><td colspan="2" class="formError">${status.errorMessage}</td></tr>
          </c:if>
        </spring:bind>
      </table>
    </td>
    <td valign="top" align="right">
      <fmt:message key="pointEdit.name.goto"/>:&nbsp;
      <div style="display:inline;"><div id="picker"></div></div>
      
      <c:if test="${!empty prevId}">
        <tag:img png="bullet_go_left" title="pagination.previous"
                onclick="window.location='data_point_edit.shtm?dpid=${prevId}'"/>
      </c:if>
      
      <c:if test="${!empty nextId}">
        <tag:img png="bullet_go" title="pagination.next"
                onclick="window.location='data_point_edit.shtm?dpid=${nextId}'"/>
      </c:if>
    </td>
  </tr>
</table>
