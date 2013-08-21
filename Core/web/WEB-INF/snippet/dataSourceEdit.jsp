<%--
    Copyright (C) 2013 Deltamation Software. All rights reserved.
    @author Terry Packer
--%>
<%@page import="com.serotonin.m2m2.Constants"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<script type="text/javascript">


    //Init the Edit Pane by getting all data source types
	function initDataSourceEdit(){

		DataSourceDwr.initDataSourceTypes(function(response){
			if (response.data.types) {
			    dwr.util.addOptions("dataSourceTypes", response.data.types, "key", "value");
			}
		});
	}
    
    /**
     * Save Data Source with call to implementation from module
    */
    function saveDataSource() {
        startImageFader("dsSaveImg", true);
        hideContextualMessages($("dataSourceProperties"));

    	saveDataSourceImpl({
            name: $get("dataSourceName"),
            xid: $get("dataSourceXid"),
            purgeOverride: $get("dataSourcePurgeOverride"),
            purgePeriod: $get("dataSourcePurgePeriod"),
            purgeType: $get("dataSourcePurgeType")
        });
    }

    function saveDataSourceCB(response) {
        stopImageFader("dsSaveImg");
        if (response.hasMessages)
            showDwrMessages(response.messages, "dataSourceGenericMessages");
        else {
            showMessage("dataSourceMessage", "<fmt:message key="dsEdit.saved"/>");
            DataSourceEditDwr.getPoints(writePointList);
        }
        getAlarms();
        //TODO Refresh Table here mabye?
    }
    
    
</script>


<!-- Edit Div -->
<div id="editDataSourceDiv"></div>
 