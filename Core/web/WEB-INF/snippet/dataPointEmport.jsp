<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Jared Wiltshire/Terry Packer
--%>

<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<tag:versionedJavascript src="/resources/dataPointEmport.js" />

<div id="dataPointEmport" style="display: none">
  <div id="dataPointEmportContent" class="dijitDialogPaneContentArea">
    <form id="msForm" method="post" action="/upload.shtm" enctype="multipart/form-data">
      <fieldset>
        <div id="msFileList" class="mangoTable"></div>
        <input type="file" id="msUploader" name="uploadedfile" multiple="multiple" />
        <input type="button" id="msReset" />
        <input type="submit" id="msSubmit" />
        
        <input type="hidden" id="dataType" name="dataType" value="dataPoint"/>
        <input type="hidden" id="dsId" name="dsId"/>
        
      </fieldset>
      
    </form>
    
    <div id="uploaderStatus" style="height: 100px;"></div>
  </div>
  
  <div class="dijitDialogPaneActionBar">
    <button type="button" id="dataPointEmportClose" ><fmt:message key="common.close" /></button>
  </div>
</div>