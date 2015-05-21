/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery', 'view/BaseUIComponent', 'mango/api', 'dstore/Rest', 'dstore/Request', 'dijit/form/filteringSelect', 'dstore/legacy/DstoreAdapter'], 
		function($, BaseUIComponent, MangoAPI, Rest, Request, FilteringSelect, DstoreAdapter){
"use strict";


function UsersView(){
	
	BaseUIComponent.apply(this, arguments);
	
	this.fillUserInputs = this.fillUserInputs.bind(this);
	
	//this.errorDiv = $('#userErrors');
	
	this.api = MangoAPI.defaultApi;
	
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
		
		//Setup the add new user link
		$('#newUser').on('click', this.loadNewUser.bind(this));
		
		//Setup the users Help Link
		$('#usersHelp').on('click', {helpId: 'userAdministration'}, this.showHelp);
	} 
	
	
 
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

UsersView.prototype.loadUser = function(user, data){
	
	this.clearErrors();
	var self = this;
	this.store.get(user).then(function(userData){
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
	$('#receiveAlarmEmails').val(userData.receiveAlarmEmails);
	$('#receiveOwnAuditEvents').val(userData.receiveOwnAuditEvents);
	if((userData.timezone === '')||(userData.timezone === null)){
		this.timezonePicker.set('value', "");
	}else
		$('#timezone').val(userData.timezone);
	$('#permissions').val(userData.permissions);
	
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
		$('#sendTestEmailImg').show();
		$('#deleteImg').show();
	}else{
		$('#permissionsRow').hide();
		$('#disabledRow').hide();
		$('#sendTestEmailImg').hide();
		$('#deleteImg').hide();
	}
}

UsersView.prototype.saveUser = function(){
	
	this.clearErrors();
	
	//Get the user info
	var user = {
		username: $('#username').val(),
		password: $('#newPassword').val(),
		email: $('#email').val(),
		phone: $('#phone').val(),
		disabled: $('#disabled').is(':checked'),
		receiveAlarmEmails: $('#sendAlarmEmails').val(),
		receiveOwnAuditEvents: $('#receiveOwnAuditEvents').is(':checked'),
		timezone: $('#timezone').val(),
		permissions:  $('#permissions').val(),
	};
	var self = this;
	if(this.newUser === true)
		this.api.postUser(user).then(function(result){
			self.showSuccess(tr('users.added'));
		}).fail(this.showError);
	else
		this.api.putUser(user).then(function(result){
			self.showSuccess(tr('users.saved'));
		}).fail(this.showError);
};

UsersView.prototype.switchUser = function(username){
	
	this.clearErrors();
	
	this.api.ajax({
        url : "/rest/v1/login/su/" + encodeURIComponent(username) + ".json"
    }).then(function(userData){
    	window.location.href = userData.homeUrl;
    }).fail(this.showError);
};



UsersView.prototype.api = null;

UsersView.prototype.store = null;
UsersView.prototype.userPicker = null;
UsersView.prototype.switchUserPicker = null;
UsersView.prototype.timezoneStore = null;
UsersView.prototype.newUser = false; //Flag to indicate we are adding a user

return UsersView;

});