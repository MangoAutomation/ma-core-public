<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Jared Wiltshire/Terry Packer
--%>

<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<script type="text/javascript" src="/resources/view/pointValue/pointValueEmport.js"></script>

<script type="text/javascript">
	//Add in our local keys
	mangoMsg['emport.filename'] = '<fmt:message key="emport.filename"/>';
	mangoMsg['emport.added'] = '<fmt:message key="emport.added"/>';
	mangoMsg['emport.removed'] = '<fmt:message key="emport.removed"/>';
	mangoMsg['emport.errors'] = '<fmt:message key="emport.errors"/>';

</script>

<div id="pointValueEmport">
  <div id="pointValueEmportContent" class="dijitDialogPaneContentArea">
    <form id="msForm" method="post" action="/upload.shtm" enctype="multipart/form-data">
      <fieldset>
        <div id="msFileList" class="mangoTable"></div>
        <hr>
        <input type="file" id="msUploader" name="uploadedfile" multiple="multiple" />
        <input type="button" id="msReset" />
        <input type="submit" id="msSubmit" />
        
        <input type="hidden" id="dataType" name="dataType" value="pointValue"/>
        <%-- for Spring Security --%>
        <input type="hidden" id="${_csrf.parameterName}" name="${_csrf.parameterName}" value="${_csrf.token}"/> 
      </fieldset>
    </form>
    <div id="importErrorBox" class="borderDiv" style="display:none">
      <tag:img id="closeImportErrorBoxImg" png="cross" onclick="closeImportErrorBox()" title="common.view.clearErrors"/>
      <div id="importErrors"></div>
    </div>    
    <div id="uploaderStatus" style="height: 100px; width:350px;"></div>
    
  </div>
  
  <div class="dijitDialogPaneActionBar">
    <button type="button" id="pointValueEmportClose" ><fmt:message key="common.close" /></button>
  </div>
</div>