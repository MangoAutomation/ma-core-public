<%--
    Copyright (C) 2013 Deltamation Software. All rights reserved.
    @author Jared Wiltshire
--%>

<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<script>
  require(["dojo/parser", "dijit/Dialog", "dijit/form/Form", "dijit/form/Button"]);
</script>

<div data-dojo-type="dijit/Dialog" data-dojo-id="exportDialog" title="<fmt:message key="emport.export"/>" style="display: none">
  <form data-dojo-type="dijit/form/Form">
    <div class="dijitDialogPaneContentArea">
      <textarea rows="20" cols="100" id="exportData"></textarea>
    </div>
    <div class="dijitDialogPaneActionBar">
      <button data-dojo-type="dijit/form/Button" type="button" data-dojo-props="onClick:function() {exportDialog.hide();}"><fmt:message key="common.close"/></button>
    </div>
  </form>
</div>
