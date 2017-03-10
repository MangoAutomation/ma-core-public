<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.vo.comment.UserCommentVO"%>




<tag:mobile-page dwr="DataPointDetailsDwr" js="/resources/stores.js">
    <script type="text/javascript">
    require([
             "dojox/mobile/parser",
             "dojox/mobile",
             "dojox/mobile/compat", // For non-webkit browsers (FF, IE)
             "dojox/mobile/FilteredListMixin",
             "dojox/mobile/EdgeToEdgeDataList",
             "dojox/mobile/ScrollableView"
     ]);
    </script>
<div data-dojo-type="dojox/mobile/View">
        <h1 data-dojo-type="dojox/mobile/Heading" data-dojo-props="fixed: 'top'">Data Points</h1>
        <div data-dojo-type="dojox/mobile/ScrollableView">
	        <ul id="list" data-dojo-type="dojox/mobile/EdgeToEdgeDataList"
	                data-dojo-mixins="dojox/mobile/FilteredListMixin"
	                data-dojo-props="placeHolder: 'Search', store: stores.dataPointDetails">
	        </ul>
        </div>
</div>




</tag:mobile-page>

