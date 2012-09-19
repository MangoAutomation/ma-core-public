/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
*/

mango.view = {};
mango.view.graphic = {};

mango.view.setEditing = false;
mango.view.setEditingContent = null;

mango.view.setData = function(stateArr) {
    var state;
    for (var i=0; i<stateArr.length; i++) {
        state = stateArr[i];
        
        // Check that the point exists. Ignore if it doesn't.
        if (!$("c"+ state.id))
            throw "Can't find point view c"+ state.id;
        
        mango.view.setContent(state);
        
        if ($("c"+ state.id +"Controls")) {
            if (state.info != null)
                $set("c"+ state.id +"Info", state.info);
            if (state.change != null) {
                if (state.change) 
                    show("c"+ state.id +"ChangeMin");
                
                if (mango.view.setEditing)
                    // If the set value is being edited, save the content
                    mango.view.setEditingContent = state.change;
                else
                    $set("c"+ state.id +"Change", state.change);
            }
            if (state.chart != null)
                $set("c"+ state.id +"Chart", state.chart);
        }
        
        mango.view.setMessages(state);
    }
};

mango.view.setMessages = function(state) {
    var warningNode = $("c"+ state.id +"Warning");
    if (warningNode && state.messages != null) {
        $set("c"+ state.id +"Messages", state.messages);
        if (state.messages)
            show(warningNode);
        else
            hide(warningNode);
    }
};

mango.view.setContent = function(state) {
    if (state.content != null) {
    	var comp = $("c"+ state.id +"Content");
    	comp.innerHTML = state.content;
        var dyn = $("dyn"+ state.id);
        if (dyn) {
            eval("var data = "+ dyn.value);
            if (data.graphic != '')
                eval(data.graphic +".setValue("+ state.id +", "+ data.value +");");
        }
        
        // Look for scripts in the content.
        mango.view.runScripts(comp);
    }
};

mango.view.runScripts = function(node) {
    var arr = [];
    mango.view.findScripts(node, arr);
    for (var i=0; i<arr.length; i++)
        eval(arr[i]);
}

mango.view.findScripts = function(node, arr) {
    for (var i=0; i<node.childNodes.length; i++) {
        var child = node.childNodes[i];
        if (child.tagName == "SCRIPT")
            arr.push(child.innerHTML);
        mango.view.findScripts(child, arr);
    }
}


mango.view.showChange = function(divId, xoffset, yoffset) {
    mango.view.setEditing = true;
    var theDiv = $(divId);
    showMenu(theDiv, xoffset, yoffset);
    
    // Automatically select the text in text boxes
    var inputElems = theDiv.getElementsByTagName("input");
    for (var i=0; i<inputElems.length; i++) {
        if (inputElems[i].id.startsWith("txtChange")) {
            var temp = inputElems[i].value;
            inputElems[i].value += " ";
            inputElems[i].value = temp;
            inputElems[i].select();
        }
    }
};

mango.view.hideChange = function(divId) {
    if ($(divId))
        hideLayer($(divId));
    mango.view.setEditing = false;
    if (mango.view.setEditingContent != null) {
        $set(divId, mango.view.setEditingContent);
        mango.view.setEditingContent = null;
    }
};

mango.view.showChart = function(componentId, event, source) {
	if (isMouseLeaveOrEnter(event, source)) {
		// Take the data in the chart textarea and put it into the chart layer div
		$set('c'+ componentId +'ChartLayer', $get('c'+ componentId +'Chart'));
        showMenu('c'+ componentId +'ChartLayer', 16, 0);
	}
}

mango.view.hideChart = function(componentId, event, source) {
	if (isMouseLeaveOrEnter(event, source))
		hideLayer('c'+ componentId +'ChartLayer');
}

function vcOver(base, amt) {
    if (!amt)
        amt = 10;
    setZIndex(base, amt);
    showLayer(base + 'Controls');
};

function vcOut(base) {
    setZIndex(base, 0);
    hideLayer(base +'Controls');
};

//
// Point details
mango.view.initPointDetails = function() {
    mango.view.setPoint = mango.view.pointDetails.setPoint;
    // Tell the long poll request that we're interested in point details data.
    mango.longPoll.pollRequest.pointDetails = true;
};

mango.view.pointDetails = {};
mango.view.pointDetails.setPoint = function(pointId, componentId, value) {
    startImageFader("pointChanging");
    DataPointDetailsDwr.setPoint(pointId, componentId, value, function(componentId) {
        stopImageFader("pointChanging");
        MiscDwr.notifyLongPoll(mango.longPoll.pollSessionId);
    });
};

mango.view.pointDetails.setData = function(state) {
    if (state.value != null)
        $("pointValue").innerHTML = state.value;
    
    if (state.time != null)
        $("pointValueTime").innerHTML = state.time;
    
    if (state.change != null) {
        show($("pointChangeNode"));
        $set("pointChange", state.change);
    }
    
    if (state.messages != null)
        $("pointMessages").innerHTML = state.messages;
};

//
// Graphics
mango.view.graphic.transform = function(xa, ya, fx, fy, mx, my, a) {
    for (var i=0; i<xa.length; i++) {
        // Scale
        xa[i] *= fx;
        ya[i] *= fy;
        
        // Rotate
        var point = mango.view.graphic.rotatePoint(xa[i], ya[i], a);
        xa[i] = point.x;
        ya[i] = point.y;
        
        // Translate
        xa[i] += mx;
        ya[i] += my;
    }
};

mango.view.graphic.rotatePoint = function(x, y, a) {
    var point = {};
    var cos = Math.cos(a)
    var sin = Math.sin(a);
    point.x = x*cos - y*sin;
    point.y = x*sin + y*cos;     
    return point;
};
