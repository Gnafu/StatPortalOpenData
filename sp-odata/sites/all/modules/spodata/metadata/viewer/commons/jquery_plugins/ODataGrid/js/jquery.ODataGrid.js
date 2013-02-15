
StatPortalOpenData.ODataGrid = StatPortalOpenData.ODataGrid || {};
/**
 * ODataGrid Plugin che sfrutta jqGrid per la visualizzazione dei dati a partire da un servizio OData
 *
 */
(function($) {

	StatPortalOpenData.ODataGrid.timeoutForUpdate = 800;
	StatPortalOpenData.ODataGrid.timerForUpdate;
	
	/**
	 * Ricava il tipo di un oggetto JS
	 *
	 * @param obj {Object} 	oggetto di cui si vuol sapere il tipo
	 * @return {String}	Restituisce il tipo dell'oggetto passato
	 *
	 */
	StatPortalOpenData.ODataGrid.toType = function toType(obj) {
		return ({}).toString.call(obj).match(/\s([a-zA-Z]+)/)[1].toLowerCase();
	}

	/**
	 * Funzione di utilità che recupera dati relativi all'ODataGrid direttamente dagli attributi HTML
	 *
	 * @param	{String}	DOMid			id dell'elemento HTML
	 * @param	{String}	propertyName	nome della proprietà di cui si vuole ottenere il valore
	 *
	 * @return il valore della proprietà richiesta
	 */
	StatPortalOpenData.ODataGrid.getData = function getData(DOMid, propertyName) {
		return $('#' + DOMid).attr('data-' + propertyName);
	}

	/**
	 * Utilità che setta dati relativi all'ODataGrid direttamente in attributi HTML
	 *
	 * @param	{String}	DOMid 			id dell'elemento HTML
	 * @param	{String}	propertyName	nome della proprietà di cui si vuole settare il valore
	 * @param	{String}	propertyValue	valore della proprietà da settare
	 *
	 */
	StatPortalOpenData.ODataGrid.setData = function setData(DOMid, propertyName, propertyValue) {
		$('#' + DOMid).attr('data-' + propertyName, propertyValue);
	}

	/**
	 * Chiama il count sulla collezione ed esegue la funzione di callback
	 *
	 * @param gridId {String} 				id della griglia
	 * @param parametersForCount {String}	parametri da aggiungere dopo il count
	 * @param callback	{function} 			funzione di callback
	 *
	 */
	StatPortalOpenData.ODataGrid.callCount = function callCount(gridId, parametersForCount, callback) {

		var isCustomService = StatPortalOpenData.ODataGrid.getData(gridId, 'isCustomService') === 'true', serviceUri = StatPortalOpenData.ODataGrid.getData(gridId, 'serviceUri'), resourcePath = StatPortalOpenData.ODataGrid.getData(gridId, 'resourcePath'), gbparameters = StatPortalOpenData.ODataGrid.getData(gridId, 'gbparameters') || '', crossDomainProxy = StatPortalOpenData.ODataGrid.getData(gridId, 'crossDomainProxy'), completeUrl, dataType, contentType;

		if(isCustomService) {
			if(gbparameters.substr(-1) === "?") {
				gbparameters = gbparameters.substring(0, gbparameters.length - 1);
			}
			// ancora la count spesso non è supportata in caso di servizio sister. Si cattura l'eccezione e si va avanti
			completeUrl = serviceUri + resourcePath + '?' + ((gbparameters !== '') ? (gbparameters) : '') + parametersForCount + "&count=true";
			dataType = 'xml';
			contentType = 'xml';
		} else {
			completeUrl = serviceUri + resourcePath + "/$count?" + parametersForCount;
			dataType = 'json';
			contentType = 'html';
		}
		
		OData.defaultHttpClient.enableJsonpCallback = true;
		OData.read(completeUrl, function(data) {
			var countN = data.results[0].Tot;
			callback(countN);
		});
		
		
	}

	/**
	 * Aggiorna i dati in griglia. Chiama anche la count se necessario (se sono stati applicati dei filtri)
	 *
	 * @param pdata	{Object} 		oggetto contenente informazioni sulla griglia (pdata.page = PAGE NUMBER, pdata.rows = PAGE SIZE, pdata.sidx = SORT INDEX, pdata.sord = SORT DIRECTION, pdata.filters = FILTERS)
	 * @param serviceUri {String} 	url del servizio OData
	 * @param singleData {Boolean}	true se si tratta di un dato singolo; false in caso di collezione
	 * @param columnNames {Array}	array contenente i nomi delle colonne della tabella
	 * @param gridId {String}		id della griglia
	 *
	 */
	StatPortalOpenData.ODataGrid.updateData = function updateData(pdata, serviceUri, resourcePath, gbparameters, singleData, gridId, columnNames, columnModel) {
		if($('#obscure').length !== 0) {
			$('#obscure').show();
		}
		
		// composizione della richiesta OData
		var actualPage = pdata.page, pageSize = pdata.rows, parameters = '?', columns = '', parametersForCount = '', filtersObj, filtersParams, singleFilter, serviceUriComposed, callbackFunction, countObj, pagecount, thegrid, dataFormatted, error, i, max;

		StatPortalOpenData.ODataGrid.setData(gridId, 'dataPageSize', pageSize);

		if(gbparameters === '') {

			for( i = 0, max = columnModel.length; i < max; i++) {
				columns += (columnModel[i].index + ',');
			}
			if(columns.substr(-1) === ",") {
				columns = columns.substring(0, columns.length - 1);
			}
			parameters = parameters + '$select=' + columns;

		} else {
			parameters = parameters + gbparameters;
		}

		// se si tratta di una collezione si richiedono i dati paginati
		if(!singleData) {
			parameters = parameters + '&$top=' + pageSize;
			parameters = parameters + '&$skip=' + ((pdata.page - 1) * pdata.rows);
			if(pdata.sidx !== '') {
				parameters = parameters + '&$orderby=' + pdata.sidx + ' ' + pdata.sord;
			}
		}

		// se ci sono filtri si crea la relativa stringa OData
		if( typeof (pdata.filters) !== 'undefined') {
			filtersObj = $.parseJSON(pdata.filters);

			if( typeof (filtersObj) !== 'undefined' && typeof (filtersObj.rules) !== 'undefined' && filtersObj.rules.length !== 0) {
				// si scorrono tutte le condizioni e si compone la stringa OData
				filtersParams = "&$filter=";
				for( i = 0, max = filtersObj.rules.length; i < max; i++) {
					singleFilter = filtersObj.rules[i];
					if(i !== 0) {
						filtersParams = filtersParams + " and ";
					}
					filtersParams = filtersParams + "substringof('" + singleFilter.data.replace(new RegExp("\'", "g"),"\'\'") + "', " + singleFilter.field + ") eq true";
				}
				parameters = parameters + filtersParams;
				parametersForCount = filtersParams;
			}
		}

		// si compone l'url
		serviceUriComposed = serviceUri + resourcePath + parameters;

		StatPortalOpenData.utility.log(serviceUriComposed);
		
		// si definisce la funzione di callback per il count (chiamato subito dopo)
		callbackFunction = function(count) {

			// TODO: questo che segue è provvisorio (FINO A CHE NON C'E' COMPATIBILITA' CON IL COUNT IN odata4j)
			// si ignorano anche eventuali eccezioni nella ricerca del count perchè alcune volte non è supportato
			if(StatPortalOpenData.ODataGrid.toType(count) !== 'number') {
				// si prende dall'oggetto
				countObj = $.xml2json(count);
				try {
					count = Number(countObj.entry.content.properties.Tot.text);
				} catch(e) {
					try{
						count = Number($.xml2json(count.xml).entry.content["m:properties"]["d:Tot"]);
					} catch(e) {
						count = 1;
					}
				}
			}

			// calcolo del numero di pagine
			pagecount = Math.floor(count / pageSize) + 1;
			thegrid = $("#" + gridId)[0];
			// se c'è almeno un risultato si richiedono i dati, altrimenti no
			if(count > 0) {
				OData.defaultHttpClient.enableJsonpCallback = true;
				OData.read(serviceUriComposed, function(data) {

					if(singleData) {
						dataFormatted = {
							results : [data],
							page : actualPage,
							pagecount : pagecount,
							count : count
						};
						thegrid.addJSONData(dataFormatted);
					} else {
						data.page = actualPage;
						data.pagecount = pagecount;
						data.count = count;
						thegrid.addJSONData(data);
					}

					//if(actualPage === 1) {
					//	resizeGrid();
					//}
					if($('#obscure').length !== 0) {
						$('#obscure').hide();
					}
					if($('#multidimensional_viewer_loading').length !== 0){
						$('#multidimensional_viewer_loading').hide();
					}

				}, function(err) {
					if($('#obscure').length !== 0) {
						$('#obscure').hide();
					}
					if($('#multidimensional_viewer_loading').length !== 0){
						$('#multidimensional_viewer_loading').hide();
					}
					error = 'Errore nel recuperare i dati';
					$('#resultsInfo').html(error);
					alert(error);
				});
			} else {
				if($('#obscure').length !== 0) {
					$('#obscure').hide();
				}
				if($('#multidimensional_viewer_loading').length !== 0){
					$('#multidimensional_viewer_loading').hide();
				}
				thegrid.addJSONData({
					results : [],
					page : 0,
					pagecount : 0,
					count : 0
				});
			}
		};
		// si chiama il count
		if(singleData) {
			callbackFunction(1);
		} else {
			StatPortalOpenData.ODataGrid.callCount(gridId, parametersForCount, callbackFunction);
		}
	}

	/**
	 * Inizializza la griglia a partire dalla struttura delle colonne
	 *
	 * @param grid {HTMLElement} 		griglia
	 * @param columnNames {Array}		Insieme dei nomi delle colonne
	 * @param columnModel {Array}		Insieme dei modelli delle colonne
	 * @param isCollection {Boolean}	true se si tratta di una collezione
	 *
	 *
	 */
	StatPortalOpenData.ODataGrid.initGrid = function initGrid(grid, columnNames, columnModel, isCollection, gridWidth, gridHeight) {

		var attr, attrName, pageSize, serviceUri = StatPortalOpenData.ODataGrid.getData(grid[0].id, 'serviceUri'), resourcePath = StatPortalOpenData.ODataGrid.getData(grid[0].id, 'resourcePath'), gbparameters = StatPortalOpenData.ODataGrid.getData(grid[0].id, 'gbparameters'), attrs = grid[0].attributes, i, max, nodes = [], values = [];
		/* TODO: dovrà essere variabile */

		pageSize = StatPortalOpenData.ODataGrid.getData(grid[0].id, 'dataPageSize');
		if( typeof (pageSize) === 'undefined' || pageSize === '') {
			pageSize = 50;
		}

		// si salvano tutti gli attributi
		
		// prende tutti gli attributi
		for( i = 0, max = attrs.length; i < max; i++) {
			attr = attrs.item(i);
			attrName = attr.nodeName;
			// si salvano tutti quelli che iniziano per 'data-' e l'id
			if(attrName.indexOf('data-') == 0 || attrName == 'id'){
				nodes.push(attrName);
				values.push(attr.nodeValue);
			}
		}

		// si svuota la griglia
		grid.jqGrid('GridUnload');
		try{
		$('#' + grid[0].id).jqGrid('GridUnload');
		}catch(Exception){}
		
		// SET ALL ATTRIBUTES
		for( i = 0, max = nodes.length; i < max; i++) {
			$('#' + grid[0].id).attr(nodes[i], values[i]);
		}
		
		var shrinkToFitValue = false;
		try{
			var selectedColumns = jQuery('.ui-multiselect-checkboxes').find('input').filter('[checked]').filter(':enabled').length;
			var numColumnsForShrink = Math.floor(gridWidth/150);
			
			if(selectedColumns < numColumnsForShrink){
				shrinkToFitValue = true;
			}
		}catch(Exception){
			
		}

		$('#' + grid[0].id).jqGrid({
			scroll: 1,
			datatype : function(pdata) {
				if($('#obscure').length !== 0) {
					$('#obscure').show();
				}
				clearTimeout(StatPortalOpenData.ODataGrid.timerForUpdate);
				StatPortalOpenData.ODataGrid.timerForUpdate = setTimeout(function(){StatPortalOpenData.ODataGrid.updateData(pdata, serviceUri, resourcePath, gbparameters, !isCollection, grid[0].id, columnNames, columnModel);},StatPortalOpenData.ODataGrid.timeoutForUpdate);
			},
			altclass : 'alernateRow',
			altRows : true,
			colNames : columnNames,
			colModel : columnModel,
			rowNum : pageSize,
			rowList : [10, 20, 30, 50],
			/*pager : $('#pager'),*/
			pager: '#pscrolling',
			height : gridHeight,
			width : gridWidth,
			viewrecords : true,
			viewsortcols : [true, 'vertical', true],
			caption : "Dati",
			cellEdit : false,
			shrinkToFit: shrinkToFitValue,
			jsonReader : {
				repeatitems : false,
				root : "results",
				page : "page",
				total : "pagecount",
				records : "count"
			}
		});

		$(".ui-jqgrid-titlebar").hide();

		$('#' + grid[0].id).filterToolbar({
			searchOnEnter : false,
			stringResult : true
		});
		
		// di default viene aggiunta l'icona di ordinamento sulla prima colonna.
		// In realt� la tabella non � ordinata, quindi � giusto che sia disabilitata
		try{
			if(jQuery('th .ui-grid-ico-sort:first').attr('class').indexOf('ui-state-disabled') == -1){
				jQuery('th .ui-grid-ico-sort:first').addClass('ui-state-disabled');
			}
		}catch(ex){
			
		}
	};


	$.fn.extend({
		ODataGrid : function(options) {
			$('#list').after('<div id="toolbar"></div> <div id="pager"></div>');

			if($('#obscure').length !== 0) {
				$('#obscure').show();
			}

			if(typeof(MIN_TABLE_HEIGHT) === 'undefined'){
				MIN_TABLE_HEIGHT = 200;
			}
			
			// se gi� presente si prendono le dimensioni che ci sono, altrimenti si ricalcolano
			var gridHeight;
			var gridWidth;
			if(state.isFullScreenMode(StatPortalOpenData.Enums.ModuleId.GRID)){
				var dimensions = StatPortalOpenData.MultidimensionalViewer.getDimensionsForFullScreen();
				gridHeight = dimensions.height;
				gridWidth = dimensions.width;
			}else{
				gridHeight = Math.max(MIN_TABLE_HEIGHT, jQuery(window).height() - StatPortalOpenData.utility.getResidualTableHeight());
				gridWidth = ($('#ODataGridContainer').length > 0 && $('#ODataGridContainer').width() > 0) ? $('#ODataGridContainer').width() : $(window).width() - 20;
			}
			
			var defaults = {
			serviceUri : 'http://odata.netflix.com/v2/Catalog/',
			resourcePath : 'Titles',
			gbparameters : '',
			crossDomainProxy : null,
			isCustomService : false,
			includeNavigationProperties : true,
			columnNames : null,
			columnModel : null,
			gridHeight : gridHeight,
			gridWidth : gridWidth
			};
			options = $.extend(defaults, options);

			return this.each(function() {

				var __e = $(this), metadataByXML = false;

				// fix per gli apici
				options.serviceUri = StatPortalOpenData.utility.reverseApexPatch(options.serviceUri);
				options.crossDomainProxy = StatPortalOpenData.utility.reverseApexPatch(options.crossDomainProxy);
				options.resourcePath = StatPortalOpenData.utility.reverseApexPatch(options.resourcePath);

				StatPortalOpenData.ODataGrid.setData(this.id, 'serviceUri', options.serviceUri);
				StatPortalOpenData.ODataGrid.setData(this.id, 'crossDomainProxy', options.crossDomainProxy);
				StatPortalOpenData.ODataGrid.setData(this.id, 'gbparameters', options.gbparameters);
				StatPortalOpenData.ODataGrid.setData(this.id, 'isCustomService', options.isCustomService/*serviceUri.indexOf('http://localhost:') !== -1*/);
				StatPortalOpenData.ODataGrid.setData(this.id, 'includeNavigationProperties', options.includeNavigationProperties);
				StatPortalOpenData.ODataGrid.setData(this.id, 'resourcePath', options.resourcePath);

				// si richiede l'XML dei metadati per capire quali colonne ci servono.
				// Nel caso non si riesca a definire la struttura del dato attraverso
				// i metadati richiediamo la prima riga del dato e ci prendiamo il nome
				// delle colonne; per quanto riguarda il tipo della colonna proviamo a
				// a fare un cast per capire se si tratta di numeri oppure stringhe.
				// Rimane comunque scoperto il caso in cui alcuni valori della prima
				// non siano presenti

				if(options.columnNames != null && options.columnModel != null) {
					// 	nel caso in cui si tratta di un servizio interno con dati strutturati
					//	abbiamo le informazioni sulle colonne dalle dimensioni e le misure.
					StatPortalOpenData.ODataGrid.initGrid(__e, options.columnNames, options.columnModel, true, options.gridWidth, options.gridHeight);
				} else if( typeof (options.jsonMetadata) !== 'undefined') {
					readMetadata(__e, StatPortalOpenData.ODataUtility.getEntitySet(jsonMetadata), StatPortalOpenData.ODataUtility.getEntityType(jsonMetadata));
				} else {
					StatPortalOpenData.ODataUtility.parseMetadataAndDoSomething(options.crossDomainProxy, options.serviceUri, options.resourcePath, options.includeNavigationProperties, function(cNames, cModel, isColl) {StatPortalOpenData.ODataGrid.initGrid(__e, cNames, cModel, isColl, options.gridWidth, options.gridHeight);
					}, function() {alert('error..ancora mai chiamata')
					});
					// richiediamo i metadati XML
				}

			});
		}
	});
})(jQuery);
