//Get request parameters passed via GET Method
$.extend({
	getUrlVars : function() {
		var vars = [], hash;
		var hashes = window.location.href.slice(
				window.location.href.indexOf('?') + 1).split('&');
		for ( var i = 0; i < hashes.length; i++) {
			hash = hashes[i].split('=');
			vars.push(hash[0]);
			vars[hash[0]] = unescape(hash[1]);
		}
		return vars;
	},
	getUrlVar : function(name) {
		return unescape($.getUrlVars()[name]);
	}
});

function verify_req_parameters(geoServerUrl, workspaceName, layerName) {	
	if ((geoServerUrl != null) && (geoServerUrl != "undefined") && (workspaceName != null) && (workspaceName != "undefined") && (layerName != null)&& (layerName != "undefined")){
		return true;
	}	
	if (kmlUrl != null){
		return true;
	}
	return false;
}
     
/*Parametri per la richiesta di layer da tematizzare*/
var geoServerUrl = $.getUrlVar('geoServerUrl');
var workspaceName = $.getUrlVar('workspaceName');
var layerName = $.getUrlVar('layerName');

/*Parametro per la richiesta di kml*/
var kmlUrl = $.getUrlVar('KML_fileURL');

var title = $.getUrlVar('title');
var description = $.getUrlVar('description');
var geoServerRESTUrl = $.getUrlVar('geoServerRESTUrl');
var classificationRESTMethodURL, getExtentMethodURL;
if(geoServerRESTUrl.charAt(geoServerRESTUrl.length-1)!="/"){
	geoServerRESTUrl = geoServerRESTUrl+ "/";
}	
classificationRESTMethodURL = geoServerRESTUrl + $.getUrlVar('classificationMethod');
getExtentMethodURL = geoServerRESTUrl + $.getUrlVar('getExtentMethod');
var wsOpenDataETLUrl = $.getUrlVar('wsOpenDataETLUrl');
var filter = $.getUrlVar('filter');
var dataId=$.getUrlVar('dataId');
var dataType=$.getUrlVar('dataType');
var opacity=$.getUrlVar('opacity');
var mapTypeTmp=$.getUrlVar('mapType');

var isMapThematization=$.getUrlVar('isThematization');

var def_var_name= "";
var def_var_lbl= "";
var def_num_classes= $.getUrlVar('numClasses');

//palette and inverted palette
var def_palette_name= $.getUrlVar('paletteName');
var def_inverted_palette= (def_palette_name.indexOf("Inverted") != -1)? true : false;
def_palette_name = def_palette_name.replace("Inverted","");

var def_border_color = ($.getUrlVar('borderColor') != "undefined") ? $.getUrlVar('borderColor') : '0_0_0';
var def_border_width = ($.getUrlVar('borderWidth') != "undefined") ? $.getUrlVar('borderWidth') : '0.5';
var def_border_style = ($.getUrlVar('borderStyle') != "undefined") ? $.getUrlVar('borderStyle') : 'solid';

var can_continue = true;

// Variabili globali
var map, openDataLayer, format, control, bounds,highlightLayer,highlightLayer2, otherLayer, kmlLayer;
var layerSchemaDict;

var externalPagesManager = new SISTER.Utils.DOM.ExternalPagesManager(),
	domManager = new SISTER.Utils.DOM.DOMManager(),
	myMapManager = new SISTER.Utils.Map.MapManager(),
	operationManager = new SISTER.Utils.Operations.OperationManager();

OpenLayers.IMAGE_RELOAD_ATTEMPTS = 3;
OpenLayers.Util.onImageLoadErrorColor = "transparent";

function initMap(){	
	
	if (!verify_req_parameters(geoServerUrl, workspaceName, layerName)) {
		window.location.href = "error.html";
	}
		
	if (layerName!="undefined"){
		
		if((title != null) && (title != "undefined") ){
			$("#mapTitle").html(title);
		}else{
			$("#mapTitle").html("Visualizzazione del layer " + layerName);
		}
		if((description != null) && (description != "undefined") ){
			$("#mapDescription").html(description);
		}else{
			$("#mapDescription").html("");
		}
			
		if (mapTypeTmp != "undefined"){
			mapType = mapTypeTmp;
		}		
		if ((opacity != "undefined") &&(opacity !="") ){
			myMapManager.initialLayerOpacity = opacity;
		}
		
		myMapManager.getFeaturesExtent(getExtentMethodURL,workspaceName,layerName,filter);	
		
		openDataLayer = myMapManager.createWMSLayer(geoServerUrl, workspaceName, layerName, format);			

		createAndShowMap();
	}
	else {			
		
		createAndShowKMLMap();
	}
	
	hideLegendPanel();
	hideLegendBorderPanel();
	prepareDialogs();
};

function createAndShowKMLMap(){
	format = 'image/png';
					
	var options = {		
		units : "m",		
		controls : [],	
		maxResolution : 0.03119921875
	};
	
	map = new OpenLayers.Map('map', options);	
	
	map.addLayers([ gphy, gmap, ghyb, gsat ]); 
	
    kmlLayer = new OpenLayers.Layer.Vector("KML", {
    	strategies: [new OpenLayers.Strategy.Fixed()],
        protocol: new OpenLayers.Protocol.HTTP({
        url: kmlUrl,
        format: new OpenLayers.Format.KML({
        	extractStyles: true, 
            extractAttributes: true,
            maxDepth: 2
            })
        })
    });
    
    kmlLayer.events.register("loadstart", openDataLayer, layerLoadStart);
	kmlLayer.events.register("tileloaded",openDataLayer, layerLoaded);
    kmlLayer.events.register("loadend", this, setKMLExtent);       
	
    map.addLayer(kmlLayer);

	map.addControl(new OpenLayers.Control.Navigation());
	
	map.addControl(new OpenLayers.Control.MousePosition({
		div : document.getElementById("coord")
	}));	
	map.addControl(new OpenLayers.Control.Zoom());	
	
	map.addControl(new OpenLayers.Control.Scale(
			document.getElementById("map_scale_div"),
			{
				updateScale: function() {
			        scale = this.map.getScale();
			        if (!scale) {
			            return;
			        }			        
			        scale = Math.round(scale);			  			 
			        this.element.innerHTML = "<b>Scala</b> 1:" + $.format.number(scale,'#,###');
			    }
			}
		));	
	
	select = new OpenLayers.Control.SelectFeature(kmlLayer);
            
    kmlLayer.events.on({
		"featureselected": onFeatureSelect,
        "featureunselected": onFeatureUnselect
	});

    map.addControl(select);
    select.activate();   
              
    layerLoaded(null);
    hideOpacityPanel();
}

function setKMLExtent(e){	
	map.zoomToExtent(kmlLayer.getDataExtent());
	layerLoaded(e);
}

function onPopupClose(evt) {
	select.unselectAll();
}

function onFeatureSelect(event) {
	var feature = event.feature;
	// Since KML is user-generated, do naive protection against
	// Javascript.
	var content = "";
	if (feature.attributes.name!=undefined){
		content += "<h2>"+feature.attributes.name + "</h2>" ;
	}
	content += feature.attributes.description;
	if (content.search("<script") != -1) {
		content = "Content contained Javascript! Escaped content below.<br>" + content.replace(/</g, "&lt;");
	}
	popup = new OpenLayers.Popup.FramedCloud("pop", 
		feature.geometry.getBounds().getCenterLonLat(),
		new OpenLayers.Size(100,100),
		content,
		null, true, onPopupClose);
		feature.popup = popup;
		map.addPopup(popup);
}

function onFeatureUnselect(event) {
	var feature = event.feature;
	if(feature.popup) {
    	map.removePopup(feature.popup);
        feature.popup.destroy();
        delete feature.popup;
	}
}

function createAndShowMap(){
	
	format = 'image/png';
		
	var projection = new OpenLayers.Projection("EPSG:900913");
	var displayProjection = new OpenLayers.Projection("EPSG:4326");
		
	bounds = myMapManager.getLayerExtent();
	
	var options = {
		projection : projection,
		displayProjection : displayProjection,		
		units : "m",		
		controls : [],
		maxExtent : bounds,
		maxResolution : 0.03119921875
	};

	map = new OpenLayers.Map('map', options);
		
	map.addLayers([ gphy, gmap, ghyb, gsat ]);
	
	if(filter && filter!=""){
		var filterParams = {cql_filter:filter};
		openDataLayer.mergeNewParams(filterParams);
	}
	
	//optional base layer
	if (otherMapUrl!=undefined){
		
		for (var i=0; i<otherMapUrl.length; i++){
        					
			var otherLayer = myMapManager.createWMSOtherLayer(otherMapUrl[i].url,otherMapUrl[i].layers,format);		
			
			map.addLayer(otherLayer);
		}
	}
		
	openDataLayer.events.register("loadstart", openDataLayer, layerLoadStart);
	openDataLayer.events.register("tileloaded",openDataLayer, layerLoaded);
	openDataLayer.events.register("loadend", openDataLayer, layerLoaded);
		
	map.addLayer(openDataLayer);		
	
	var selectStyle = new OpenLayers.StyleMap({
         "default": new OpenLayers.Style({
        	 fillColor: "#ffff00",
             strokeColor: "#3399ff",
             fillOpacity: 0.5,
             graphicZIndex: 2 
         })
     });
     var infoStyle = new OpenLayers.StyleMap({
         "default": new OpenLayers.Style({
        	 fillColor: "#ffff00",
             strokeColor: "#ffff00",
             fillOpacity: 0.5,
             graphicZIndex: 2 
         })
     });
     	 
	 highlightLayer = new OpenLayers.Layer.Vector();
	 highlightLayer.styleMap=infoStyle;
     map.addLayer(highlightLayer);
      
     highlightLayer2 = new OpenLayers.Layer.Vector();
     highlightLayer2.styleMap=selectStyle;
     map.addLayer(highlightLayer2);
     
     var identifyVendorParams=null;
     if(filter && filter!=""){
     	identifyVendorParams = {"CQL_FILTER": filter};
     }
    
     mapControls ={
     	identify: new OpenLayers.Control.WMSGetFeatureInfo({
            url: geoServerUrl,
            layerUrls: [geoServerUrl],
            title: 'Identify features',
            drillDown: true,
            layers: [openDataLayer],
            queryVisible: true,
            maxFeatures:100,
            infoFormat: 'application/vnd.ogc.gml',
            vendorParams:identifyVendorParams
        })
     }
     
     mapControls["identify"].events.register("getfeatureinfo", this, getFeatureInfoRslt);
     
     for(var key in mapControls) {
		control = mapControls[key];		
		map.addControl(control);
     }  
    
    map.addControl(new OpenLayers.Control.Navigation());
	
	map.addControl(new OpenLayers.Control.MousePosition({
		div : document.getElementById("coord")
	}));	
	map.addControl(new OpenLayers.Control.Zoom());	
	map.addControl(new OpenLayers.Control.Scale(
			document.getElementById("map_scale_div"),
			{
				updateScale: function() {
			        scale = this.map.getScale();
			        if (!scale) {
			            return;
			        }			        
			        scale = Math.round(scale);			  			 
			        this.element.innerHTML = "<b>Scala</b> 1:" + $.format.number(scale,'#,###');
			    }
			}
			));
	var overviewMapSize =  new OpenLayers.Size(80, 80);
	var overviewControl = new OpenLayers.Control.OverviewMap({ size: overviewMapSize, div: document.getElementById("overviewPanel") });	
    map.addControl(overviewControl);	
    
    if (!map.getCenter()) {
    	if(bounds){
    		map.zoomToExtent(bounds);
    	}
    }
		
    activateControl("identify");

    var pl = new SOAPClientParameters();
    pl.add("dataUid",dataId);
	
    SOAPClient.invoke(wsOpenDataETLUrl, "getAliases", pl, true, getAliasesHandler);

	if (isMapThematization=="false"){
		doClassify(true);
	}
		
}


function handleMeasurements(event) {
	var geometry = event.geometry;
	var units = event.units;
	var order = event.order;
	var measure = event.measure;
	var element = document.getElementById('output');
	var out = "";
	if (order == 1) {
		out += "Lunghezza " + measure.toFixed(3) + " " + units;
	} else {
		out += "Area " + measure.toFixed(3) + " " + units + "2";
	}
	element.innerHTML = out;
}

function activateControl(element_name){	
	for (key in mapControls) {
		var control = mapControls[key];
		if (element_name == key) {
			control.activate();
		} else {
			control.deactivate();
		}
	}
}

function prepareDialogs(){		
	$("#identifyPanel").load(externalPagesManager.getIdentifyResultPanel(), function () {  
		$( "#identifyRslt" ).dialog({
			autoOpen: false,
			height: 300,
			width: 505,
			modal: false,
			minWidth: 505,
			maxWidth: 505,
			buttons: {			
				"Chiudi": function() {					
					$( this ).dialog( "close" );
				}
			},
			close: function() {
				$("#identifyRslt").innerHTML="";
				highlightLayer.destroyFeatures();
				highlightLayer2.destroyFeatures();
			}
		});
	});
	
	loadOpacityPanel();	
	
	$('#classifyPanel').load(externalPagesManager.getClassifyFormPanel(), function () {
			
			$( "#classifyParams" ).dialog({
				autoOpen: false,
				height: 350,
				width: 410,
				modal: true,
				buttons: {					
					"Classifica": function() {
						doClassify(false);
						$( this ).dialog( "close" );
					},
					"Annulla": function() {
						$( this ).dialog( "close" );
					}
				},
				close: function() {			
				}
			});
		});
				
		$("#legendPanel").load(externalPagesManager.getLegendPanel(), function (){
			
			attachLegendListener();
			
		});
				
		$("#legendBorderPanel").load(externalPagesManager.getLegendBorderPanel(), function (){
			
			attachLegendBorderListener();
			
		});	
		
}

function loadOpacityPanel(){
	//trasparenza		
	$("#opacityPanel").load(externalPagesManager.getOpactiyPanel(), function (){		
		$( "#slider" ).slider({
			value: myMapManager.initialLayerOpacity*100,
			min: 0,
			max: 100,
			step: 1,
			slide: function( event, ui ) {
				$( "#opacity" ).text( ui.value + "%" );							
			},
			stop: function( event, ui ) {
				myMapManager.setLayerOpacity(openDataLayer, ui.value/100);					
				myMapManager.initialLayerOpacity = ui.value/100;
			}			
		});
		$( "#opacity" ).text( $( "#slider" ).slider( "value" ) + "%" );		
	});
}

function loadSymbolSettings(){
	//trasparenza		
	$("#symbolPanel").load(externalPagesManager.getSymbolSettings(), function (){		
			$( "#symSettings" ).text("aaaaa");		
	});
}

function openOverviewMap(){
	$(".olControlOverviewMapElement").show();	
	$("#overviewPanelBtn").removeClass("closed");
	$("#overviewPanelBtn").attr('title', 'Chiudi');
}

function closeOverviewMap(){
	$(".olControlOverviewMapElement").hide();	
	$("#overviewPanelBtn").addClass("closed");
	$("#overviewPanelBtn").attr('title', 'Apri la mappa di overview');
}

function getAliasesHandler(r){
       
	var _X2JS = new X2JS();
    var obj = _X2JS.xml_str2json(r);
    var res = obj.Envelope.Body.getAliasesResponse.getAliasesReturn;
        
	layerSchemaDict = {};

    var aliases = undefined;
    if (res != undefined){         
    	aliases = jQuery.parseJSON(res);       
    	
		for (var i=0; i<aliases.aliases.length; i++)
    	{
    		layerSchemaDict[aliases.aliases[i].physicalName] = aliases.aliases[i];    		
    	}
    }
};


/**
 * Restituisce l'hashCode di una stringa (Ã¨ esattamente l'implementazione JAVA)
 *
 * @param {String} stringa del quale si vuole l'hashcode
 *
 * @return {String} hashcode della stringa passata
 */
function getHashCode(str) {
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

function getMapOption(){
	var option = {};
	
	if (layerName != "undefined"){
		
		option.opacity = myMapManager.initialLayerOpacity;
		
		switch (map.baseLayer.type){
			case "terrain":
				option.mapType = "phy";
				break;
			case "roadmap":
				option.mapType = "street";
				break;
			case "satellite":
				option.mapType = "sat";
				break;
			case "hybrid":
				option.mapType = "hyb";
				break;
		 	default:
		 		option.mapType = "street";
		 	}
		
		option.numClasses = $("#numClasses").val()? $("#numClasses").val() : def_num_classes;
		
		var palette_name = $('input:radio[name=palettes]:checked').val() ?  $('input:radio[name=palettes]:checked').val() : def_palette_name;
		if (def_inverted_palette)
		{
			palette_name = palette_name+"Inverted";	
		}		
		option.paletteName = palette_name;
		
		option.borderColor = def_border_color;
		option.borderWidth = def_border_width;
		option.borderStyle = def_border_style;
	}
	else{
		option.KML_fileURL = kmlUrl;
	}
	
	return option;
};
	

function layerLoaded(e) {    
	//console.log("layerLoaded");
    $("#loadingPanel").hide(); 
};

function layerLoadStart(e){
	//console.log("layerLoadStart");
	$("#loadingImg").center();
	$("#loadingPanel").show();
};

function tileLoaded(e){
	//console.log("tileLoaded");
}

jQuery.fn.center = function () {
    this.css("position","absolute");
    this.css("top", Math.max(0, (($(window).height() - $(this).outerHeight()) / 2) + 
                                                $(window).scrollTop()) + "px");
    this.css("left", Math.max(0, (($(window).width() - $(this).outerWidth()) / 2) + 
                                                $(window).scrollLeft()) + "px");
    return this;
}
