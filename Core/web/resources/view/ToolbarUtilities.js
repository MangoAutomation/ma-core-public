/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery', 'view/BaseUIComponent'], 
		function($, BaseUIComponent){
"use strict";

function ToolbarUtilities(){
	BaseUIComponent.apply(this, arguments);
	
	
};

ToolbarUtilities.prototype = Object.create(BaseUIComponent.prototype);


return ToolbarUtilities;

});