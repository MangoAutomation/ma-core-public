/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery', 'view/BaseUIComponent', 'dstore/Rest', 'dstore/Request', 
        'dijit/form/filteringSelect', 'dstore/legacy/DstoreAdapter', 'dijit/TooltipDialog',
        'dijit/popup', 'dojo/dom'], 
		function($, BaseUIComponent, Rest, Request, 
				FilteringSelect, DstoreAdapter, TooltipDialog, Popup){
"use strict";


function UsersView(){
	
	BaseUIComponent.apply(this, arguments);
	
	this.fillUserInputs = this.fillUserInputs.bind(this);
	
	//this.errorDiv = $('#userErrors');
	
	this.store = new Rest({
		target: '/rest/v1/users',
		idProperty: 'username'
	});
	

	//If we have the admin section then set it up
	if(currentUser.admin === true){
		this.switchUserPicker = new FilteringSelect({
		    store: new DstoreAdapter(this.store),
		    searchAttr: 'username',
		    pageSize: 100,
		    queryExpr: '*${0}*',
		    autoComplete: false,
		    placeholder: tr('users.selectUserToBecome'),
		}, 'switchUserPicker');
		this.switchUserPicker.on('change', this.switchUser.bind(this));
		
		this.userPicker = new FilteringSelect({
		    store: new DstoreAdapter(this.store),
		    searchAttr: 'username',
		    pageSize: 100,
		    queryExpr: '*${0}*',
		    autoComplete: false,
		    placeholder: tr('users.selectUserToEdit')
		}, 'userPicker');
		this.userPicker.on('change', this.loadUser.bind(this));
		
		//Setup the disabled check box click to modify the user image
		$('#disabled').on('click', this.updateUserImage.bind(this));
		
		//Setup a watch to modify image if permissions are removed
		$('#permissions').on('keyup', this.updateUserImage.bind(this));
		
		//Setup the add new user link
		$('#newUser').on('click', this.loadNewUser.bind(this));
		
		//Setup the Delete User link
		$('#deleteUser').on('click', this.deleteUser.bind(this));
		
		//Setup the Send Email link
		$('#sendTestEmail').on('click', this.sendTestEmail.bind(this));
		
		//Setup Permissions Viewer
		$('#permissionsViewer').on('click', this.showPermissionList.bind(this));
		
	} 
	
	//Setup the users Help Link
	$('#usersHelp').on('click', {helpId: 'userAdministration'}, this.showHelp.bind(this));
	
	
 
	this.timezoneStore = new Rest({
		target: '/rest/v1/server/timezones',
		idProperty: 'id'
	}).sort([{property: 'id', descending: false}]);
	
	this.timezonePicker = new FilteringSelect({
	    store: new DstoreAdapter(this.timezoneStore),
	    searchAttr: 'name',
	    pageSize: 100,
	    queryExpr: '*${0}*',
	    autoComplete: false,
        labelFunc: function(item, store) {
            return item.name;
        },
        // override set('item', obj) so displayedValue comes from labelFunc()
        _setItemAttr: function(item, priorityChange, displayedValue) {
            FilteringSelect.prototype._setItemAttr.apply(this, [item, priorityChange, this.labelFunc(item)]);
        }
	}, 'timezone');

	
	//Setup the save image link
	$('#saveUser').on('click', this.saveUser.bind(this));
   	
}

UsersView.prototype = Object.create(BaseUIComponent.prototype);

UsersView.prototype.loadNewUser = function(){
	this.clearErrors();
	var self = this;
	this.api.newUser().then(function(user){
		self.newUser = true;
		self.fillUserInputs(user);
	}).fail(this.showError);
	
}

/**
 * Load a user
 * @param username - String
 */
UsersView.prototype.loadUser = function(username){
	
	this.clearErrors();
	var self = this;
	this.store.get(username).then(function(userData){
		self.newUser = false;
		$('#userEditView').show();
		self.fillUserInputs(userData);
	});
};

UsersView.prototype.fillUserInputs = function(userData){
	
	var usernameInput = $('#username');
	if(userData.username === currentUser.username)
		usernameInput.prop('disabled', true);
	else
		usernameInput.prop('disabled', false);
	usernameInput.val(userData.username);
	
	$('#email').val(userData.email);
	$('#phone').val(userData.phone);
	$('#disabled').prop('checked', userData.disabled);
	$('#receiveAlarmEmails').val(userData.receiveAlarmEmails);
	$('#receiveOwnAuditEvents').prop('checked', userData.receiveOwnAuditEvents);
	if((userData.timezone === '')||(userData.timezone === null)){
		this.timezonePicker.set('value', "");
	}else
		$('#timezone').val(userData.timezone);
	$('#permissions').val(userData.permissions);
	
	//Set the password to blank no matter what
	$('#password').val('');
	
	//Switch out the png based on the user type
	if(userData.disabled === true){
		this.updateImage($('#userImg'), tr('common.disabled'), '/images/user_disabled.png');
	} else if(userData.admin === true){
		this.updateImage($('#userImg'), tr('common.administrator'), '/images/user_suit.png');
	}else{
		this.updateImage($('#userImg'), tr('common.user'), '/images/user_green.png');
	}
	
	//Now Check that the logged in user is admin to allow other edits
	if(currentUser.admin === true){
		$('#permissionsRow').show();
		$('#disabledRow').show();
		$('#sendTestEmail').show();
		$('#deleteUser').show();
	}else{
		$('#permissionsRow').hide();
		$('#disabledRow').hide();
		$('#sendTestEmail').hide();
		$('#deleteUser').hide();
	}
};

UsersView.prototype.saveUser = function(){
	
	this.clearErrors();
	
	//Get the user info
	var user = {
		username: $('#username').val(),
		password: $('#password').val(),
		email: $('#email').val(),
		phone: $('#phone').val(),
		disabled: $('#disabled').is(':checked'),
		receiveAlarmEmails: $('#receiveAlarmEmails').val(),
		receiveOwnAuditEvents: $('#receiveOwnAuditEvents').is(':checked'),
		timezone: this.timezonePicker.get('value'), //Dojo widget
		permissions:  $('#permissions').val(),
	};
	var self = this;
	if(this.newUser === true)
		this.api.postUser(user).then(function(result){
			self.showSuccess(tr('users.added'));
			self.newUser = false;
		}).fail(this.showError);
	else
		this.api.putUser(user).then(function(result){
			self.showSuccess(tr('users.saved'));
		}).fail(this.showError);
};

UsersView.prototype.deleteUser = function(){
	var username = $('#username').val();
	var self = this;
	if(confirm(tr('users.deleteConfirm'))){
		this.api.deleteUser(username).done(function(user){
			self.showSuccess(tr('users.deleted', user.username));
			//Load in the top user on the list
			self.store.fetchRange({start: 0, end: 1}).then(function(userArray){
				self.loadUser(userArray[0].username);
			});
		}).fail(this.showError);
	}
};

UsersView.prototype.switchUser = function(username){
	
	this.clearErrors();
	
	this.api.ajax({
        url : "/rest/v1/login/su/" + encodeURIComponent(username) + ".json"
    }).then(function(userData){
    	window.location.href = userData.homeUrl;
    }).fail(this.showError);
};

UsersView.prototype.showPermissionList = function(){
	
	var self = this;
	this.api.getAllUserGroups($('#permissions').val()).then(function(groups){

		if (self.permissionsDialog != null)
            self.permissionsDialog.destroy();
		var content = "";
		if (groups.length == 0)
		    content = tr('users.permissions.nothingNew');
		else {
		    for (var i=0; i<groups.length; i++)
		        content += "<a id='perm-"+ self.escapeQuotes(groups[i]) +"' class='ptr permissionStr'>"+ groups[i] +"</a>";
		}
		
		self.permissionsDialog = new TooltipDialog({
            id: 'permissionsTooltipDialog',
            content: content,
            onMouseLeave: function() { 
                Popup.close(self.permissionsDialog);
            }
        });
        
        Popup.open({
            popup: self.permissionsDialog,
            around: $('#permissions')[0]
        });
		
        //Assign onclick to all links and send in the group in the data of the event
		$('.permissionStr').each(function(i){
			$(this).on('click', {group: $(this).attr('id').substring(5)}, self.addGroup.bind(self));
		});
	});
	
};

UsersView.prototype.addGroup = function(event){
	var groups = $get("permissions");
	if (groups.length > 0 && groups.substring(groups.length-1) != ",")
		groups += ",";
	groups += event.data.group;
	$('#permissions').val(groups);
	this.showPermissionList();
};

UsersView.prototype.sendTestEmail = function(){
	
	var email = $('#email').val();
	var username = $('#username').val();
	var self = this;
	this.api.ajax({
		type : 'PUT',
		contentType: "application/json",
        url : '/rest/v1/server/email/test.json?email='+ encodeURIComponent(email) + '&username=' + encodeURIComponent(username),
    }).done(function(response){
    	self.showSuccess(response);
    }).fail(this.showError);
};

UsersView.prototype.updateUserImage = function(){
	//Switch out the png based on the user type
	var disabled = $('#disabled').is(':checked');
	var admin = false;
	if($('#permissions').val().match(/.*superadmin.*/))
		admin = true;
	if(disabled === true){
		this.updateImage($('#userImg'), tr('common.disabled'), '/images/user_disabled.png');
	} else if(admin === true){
		this.updateImage($('#userImg'), tr('common.administrator'), '/images/user_suit.png');
	}else{
		this.updateImage($('#userImg'), tr('common.user'), '/images/user_green.png');
	}
};


UsersView.prototype.api = null;
UsersView.prototype.store = null;
UsersView.prototype.userPicker = null;
UsersView.prototype.switchUserPicker = null;
UsersView.prototype.timezoneStore = null;
UsersView.prototype.newUser = false; //Flag to indicate we are adding a user
UsersView.prototype.permissionsDialog = null;

return UsersView;

});