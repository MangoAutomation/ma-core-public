    dojo.require("dijit.Calendar");
    
    // Tell the log poll that we're interested in monitoring pending alarms.
    mango.longPoll.pollRequest.pendingAlarms = true;
  

    
    function updatePendingAlarmsContent(content) {
        hide("hourglass");
        
        $set("pendingAlarms", content);
        if (content) {
            show("ackAllDiv");
            hide("noAlarms");
        }
        else {
            $set("pendingAlarms", "");
            hide("ackAllDiv");
            show("noAlarms");
        }
    }
    
    function doSearch(page, date) {
        setDisabled("searchBtn", true);
        $set("searchMessage", '<fmt:message key="events.search.searching"/>');
        EventsDwr.search($get("eventId"), $get("eventType"), $get("eventStatus"), $get("alarmLevel") || null,
                $get("keywords"), $get("dateRangeType"), $get("relativeType"), $get("prevPeriodCount"), 
                $get("prevPeriodType"), $get("pastPeriodCount"), $get("pastPeriodType"), $get("fromNone"), 
                $get("fromYear"), $get("fromMonth"), $get("fromDay"), $get("fromHour"), $get("fromMinute"), 
                $get("fromSecond"), $get("toNone"), $get("toYear"), $get("toMonth"), $get("toDay"), $get("toHour"), 
                $get("toMinute"), $get("toSecond"), page, date, function(results) {
            $set("searchResults", results.data.content);
            setDisabled("searchBtn", false);
            $set("searchMessage", results.data.resultCount);
        });
    }

    function jumpToDate(parent) {
        var div = $("datePickerDiv");
        show(div);
        var bounds = getAbsoluteNodeBounds(parent);
        div.style.top = bounds.y +"px";
        div.style.left = bounds.x +"px";
    }

    var dptimeout = null;
    function expireDatePicker() {
        dptimeout = setTimeout(function() { hide("datePickerDiv"); }, 500);
    }

    function cancelDatePickerExpiry() {
        if (dptimeout) {
            clearTimeout(dptimeout);
            dptimeout = null;
        }
    }

    function jumpToDateClicked(date) {
        var div = $("datePickerDiv");
        if (isShowing(div)) {
            hide(div);
            doSearch(0, date);
        }
    }

    function newSearch() {
        var x = dijit.byId("datePicker");
        x.goToToday();
        doSearch(0);
    }
    
    function silenceAll() {
        MiscDwr.silenceAll(function(result) {
            var silenced = result.data.silenced;
            for (var i=0; i<silenced.length; i++)
                setSilenced(silenced[i], true);
        });
    }
    

    
    function exportEvents() {
        startImageFader($("exportEventsImg"));
        EventsDwr.exportEvents($get("eventId"), $get("eventSourceType"), $get("eventStatus"), $get("alarmLevel"),
                $get("keywords"), $get("dateRangeType"), $get("relativeType"), $get("prevPeriodCount"), 
                $get("prevPeriodType"), $get("pastPeriodCount"), $get("pastPeriodType"), $get("fromNone"), 
                $get("fromYear"), $get("fromMonth"), $get("fromDay"), $get("fromHour"), $get("fromMinute"), 
                $get("fromSecond"), $get("toNone"), $get("toYear"), $get("toMonth"), $get("toDay"), $get("toHour"), 
                $get("toMinute"), $get("toSecond"), function(data) {
            stopImageFader($("exportEventsImg"));
            window.location = "eventExport/eventData.csv";
        });
    }
