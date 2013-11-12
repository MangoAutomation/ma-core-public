<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="PublisherListDwr" onload="init">
  <script type="text/javascript">
    function init() {
        PublisherListDwr.init(function(response) {
            dwr.util.addOptions("publisherTypes", response.data.types, "key", "value");
            updatePublisherList(response.data.publishers);
        });
    }
    
    function addPublisher() {
        window.location = "publisher_edit.shtm?typeId="+ $get("publisherTypes");
    }
    
    function updatePublisherList(publishers) {
        dwr.util.removeAllRows("publisherList");
        dwr.util.addRows("publisherList", publishers,
            [
                function(p) { return "<b>"+ p.name +"</b>"; },
                function(p) { return p.typeDescription; },
                function(p) { return p.configDescription; },
                function(p) {
                    if (p.enabled)
                        return '<img src="images/transmit_go.png" title="<fmt:message key="common.enabledToggle"/>" '+
                            'class="ptr" onclick="togglePublisher('+ p.id +')" id="pImg'+ p.id +'"/>';
                    return '<img src="images/transmit_stop.png" title="<fmt:message key="common.disabledToggle"/>" '+
                        'class="ptr" onclick="togglePublisher('+ p.id +')" id="pImg'+ p.id +'"/>';
                },
                function(p) {
                    return '<a href="publisher_edit.shtm?pid='+ p.id +'"><img src="images/transmit_edit.png" '+
                        'title="<fmt:message key="common.edit"/>"/></a> '+
                        '<img src="images/transmit_delete.png" title="<fmt:message key="common.delete"/>" id="deleteImg'+ p.id +'" '+
                        'class="ptr" onclick="deletePublisher('+ p.id +')"/>';
                }
            ],
            {
                rowCreator: function(options) {
                    var tr = document.createElement("tr");
                    tr.id = "publisherRow"+ options.rowData.id;
                    tr.className = "row"+ (options.rowIndex % 2 == 0 ? "" : "Alt");
                    return tr;
                },
                cellCreator: function(options) {
                    var td = document.createElement("td");
                    if (options.cellNum == 3)
                        td.align = "center";
                    return td;
                }
            });
        display("noPublishers", publishers.length == 0);
    }
    
    function togglePublisher(id) {
        var imgNode = $("pImg"+ id);
        if (!hasImageFader(imgNode)) {
            startImageFader(imgNode);
            PublisherListDwr.togglePublisher(id, function(result) {
                updateStatusImg($("pImg"+ result.data.id), result.data.enabled);
            });
        }
    }
    
    
    function deletePublisher(id) {
        if (confirm("<fmt:message key="publisherList.deleteConfirm"/>")) {
            startImageFader("deleteImg"+ id);
            PublisherListDwr.deletePublisher(id, function(publisherId) {
                stopImageFader("deleteImg"+ publisherId);
                var row = $("publisherRow"+ publisherId);
                row.parentNode.removeChild(row);
            });
        }
    }
    
    function updateStatusImg(imgNode, enabled) {
        stopImageFader(imgNode);
        setPublisherStatusImg(enabled, imgNode);
    }
  </script>
  
  <table cellspacing="0" cellpadding="0">
    <tr>
      <td>
        <tag:img png="transmit" title="publisherList.publishers"/>
        <span class="smallTitle"><fmt:message key="publisherList.publishers"/></span>
        <tag:help id="publisherList"/>
      </td>
      <td align="right">
        <select id="publisherTypes"></select>
        <tag:img png="transmit_add" title="common.add" onclick="addPublisher()"/>
      </td>
    </tr>
    
    <tr>
      <td colspan="2">
        <table cellspacing="1" cellpadding="0" border="0">
          <tr class="rowHeader">
            <td><fmt:message key="publisherList.name"/></td>
            <td><fmt:message key="publisherList.type"/></td>
            <td><fmt:message key="publisherList.config"/></td>
            <td><fmt:message key="publisherList.status"/></td>
            <td></td>
          </tr>
          <tbody id="noPublishers" style="display:none"><tr><td colspan="5"><fmt:message key="publisherList.noRows"/></td></tr></tbody>
          <tbody id="publisherList"></tbody>
        </table>
      </td>
    </tr>
  </table>
</tag:page>