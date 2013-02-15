StatPortalOpenData.ODataUtility = StatPortalOpenData.ODataUtility || {};
StatPortalOpenData.ODataUtility.streetViewX;
StatPortalOpenData.ODataUtility.streetViewY;


(function($) {

	
	
/**
 * Imposta il fullscrenn del viewer attivo.  
 */
StatPortalOpenData.ODataUtility.toFullscreen = function toFullscreen() {
	// capire qual'Ë il modulo attivo. In realt‡ ce ne potrebbe essere pi˘ di uno (e quindi andrebbe suddiviso lo schermo).
	// Attualmente assumiamo che siano esclusivi

	var moduleFound = true;
		if (state.isModuleActive(StatPortalOpenData.Enums.ModuleId.GRID)) {
			StatPortalOpenData.MultidimensionalViewer.toFullScreen();
		} else if (state.isModuleActive(StatPortalOpenData.Enums.ModuleId.MAP)) {
			StatPortalOpenData.GeoViewer.toFullScreen();
		} else if (state.isModuleActive(StatPortalOpenData.Enums.ModuleId.GRAPH)) {
			StatPortalOpenData.GraphViewer.toFullScreen();
		} else if (state.isModuleActive(StatPortalOpenData.Enums.ModuleId.MAP3D)) {
			StatPortalOpenData.EarthViewer.toFullScreen();
		}else{
			moduleFound = false;
		}
		
		if(moduleFound){
			// aggiungo il listener al tasto esc (per ritornare allo stato normale)
			jQuery(document).keyup(StatPortalOpenData.ODataUtility.keyUpEscListener);
			// si attiva l'immagine per la chiusura
		}
	
};

/**
 * Mostra un particolare messaggio all'utente
 */
StatPortalOpenData.ODataUtility.showMessage = function showMessage(selector, type, msg){
	jQuery(selector).removeClass('info-background');
	jQuery(selector).removeClass('warning-background');
	jQuery(selector).removeClass('error-background');
	jQuery(selector).addClass(type + '-background');
	jQuery(selector).html(msg);
	jQuery(selector).css('display', 'inline-block');
};

/**
 * Nasconde eventuali messaggi di info per gli utenti
 */
StatPortalOpenData.ODataUtility.hideMessage = function hideMessage(selector){
	jQuery(selector).html('');
	jQuery(selector).css('display', 'none');
};

/**
 * Generazione del link per i vari viewer
 */
StatPortalOpenData.ODataUtility.toggleExportLink = function toggleExportLink(){
	
	if(jQuery('#exportLinkDiv').length === 0){
		var tabSelectedIndex = $("#dataset-container").tabs('option', 'selected');
		var tabSelectedId = $("#tab_parent_ul li")[tabSelectedIndex].id;
		
		// propriet‡ della mappa
		var mapOptions = '';
		if(tabSelectedId == 'Mappa'){
			try{
				var mapOprionsObj = document.getElementById('mapViewer').contentWindow.getMapOption();
				
				// se si tratta di file KML
				var kmlOpt = mapOprionsObj.KML_fileURL;
				if(	typeof(kmlOpt) != 'undefined' && kmlOpt != 'null' && kmlOpt != '' ){
					mapOptions = '&KML_fileURL=' + kmlOpt;
				}else{
					// recuperiamo variabili, dimensioni ed eventuali filtri
					var meas = jQuery('#geoviewerMeasures').val();
					var territorialDim = '';
					var temporalDim = '';
					var temporalFilterVal = '';
						
					var temporalLabel = jQuery('#ddl-temporal-label');
					var temporalValue = jQuery('#ddl-temporal-value');
					var filters = '';
					
					if(meas != StatPortalOpenData.GeoViewer.GEOMETRY_VALUE){
						territorialDim = '&territorialDim=' + jQuery('#geoviewerDimensions').val();
					}
					
					if(temporalLabel.length > 0 && temporalValue.length > 0){
						temporalDim = temporalLabel.val();
						temporalFilterVal = temporalValue.val();
						filters = '&temporalDim=' + temporalDim + '&temporalFilterVal=' + temporalFilterVal;
					}
					
					var borderOpt = '';
					if(	typeof(mapOprionsObj.borderColor != 'undefined') && mapOprionsObj.borderColor != 'null' &&
						typeof(mapOprionsObj.borderStyle != 'undefined') && mapOprionsObj.borderStyle != 'null' && 
						typeof(mapOprionsObj.borderWidth != 'undefined') && mapOprionsObj.borderWidth != 'null'){
						borderOpt = '&borderColor=' + mapOprionsObj.borderColor +
									'&borderStyle=' + mapOprionsObj.borderStyle +
									'&borderWidth=' + mapOprionsObj.borderWidth;
					}
					
					mapOptions = 	'&opacity=' + mapOprionsObj.opacity + 
									'&mapType=' +  mapOprionsObj.mapType + 
									'&numClasses=' + mapOprionsObj.numClasses + 
									'&paletteName=' + mapOprionsObj.paletteName + 
									borderOpt +
									'&measure=' + meas + 
									territorialDim + 
									filters;
				}
			}catch(err){
			}
		}
		
		jQuery('#dataset-container').append('<div id="exportLinkDiv" style="text-align: center;font-weight: bold;padding:5px;position: absolute; right:25px; top:-40px; height:60px;background: white;border: 1px solid silver;">Copia e incolla questo link<br/><input id="exportLinkTxt" size="60" type="text" value="' + location.protocol + '//' + location.host + location.pathname + '?t=' + tabSelectedId + mapOptions + '" /></div>');
		jQuery('#exportLinkTxt').select();	
		// bind del click su tutta la pagina
		window.setTimeout("jQuery(document).bind('click', StatPortalOpenData.ODataUtility.clickOutExportLinkHandler);", 200);
	}else{
		StatPortalOpenData.ODataUtility.unbindAndRemoveExportLink();
	}
	
};

/**
 * Gestione del click esterno al pannello del contenitore dell'url
 */
StatPortalOpenData.ODataUtility.clickOutExportLinkHandler = function clickOutExportLinkHandler(event){
	var srcElementId = event.srcElement.id;
	if(srcElementId !== 'exportLinkTxt' && srcElementId !== 'exportLinkDiv' && jQuery('#exportLinkDiv').length !== 0){
		StatPortalOpenData.ODataUtility.unbindAndRemoveExportLink();
	}
};


StatPortalOpenData.ODataUtility.unbindAndRemoveExportLink = function unbindAndRemoveExportLink(){
	$(document).unbind('click', StatPortalOpenData.ODataUtility.clickOutExportLinkHandler);
	jQuery("#exportLinkDiv").remove();
};

/**
 * Reimposta a dimensione normale il viewer attivo
 */
StatPortalOpenData.ODataUtility.toNormalSize = function toNormalSize(){
	jQuery('#closeFullscreen').remove();
	jQuery(document).unbind("keyup", StatPortalOpenData.ODataUtility.keyUpEscListener);
	
	if (state.isModuleActive(StatPortalOpenData.Enums.ModuleId.GRID)) {		
		StatPortalOpenData.MultidimensionalViewer.toNormalSize();
	} else if (state.isModuleActive(StatPortalOpenData.Enums.ModuleId.MAP)) {
		StatPortalOpenData.GeoViewer.toNormalSize();
	} else if (state.isModuleActive(StatPortalOpenData.Enums.ModuleId.GRAPH)) {
		StatPortalOpenData.GraphViewer.toNormalSize();
	} else if (state.isModuleActive(StatPortalOpenData.Enums.ModuleId.MAP3D)) {
		StatPortalOpenData.EarthViewer.toNormalSize();
	}
};

/**
 * Listener per l'evento "tasto esc premuto", utilizzato per reimpostare le dimensioni normali ai viewer
 */
StatPortalOpenData.ODataUtility.keyUpEscListener = function keyUpEscListener(e){
	if (e.keyCode == 27) {
		StatPortalOpenData.ODataUtility.toNormalSize();	
	}   
};
	
	
/**
 * Funzione che ridimensiona la griglia
 *
 * @param gridId	{String}	id della griglia da ridimensionare
 */
StatPortalOpenData.ODataUtility.resizeGrid = function resizeGrid(gridId) {
	gridId = gridId || 'list';

	var gridHeight = ($('#ODataGridContainer').length > 0 && $('#ODataGridContainer').height() > 0) ? $('#ODataGridContainer').height() : $(window).height() - 150;
	var gridWidth = ($('#ODataGridContainer').length > 0 && $('#ODataGridContainer').width() > 0) ? $('#ODataGridContainer').width() : $(window).width() - 20;
	
	jQuery('#' + gridId).setGridWidth(gridWidth);
	jQuery('#' + gridId).setGridHeight(gridHeight);
};


/**
 *  Inizializza i viewer
 */
StatPortalOpenData.ODataUtility.initViews = function initViews(mIdData) {
		
	StatPortalOpenData.ODataUtility.initializeActiveModules(state.getColumnStructure(), mIdData);
};


/**
 * Inizializza i moduli attivi
 */
StatPortalOpenData.ODataUtility.initializeActiveModules = function initializeActiveModules(columnStructure, mIdData) {
	jQuery('#viewer_img_button').css('visibility', 'visible'); 
	if(state.isModuleActive(StatPortalOpenData.Enums.ModuleId.GRID)){
		StatPortalOpenData.ODataUtility.initColumnSelector();
		StatPortalOpenData.ODataUtility.initGrid(state.getServiceUri(), state.getResourcePath(), StatPortalOpenData.ODataUtility.composeParameters(), columnStructure.columnNames, columnStructure.columnModel, state.areIncludedNavigationProperties());
		// si inizializza in controllo della selezione delle colonne
      }
      
      if(state.isModuleActive(StatPortalOpenData.Enums.ModuleId.MAP)){
        StatPortalOpenData.GeoViewer.initMap(mIdData);  
      }
      
      if(state.isModuleActive(StatPortalOpenData.Enums.ModuleId.GRAPH)){
      	StatPortalOpenData.GraphViewer.initGraphController(); 
        }
};

/**
 * Attiva la funzione di aggregazione nel controllo delle colonne della griglia quando necessario. 
 * In particolare se c'Ë almeno una dimensione non checkata si attivano le funzioni di aggregazione
 * per le misure e si disabilitano le variabili descrittive (sulle quali non Ë possibile fare delle 
 * statistiche)
 */
StatPortalOpenData.ODataUtility.activeAggregationFunctionIfNecessary = function activeAggregationFunctionIfNecessary() {

	var numCheckedDimension = 0;
	var numDimension = jQuery("input.dimension:checkbox").length;
	
	$("input.dimension:checkbox").each(function() {
		if(this.checked) {
			numCheckedDimension++;
		}
	});

	if(numCheckedDimension === 0 || numCheckedDimension === numDimension){
		// se l'utente ha selezionato 0 dimensioni (o tutte) significa che 
		// si vuole vedere la tabella senza raggruppamenti
		$('.aggregationFun').css('visibility', 'hidden');
		$('.genericColumn').removeAttr("disabled");
	}else{
		// si rendono visibili le funzioni di aggregazione
		$('.aggregationFun').css('visibility', 'visible');
		// si disabilitano le variabili descrittive
		$('.genericColumn').attr("disabled", true);
	}
	
};

/**
 * Permette di inserire una nuova colonna negli oggetti columnNames e columnModel passati come parametri
 *
 * @param columnNames {Array} insieme dei nomi delle colonne
 * @param columnModel {Array} insieme delle strutture delle singole colonne
 * @param name {String} nome della colonna
 * @param alias {String} alias della colonna
 * @param search {boolean} se true risulta attiva la ricerca sulla colonna
 *
 */
StatPortalOpenData.ODataUtility.insertColumn = function insertColumn(columnNames, columnModel, name, alias, search, formatter, sortable) {
	
	if(typeof(sortable) === 'undefined'){
		sortable = true;
	}
	
	if(typeof(formatter) === 'undefined'){
		formatter = 'none';
	}
	
	// si mette il formatter di default
	if(formatter == 'none'){
		formatter = StatPortalOpenData.ODataUtility.defaultFormatter;
	}
	
	if(alias.toUpperCase() == 'DO_X'){
		alias = 'Longitudine';
	}else if(alias.toUpperCase() == 'DO_Y'){
		alias = 'Latitudine';
	}
	
	columnNames.push(alias.replace("'", " "));
	columnModel.push({
		name : name,
		index : name,
		editable : false,
		formatter : formatter,
		search : search,
		sortable : sortable,
		searchoptions : {}
	});
};

/**
 * Inizializza l'ODataGrid e si registra agli eventi necessari per i successivi aggiornamenti di stato
 *
 * @param serviceUri {String} url del servizio OData
 * @param resourcePath {String} path della risorsa OData
 * @param GBParameters {String} eventuali parametri da aggiungere in fase di inizializzazione
 * @param columnNames {Array} insieme dei nomi delle colonne, se gi√† conosciuto; altrimenti in caso di null si recupera le colonne dai metadati del servizio OData
 * @param columnModel {Array} insieme di strutture delle colonne, se gi√† conosciuto; altrimenti in caso di null si recupera le colonne dai metadati del servizio OData
 * @param includeNavigationProperties {boolean} indica se si vogliono visualizzare le navigationProperties OData (ovvero se si vogliono navigare i dati dalla griglia)
 *
 */
StatPortalOpenData.ODataUtility.initGrid = function initGrid(serviceUri, resourcePath, GBParameters, columnNames, columnModel, includeNavigationProperties) {

	StatPortalOpenData.eventsManager.unsubscribe(StatPortalOpenData.ODataUtility.ODataEvent_columnChanged_Grid_Handler, 'ODataEvent_columnChanged');

	$('#list').ODataGrid({
		serviceUri : serviceUri,
		resourcePath : resourcePath,
		gbparameters : GBParameters,
		jsonMetadata : state.getJsonMetadata(),
		crossDomainProxy : state.getCrossDomainProxy(),
		includeNavigationProperties : includeNavigationProperties,
		isCustomService : state.isStructuredData(),
		columnNames : columnNames,
		columnModel : columnModel
	});

	StatPortalOpenData.eventsManager.subscribe(StatPortalOpenData.ODataUtility.ODataEvent_columnChanged_Grid_Handler, 'ODataEvent_columnChanged');
};

StatPortalOpenData.ODataUtility.removeInitialLoading = function removeInitialLoading(){
	$('#initial_viewer_loading').removeClass('initial_viewer_loading');
	$('#dataset-container').removeClass('initial_metadata_loading');
	$('#metadata-header').removeClass('initial_metadata_loading');
};

StatPortalOpenData.ODataUtility.indexForTabs;

StatPortalOpenData.ODataUtility.selectedTabHandler = function selectedTabHandler(){ 
	   // si deve togliere la visualizzazione del link
	   StatPortalOpenData.ODataUtility.unbindAndRemoveExportLink();
	   
	   if($('#graph_viewer_loading').length > 0){
		   $('#graph_viewer_loading').show();
	   }
	   else if($('#geo_viewer_loading').length > 0){
		   $('#geo_viewer_loading').show();
	   }
	   else if($('#multidimensional_viewer_loading').length !== 0){
			$('#multidimensional_viewer_loading').show();
	   }
	   else if($('#earth_viewer_loading').length !== 0){
			$('#earth_viewer_loading').show();
	   }
	   
	   try{
		   var tabSelectedId = $("#tab_parent_ul li")[StatPortalOpenData.ODataUtility.indexForTabs].id;
		   //window.location.hash = '#v=' + tabSelectedId;
		   if(tabSelectedId === 'Tabella' || tabSelectedId === 'Mappa' || tabSelectedId === 'Mappa3D' || tabSelectedId === 'Grafico'){
			   $('#viewer_img_button').css('visibility', 'visible'); 
		   }
		   else {
			   $('#viewer_img_button').css('visibility', 'hidden');					   
		   }
	   }catch(Exception){
		   $('#viewer_img_button').css('visibility', 'hidden');
	   }
};

/**
 * Gestione del caricamento dello Stato del dato (misure, dimensioni, colonne generiche, ...)
 */
StatPortalOpenData.ODataUtility.loadState = function loadState(mIdData){
	
	StatPortalOpenData.ODataUtility.waitingFromClick = false;
	
	// si inizializza il tab
	$("#dataset-container").tabs({
		   selected: 0,
		   select: function(event, ui) {
			   // si controlla se Ë finito il caricamento, altrimenti ci mettiamo in attesa dell'evento 
			   // che ci indica il completamento del caricamento
			   StatPortalOpenData.ODataUtility.indexForTabs = ui.index;
			   //StatPortalOpenData.utility.log('Selezionato un tab');
			   if(!StatPortalOpenData.ODataUtility.loadStateCompleted){
				   //StatPortalOpenData.utility.log('Stato non ancora caricato');
				   StatPortalOpenData.ODataUtility.waitingFromClick = true;
				   // ci mettiamo in attesa
				   $('body').bind('loadStateCompleted', function() {
					   //StatPortalOpenData.utility.log('Evento bindato');
					   $('#dataset-container').tabs({ selected: StatPortalOpenData.ODataUtility.indexForTabs});
						// si rimuove il loading
						StatPortalOpenData.ODataUtility.removeInitialLoading();
						// si fa quello che si dovrebbe fare
						StatPortalOpenData.ODataUtility.selectedTabHandler();
						StatPortalOpenData.ODataUtility.waitingFromClick = false;
					});
			   }else{
				   //StatPortalOpenData.utility.log('Stato gi‡ caricato');
				   // si fa direttamente quello che si dovrebbe fare
				   StatPortalOpenData.ODataUtility.selectedTabHandler();
			   }
		   }
	});
	
	// se non si tratta di un link diretto a un tab si toglie il loading
	var initializationTabByLink = StatPortalOpenData.ODataUtility.getURLParameter('t');
	if(initializationTabByLink == 'null' || initializationTabByLink == 'Descrizione' || initializationTabByLink == 'Scarica' || initializationTabByLink == 'Commenti'){
		StatPortalOpenData.ODataUtility.removeInitialLoading();
	}
	
	StatPortalOpenData.dataUid = mIdData;
	
	if(typeof(mIdData) === 'undefined' || mIdData == '-1'){
		alert(StatPortalOpenData.LOAD_ERROR);
		jQuery('#Tabella').remove();
		jQuery('#Scarica').remove();
		jQuery('#Grafico').remove();
		jQuery('#Mappa2D').remove();
		jQuery('#Mappa3D').remove();
		return;
	}
	
	var configOptions, config, stateOptions;
	// INIZIALIZZAZIONE DELLO STATO
	var resourcePath = StatPortalOpenData.RESOURCE_PATH;
	var includeNavigationProperties = false;
	
	configOptions = {
		dataType : /*StatPortalOpenData.Enums.DataType.MD_DATA*/0,
		crossDomainProxy : '',
		includeNavigationProperties : includeNavigationProperties,
		serviceUri : StatPortalOpenData.ODataServices.ODATA4J_SERVICE,
		resourcePath : resourcePath, 
		dataId : mIdData
	};
	config = new StatPortalOpenData.Configuration(configOptions);

	stateOptions = {
		configuration : config,
		modules : []
	};
	state = new StatPortalOpenData.State(stateOptions);
	
	
	var i, max, dimension, measure, columnNames = null, columnModel = null, mdData;
	
	if(state.isStructuredData()) {
		// togliamo l'eventuale DataRows
		mdData = state.getResourcePath();
		if(state.getResourcePath().indexOf('/DataRows') != -1) {
			mdData = state.getResourcePath().replace('/DataRows', '');
		}
		columnNames = [];
		columnModel = [];

		OData.defaultHttpClient.enableJsonpCallback = true;
		
		OData.read(state.getServiceUri() + mdData , function(data) {
		// si legge la tipologia del dato e si setta nello stato
			state.setDataType(data.idLuDataType);	

		// chiediamo le Dimensioni
		OData.read(state.getServiceUri() + mdData + '/DataDimensions', function(dimensions) {

			for( i = 0, max = dimensions.results.length; i < max; i++) {
				dimension = dimensions.results[i];
				state.addDimension(new StatPortalOpenData.Dimension({
					alias : dimension.alias,
					name : StatPortalOpenData.ODataUtility.getUniqueIdentifier(dimension.alias),
					aggregated : false,
					id : dimension.id,
					cardinality : dimension.cardinality,
					dimType : dimension.dimType,
					differentDistinctCount : dimension.differentDistinctCount
				}));
			}
			//StatPortalOpenData.utility.log('Caricamento dimensioni completato');
			// chiediamo le Misure
			OData.read(state.getServiceUri() + mdData + '/DataMeasures', function(measures) {
				var columnStructure;

				for( i = 0, max = measures.results.length; i < max; i++) {
					measure = measures.results[i];
					state.addMeasure(new StatPortalOpenData.Measure({
						alias : measure.alias,
						name : StatPortalOpenData.ODataUtility.getUniqueIdentifier(measure.alias),
						visible : true,
						aggFun : '',
						id : measure.id
					}));
				}
				//StatPortalOpenData.utility.log('Caricamento misure completato');
				// chiediamo la struttura delle colonne
				OData.read(state.getServiceUri() + state.getResourcePath().replace('/DataRows', '/DataColumns'), function(columns) {
					var results = columns.results;
					
					var dataType = state.getDataType();
					
					columnStructure = StatPortalOpenData.ODataUtility.getColumnsStructure(results);
					state.setColumnStructure(columnStructure);
					
					for( i = 0, max = results.length; i < max; i++) {
						var columnI = results[i];
						if(columnI.columnType === "OTHER" || columnI.columnType === "GENERIC_COLUMN"){
							
							if(columnI.logicalName.toUpperCase() === 'DO_X'){
								StatPortalOpenData.ODataUtility.streetViewX = StatPortalOpenData.ODataUtility.getUniqueIdentifier(columnI.logicalName);
							}else if(columnI.logicalName.toUpperCase() === 'DO_Y'){
								StatPortalOpenData.ODataUtility.streetViewY = StatPortalOpenData.ODataUtility.getUniqueIdentifier(columnI.logicalName);
							}
							
							if(!(dataType === StatPortalOpenData.Enums.MdLuDataType.TERRITORIALE && (columnI.physicalName.toUpperCase() === 'THE_GEOM' || columnI.physicalName.toUpperCase() === 'GID' /*|| columnI.logicalName.toUpperCase() === 'DO_X' || columnI.logicalName.toUpperCase() === 'DO_Y'*/ || columnI.physicalName.toUpperCase() === 'DO_IND'))){
								var columnAlias = columnI.logicalName;
								state.addColumn(new StatPortalOpenData.Column({
									id : columnI.id,
									alias : columnAlias,
									name : StatPortalOpenData.ODataUtility.getUniqueIdentifier(columnI.logicalName),
									visible : true, 
									differentDistinctCount : columnI.differentDistinctCount
								}));								
							}
						}
					}
					//StatPortalOpenData.utility.log('Caricamento della struttura del dato completato');
					StatPortalOpenData.ODataUtility.initTabs();					
				}, function(err) {
					// TODO: gestione dell'errore
				});
				
			}, function(err) {
				// TODO: gestione dell'errore
			});
		}, function(err) {
			// TODO: gestione dell'errore
		});
	}, function(err) {
		// TODO: gestione dell'errore
	});
		// aggiorniamo lo stato
	} else {
		StatPortalOpenData.ODataUtility.parseMetadataAndDoSomething(state.getCrossDomainProxy(), state.getServiceUri(), state.getResourcePath(), state.areIncludedNavigationProperties(), StatPortalOpenData.ODataUtility.metadataReceivedHandler, function() {alert('error..ancora mai chiamata')
		});
	}
	
};

/**
 * Utility per controllare lo stato dell'oggetto js
 */
StatPortalOpenData.ODataUtility.isUndefinedNullOrEmpty = function isUndefinedNullOrEmpty(variable){
	return (typeof(variable) == 'undefined' || variable == undefined || variable === undefined || variable == null || variable === null || variable == 'null' || variable === 'null' || variable == '' || variable === '');
};

/**
 * Utility per controllare lo stato dell'oggetto js
 */
StatPortalOpenData.ODataUtility.isUndefinedOrNull = function isUndefinedOrNull(variable){
	return (typeof(variable) == 'undefined' || variable == undefined || variable === undefined || variable == null || variable === null || variable == 'null' || variable === 'null');
};

/**
 * Inizializzazione dei tabs
 */
StatPortalOpenData.ODataUtility.initTabs = function initTabs(){
	// leggiamo da url il tab che si desidera visualizzare
	var tabToOpen = StatPortalOpenData.ODataUtility.getURLParameter('t');
	var idToOpen = 0;
	// si cerca l'indice del tab che vogliamo che sia aperto
	$("#tab_parent_ul li").each(function(index) {
		if(this.id === tabToOpen){
			idToOpen = index;
			return false;
		}
	});
	
	StatPortalOpenData.ODataUtility.loadStateCompleted = true;
	// si seleziona il tab corretto (se non siamo in attesa causata da altri click)
	if(!StatPortalOpenData.ODataUtility.waitingFromClick){
		$('#dataset-container').tabs({ selected: idToOpen });
	}
	// si lancia l'evento a cui Ë necessario registrarsi nella selezione di un tab
	//StatPortalOpenData.utility.log('Evento di caricamento dello stato lanciato');
	$("body").trigger('loadStateCompleted');
};

/**
 * Resituisce il valore del parametro passato via get
 */
StatPortalOpenData.ODataUtility.getURLParameter = function getURLParameter(name) {
    return decodeURI(
        (RegExp(name + '=' + '(.+?)(&|$)').exec(location.search)||[,null])[1]
    );
};



/**
 * handler per quando l'utente cambia le colonne da visualizzare
 *
 * @param obj {Object} eventuale oggetto passato dal lancio dell'evento
 */
StatPortalOpenData.ODataUtility.ODataEvent_columnChanged_Grid_Handler = function ODataEvent_columnChanged_Grid_Handler(obj) {
	var myGBParameters = StatPortalOpenData.ODataUtility.composeParameters();
	columnStructure = StatPortalOpenData.ODataUtility.getColumnsStructure();
	StatPortalOpenData.ODataUtility.initGrid(state.getServiceUri(), state.getResourcePath(), myGBParameters, columnStructure.columnNames, columnStructure.columnModel, state.areIncludedNavigationProperties());
};

/**
 * Compone eventuali parametri per la group by (o select) a partire dallo stato
 *
 * @return i parametri da appendere alla richiesta OData
 *
 */
StatPortalOpenData.ODataUtility.composeParameters = function composeParameters() {
	var i, max, dimLen, measLen, genericColumnsLen, parameters = '', GBparameters = '', selectList = [], almostOneGenericColumnVisible = false;
	var dimensionsClone = state.getDimensions();
	var measuresClone = state.getMeasures();
	var columnsClone = state.getColumns();
	var numDimAggregated = 0;
	
	dimLen = state.getDimensionsCount();
	measLen = state.getMeasuresCount();
	genericColumnsLen = state.getColumnsCount();

	if(genericColumnsLen > 0){
		for( i = 0, max = genericColumnsLen; i < max; i++) {
			var genericColumnI = columnsClone[i];
			if(genericColumnI.getVisible()){
				almostOneGenericColumnVisible = true;
				if(	genericColumnI.getName() != StatPortalOpenData.ODataUtility.getUniqueIdentifier('DO_X') &&
					genericColumnI.getName() != StatPortalOpenData.ODataUtility.getUniqueIdentifier('DO_Y')){
					selectList.push(StatPortalOpenData.ODataUtility.getUniqueIdentifier(genericColumnI.getAlias()));
				}
			}
		}
	}

	// nel caso di shape file si aggiungono anche le coordinate (x streetView)
	if(state.getDataType() === StatPortalOpenData.Enums.MdLuDataType.TERRITORIALE && typeof(StatPortalOpenData.ODataUtility.streetViewX) !== 'undefined' && typeof(StatPortalOpenData.ODataUtility.streetViewY) !== 'undefined'){
		selectList.push(StatPortalOpenData.ODataUtility.streetViewX);
		selectList.push(StatPortalOpenData.ODataUtility.streetViewY);
	}
	
	parameters = '$select=' + selectList.join();

	var aggregation = false;
	if(dimLen > 0){
		GBparameters += 'dimensionList=';
		// si scorrono tutte le dimensioni 
		for( i = 0, max = dimLen; i < max; i++) {
			var dimI = dimensionsClone[i];
			if(!dimI.getAggregated()) {
				GBparameters += dimI.getId() + ';';
				parameters += ',' + dimI.getName();
			}else{
				numDimAggregated++;
				aggregation = true;
			}
		}
	}
	
	if(dimensionsClone.length === numDimAggregated){
		aggregation = false;
	}
	
	if(measLen > 0){
		if(dimLen > 0){
			if(GBparameters.substr(-1) === ";") {
				GBparameters = GBparameters.substring(0, GBparameters.length - 1);
			}
			GBparameters += '&';
		}
		GBparameters += 'measureList=';
		// si scorrono tutte le misure
		for( i = 0, max = measLen; i < max; i++) {
			var measI = measuresClone[i];
			if(measI.getVisible()) {
				GBparameters += measI.getId() + ':' + measI.getAggFun() + ';';
				parameters += ',' + measI.getName();
			}
		}
	}
	
	// si cancella l'ultimo punto e virgola da GBparameters, se c'Ë
	if(GBparameters.substr(-1) === ";") {
		GBparameters = GBparameters.substring(0, GBparameters.length - 1);
	}
	
	parameters = parameters.replace('$select=,', '$select=');

	if(!aggregation){
		return parameters;
	}else{
		return GBparameters;
	}
};

/**
 * Suggerimento di funzioni di aggregazione in base al nome (es. se si tratta di una misura percentuale non ha senso farci la somma; proponiamo quindi la media)
 */
StatPortalOpenData.ODataUtility.aggregationFunctionSuggestedByName = function aggregationFunctionSuggestedByName(name) {
	
	var nameToLower = name.toLowerCase(); 
	if(nameToLower.indexOf('percent') == 0 || nameToLower.indexOf('indicatore') == 0 || nameToLower.indexOf('indice') == 0){
		return 'AVG';
	}
	
	return 'SUM';
};

/**
 * Inizializza il selettore di colonne
 */
StatPortalOpenData.ODataUtility.initColumnSelector = function initColumnSelector() {
	var htmlController = '<select id="columnSelector" name="columnSelector" multiple="multiple">';
	//var fieldTmp = '';
	
	var dimensions = state.getDimensions();
	$.each(dimensions, function(i, val) {
		htmlController += '<option class="field dimension" data-alias="' + val.getAlias() + '" data-id="' + val.getId() + '" name="' + val.getName() + '">' + val.getAlias() + '</option>';
	});
	
	var measures = state.getMeasures();
	$.each(measures, function(i, val) {
		var aggFunSuggested = StatPortalOpenData.ODataUtility.aggregationFunctionSuggestedByName(val.getAlias());
		htmlController += '<option class="field measure" data-aggfun="' + aggFunSuggested + '" data-alias="' + val.getAlias() + '" data-id="' + val.getId() + '" name="' + val.getName() + '">' + val.getAlias() + '</option>';
	});
	
	var columns = state.getColumns();
	$.each(columns, function(i, val) {
		var columnAlias = val.getAlias();
		
		if(columnAlias.toLowerCase() == 'do_x'){
			columnAlias = 'Longitudine';
		}else if(columnAlias.toLowerCase() == 'do_y'){
			columnAlias = 'Latitudine';	
		}
		if(/*columnAlias.toLowerCase() != 'do_x' && columnAlias.toLowerCase() != 'do_y' && */columnAlias.toLowerCase() != 'do_ind'){
			htmlController += '<option class="field genericColumn" data-id="' + val.getId() + '" name="' + val.getName() + '">' + columnAlias + '</option>';
		}
	});
	
	htmlController += '</select>';
	$("#controller").html(htmlController);
	$("#columnSelector").multiselect({
		selectedText : "Colonne visibili: # di #"
	});
	$("#columnSelector").multiselect("checkAll");
};



/**
 * Applica i cambiamenti effettuati attraverso il selettore di colonne.
 * Una volta aggiornato lo stato lancia gli opportuni eventi
 *
 */
StatPortalOpenData.ODataUtility.applyControllerChange = function applyControllerChange() {
	var /*newDimensions = [], newMeasures = [], newColumns = [],*/ almostOneDimensionVisible = false, almostOneMeasureVisible = false, almostOneFieldVisible = false;
	// si scorrono tutte le dimensioni
	var doAggregation = false;

	// dimensioni
	$("input.dimension:checkbox").each(function() {
		var dimensionChecked = this.checked;
		if(!dimensionChecked) {
			doAggregation = true;
		}
		
		state.setDimensionAggregation(this.name, !dimensionChecked);
		almostOneDimensionVisible = almostOneDimensionVisible || dimensionChecked;
	});
	// misure
	$("input.measure:checkbox").each(function() {
		var funAgg = $("SELECT[name='" + this.name + "']").val(), measureVisible = this.checked;
		var aggFunToSet = (doAggregation && this.checked) ? funAgg : '';
		state.setMeasureVisibilityAndAggregation(this.name, measureVisible, aggFunToSet);
		almostOneMeasureVisible = almostOneMeasureVisible || measureVisible;
	});
	// colonne
	$("input.genericColumn:checkbox").each(function() {
		var fieldVisible = this.checked;
		
		state.setColumnVisibility(this.name, fieldVisible);
		almostOneFieldVisible = almostOneFieldVisible || fieldVisible;
	});
	
	if(!almostOneFieldVisible && !almostOneMeasureVisible && !almostOneDimensionVisible){
		alert('Deve essere selezionato almeno un campo');
	} else {
		StatPortalOpenData.eventsManager.trigger({}, 'ODataEvent_columnChanged');
	}
};

/**
 * Ricava la nuova struttura delle colonne
 *
 * @return {Object} oggetto con 2 propriet√†: columnNames e columnModel, che rappresentano la struttura delle colonne
 */
StatPortalOpenData.ODataUtility.getColumnsStructure = function getColumnsStructure(dataColumnsResult) {
	
	var columnNames = [], columnModel = [];
	
	// inizializzazione della prima colonna dedicata allo streetview
	if(typeof(dataColumnsResult) !== 'undefined'){
		var idxCoord = 0, contains_do_x = false, contains_do_y = false, counterMax = dataColumnsResult.length;
		for( idxCoord = 0; idxCoord < counterMax; idxCoord++) {
			if(dataColumnsResult[idxCoord].logicalName.toLowerCase() == 'do_x'){
				contains_do_x = true;
			}else if(dataColumnsResult[idxCoord].logicalName.toLowerCase() == 'do_y'){
				contains_do_y = true;
			}
		}

		if(contains_do_x && contains_do_y){
			columnNames.push('');
			columnModel.push({
				name : '',
				index : '',
				editable : false,
				formatter : StatPortalOpenData.ODataUtility.streetViewFormatter,
				search : false,
				sortable : false,
				searchoptions : {},
				width : 40
			});
			
		}
	}
	
	if(state.isStructuredData()){
	
		if(typeof(dataColumnsResult) !== 'undefined'){
			// va composta la struttura
			var counter = 0;
			var counterMax = dataColumnsResult.length;
			
			for( counter = 0; counter < counterMax; counter++) {
				var columnI = dataColumnsResult[counter];
				var activeSearch = (columnI.columnType !== "MEASURE");
				var formatter = (columnI.columnType === "MEASURE") ? StatPortalOpenData.ODataUtility.measureFormatter : 'none';
				if(!(/*state.getDataType() === StatPortalOpenData.Enums.MdLuDataType.TERRITORIALE &&*/ ((columnI.physicalName.toUpperCase() === 'THE_GEOM' || columnI.physicalName.toUpperCase() === 'GID' || /*columnI.logicalName.toUpperCase() === 'DO_X' || columnI.logicalName.toUpperCase() === 'DO_Y' || */columnI.physicalName.toUpperCase() === 'DO_IND')))){
					var logicalName = columnI.logicalName;
					StatPortalOpenData.ODataUtility.insertColumn(columnNames, columnModel, StatPortalOpenData.ODataUtility.getUniqueIdentifier(columnI.logicalName), logicalName, activeSearch, formatter);
				}
			}
			
		}else{
			
			var isAggregation = state.isAggregation();
			if(isAggregation){
				// si mettono prima le dimensioni (visibili)
				var dimensionsForAggregation = state.getDimensions();
				var dimensionsForAggregationLen = state.getDimensionsCount();
				var dimensionsForAggregationIdx = 0;
				for(dimensionsForAggregationIdx = 0; dimensionsForAggregationIdx < dimensionsForAggregationLen; dimensionsForAggregationIdx++){
					var dimensionForAggregation = dimensionsForAggregation[dimensionsForAggregationIdx];
					if(dimensionForAggregation.getVisible()){
						// si inserisce la dimensione
						StatPortalOpenData.ODataUtility.insertColumn(columnNames, columnModel, StatPortalOpenData.ODataUtility.getUniqueIdentifier(dimensionForAggregation.getAlias()), dimensionForAggregation.getAlias(), true, 'none');
					}
				}
				
				// poi le misure (visibili)
				var measuresForAggregation = state.getMeasures();
				var measuresForAggregationLen = state.getMeasuresCount();
				var measuresForAggregationLenIdx = 0;
				for(measuresForAggregationLenIdx = 0; measuresForAggregationLenIdx < measuresForAggregationLen; measuresForAggregationLenIdx++){
					var measureForAggregation = measuresForAggregation[measuresForAggregationLenIdx];
					if(measureForAggregation.getVisible()){
						StatPortalOpenData.ODataUtility.insertColumn(columnNames, columnModel, StatPortalOpenData.ODataUtility.getUniqueIdentifier(measureForAggregation.getAlias()), StatPortalOpenData.ODataUtility.getAggFunMapping(measureForAggregation.getAggFun()) + ' DI ' +measureForAggregation.getAlias(), false, StatPortalOpenData.ODataUtility.measureFormatter);
					}
				}
				
			}else{
				// recupero la struttura dall'oggetto state
				var columnsStructure = state.getColumnStructure();
				var idxCS, CSlen = columnsStructure.columnNames.length;
				// si legge dallo stato quello che Ë visibile e quello che non Ë visibile
				
				for(idxCS=0; idxCS < CSlen; idxCS++){
					var name = columnsStructure.columnModel[idxCS].index;
					var column = state.getColumnByName(name);
					if(column !== null && column.getVisible()){
						// se si tratta di colonna generica si mette solo se non siamo nel caso di aggregazione
						if(!((column instanceof StatPortalOpenData.Column) && isAggregation)){
							var activeSearch = !(column instanceof StatPortalOpenData.Measure);
							var formatter = (column instanceof StatPortalOpenData.Measure) ? StatPortalOpenData.ODataUtility.measureFormatter : 'none';
							StatPortalOpenData.ODataUtility.insertColumn(columnNames, columnModel, StatPortalOpenData.ODataUtility.getUniqueIdentifier(column.getAlias()), column.getAlias(), activeSearch, formatter);
						}
					}
				}
			}
		}
		
	}else{
		
		genericColumnsLen = state.getColumnsCount();
		var columnsClone = state.getColumns();
		if(genericColumnsLen > 0) {
			var dataType = state.getDataType();
			for( i = 0, max = genericColumnsLen; i < max; i++) {
				var fieldI = columnsClone[i];
				if(fieldI.visible) {
					StatPortalOpenData.ODataUtility.insertColumn(columnNames, columnModel, fieldI.name, fieldI.alias, false);
				}
			}
		} else {
			alert('colonne vuote');
		}

	}
	
	return {
		columnNames : columnNames,
		columnModel : columnModel
	}
	
};

/**
 * Gestione del mapping tra identificativo della funzione di aggregazione e nome da visualizzare
 */
StatPortalOpenData.ODataUtility.getAggFunMapping = function getAggFunMapping(aggFun){
	switch (aggFun) {
	   case 'SUM':
		   return 'SOMMA';
	   case 'AVG':
		   return 'MEDIA';
	   case 'MIN':
		   return 'MINIMO';
	   case 'MAX':
		   return 'MASSIMO';
	}
	
	return aggFun;
}


/**
 * Handler per la ricezione dei metadati. Aggiorna lo stato
 */
StatPortalOpenData.ODataUtility.metadataReceivedHandler = function metadataReceivedHandler(cNames, cModel, isColl) {
	var cI, cMax, column;

	for( cI = 0, cMax = cModel.length; cI < cMax; cI++) {
		column = cModel[cI];
		state.addColumn({
			name : column.name
		});
	}

	StatPortalOpenData.ODataUtility.initColumnSelector();
	StatPortalOpenData.ODataUtility.initGrid(state.getServiceUri(), state.getResourcePath(), '', cNames, cModel, state.areIncludedNavigationProperties());

};

/**
 * Ricava l'identificatore unico stabilito con Marco (campo esposto da OData4J)
 * @param name {String} nome dal quale vogliamo ricavare un identificativo
 * @return {String} un identificativo unico per il nome passato
 */
StatPortalOpenData.ODataUtility.getUniqueIdentifier = function getUniqueIdentifier(name) {	
	var hashCodeStr = Math.abs(StatPortalOpenData.utility.getHashCode(name)).toString();
	var prefix = 'C' + name.replace(RegExp('[^\\w^0-9]', 'g'), '_') + '_';
	var candidateName = prefix.substring(0, Math.min(prefix.length, 60 - hashCodeStr.length)) + hashCodeStr;
	return candidateName;
};

/**
 * Formatter per lo streetview
 */
StatPortalOpenData.ODataUtility.streetViewFormatter = function streetViewFormatter(cellvalue, options, rowObject) {
	var my_x = rowObject[StatPortalOpenData.ODataUtility.streetViewX];
	var my_y = rowObject[StatPortalOpenData.ODataUtility.streetViewY];
	if(typeof(my_y) !== 'undefined' && typeof(my_x) !== 'undefined'){
		if((Number(my_y) == 0 && Number(my_x) == 0) || (my_y == '' && my_x == '')){
			return '';
		}
		return '<a title="Vedi su Google Maps" href="http://maps.google.it/maps?q=' + my_y + ',' + my_x + '&z=18&layer=c&cbll=' + my_y + ',' + my_x + '&cbp=11,0,0,0,0" target="_blank"><img alt="Vedi su Google Maps" height="25" width="25" style="vertical-align:middle" src="/sites/all/modules/spodata/metadata/viewer/multidimensional_viewer/img/streetView.png" /></a>';	
	}
	
	return '';
};

/**
 * Formatter di default (serve per formattare correttamente links e email)
 */
StatPortalOpenData.ODataUtility.defaultFormatter = function defaultFormatter(cellvalue, options, rowObject){
	// si controlla se si tratta di link/mail
	return StatPortalOpenData.ODataUtility.getTextWithLinksAndMailFormatted(cellvalue);
	return cellvalue;
};

/**
 * Espressione regolare per controllare la validit‡ di una mail
 */
StatPortalOpenData.ODataUtility.isValidMail = function isValidMail(email) { 
    var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(email);
};

/**
 * Formatta una stringa per il viewer considerando link e mail presenti
 */
StatPortalOpenData.ODataUtility.getTextWithLinksAndMailFormatted = function getTextWithLinksAndMailFormatted(text) { 
	
	var linkWithoutHttpExp =  /((^|(\s)+)(www\.)[-A-Z0-9+&@#\/%?=~_|!:,.;]+\.[-A-Z0-9+&@#\/%=~_|]+)/ig;
	text =  text.replace(linkWithoutHttpExp,'http://' + jQuery.trim('$1'));
	
	var linkWithoutHttpExp2 =  /(http:\/\/ www)/ig;
	text =  text.replace(linkWithoutHttpExp2,' http://www');
	
	var linkExp = /(\b(https?|ftp|file):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
	text =  text.replace(linkExp,'<a style="color:blue;" target="_new" href="$1">$1</a>');

	var mailExp = /(\b(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,})))/;
	text =  text.replace(mailExp,'<a style="color:blue;" href="mailto:$1">$1</a>');
	
	return text;
};


/**
 *
 * Formatta le celle che contenenti link ad altri oggetti OData
 *
 * @param	{Object}	cellvalue	valore della cella
 * @param	{Object}	options		opzioni
 * @param	{Object}	rowObject	rowObject
 *
 * @return 	{String} la stringa che rappresenta un link OData
 */
StatPortalOpenData.ODataUtility.linkFormatter = function linkFormatter(cellvalue, options, rowObject) {

	var serviceUri = state.getServiceUri(), crossDomainProxy = state.getCrossDomainProxy(), resourcePath = state.getResourcePath(), ret = 'Object';

	if( typeof (cellvalue) !== 'undefined' && typeof (cellvalue.uri) !== 'undefined') {
		resourcePath = cellvalue.uri.replace(serviceUri, "");
		ret = "<a href='javascript:void(0);' onclick=\"$('#" + options.gid + "').ODataGrid({serviceUri : '" + StatPortalOpenData.utility.applyApexPatch(serviceUri) + "', resourcePath : '" + StatPortalOpenData.utility.applyApexPatch(resourcePath) + "', crossDomainProxy: '" + StatPortalOpenData.utility.applyApexPatch(crossDomainProxy) + "'})\";>" + cellvalue.type + "</a>";
	} else if( typeof (cellvalue) !== 'undefined' && typeof (cellvalue.__deferred) !== 'undefined') {
		resourcePath = cellvalue.__deferred.uri.replace(serviceUri, "");
		ret = "<a href='javascript:void(0);' onclick=\"$('#" + options.gid + "').ODataGrid({serviceUri : '" + StatPortalOpenData.utility.applyApexPatch(serviceUri) + "', resourcePath : '" + StatPortalOpenData.utility.applyApexPatch(resourcePath) + "', crossDomainProxy: '" + StatPortalOpenData.utility.applyApexPatch(crossDomainProxy) + "'})\";>" + resourcePath + "</a>";
	}
	return ret;
};

/**
 * Formatter per le misure
 */
StatPortalOpenData.ODataUtility.measureFormatter = function measureFormatter(cellvalue, options, rowObject) {
	if(cellvalue === null || cellvalue === 'undefined' || typeof(cellvalue) === 'undefined'){
		return '';
	}
	var cellValueCopy = cellvalue;
	
	var ret = '<div class="alignRight">' + StatPortalOpenData.ODataUtility.formatNumber(cellValueCopy) + '</div>';
	return ret;
};

/**
 * Formatter per i numeri (con separatore di migliaia e decimali)
 */
StatPortalOpenData.ODataUtility.formatNumber = function formatNumber(numberToFormat) {
	return StatPortalOpenData.ODataUtility.addSeparatorsNF(numberToFormat, '.', ',', '.');
};

/**
 * Utility per la formattazione dei numeri
 */
StatPortalOpenData.ODataUtility.addSeparatorsNF = function addSeparatorsNF(nStr, inD, outD, sep) {
	nStr += '';
	var dpos = nStr.indexOf(inD);
	var nStrEnd = '';
	if (dpos != -1) {
		nStrEnd = outD + nStr.substring(dpos + 1, nStr.length);
		nStr = nStr.substring(0, dpos);
	}
	var rgx = /(\d+)(\d{3})/;
	while (rgx.test(nStr)) {
		nStr = nStr.replace(rgx, '$1' + sep + '$2');
	}
	return nStr + nStrEnd;
};

/**
 * Formatta le date a partire dal formato restituito da OData
 *
 * @param	{Object} cellvalue	valore della cella
 * @param 	{Object} options	opzioni
 * @param 	{Object} rowObject	rowObject
 *
 * @return 	{String} la data formattata
 *
 */
StatPortalOpenData.ODataUtility.dataFormatter = function dataFormatter(cellvalue, options, rowObject) {
	var startDate, indexOfStartDate, indexOfEndDate, date, day, ret = 'ND';

	if(cellvalue !== null) {
		startDate = 'Date(';
		indexOfStartDate = cellvalue.indexOf(startDate);
		indexOfEndDate = cellvalue.indexOf(')');
		if(indexOfStartDate !== -1 && indexOfEndDate !== -1 && indexOfEndDate > indexOfStartDate) {
			date = new Date(Number(cellvalue.substring(indexOfStartDate + startDate.length, indexOfEndDate)));
			day = date.getDate();
			ret = ((day < 10) ? '0' + day : day) + '/' + (date.getMonth() + 1) + '/' + date.getFullYear();
		}
	}
	return ret;
};

/**
 * Verifica se il tipo passato come parametro sia un numero
 *
 * @param 	{String} type tipo da verificare
 *
 * @return 	{Boolean}	true se il tipo passato √® un numero
 *
 */
StatPortalOpenData.ODataUtility.isODataNumberType = function isODataNumberType(type) {
	return (type === 'Edm.Int16' || type === 'Edm.Int32' || type === 'Edm.Int64' || type === 'Edm.Decimal' || type === 'Edm.Double' || type === 'Edm.Single' );
};

/**
 * Verifica se il tipo passato come parametro sia di tipo semplice (int, decimal, double, binary, boolean, date, string, ...)
 *
 * @param	{String} type		tipo da verificare
 *
 * @return	{Boolean}	true se il tipo passato √® di tipo semplice
 *
 */
StatPortalOpenData.ODataUtility.isODataSimpleType = function isODataSimpleType(type) {
	return (type === 'Edm.Int16' || type === 'Edm.Int32' || type === 'Edm.Int64' || type === 'Edm.Decimal' || type === 'Edm.Double' || type === 'Edm.Binary' || type === 'Edm.Boolean' || type === 'Edm.Byte' || type === 'Edm.DateTime' || type === 'Edm.DateTimeOffset' || type === 'Edm.Time' || type === 'Edm.Single' || type === 'Edm.Guid' || type === 'Edm.String');
};

/**
 * Verifica se il tipo passato come parametro sia di tipo data(DateTime, DateTimeOffset, Time)
 *
 * @param	{String} type		tipo da verificare
 *
 * @return	{Boolean}	true se il tipo passato √® di tipo data
 *
 */
StatPortalOpenData.ODataUtility.isODataDataType = function isODataDataType(type) {
	return (type === 'Edm.DateTime' || type === 'Edm.DateTimeOffset' || type === 'Edm.Time');
};

/**
 * Restituisce la funzione per la corretta formattazione del tipo passato come parametro
 *
 * @param	{String} type tipo della cella
 *
 * @return	{function} funzione che formatta correttamente il tipo passato
 *
 */
StatPortalOpenData.ODataUtility.getFormatter = function getFormatter(type) {
	var ret = 'none';

	if(StatPortalOpenData.ODataUtility.isODataDataType(type)) {
		if(type === 'Edm.DateTime') {
			ret = StatPortalOpenData.ODataUtility.dataFormatter;
		} else if(type === 'Edm.DateTimeOffset') {

		} else if(type === 'Edm.Time') {

		}
	} else if(!StatPortalOpenData.ODataUtility.isODataSimpleType(type)) {
		ret = StatPortalOpenData.ODataUtility.linkFormatter;
	}
	return ret;
};

/**
 * Restituisce l'EntityType a partire dai metadati
 *
 * @param jsonMetadata {String} metadati in formato JSON
 *
 * @return l'EntityType
 *
 */
StatPortalOpenData.ODataUtility.getEntityType = function getEntityType(jsonMetadata) {
	if( typeof (jsonMetadata.DataServices) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema.EntityType) !== 'undefined') {
		return jsonMetadata.DataServices.Schema.EntityType;
	} else if( typeof (jsonMetadata.DataServices) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[0]) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[0].EntityType) !== 'undefined') {
		return jsonMetadata.DataServices.Schema[0].EntityType;
	} else if( typeof (jsonMetadata.DataServices) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[1]) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[1].EntityType) !== 'undefined') {
		return jsonMetadata.DataServices.Schema[1].EntityType;
	}
};

/**
 * Ricava il tipo di un oggetto JS
 *
 * @param obj {Object} 	oggetto di cui si vuol sapere il tipo
 * @return {String}	Restituisce il tipo dell'oggetto passato
 *
 */
StatPortalOpenData.ODataUtility.toType = function toType(obj) {
	return ({}).toString.call(obj).match(/\s([a-zA-Z]+)/)[1].toLowerCase();
};

/**
 * Restituisce l'EntitySet a partire dai metadati
 *
 * @param jsonMetadata {String} metadati in formato JSON
 *
 * @return l'EntitySet
 *
 */
StatPortalOpenData.ODataUtility.getEntitySet = function getEntitySet(jsonMetadata) {
	if( typeof (jsonMetadata.DataServices) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema.EntityContainer) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema.EntityContainer.EntitySet) !== 'undefined') {
		return jsonMetadata.DataServices.Schema.EntityContainer.EntitySet;
	} else if( typeof (jsonMetadata.DataServices) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[1]) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[1].EntityContainer) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[1].EntityContainer.EntitySet) !== 'undefined') {
		return jsonMetadata.DataServices.Schema[1].EntityContainer.EntitySet;
	} else if( typeof (jsonMetadata.DataServices) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[0]) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[0].EntityContainer) !== 'undefined' && typeof (jsonMetadata.DataServices.Schema[0].EntityContainer.EntitySet) !== 'undefined') {
		return jsonMetadata.DataServices.Schema[0].EntityContainer.EntitySet;
	}
};

StatPortalOpenData.ODataUtility.parseMetadataAndDoSomething = function parseMetadataAndDoSomething(crossDomainProxy, serviceUri, resourcePath, includeNavigationProperties, finalCallback, finalError) {

	StatPortalOpenData.utility.requestCrossDomain({
		completeUrl : serviceUri + "$metadata",
		crossDomainProxy : crossDomainProxy,
		dataType : 'xml',
		contentType : 'xml',
		callback : function(xmlData) {
			// convertiamo i metadati in JSON
			var jsonMetadata = $.xml2json(xmlData), entitySet = StatPortalOpenData.ODataUtility.getEntitySet(jsonMetadata), entityType = StatPortalOpenData.ODataUtility.getEntityType(jsonMetadata), entitySetPresume = resourcePath, indexOfAnswer = resourcePath.indexOf('?'), lastIndexOfSlash, entitySetFound = false, lastDotIdx, isCollection, entityForComparison, entityFound = false, entity, prop, attrType, navProperties, completeUrlForDataType, columnNames = [], columnModel = [], i, max, i2, max2;

			if(indexOfAnswer !== -1) {
				// se ci sono parametri si tagliano fuori
				entitySetPresume = entitySetPresume.substring(0, indexOfAnswer);
			}
			lastIndexOfSlash = entitySetPresume.lastIndexOf('/');
			if(lastIndexOfSlash !== -1) {
				// togliamo tutto quello che c'√® prima dello slash
				entitySetPresume = entitySetPresume.substring(lastIndexOfSlash + 1, entitySetPresume.length);
			}

			// si cerca prima nell'EntitySet (per capire se √® una collezione)
			for( i = 0, max = entitySet.length; i < max; i++) {
				if(entitySet[i].Name === entitySetPresume) {
					entitySetFound = true;
					entity = entitySet[i].EntityType;
					lastDotIdx = entity.lastIndexOf('.');
					if(lastDotIdx !== -1) {
						entity = entity.substring(lastDotIdx + 1, entity.length);
					}
					break;
				}
			}
			isCollection = ( typeof (entity) !== 'undefined');
			entityForComparison = isCollection ? entity : entitySetPresume;

			for( i = 0, max = entityType.length; i < max; i++) {
				if(entityType[i].Name === entityForComparison) {
					entityFound = true;
					// si scorrono tutte le propriet√†
					prop = entityType[i].Property;

					if(!$.isArray(prop)) {
						prop = [prop];
					}

					for( i2 = 0, max2 = prop.length; i2 < max2; i2++) {
						attrType = prop[i2].Type;
						columnNames.push(prop[i2].Name);
						columnModel.push({
							name : prop[i2].Name,
							index : prop[i2].Name,
							editable : false,
							formatter : StatPortalOpenData.ODataUtility.getFormatter(attrType),
							search : ((prop[i2].Type === 'Edm.String') ? true : false),
							searchoptions : (prop[i2].Type !== 'Edm.String') ? {} : {
								sopt : ['eq']
							}
						});
					}

					// se richiesto si aggiungono anche le navigationProperties
					if(includeNavigationProperties === true) {
						navProperties = entityType[i].NavigationProperty;

						if(!$.isArray(navProperties)) {
							navProperties = [navProperties];
						}

						for(var i2 = 0, max2 = navProperties.length; i2 < max2; i2++) {
							columnNames.push(navProperties[i2].Name);
							columnModel.push({
								name : navProperties[i2].Name,
								index : navProperties[i2].Name,
								editable : false,
								formatter : StatPortalOpenData.ODataUtility.linkFormatter,
								search : false,
								sortable : false,
								searchoptions : {}
							});
						}
					}
					break;
				}
			}

			// se a questo punto non si ha il modello delle colonne si deve ricavare dai dati (chiedendo il primo)
			if(columnNames.length === 0 || columnModel.length === 0) {

				OData.defaultHttpClient.enableJsonpCallback = true;
				completeUrlForDataType = serviceUri + resourcePath + '?$top=1';
				OData.read(completeUrlForDataType, function(data) {

					var objForMetadata = null, attributes, attributeType, columnNames = [], columnModel = [], i, max;

					if( typeof (data) !== 'undefined' && typeof (data.results) !== 'undefined' && typeof (data.results[0]) !== 'undefined') {
						objForMetadata = data.results[0];
						//singleData = false;
					} else if( typeof (data) !== 'undefined') {
						//singleData = true;
						objForMetadata = data;
					}

					// si recupera la lista degli attributi
					attributes = Object.keys(objForMetadata);

					for( i = 0, max = attributes.length; i < max; i++) {

						if(attributes[i] !== '__metadata') {
							attributeType = StatPortalOpenData.ODataUtility.toType(objForMetadata[attributes[i]]);

							columnNames.push(attributes[i]);
							columnModel.push({
								name : attributes[i],
								index : attributes[i],
								editable : false,
								formatter : ((attributeType === 'object') ? StatPortalOpenData.ODataUtility.linkFormatter : 'none'),
								search : ((attributeType === 'number' || attributeType === 'object' || attributeType === 'null') ? false : true),
								searchoptions : (attributeType === 'number' || attributeType === 'null') ? {} : {
									sopt : ['eq']
								}
							});
						}
					}

					if(finalCallback && typeof (finalCallback) === "function") {
						// execute the callback, passing parameters as necessary
						finalCallback(columnNames, columnModel, true);
					}
				});
			} else {
				finalCallback(columnNames, columnModel, isCollection);
			}
		}
	});

};


})(jQuery);