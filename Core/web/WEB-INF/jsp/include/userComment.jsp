<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%--
  Usage:
    For event comments, your table of events should be id'd "eventsTable". Each events table row should include a
    table with a tbody id'd eventComments<eventId>.
    
    For data point comments, ...
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.vo.UserComment"%>
<script type="text/javascript">
  dojo.require("dijit.Dialog");
  
  var commentTypeId;
  var commentReferenceId;
  var saveCommentCallback;
  
  /**
   * Open a comment dialog and allow a user defined callback
  **/
  function openCommentDialog(typeId, referenceId,callback) {
      commentTypeId = typeId;
      commentReferenceId = referenceId;
      
      if(typeof(callback) == 'function')
    	  saveCommentCallback = callback;
      else
    	  saveCommentCallback = saveCommentCB;
      
      $set("commentText", "");
      dijit.byId("CommentDialog").show();
      $("commentText").focus();
  }
  
  function saveComment() {
      var comment = $get("commentText");
      MiscDwr.addUserComment(commentTypeId, commentReferenceId, comment, saveCommentCallback);
  }
  
  function saveCommentCB(comment) {
      if (!comment)
          alert("<fmt:message key="notes.enterComment"/>");
      else {
          closeCommentDialog();
          
          // Add a row for the comment by cloning the template.
         
          var commentsNode;
          if (commentTypeId == <%= UserComment.TYPE_EVENT %>)
              commentsNode = $("eventComments"+ commentReferenceId);
          else if (commentTypeId == <%= UserComment.TYPE_POINT %>)
              commentsNode = $("pointComments"+ commentReferenceId);
          //Since we are now using this outside of the old format
          if(commentsNode != null){
        	  var content = $("comment_TEMPLATE_").cloneNode(true);
              updateTemplateNode(content, comment.ts);
	          commentsNode.appendChild(content);
	          $("comment"+ comment.ts +"UserTime").innerHTML = comment.prettyTime +" <fmt:message key="notes.by"/> "+ comment.username;
	          $("comment"+ comment.ts +"Text").innerHTML = comment.comment;
          }
      }
  }
  
  function closeCommentDialog() {
      dijit.byId("CommentDialog").hide();
  }
</script>
<style type="text/css">
  .dijitDialog {
      background : #eee;
      border : 1px solid #999;
      -moz-border-radius : 5px;
      padding : 4px;
  }
  .dijitDialogTitleBar { display: none; }
  #eventsTable .row td { vertical-align: top; }
  #eventsTable .rowAlt td { vertical-align: top; }
</style>

<div dojoType="dijit.Dialog" id="CommentDialog" bgColor="white" bgOpacity="0.5" toggle="fade" toggleDuration="250">
  <span class="smallTitle"><fmt:message key="notes.addNote"/></span>
  <table>
    <tr>
      <td><textarea rows="8" cols="50" id="commentText"></textarea></td>
    </tr>
    <tr>
      <td align="center">
        <input type="button" value="<fmt:message key="notes.save"/>" onclick="saveComment();"/>
        <input type="button" value="<fmt:message key="notes.cancel"/>" onclick="closeCommentDialog();"/>
      </td>
    </tr>
  </table>
</div>

<table style="display:none;">
  <tr id="comment_TEMPLATE_">
    <td valign="top" width="16"><tag:img png="comment" title="notes.note"/></td>
    <td valign="top">
      <span id="comment_TEMPLATE_UserTime" class="copyTitle"><fmt:message key="notes.timeByUsername"/></span><br/>
      <span id="comment_TEMPLATE_Text"></span>
    </td>
  </tr>
</table>