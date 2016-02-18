/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * 
 * @author Terry Packer
 */

define([ 'jquery' ], function($, BaseUIComponent) {
	"use strict";

	function HTML5SoundPlayer(options) {
		
		var self = this;

		this.level1 = new Audio('/audio/information.mp3');
		this.level1.addEventListener("ended", function() {
			self._repeatDelay();
		});
		this.level2 = new Audio('/audio/urgent.mp3');
		this.level2.addEventListener("ended", function() {
			self._repeatDelay();
		});
		this.level3 = new Audio('/audio/critical.mp3');
		this.level3.addEventListener("ended", function() {
			self._repeatDelay();
		});
		this.level4 = new Audio('/audio/lifesafety.mp3');
		this.level4.addEventListener("ended", function() {
			self._repeatDelay();
		});

		this.onLoadFinished = true;
		this.mute = true;
		
		$.extend(this, options);
	}

	/**
	 * Alarm Level 1 Sound 
	 */
	HTML5SoundPlayer.prototype.level1 = null;
	
	/**
	 * Alarm Level 2 Sound 
	 */
	HTML5SoundPlayer.prototype.level2 = null;

	/**
	 * Alarm Level 3 Sound 
	 */
	HTML5SoundPlayer.prototype.level3 = null;

	/**
	 * Alarm Level 4 Sound 
	 */
	HTML5SoundPlayer.prototype.level4 = null;

	/**
	 * Are the files finished loading
	 */
	HTML5SoundPlayer.prototype.onLoadFinished = false;

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
	 * Stop repeating
	 */
	HTML5SoundPlayer.prototype._stopRepeat = function(sId) {
		this[this.soundId].stop();
		clearTimeout(this.timeoutId);
	};

	/**
	 * Repeat the sound
	 */
	HTML5SoundPlayer.prototype._repeat = function() {
		if (this.onLoadFinished === true) {
			if (this.soundId && !this.mute) {
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