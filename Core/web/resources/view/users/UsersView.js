/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery', 'view/BaseUIComponent', 'dstore/Rest',
        'dijit/form/FilteringSelect', 'dstore/legacy/DstoreAdapter', 'dojo/dom'], 
		function($, BaseUIComponent, Rest, FilteringSelect, DstoreAdapter){
"use strict";


function UsersView(){
	
	BaseUIComponent.apply(this, arguments);
   	
}

UsersView.prototype = Object.create(BaseUIComponent.prototype);

UsersView.prototype.store = null;
UsersView.prototype.userPicker = null;
UsersView.prototype.switchUserPicker = null;
UsersView.prototype.timezoneStore = null;
UsersView.prototype.newUser = false; //Flag to indicate we are adding a user


/**
 * Do the heavy lifting of setting up the view
 */
UsersView.prototype.setupView = function(){
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
		    placeholder: this.tr('users.selectUserToBecome'),
		}, 'switchUserPicker');
		this.switchUserPicker.on('change', this.switchUser.bind(this));
		
		this.userPicker = new FilteringSelect({
		    store: new DstoreAdapter(this.store),
		    searchAttr: 'username',
		    pageSize: 100,
		    queryExpr: '*${0}*',
		    autoComplete: false,
		    placeholder: this.tr('users.selectUserToEdit')
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
		$('#permissionsViewer').on('click', {inputNode: $('#permissions')}, this.showPermissionList.bind(this));
		
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
};

UsersView.prototype.loadNewUser = function(){
	this.clearErrors();
	var self = this;
	this.api.newUser().then(function(user){
		self.newUser = true;
		self.fillUserInputs(user);
	}).fail(this.showError);
	
};

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
	$('#muted').prop('checked', userData.muted);
	$('#disabled').prop('checked', userData.disabled);
	$('#homeURL').val(userData.homeUrl);
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
		this.updateImage($('#userImg'), this.tr('common.disabled'), '/images/user_disabled.png');
	} else if(userData.admin === true){
		this.updateImage($('#userImg'), this.tr('common.administrator'), '/images/user_suit.png');
	}else{
		this.updateImage($('#userImg'), this.tr('common.user'), '/images/user_green.png');
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
		muted: $('#muted').is(':checked'),
		disabled: $('#disabled').is(':checked'),
		homeUrl: $('#homeURL').val(),
		receiveAlarmEmails: $('#receiveAlarmEmails').val(),
		receiveOwnAuditEvents: $('#receiveOwnAuditEvents').is(':checked'),
		timezone: this.timezonePicker.get('value'), //Dojo widget
		permissions:  $('#permissions').val(),
	};
	var self = this;
	if(this.newUser === true)
		this.api.postUser(user).then(function(result){
			self.showSuccess(self.tr('users.added'));
			self.newUser = false;
		}).fail(this.showError);
	else
		this.api.putUser(user).then(function(result){
			self.showSuccess(self.tr('users.saved'));
		}).fail(this.showError);
};

UsersView.prototype.deleteUser = function(){
	var username = $('#username').val();
	var self = this;
	if(confirm(this.tr('users.deleteConfirm'))){
		this.api.deleteUser(username).done(function(user){
			self.showSuccess(self.tr('users.deleted', user.username));
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
		this.updateImage($('#userImg'), this.tr('common.disabled'), '/images/user_disabled.png');
	} else if(admin === true){
		this.updateImage($('#userImg'), this.tr('common.administrator'), '/images/user_suit.png');
	}else{
		this.updateImage($('#userImg'), this.tr('common.user'), '/images/user_green.png');
	}
};


return UsersView;

});