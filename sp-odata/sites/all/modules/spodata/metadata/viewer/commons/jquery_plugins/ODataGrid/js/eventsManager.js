StatPortalOpenData.eventsManager = {
	subscribers : {
		any : []
	},
	subscribe : function(fn, type) {
		type = type || 'any';
		if( typeof this.subscribers[type] === 'undefined') {
			this.subscribers[type] = [];
		}
		this.subscribers[type].push(fn);
	},
	unsubscribe : function(fn, type) {
		this.visitSubscribers('unsubscribe', fn, type);
	},
	trigger : function(obj, type) {
		this.visitSubscribers('trigger', obj, type);
	},
	visitSubscribers : function(action, arg, type) {
		var eventType = type || 'any', subscribersToEvent = this.subscribers[eventType], i;
		if( typeof (subscribersToEvent) != 'undefined') {
			var max = subscribersToEvent.length;
			for( i = 0; i < max; i += 1) {
				if(action === 'trigger') {
					subscribersToEvent[i](arg);
				} else if(action === 'unsubscribe') {
					if(subscribersToEvent[i] === arg) {
						subscribersToEvent.splice(i, 1);
					}
				}
			}
		}

	}
}