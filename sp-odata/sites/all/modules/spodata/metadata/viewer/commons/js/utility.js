(function($) {
StatPortalOpenData.utility = StatPortalOpenData.utility || {};


StatPortalOpenData.utility.log = function log(message){
	
	if(typeof(console) != 'undefined' && console != undefined && typeof(console.log) === 'function'){
		console.log(message);
	}
};

/**
 * Funzione che permette di fare richieste cross domain (utilizzando il proxy passato tra le opzioni)
 *
 * @param options	{Object} 	opzioni per la richiesta cross domain (dataType, contentType, funzione di callback, url)
 *
 */
StatPortalOpenData.utility.requestCrossDomain = function requestCrossDomain(options) {

	var defaults = {
		dataType : 'json',
		contentType : 'html',
		callback : function() {
		}
	}, urlWithProxy;

	if(options.completeUrl === undefined) {
		throw "Mancano parametri richiesti per la funzione requestCrossDomain";
	}
	options = $.extend(defaults, options);
	
	if(options.crossDomainProxy){
		urlWithProxy = ((options.crossDomainProxy) ? options.crossDomainProxy + '?q=' : '') + options.completeUrl.replace(/&/g, "%26").replace('=','%3D') + ((options.crossDomainProxy) ? '&hl=0000000000' : '');
	}else{
		urlWithProxy = options.completeUrl;
	}

	$.ajax({
		url : options.completeUrl,
		timeout : 1000000,
		type : "GET",
		success : options.callback,
		error : function(xhr, ajaxOptions, thrownError) {
			//TODO: gestione dell'errore
		}
	});
};

/**
 * Restituisce l'hashCode di una stringa (è esattamente l'implementazione JAVA)
 *
 * @param {String} stringa del quale si vuole l'hashcode
 *
 * @return {String} hashcode della stringa passata
 */
StatPortalOpenData.utility.getHashCode = function getHashCode(str) {
	var hash = 0;
	if(str.length == 0)
		return hash;
	for( i = 0; i < str.length; i++) {
		character = str.charCodeAt(i);
		hash = ((hash << 5) - hash) + character;
		hash = hash & hash;
		// Convert to 32bit integer
	}
	return hash;
}

/**
 * Implementazione dell'outerHTML funzionante anche per Firefox
 */
StatPortalOpenData.utility.outerHTML = function outerHTML(node) {
	// if IE, Chrome take the internal method otherwise build one
	return node.outerHTML || (function(n) {
		var div = document.createElement('div'), h;
		div.appendChild(n.cloneNode(true));
		h = div.innerHTML;
		div = null;
		return h;
	})(node);
}

/**
 * Patch per gli apici. Aggiunge 'xxxAPEXxxx' al posto del singolo apice.
 * Per recuperare la stringa corretta sarà poi necessario utilizzare la funzione reverseApexPatch
 *
 * @param {String} text testo su cui applicare la patch
 * @return {String} la stringa con i singoli apici sostituiti
 *
 */
StatPortalOpenData.utility.applyApexPatch = function applyApexPatch(text) {
	return text.replace(new RegExp("'", "g"), "xxxAPEXxxx");
}

/**
 * Patch per gli apici. Rimuove 'xxxAPEXxxx' e aggiunge il singolo apice.
 * Serve per recuperare la stringa formattata correttamente
 *
 * @param {String} text 	testo su cui applicare la patch
 * @return {String}			la stringa con i singoli apici sostituiti
 *
 */
StatPortalOpenData.utility.reverseApexPatch = function reverseApexPatch(text) {
	return text.replace(new RegExp("xxxAPEXxxx", "g"), "'");
}

/**
 * Calcola lo spazio residuo per l'altezza della tabella
 */
StatPortalOpenData.utility.getResidualTableHeight = function getResidualTableHeight(viewer) {
	if(viewer !== 'MAP' && viewer !== 'TABLE' && viewer !== 'GRAPH'){
		viewer = 'TABLE';
	}
	
		var height = $('.ui-tabs-nav').outerHeight();
		height += $('#metadata-header').outerHeight();
		height += $('#header').outerHeight();
		height += $('#main-menu').outerHeight();
		height += $('#footer-wrapper').outerHeight();
		
		if(viewer === 'TABLE'){
			height += $('#controllerContainer').outerHeight();
			height += 80; // header e pager della jqGrid
			height += 100; 
		}
		if(viewer === 'MAP'){
			height += 80;
		}
		if(viewer === 'GRAPH'){
			height += $('#chart-controller').outerHeight();
			height += 105;
		}
		
	
	return height;
}




})(jQuery);