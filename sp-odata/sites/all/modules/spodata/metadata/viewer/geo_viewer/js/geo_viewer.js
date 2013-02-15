StatPortalOpenData.GeoViewer = StatPortalOpenData.GeoViewer || {};

var MIN_MAP_HEIGHT = 400;
StatPortalOpenData.GeoViewer.GEOMETRY_VALUE = 'geometry';
StatPortalOpenData.GeoViewer.KML_VALUE = 'kml';
StatPortalOpenData.GeoViewer.ActualDimensionValueSelected = '';

function initMapFromDrupal(id) {
	
	state.setNewAndOnlyModuleActive(StatPortalOpenData.Enums.ModuleId.MAP);
	
	// inizializzazione di trasparenza e numero di classi della mappa
	StatPortalOpenData.GeoViewer.mapVisualParameters = {};
	StatPortalOpenData.GeoViewer.mapVisualParameters.opacity = StatPortalOpenData.ODataUtility.getURLParameter('opacity');
	StatPortalOpenData.GeoViewer.mapVisualParameters.mapType = StatPortalOpenData.ODataUtility.getURLParameter('mapType');
	StatPortalOpenData.GeoViewer.mapVisualParameters.numClasses = StatPortalOpenData.ODataUtility.getURLParameter('numClasses');
	StatPortalOpenData.GeoViewer.mapVisualParameters.paletteName = StatPortalOpenData.ODataUtility.getURLParameter('paletteName');

	StatPortalOpenData.GeoViewer.mapVisualParameters.borderColor = StatPortalOpenData.ODataUtility.getURLParameter('borderColor');
	StatPortalOpenData.GeoViewer.mapVisualParameters.borderStyle = StatPortalOpenData.ODataUtility.getURLParameter('borderStyle');
	StatPortalOpenData.GeoViewer.mapVisualParameters.borderWidth = StatPortalOpenData.ODataUtility.getURLParameter('borderWidth');
	
	StatPortalOpenData.GeoViewer.mapVisualParameters.KML_fileURL = StatPortalOpenData.ODataUtility.getURLParameter('KML_fileURL');
	
	StatPortalOpenData.GeoViewer.initializeMeasures();
	StatPortalOpenData.GeoViewer.initializeTerritorialDimensions();
	
	// si selezionano gli eventuali variabile/dimensione/filtri passati tra i parametri
	StatPortalOpenData.GeoViewer.initializeVarDimFiltersValues();
	
	StatPortalOpenData.GeoViewer.initializeTemporalDimensions(id);
	
	jQuery("#mapViewer").height(Math.max(MIN_MAP_HEIGHT, jQuery(window).height() - StatPortalOpenData.utility.getResidualTableHeight('MAP')));
	
	jQuery(window).unbind('resize', StatPortalOpenData.GeoViewer.windowResizeHandlerForGeoViewer);
	jQuery(window).bind('resize', StatPortalOpenData.GeoViewer.windowResizeHandlerForGeoViewer);	
}

/**
 * Mostra un particolare messaggio all'utente
 */
StatPortalOpenData.GeoViewer.showMessage = function showMessage(type, msg){
	StatPortalOpenData.ODataUtility.showMessage('#infoMapForUser', type, msg);
	jQuery('#geo_viewer_loading').hide();
};

/**
 * Nasconde eventuali messaggi di info per gli utenti
 */
StatPortalOpenData.GeoViewer.hideMessage = function hideMessage(){
	StatPortalOpenData.ODataUtility.hideMessage('#infoMapForUser');
};

/**
 * Inizializza i filtri della mappa
 */
StatPortalOpenData.GeoViewer.initializeVarDimFiltersValues = function initializeVarDimFiltersValues(){
	var measureSelected = StatPortalOpenData.ODataUtility.getURLParameter('measure');
	var territorialDimSelected = StatPortalOpenData.ODataUtility.getURLParameter('territorialDim');
	var temporalDimLabelSelected = StatPortalOpenData.ODataUtility.getURLParameter('temporalDim');
	
	if(!StatPortalOpenData.ODataUtility.isUndefinedNullOrEmpty(StatPortalOpenData.ODataUtility.getURLParameter('KML_fileURL'))){
		jQuery('#geoviewerMeasures').val(StatPortalOpenData.GeoViewer.KML_VALUE);
	}
	else if(!StatPortalOpenData.ODataUtility.isUndefinedNullOrEmpty(measureSelected)){
		jQuery('#geoviewerMeasures').val(measureSelected);
	}
	
	if(!StatPortalOpenData.ODataUtility.isUndefinedNullOrEmpty(territorialDimSelected)){
		jQuery('#geoviewerDimensions').val(territorialDimSelected);
		jQuery('#temporalDimensionFilter').show();
		jQuery('#geoviewerDimensions').show();
		jQuery('#map-for').show();
		StatPortalOpenData.GeoViewer.ActualDimensionValueSelected = territorialDimSelected;
	}
	
	if(!StatPortalOpenData.ODataUtility.isUndefinedNullOrEmpty(temporalDimLabelSelected)){
		jQuery('#ddl-temporal-label').val(temporalDimLabelSelected);
	}
	
	// l'inizializzazione del valore della variabile temporale è stata spostata sucessivamente 
	// (perchè il recupero dei vari valori è asincrono!
};

StatPortalOpenData.GeoViewer.windowResizeHandlerForGeoViewer = function windowResizeHandlerForGeoViewer(){
	var newHeight = Math.max(MIN_MAP_HEIGHT, jQuery(window).height() - StatPortalOpenData.utility.getResidualTableHeight('MAP'));
	if(jQuery("#mapViewer").height() != newHeight){
		jQuery("#mapViewer").height(newHeight);
	}
};

StatPortalOpenData.GeoViewer.windowResizeHandlerForGeoViewerFullScreen = function windowResizeHandlerForGeoViewerFullScreen(){
	var dimFullScreen = StatPortalOpenData.GeoViewer.getDimensionsForFullScreen();
	jQuery('#mapViewer').attr('height', dimFullScreen.height);
	jQuery('#mapViewer').attr('width', dimFullScreen.width);
};

var oldGeoViewerWidth;

StatPortalOpenData.GeoViewer.toFullScreen = function toFullScreen(){
	jQuery('#map-viewer-container').append('<img id="closeFullscreen" class="closeFullscreen" onclick="javascript:StatPortalOpenData.ODataUtility.toNormalSize();" src="/sites/all/modules/spodata/metadata/viewer/commons/img/close.png" />');
	jQuery(window).unbind('resize', StatPortalOpenData.GeoViewer.windowResizeHandlerForGeoViewer);
	jQuery(window).bind('resize', StatPortalOpenData.GeoViewer.windowResizeHandlerForGeoViewerFullScreen);
	
	jQuery('#map-viewer-container').addClass('multidimensionalFullScreen');
	oldGeoViewerWidth = jQuery('#mapViewer').attr('width');
	jQuery('#mapViewer').css('height', '');
	jQuery('#mapViewer').attr('height', StatPortalOpenData.GeoViewer.getDimensionsForFullScreen().height);
	jQuery('#mapViewer').attr('width', StatPortalOpenData.GeoViewer.getDimensionsForFullScreen().width);
	// re si faceva il reload si perdeva la tematizzazione. Mi sembra che si fosse aggiunta 
	// la riga per far si che ricaricasse per bene la mappa di sfondo (ma sembra che lo faccia lo stesso)
	//document.getElementById('mapViewer').contentDocument.location.reload(true);
};

/**
 * Gestisce la creazione della tematizzazione
 * @param thematizationLayerResult
 */
StatPortalOpenData.GeoViewer.createThematizationLayerCallback = function createThematizationLayerCallback(thematizationLayerResult){
	
	var parameters = StatPortalOpenData.GeoViewer.composeParametersForGisViewer(thematizationLayerResult, true);
    
    if(parameters !== ''){
        var mapViewer = jQuery('#mapViewer').attr('src', StatPortalOpenData.ODataServices.GEO_VIEWER + parameters);
       
        mapViewer.unbind('load');
        mapViewer.bind('load', function (){
        	// va selezionata su mappa la variabile corretta
        	var measureVal = StatPortalOpenData.GeoViewer.getMeasureSelected();
        	var measureLabel = StatPortalOpenData.GeoViewer.getMeasureTextSelected();
        	document.getElementById('mapViewer').contentWindow.doLegendClassify(measureVal, measureLabel);
        	mapViewer.unbind('load');
        });
        
    }else{
    	StatPortalOpenData.GeoViewer.printMapGeneralErrorMessage(48);
    }
};

/**
 * Gestione degli errori della mappa
 * @param errorNumber
 */
StatPortalOpenData.GeoViewer.printMapGeneralErrorMessage = function printMapGeneralErrorMessage(errorNumber){
	StatPortalOpenData.GeoViewer.showMessage('error', 'Impossibile visualizzare la mappa');
};


StatPortalOpenData.GeoViewer.createShapeLayerCallback = function createShapeLayerCallback(shapeLayerResult){
	var parameters = StatPortalOpenData.GeoViewer.composeParametersForGisViewer(shapeLayerResult, false);
	
	if (parameters !== '') {
		jQuery('#mapViewer').attr('src',
				StatPortalOpenData.ODataServices.GEO_VIEWER + parameters).load(
						function() {
							jQuery('#geo_viewer_loading').hide();
				});
	}else{
    	StatPortalOpenData.GeoViewer.printMapGeneralErrorMessage(49);
    }
	
};

StatPortalOpenData.GeoViewer.getTerritorialDimensionUid = function getTerritorialDimensionUid(){
	return jQuery('#geoviewerDimensions option:selected').val();
};

StatPortalOpenData.GeoViewer.getTemporalDimensionUid = function getTemporalDimensionUid(){
	return jQuery('#ddl-temporal-label option:selected').val();
};

StatPortalOpenData.GeoViewer.getTemporalDimensionValue = function getTemporalDimensionValue(){
	return jQuery('#ddl-temporal-value option:selected').val();
};

StatPortalOpenData.GeoViewer.getMeasureSelected = function getMeasureSelected(){
	return jQuery('#geoviewerMeasures option:selected').val();
};

StatPortalOpenData.GeoViewer.getMeasureTextSelected = function getMeasureTextSelected(){
	return jQuery('#geoviewerMeasures option:selected').text();
};

/**
 * Chiamata per la creazione del layer dello shape file
 * @param dataUid	uid del dato
 * @param errorNumber	identificativo di eventuali errori
 */
StatPortalOpenData.GeoViewer.createShapeLayer = function createShapeLayer(dataUid, errorNumber){
	jQuery('#geo_viewer_loading').show();
	var createShapeLayerRequest = state.getServiceUri() + 'CreateShapeLayer?dataUid=\''+ dataUid + '\'&$format=json';
	StatPortalOpenData.utility.log(createShapeLayerRequest);
	OData.defaultHttpClient.enableJsonpCallback = true;
	OData.read(createShapeLayerRequest, StatPortalOpenData.GeoViewer.createShapeLayerCallback, function(err) {StatPortalOpenData.GeoViewer.printMapGeneralErrorMessage(errorNumber);});
};

/**
 * Chiamata per la creazione di un layer di tematizzazione
 * @param dataUid	uid del dato
 * @param errorNumber	identificativo di eventuali errori
 */
StatPortalOpenData.GeoViewer.createThematizationLayer = function createThematizationLayer(dataUid, errorNumber){
	jQuery('#geo_viewer_loading').show();
	StatPortalOpenData.GeoViewer.hideMessage();
	var territorialDimensionUid = StatPortalOpenData.GeoViewer.getTerritorialDimensionUid();
	var temporalDimensionUid = typeof(StatPortalOpenData.GeoViewer.getTemporalDimensionUid()) != 'undefined' ? StatPortalOpenData.GeoViewer.getTemporalDimensionUid() : '';
	var temporalDimensionValue = typeof(StatPortalOpenData.GeoViewer.getTemporalDimensionValue()) != 'undefined' ? StatPortalOpenData.GeoViewer.getTemporalDimensionValue() : '';
	var createThematizationLayerRequest = state.getServiceUri() + 'CreateThematizationLayer?dataUid=\'' + dataUid + '\'&territorialDimensionUid=\'' + territorialDimensionUid + '\'&temporalDimensionUid=\'' + temporalDimensionUid +'\'&temporalDimensionValue=\'' + temporalDimensionValue + '\'&$format=json';
	
	StatPortalOpenData.utility.log(createThematizationLayerRequest);
	
	OData.defaultHttpClient.enableJsonpCallback = true;
	OData.read(createThematizationLayerRequest, StatPortalOpenData.GeoViewer.createThematizationLayerCallback, function(err) {StatPortalOpenData.GeoViewer.printMapGeneralErrorMessage(errorNumber);});
};

/**
 * Inizializzazione della mappa dato l'uid del dato
 * @param mIdData	uid del dato
 */
StatPortalOpenData.GeoViewer.initMap = function initMap(mIdData){
	  
	var measureVal = StatPortalOpenData.GeoViewer.getMeasureSelected();
	  
	  if(measureVal === StatPortalOpenData.GeoViewer.GEOMETRY_VALUE){
		  StatPortalOpenData.GeoViewer.createShapeLayer(mIdData, 43);
		  StatPortalOpenData.GeoViewer.ActualDimensionValueSelected = '';
	  }else if(measureVal === StatPortalOpenData.GeoViewer.KML_VALUE){
		  StatPortalOpenData.GeoViewer.initMapFromKML(mIdData);
		  StatPortalOpenData.GeoViewer.ActualDimensionValueSelected = '';
	  }else{
		  StatPortalOpenData.GeoViewer.createThematizationLayer(mIdData, 44);
		  StatPortalOpenData.GeoViewer.ActualDimensionValueSelected = StatPortalOpenData.GeoViewer.getTerritorialDimensionUid();
	  }
	  
	};
	
	
/**
 * Si richiede il file kml che rappresenta il dato e nella callback si chiamerà il gisviewer
 * @param mIdData
 */
StatPortalOpenData.GeoViewer.initMapFromKML = function initMapFromKML(mIdData){
	var createExportKMLRequest = state.getServiceUri() + 'ExportKML?uid=\''+ mIdData + '\'&$format=json';
	
	StatPortalOpenData.utility.log(createExportKMLRequest);
	OData.defaultHttpClient.enableJsonpCallback = true;
	OData.read(createExportKMLRequest, StatPortalOpenData.GeoViewer.initMapFromKMLCallBack, StatPortalOpenData.GeoViewer.initMapError);
};

/**
 * Gestione dell'errore nel recupero del KML
 */
StatPortalOpenData.GeoViewer.initMapError = function initMapError(){
	StatPortalOpenData.GeoViewer.printMapGeneralErrorMessage(55);
};
	

/**
 * Chiamata al geoviewer passandogli il KML
 * @param exportKMLResult
 */
StatPortalOpenData.GeoViewer.initMapFromKMLCallBack = function initMapFromKMLCallBack(exportKMLResult){
	// qua viene restituito l'URL del file, del quale dovremmo richiederne il contenuto
	try{
		KML_fileURL = exportKMLResult.url;
		var parameters =  '?' + 'KML_fileURL=' + KML_fileURL + '&filter=' + '';
		jQuery('#mapViewer').attr('src',
				StatPortalOpenData.ODataServices.GEO_VIEWER + parameters).load(
						function() {
							jQuery('#geo_viewer_loading').hide();
		});
	}catch(Exception){
		StatPortalOpenData.GeoViewer.printMapGeneralErrorMessage(56);
	}
};

/**
 * Restituisce le dimensioni disponibili per una visualizzazione della griglia a tutto schermo
 */
StatPortalOpenData.GeoViewer.getDimensionsForFullScreen = function getDimensionsForFullScreen(){
	return {
		height: jQuery(window).height() - 33,
		width:jQuery(window).width() - 10 
	};
};

/**
 * Gestione del ridimensionamento della finestra (ritorno dal fullscreen a dimensioni normali)
 */
StatPortalOpenData.GeoViewer.toNormalSize = function toNormalSize(){
	jQuery('#map-viewer-container').removeClass('multidimensionalFullScreen');
	StatPortalOpenData.GeoViewer.windowResizeHandlerForGeoViewer();
	jQuery('#mapViewer').attr('width', oldGeoViewerWidth);
	jQuery(window).bind('resize', StatPortalOpenData.GeoViewer.windowResizeHandlerForGeoViewer);
	jQuery(window).unbind('resize', StatPortalOpenData.GeoViewer.windowResizeHandlerForGeoViewerFullScreen);
	// re si faceva il reload si perdeva la tematizzazione. Mi sembra che si fosse aggiunta 
	// la riga per far si che ricaricasse per bene la mappa di sfondo (ma sembra che lo faccia lo stesso)
	//document.getElementById('mapViewer').contentDocument.location.reload(true);
};

/**
 * Gestione del cambio di misura da interfaccia
 * @param dataId	uid del dato
 */
StatPortalOpenData.GeoViewer.measureChangedHandler = function measureChangedHandler(dataId){
	StatPortalOpenData.GeoViewer.hideMessage();
	var measureVal = jQuery('#geoviewerMeasures option:selected').val();
	var measureLabel = jQuery('#geoviewerMeasures option:selected').text();
	var dimVal = jQuery('#geoviewerDimensions option:selected').val();
	if(measureVal === StatPortalOpenData.GeoViewer.GEOMETRY_VALUE){
		jQuery('#temporalDimensionFilter').hide();
		jQuery('#geoviewerDimensions').hide();
		jQuery('#map-for').hide();
		
		StatPortalOpenData.GeoViewer.ActualDimensionValueSelected = '';
		StatPortalOpenData.GeoViewer.createShapeLayer(dataId, 46);
		
	}else{		
		if(StatPortalOpenData.GeoViewer.ActualDimensionValueSelected === dimVal){
			// si cambia solo la variabile tematizzata
			document.getElementById('mapViewer').contentWindow.doLegendClassify(measureVal, measureLabel);
		}else{
			// si richiede un nuovo layer
			StatPortalOpenData.GeoViewer.ActualDimensionValueSelected = dimVal;
			StatPortalOpenData.GeoViewer.createThematizationLayer(dataId, 47);
		}
		// si attiva il menu di scelta delle dimensioni
		jQuery('#geoviewerDimensions').show();
		jQuery('#map-for').show();
		jQuery('#temporalDimensionFilter').show();
	}
};

/**
 * Gestione del cambio di dimensione territoriale da interfaccia
 */
StatPortalOpenData.GeoViewer.territorialDimensionChangedHandler = function territorialDimensionChangedHandler(dataId){
	// non importa quale misura sia selezionata, tanto la vista viene creata per tutte
	StatPortalOpenData.GeoViewer.createThematizationLayer(dataId, 47);
};

/**
 * Inizializzazione delle dimensioni temporali
 * @param id	uid del dato
 */
StatPortalOpenData.GeoViewer.initializeTemporalDimensions = function initializeTemporalDimensions(id){
	var dimensions = state.getDimensions();
	var dimensionsLen = dimensions.length;
	var temporalDimensions = [];
	var idxDim = 0;
	for(idxDim=0; idxDim<dimensionsLen; idxDim++){
		if(dimensions[idxDim].getDimType() == "TEMPORALE"){
			//jQuery('#geoviewerDimensions').append(jQuery("<option />").val(dimensions[idxDim].getId()).text(dimensions[idxDim].getAlias()));
			temporalDimensions.push({
				id: dimensions[idxDim].getId(),
				alias: dimensions[idxDim].getAlias(),
				name: dimensions[idxDim].getName()
			});
		}	
	}
	
	var temporalDimensionsLen = temporalDimensions.length;
	
	// se c'è almeno una dimensione temporale proponiamo il filtro
	if(temporalDimensionsLen > 0){
		
		// se c'è un'unica dimensione temporale mettiamo una label, altrimenti un menu a tendina
		if(temporalDimensionsLen > 1){
			alert("Non sono ancora supportati i filtri su dimensioni temporali multiple. Filtri disponibili solo su '" + (temporalDimensions[0].alias)  + "'");
		}
		var selectToAppend = '';
		
		selectToAppend = selectToAppend + '<select id="ddl-temporal-label" disabled="disabled">';
		selectToAppend = selectToAppend + '<option value=' + temporalDimensions[0].id + '>' + (temporalDimensions[0].alias) + '</option>';
		selectToAppend = selectToAppend + '</select>';
		
		jQuery('#temporalDimensionFilterLabel').html(selectToAppend);
		
		// popolare la tendina con i vari valori della dimensione temporale (richiesta odata)
		var odataDistinctRequest = state.getServiceUri() + state.getResourcePath().replace('DataRows', 'DistinctCountRows') + '?distinctField=' + temporalDimensions[0].name + '&fieldToSort=LABEL&sortingDirection=DESC';
		OData.defaultHttpClient.enableJsonpCallback = true;
		
		
		OData.read(odataDistinctRequest, function(data) {
			selectToAppend = '';
			jQuery('#temporalDimensionFilterValue').html('');
			selectToAppend = selectToAppend + '<select id="ddl-temporal-value" onChange="StatPortalOpenData.GeoViewer.territorialDimensionChangedHandler(StatPortalOpenData.GeoViewer.dataId);">';
			var dataLen = data.results.length, idxDataLen = 0;
			for(idxDataLen = 0; idxDataLen < dataLen; idxDataLen++){
				selectToAppend = selectToAppend + '<option value=' + (data.results[idxDataLen].label) + '>' + (data.results[idxDataLen].label) + '</option>';
			}
			
			if(dataLen > 1){
				selectToAppend = selectToAppend + '<option value="">Tutti</option>';
			}
			
			selectToAppend = selectToAppend + '</select>';
			jQuery('#temporalDimensionFilterValue').append(selectToAppend);
			
			if(dataLen === 1){
				jQuery('#ddl-temporal-value').attr('disabled', true);
			}
			
			// si inizializza l'eventuale valore passato nell'url
			var temporalDimValueSelected = StatPortalOpenData.ODataUtility.getURLParameter('temporalFilterVal');
			if(!StatPortalOpenData.ODataUtility.isUndefinedNullOrEmpty(temporalDimValueSelected)){
				jQuery('#ddl-temporal-value').val(temporalDimValueSelected);
			}else if(location.href.indexOf('temporalFilterVal') != -1 && temporalDimValueSelected == 'null'){
				// significa tutti
				jQuery('#ddl-temporal-value').val('');
			}
			
			StatPortalOpenData.ODataUtility.initViews(id);
			
		}, function(err) {
			StatPortalOpenData.GeoViewer.showMessage('error', 'Impossibile visualizzare la mappa');
		});
			
	}else{
		jQuery('#temporalDimensionFilterInternal').hide();
		StatPortalOpenData.ODataUtility.initViews(id);
	}
	
};

/**
 * Inizilizzazione delle dimensioni temporali
 */
StatPortalOpenData.GeoViewer.initializeTerritorialDimensions = function initializeTerritorialDimensions(){
	// Si scorrono le dimensioni per capire se cen'è almeno una territoriale.
	// Altrimenti si tratta di uno shape
	var almostOneTerritorial = false;
	var dimensions = state.getDimensions();
	var dimensionsLen = dimensions.length;
	var idxDim = 0;
	for(idxDim=0; idxDim<dimensionsLen; idxDim++){
		if(dimensions[idxDim].getDimType() == "TERRITORIALE"){
			jQuery('#geoviewerDimensions').append(jQuery("<option />").val(dimensions[idxDim].getId()).text(dimensions[idxDim].getAlias()));
			almostOneTerritorial = true;
		}	
	}

	// TODO: il menu per la scelta delle dimensioni si deve attivare solo se non è attivo
	if(	jQuery('#geoviewerMeasures').val() === StatPortalOpenData.GeoViewer.GEOMETRY_VALUE ||
		jQuery('#geoviewerMeasures').val() === 	StatPortalOpenData.GeoViewer.KML_VALUE){
		jQuery('#geoviewerDimensions').hide();
		jQuery('#map-for').hide();
		jQuery('#temporalDimensionFilter').hide();
	}else if(almostOneTerritorial){
		// si attiva il menu a tendina
		jQuery('#geoviewerDimensions').show();
		jQuery('#map-for').show();
		jQuery('#temporalDimensionFilter').show();
	}else{
		StatPortalOpenData.GeoViewer.showMessage('error', 'Impossibile visualizzare la mappa');
	}
};

var getAliasJsonResponse = null;
var aliasPhysicalMap = null;

/**
 * Inizializzazione delle misure
 */
StatPortalOpenData.GeoViewer.initializeMeasures = function initializeMeasures(){
	var measureOptions = jQuery('#geoviewerMeasures');
	var optionToAppend;
	
	jQuery('#show-map-as-img').show();
	
	// se si tratta di uno shape si aggiunge anche l'opzione di visualizzazione della geometria
	if(state.getDataType() === StatPortalOpenData.Enums.MdLuDataType.TERRITORIALE){
		optionToAppend = jQuery('<option selected="selected" />').val(StatPortalOpenData.GeoViewer.GEOMETRY_VALUE).text('Geometria').attr('data-type', 'geometry');
		measureOptions.append(optionToAppend);
	}else if(state.getDataType() === StatPortalOpenData.Enums.MdLuDataType.TERRITORIALE_KML){
		// si rimuove l'export della mappa come immagine
		jQuery('#show-map-as-img').hide();
		optionToAppend = jQuery('<option selected="selected" />').val(StatPortalOpenData.GeoViewer.KML_VALUE).text('KML').attr('data-type', 'KML');
		measureOptions.append(optionToAppend);
	}
	
	// si aggiungono le misure e la count per dimensione territoriale (se c'è almeno una dimensione territoriale)
	if(state.containsAlmostOneTerritorialDimension()){
		
		var measures = state.getMeasures();
		
		getAliasJsonResponse = null;
		// si fa la call alla getAliases
		var dataId = state.getDataId();
		if(dataId != '-1'){
			var pl = new SOAPClientParameters();
			pl.add('dataUid', dataId);
			SOAPClient.invoke(StatPortalOpenData.ODataServices.OPENDATA_ETL, "getAliases", pl, false, StatPortalOpenData.GeoViewer.getAliasesHandler);
			
			// si crea un map <nome logico, nome fisico> indicizzato per nome logico
			aliasPhysicalMap = new Object();
			var idxAliasJsonResponse = 0;
			for(idxAliasJsonResponse = 0; idxAliasJsonResponse < getAliasJsonResponse.aliases.length; idxAliasJsonResponse++){
				var phN = getAliasJsonResponse.aliases[idxAliasJsonResponse].physicalName;
				var alN = getAliasJsonResponse.aliases[idxAliasJsonResponse].alias;
				aliasPhysicalMap[alN] = phN;
			}
			
			jQuery.each(measures, function() {
				var physicalName = aliasPhysicalMap[this.getAlias()];
				// se non presente non si aggiunge. Non dovrebbe mai succedere
				if(physicalName != null && typeof(physicalName) != 'undefined'){
					optionToAppend = jQuery("<option />").val(physicalName).text(this.getAlias()).attr('data-type', 'measure');
					measureOptions.append(optionToAppend);	
				}else{
					StatPortalOpenData.GeoViewer.showMessage('error','Impossibile recuperare l\'etichetta ' + this.getAlias());
				}
			});
		
			var countValue = /*StatPortalOpenData.ODataUtility.getUniqueIdentifier(*/'v_el_count'/*)*/;
			var countLabel = 'Numero di elementi';
			optionToAppend = jQuery("<option />").val(countValue).text(countLabel).attr('data-type', 'countStar');
			measureOptions.append(optionToAppend);
			
		}else{
			StatPortalOpenData.GeoViewer.showMessage('error', 'Mappa non disponibile (impossibile recuperare le etichette)');
		}
	}
};

/**
 * Gestione della richiesta degli alias
 * @param r
 */
StatPortalOpenData.GeoViewer.getAliasesHandler = function getAliasesHandler(r){
	try{
		var _X2JS = new X2JS();
	    var obj = _X2JS.xml_str2json(r);
	    var res = obj.Envelope.Body.getAliasesResponse.getAliasesReturn;
	    getAliasJsonResponse = jQuery.parseJSON(res);
	}catch(Exception){
		StatPortalOpenData.GeoViewer.showMessage('error', 'Mappa non disponibile (impossibile recuperare le etichette)');
	}
};

/**
 * Composizione dei parametri per il GisViewer
 * @param res
 * @param isThematization
 * @returns {String}
 */
StatPortalOpenData.GeoViewer.composeParametersForGisViewer = function composeParametersForGisViewer(res, isThematization){
	try{
		
		var layerName = res.layerName.substring(res.layerName.indexOf('<LayerName>') + '<LayerName>'.length, res.layerName.indexOf('</LayerName>'));
		
		if(layerName === ''){
	    	return '';
	    }else{
	    	var workspaceName = res.workspaceName;
	    	
	    	var measuresList = '', measuresLabelList = '';
	    	if(isThematization){
	    		// si prendono le misure, che sono i campi sui quali potremmo tematizzare
	            var measures = state.getMeasures();
	            var i = 0;
	            for(i=0;i<measures.length; i++){
	              measuresList += measures[i].getName() + ((i !== measures.length - 1) ? '||' : '');
	              measuresLabelList += measures[i].getAlias() + ((i !== measures.length - 1) ? '||' : '');
	            }
	    	}
	    	
	    	var geoServerUrl = res.geoServerUrl;
			var geoServerRestUrl = res.geoServerRestUrl;
	    	
			var opacity = (StatPortalOpenData.GeoViewer.mapVisualParameters.opacity == 'null') ? '' : 'opacity=' + StatPortalOpenData.GeoViewer.mapVisualParameters.opacity + '&';
			var mapType = (StatPortalOpenData.GeoViewer.mapVisualParameters.mapType == 'null') ? '' : 'mapType=' + StatPortalOpenData.GeoViewer.mapVisualParameters.mapType + '&';
			var numClasses = (StatPortalOpenData.GeoViewer.mapVisualParameters.numClasses == 'null' || !isThematization) ? 'numClasses=5&' : 'numClasses=' + StatPortalOpenData.GeoViewer.mapVisualParameters.numClasses + '&';
			var paletteName = (StatPortalOpenData.GeoViewer.mapVisualParameters.paletteName == 'null' || !isThematization) ? 'paletteName=Reds&' : 'paletteName=' + StatPortalOpenData.GeoViewer.mapVisualParameters.paletteName + '&';
			
			var borderColor = (StatPortalOpenData.GeoViewer.mapVisualParameters.borderColor == 'null') ? '' : 'borderColor=' + StatPortalOpenData.GeoViewer.mapVisualParameters.borderColor + '&';
			var borderStyle = (StatPortalOpenData.GeoViewer.mapVisualParameters.borderStyle == 'null') ? '' : 'borderStyle=' + StatPortalOpenData.GeoViewer.mapVisualParameters.borderStyle + '&';
			var borderWidth = (StatPortalOpenData.GeoViewer.mapVisualParameters.borderWidth == 'null') ? '' : 'borderWidth=' + StatPortalOpenData.GeoViewer.mapVisualParameters.borderWidth + '&';
			
	    	var parameters =  '?' + 
            'geoServerUrl=' + geoServerUrl + '&' + 
            'geoServerRESTUrl=' + geoServerRestUrl + '&' +
            'classificationMethod=' + 'classify' + '&' + 
            'getExtentMethod=' + 'getextent' + '&' + 
            'workspaceName=' + workspaceName + '&' + 
            'layerName=' + layerName + '&' + 
            'filter=' + '' + '&' +
             opacity + 
             mapType + 
             numClasses + 
             paletteName + 
             borderColor + 
             borderStyle + 
             borderWidth;
            
	    	parameters += 'wsOpenDataETLUrl=' + StatPortalOpenData.ODataServices.OPENDATA_ETL + '&' +
	    	'dataId=' + StatPortalOpenData.dataUid + '&' + 
	    	'dataType=' + state.getDataType() + '&' + 
	    	'isThematization=' + isThematization;
            
	    	return parameters;
	    	
	    }
	}catch(Exception){
		StatPortalOpenData.GeoViewer.showMessage('error', 'Impossibile visualizzare la mappa');
		return '';
	}
};