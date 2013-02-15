StatPortalOpenData.EarthViewer = StatPortalOpenData.EarthViewer || {};

var kmlForGoogleHearth = '';
var ge;
var KML_fileURL;

StatPortalOpenData.EarthViewer.GENERAL_ERROR_MESSAGE = 'Gentile utente, sembra che per motivi tecnici il suo browser o impostazioni nella sua rete non consentano l\'esecuzione della visualizzazione in 3D. A questa pagina (http://support.google.com/maps/bin/topic.py?hl=en&topic=28397&parent=10781&ctx=topic Trovera\'; maggiori informazioni al riguardo. Per altre informazioni ci puo\' contattare alla nostra casella supporto@dati.open.it';
StatPortalOpenData.EarthViewer.alertErrorMessage =  function alertErrorMessage(errorMessage, errorCode) {
	alert(errorMessage + '(' + errorCode + ')');
};

/**
 * Richiede il kml al web service ed inizializza il plugin di google earth
 * @param id id dell'mdData
 */
StatPortalOpenData.EarthViewer.initEarthFromDrupal =  function initEarthFromDrupal(id) {
	
	// si richiede il kml dello shape file
	if(typeof(state) === 'undefined'){
		state = new StatPortalOpenData.State();
	}
	
	state.setNewAndOnlyModuleActive(StatPortalOpenData.Enums.ModuleId.MAP3D);
	jQuery('#map3d-viewer').height(Math.max(MIN_MAP_HEIGHT, jQuery(window).height() - StatPortalOpenData.utility.getResidualTableHeight('MAP')));
	jQuery('#map3d').height(Math.max(MIN_MAP_HEIGHT, jQuery(window).height() - StatPortalOpenData.utility.getResidualTableHeight('MAP')));
	jQuery(window).unbind('resize', StatPortalOpenData.EarthViewer.windowResizeHandlerForEarthViewer);
	jQuery(window).bind('resize', StatPortalOpenData.EarthViewer.windowResizeHandlerForEarthViewer);
	
	
	var createExportKMLRequest = state.getServiceUri() + 'ExportKML?uid=\''+ id + '\'&$format=json';
	
	StatPortalOpenData.utility.log(createExportKMLRequest);
	OData.defaultHttpClient.enableJsonpCallback = true;
	OData.read(createExportKMLRequest, StatPortalOpenData.EarthViewer.exportKMLCallback, function(err) {StatPortalOpenData.EarthViewer.alertErrorMessage(StatPortalOpenData.EarthViewer.GENERAL_ERROR_MESSAGE, 2);});
};

/**
 * Callback che gestisce la visualizzazione su google earth del kml
 * @param exportKMLResult
 */
StatPortalOpenData.EarthViewer.exportKMLCallback = function exportKMLCallback(exportKMLResult){
	// qua viene restituito l'URL del file, del quale dovremmo richiederne il contenuto
	try{
		KML_fileURL = exportKMLResult.url;
		google.earth.createInstance('map3d', StatPortalOpenData.EarthViewer.initEarthCallback, StatPortalOpenData.EarthViewer.failureEarthCallback);
	}catch(Exception){
		StatPortalOpenData.EarthViewer.alertErrorMessage(StatPortalOpenData.EarthViewer.GENERAL_ERROR_MESSAGE, 1);
	}	
};

/**
 * Funzione di callback nel caso in cui l'inizializzazione del plugin sia andata a buon fine
 * Viene settato il kml
 * @param instance
 */
StatPortalOpenData.EarthViewer.initEarthCallback =	function initEarthCallback(instance) {
	  
	try{
		  ge = instance;
		  ge.getWindow().setVisibility(true);
		  
		  var layerRoot = ge.getLayerRoot();
		  var options = ge.getOptions();
		  options.setStatusBarVisibility(true);
		  options.setGridVisibility(false);
		  options.setOverviewMapVisibility(false);
		  options.setScaleLegendVisibility(true);
		  options.setAtmosphereVisibility(true);
		  options.setMouseNavigationEnabled(true);
		  
		  // add a navigation control
		  ge.getNavigationControl().setVisibility(ge.VISIBILITY_AUTO);
		  
		  // add some layers
		  layerRoot.enableLayerById(ge.LAYER_BUILDINGS, false);
		  layerRoot.enableLayerById(ge.LAYER_TERRAIN, true);
		  layerRoot.enableLayerById(ge.LAYER_BORDERS, true);
		  layerRoot.enableLayerById(ge.LAYER_ROADS, true);
	
		  google.earth.fetchKml(ge, KML_fileURL, function(kmlObject) {
			  var networkLink = ge.createNetworkLink("");
			  networkLink.setFlyToView(true);
			  var link = ge.createLink("");
			  link.setHref(KML_fileURL);
			  networkLink.setLink(link);
			  ge.getFeatures().appendChild(networkLink);
		  });
		  
	}catch(Exception){
		StatPortalOpenData.EarthViewer.alertErrorMessage(StatPortalOpenData.EarthViewer.GENERAL_ERROR_MESSAGE, '2');
	}
	//window.setTimeout("StatPortalOpenData.EarthViewer.flyToPoint(" + 43.5 + "," + 11.5 + ");", 1000); 
	window.setTimeout("jQuery('#earth_viewer_loading').hide();", 500);
	StatPortalOpenData.ODataUtility.removeInitialLoading();
};


/**
 * Date le coordinate si zooma verso il punto indicato. TODO: da sostituire con la flyToObject
 * @param lat
 * @param lon
 */
StatPortalOpenData.EarthViewer.flyToPoint = function flyToPoint(lat, lon){
	//StatPortalOpenData.EarthViewer.flyToObject(ge);
	var la=ge.createLookAt('');
	la.set(lat,lon,1500000,ge.ALTITUDE_RELATIVE_TO_GROUND,0,0,5);
	ge.getView().setAbstractView(la);
};


/**
 * Funzione di callback in caso di errore nell'inizializzazione di google earth
 * @param errorCode
 */
StatPortalOpenData.EarthViewer.failureEarthCallback = function failureEarthCallback(errorCode) {
	if(!google.earth.isInstalled()){
		// si lascia gestire a google l'installazione del plugin
		jQuery('#earth_viewer_loading').hide();
	}else{
		StatPortalOpenData.EarthViewer.alertErrorMessage(StatPortalOpenData.EarthViewer.GENERAL_ERROR_MESSAGE, errorCode);	
	}
};

/**
 * Handler per il ridimensionamento della finestra
 */
StatPortalOpenData.EarthViewer.windowResizeHandlerForEarthViewer = function windowResizeHandlerForEarthViewer(){
	var newHeight = Math.max(MIN_MAP_HEIGHT, jQuery(window).height() - StatPortalOpenData.utility.getResidualTableHeight('MAP'));
	if(jQuery("#map3d-viewer").height() != newHeight){
		jQuery("#map3d-viewer").height(newHeight);
	}
};

StatPortalOpenData.EarthViewer.windowResizeHandlerForEarthViewerFullScreen = function windowResizeHandlerForEarthViewerFullScreen(){
	var dimFullScreen = StatPortalOpenData.EarthViewer.getDimensionsForFullScreen();
	jQuery('#map3d-viewer').css('height', dimFullScreen.height);
	jQuery('#map3d-viewer').css('width', dimFullScreen.width);
	jQuery('#map3d').css('width', dimFullScreen.width + 'px');
	jQuery('#map3d').css('height', dimFullScreen.height + 'px');
};

var oldEarthViewerWidth;

/**
 * Handler per la gestione del fullscreen
 */
StatPortalOpenData.EarthViewer.toFullScreen = function toFullScreen(){

	jQuery('#earth-viewer-container').append('<img id="closeFullscreen" class="closeFullscreen" onclick="javascript:StatPortalOpenData.ODataUtility.toNormalSize();" src="/sites/all/modules/spodata/metadata/viewer/commons/img/close.png" />');
	jQuery(window).unbind('resize', StatPortalOpenData.EarthViewer.windowResizeHandlerForEarthViewer);
	jQuery(window).bind('resize', StatPortalOpenData.EarthViewer.windowResizeHandlerForEarthViewerFullScreen);
	jQuery('#toolbarHearthContainerDummy').css('min-height', '30px');
	jQuery('#earth-viewer-container').addClass('multidimensionalFullScreen');
	oldEarthViewerWidth = jQuery('#map3d-viewer').css('width');
	jQuery('#map3d-viewer').css('height', StatPortalOpenData.EarthViewer.getDimensionsForFullScreen().height);
	jQuery('#map3d-viewer').css('width', StatPortalOpenData.EarthViewer.getDimensionsForFullScreen().width);
	jQuery('#map3d').css('width', StatPortalOpenData.EarthViewer.getDimensionsForFullScreen().width + 'px');
	jQuery('#map3d').css('height', StatPortalOpenData.EarthViewer.getDimensionsForFullScreen().height + 'px');
};


StatPortalOpenData.EarthViewer.getDimensionsForFullScreen = function getDimensionsForFullScreen(){
	return {
		height: jQuery(window).height() - 35,
		width:jQuery(window).width() - 10 
	};
};

/**
 * Handler per la gestione del ritorno da fullscreen ad una grandezza normale della finestra
 */
StatPortalOpenData.EarthViewer.toNormalSize = function toNormalSize(){
	jQuery('#earth-viewer-container').removeClass('multidimensionalFullScreen');
	jQuery('#toolbarHearthContainerDummy').css('min-height', '0px');
	StatPortalOpenData.EarthViewer.windowResizeHandlerForEarthViewer();
	jQuery('#map3d-viewer').css('width', oldEarthViewerWidth);
	jQuery('#map3d').css('width', oldEarthViewerWidth);
	jQuery(window).bind('resize', StatPortalOpenData.EarthViewer.windowResizeHandlerForEarthViewer);
	jQuery(window).unbind('resize', StatPortalOpenData.EarthViewer.windowResizeHandlerForEarthViewerFullScreen);
};