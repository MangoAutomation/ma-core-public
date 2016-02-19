/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * 
 * @author Terry Packer
 */

define([ 'jquery' ], function($, BaseUIComponent) {
	"use strict";

	function HTML5SoundPlayer(options) {
		
		var self = this;

		this.level1 = new Audio();
		this.level1.addEventListener("ended", function(){self._repeatDelay();});
		this.level1.addEventListener('error', function(evt){
			console.log('Level1 Sound Failed to load');
			self.hasLevel1 = false;
			self.level1Ready = true;
		});
		this.level1.addEventListener('loadeddata', function(evt){
			self.hasLevel1 = true;
			self.level1Ready = true;
		});
		this.level1.src = '/audio/information.mp3';
		
		this.level2 = new Audio();
		this.level2.addEventListener("ended", function(){self._repeatDelay();});
		this.level2.addEventListener('error', function(evt){
			console.log('Level2 Sound Failed to load');
			self.hasLevel2 = false;
			self.level2Ready = true;
		});
		this.level2.addEventListener('loadeddata', function(evt){
			self.hasLevel2 = true;
			self.level2Ready = true;
		});
		this.level2.src = '/audio/urgent.mp3';
		
		this.level3 = new Audio();
		this.level3.addEventListener("ended", function(){self._repeatDelay();});
		this.level3.addEventListener('error', function(evt){
			console.log('Level3 Sound Failed to load');
			self.hasLevel3 = false;
			self.level3Ready = true;
		});
		this.level3.addEventListener('loadeddata', function(evt){
			self.hasLevel3 = true;
			self.level3Ready = true;
		});
		this.level3.src = '/audio/critical.mp3';
		
		
		this.level4 = new Audio();
		this.level4.addEventListener("ended", function(){self._repeatDelay();});
		this.level4.addEventListener('error', function(evt){
			console.log('Level4 Sound Failed to load');
			self.hasLevel4 = false;
			self.level4Ready = true;
		});
		this.level4.addEventListener('loadeddata', function(evt){
			self.hasLevel4 = true;
			self.level4Ready = true;
		});
		this.level4.src = '/audio/lifesafety.mp3';

		this.mute = true;
		
		$.extend(this, options);
	}

	/**
	 * Alarm Level 1 Sound 
	 */
	HTML5SoundPlayer.prototype.level1 = null;
	/**
	 * Does the sound exist and is it loaded
	 */
	HTML5SoundPlayer.prototype.hasLevel1 = false;
	/**
	 * Has the sound retrieval finished, either success or fail
	 */
	HTML5SoundPlayer.prototype.level1Ready = false;
	
	/**
	 * Alarm Level 2 Sound 
	 */
	HTML5SoundPlayer.prototype.level2 = null;
	/**
	 * Does the sound exist and is it loaded
	 */
	HTML5SoundPlayer.prototype.hasLevel2 = false;
	/**
	 * Has the sound retrieval finished, either success or fail
	 */
	HTML5SoundPlayer.prototype.level2Ready = false;
	
	/**
	 * Alarm Level 3 Sound 
	 */
	HTML5SoundPlayer.prototype.level3 = null;
	/**
	 * Does the sound exist and is it loaded
	 */
	HTML5SoundPlayer.prototype.hasLevel3 = false;
	/**
	 * Has the sound retrieval finished, either success or fail
	 */
	HTML5SoundPlayer.prototype.level3Ready = false;

	/**
	 * Alarm Level 4 Sound 
	 */
	HTML5SoundPlayer.prototype.level4 = null;
	/**
	 * Does the sound exist and is it loaded
	 */
	HTML5SoundPlayer.prototype.hasLevel4 = false;
	/**
	 * Has the sound retrieval finished, either success or fail
	 */
	HTML5SoundPlayer.prototype.level4Ready = false;

	/**
	 * Currently playing sound id
	 */
	HTML5SoundPlayer.prototype.soundId = null;
	/**
	 * Is the player muted
	 */
	HTML5SoundPlayer.prototype.mute = false;
	/**
	 * The id of our timeout timer task
	 */
	HTML5SoundPlayer.prototype.timeoutId = null;
	
	/**
	 * Play the sound 
	 * 'level1', 'level2', 'level3', 'level4'
	 */
	HTML5SoundPlayer.prototype.play = function(soundId) {
		this.stop();
		this.soundId = soundId;
		if (!this.mute)
			this._repeat();
	};

	/**
	 * Stop playing the current sound
	 */
	HTML5SoundPlayer.prototype.stop = function() {
		if (this.soundId) {
			var sid = this.soundId;
			this.soundId = null;
			this._stopRepeat(sid);
		}
	};

	/**
	 * Are we muted?
	 */
	HTML5SoundPlayer.prototype.isMute = function() {
		return this.mute;
	};

	/**
	 * Set the mute state
	 */
	HTML5SoundPlayer.prototype.setMute = function(muted) {
		if (muted != this.mute) {
			this.mute = muted;
			if (this.soundId) {
				if (muted)
					this._stopRepeat(this.soundId);
				else
					this._repeat();
			}
		}
	};
	
	/**
	 * Does the specific sound exist
	 */
	HTML5SoundPlayer.prototype.hasSound = function(soundId){
    	switch(soundId){
    	case 'level1':
    		return this.hasLevel1;
    	case 'level2':
    		return this.hasLevel2;
    	case 'level3':
    		return this.hasLevel3;
    	case 'level4':
    		return this.hasLevel4;
    	default:
    	return false;
    	}
    };
	
    /**
     * Is the HTML5 Sound Player Ready to Play all available sounds
     */
    HTML5SoundPlayer.prototype.playerReady = function(){
    	return ((this.level1Ready === true) && 
    		(this.level2Ready  === true) &&
    		(this.level3Ready  === true) &&
    		(this.level4Ready === true));
    };

	/**
	 * Stop repeating
	 */
	HTML5SoundPlayer.prototype._stopRepeat = function(sId) {
		if(this.playerReady() && this.hasSound(sId)){
			this[sId].pause();
			this[sId].currentTime = 0;
		}
		clearTimeout(this.timeoutId);
	};

	/**
	 * Repeat the sound
	 */
	HTML5SoundPlayer.prototype._repeat = function() {
		if (this.playerReady() === true) {
			if (this.soundId && !this.mute && this.hasSound(this.soundId)) {
				this[this.soundId].play();

			}
		} else{
			// Wait for the sound manager to load.
			var self = this;
			setTimeout(function(){self._repeat();}, 500);
		}
	};

	/**
	 * Delay before repeating
	 */
	HTML5SoundPlayer.prototype._repeatDelay = function() {
		if (this.soundId && !this.mute){
			var self = this;
			this.timeoutId = setTimeout(function(){self._repeat();}, 10000);
		}
	};
	
	
	return HTML5SoundPlayer;
});