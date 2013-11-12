<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Jared Wiltshire/Terry Packer
--%>

<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<script type="text/javascript" src="/resources/view/pointValue/pointValueEmport.js"></script>

<div id="pointValueEmport" >
  <div id="pointValueEmportContent" class="dijitDialogPaneContentArea">
    <form id="msForm" method="post" action="/upload.shtm" enctype="multipart/form-data">
      <fieldset>
        <div id="msFileList" class="mangoTable"></div>
        <input type="file" id="msUploader" name="uploadedfile" multiple="multiple" />
        <input type="button" id="msReset" />
        <input type="submit" id="msSubmit" />
        
        <input type="hidden" id="dataType" name="dataType" value="pointValue"/>
        
      </fieldset>
      
    </form>
    
    <div id="uploaderStatus" style="height: 100px; width:350px;"></div>
  </div>
  
  <div class="dijitDialogPaneActionBar">
    <button type="button" id="pointValueEmportClose" ><fmt:message key="common.close" /></button>
  </div>
</div>