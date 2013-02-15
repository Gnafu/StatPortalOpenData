var SISTER = SISTER || {};

SISTER.namespace = function (ns_string) {
    var parts = ns_string.split("."),
        parent = SISTER,
        i;    
    if (parts[0] === "SISTER") {
        parts = parts.slice(1);
    }
    for (var i = 0, max = parts.length; i < max; i++) {        
        if (typeof parent[parts[i]] === "undefined") {
            parent[parts[i]] = {};
        }
        parent = parent[parts[i]];
    }
    return parent;
};
SISTER.namespace("SISTER.Utils.Map");
SISTER.namespace("SISTER.Utils.Operations");
SISTER.namespace("SISTER.Utils.DOM");

SISTER.Utils.Map.MapManager = function MapManager(){
	var _layerExtent=null;
	
	this.initialLayerOpacity = 0.7;
	
	this.getLayerExtent=function getLayerExtent(){
		return _layerExtent;
	};

	this.createWMSLayer = function createWMSLayer(url, workspace,layer,format){
		var _wms_layer;
		_wms_layer = new OpenLayers.Layer.WMS(layer, url, {
			layers : workspace + ":" + layer,
			format : format,
			tiled : false,
			transparent : true,
			legend : true
		}, {		   
			unsupportedBrowsers: [],
			singleTile : true,
			ratio : 1,
			opacity : this.initialLayerOpacity,
			isBaseLayer : false,
			visibility : true,
			tileOptions: {maxGetUrlLength: 2048}			
		});
		return _wms_layer;		
	};
	
	this.createWMSOtherLayer = function createWMSOtherLayer(url, layer,format){
		var _wms_layer;
		_wms_layer = new OpenLayers.Layer.WMS(layer, url, {
			layers : layer,
			format : format,
			tiled : false,
			transparent : true,
			legend : true
		}, {		   
			unsupportedBrowsers: [],
			singleTile : true,
			ratio : 1,
			opacity : this.initialLayerOpacity,
			isBaseLayer : false,
			visibility : true,
			tileOptions: {maxGetUrlLength: 2048}			
		});
		return _wms_layer;			
	};
	
	this.setLayerOpacity = function setLayerOpacity(layer,opacity){	
		
		layer.setOpacity(opacity);
		layer.redraw();	
	};
    
	this.getFeaturesExtent = function getFeaturesExtent(getExtentMethod,workspaceName,layerName,filter){
		var token = username + ":" + password;
		var authCode = "Basic " + $.base64.encode(token);
		var _methodUrl = getExtentMethod + "?" + workspaceName + "," + layerName;
		if(filter && filter!=""){
			_methodUrl += "," + filter;
		}
		
		$.ajax( {
			url: _methodUrl,
			type : "GET",			
			async:false,
		  	crossDomain : true,
		  	headers : {
				"Authorization" : authCode
			},
			dataType: "xml"
		})		
		.done(function (obj){
			if(obj){		
				var xml = operationManager.unparseXml(obj);	
				var _X2JS = new X2JS();				
			    var jsonEnvelope = _X2JS.xml_str2json( xml );
				if(jsonEnvelope.Envelope){
					var ext = jsonEnvelope.Envelope;
					var bounds = new OpenLayers.Bounds();
					bounds.left= parseFloat(ext.xmin);
					bounds.bottom= parseFloat(ext.ymin);
					bounds.right= parseFloat(ext.xmax);
					bounds.top= parseFloat(ext.ymax);
					
					_layerExtent = bounds;
				}
			}
		});
	};
	
	this.getWMSLayerExtent = function getWMSLayerExtent(geoserverUrl,layer){		
		var token = username + ":" + password;
		var authCode = "Basic " + $.base64.encode(token);		
		
		var jqxhr = $.ajax( {
			url: geoserverUrl + "layers/" + layer + ".json",
			type : "GET",	
			async:false,
		 	crossDomain : true,
		 	headers : {
				"Authorization" : authCode
			}		
		})
		.done(function (obj){
			if(obj){
				var token = username + ":" + password;
				var authCode = "Basic " + $.base64.encode(token);
				if(obj.layer){
					var _url = obj.layer.resource.href;
					if(_url){
						var jqxhr = $.ajax( {
							url: _url,
							type : "GET",			
						 	crossDomain : true,
						 	headers : {
								"Authorization" : authCode
							}							
						})
						.done(function (obj){
							if(obj){					
								if(obj.featureType){
									var ext = obj.featureType.nativeBoundingBox;
									var bounds = new OpenLayers.Bounds();
									bounds.left= ext.minx;
									bounds.bottom= ext.miny;
									bounds.right= ext.maxx;
									bounds.top= ext.maxy;
									
									_layerExtent = bounds;
								}
							}
						});
					}
				}
			}
		});		
	};
	
};

SISTER.Utils.Operations.OperationManager = function OperationManager(){
		
	this.unparseXml = function unparseXml(xmlSource){
		var xmlStr;
		if(xmlSource.xml!=null){
			xmlStr= xmlSource.xml;
		}else{
			xmlStr = (new XMLSerializer()).serializeToString(xmlSource);
		}
		if(xmlStr==="")
			return null;
		return xmlStr;
	};
	
};

SISTER.Utils.DOM.DOMManager = function DOMManager() {
   
	this.createLegendAndBorderItemDiv = function createLegendAndBorderItemDiv(geometry_type, fill_color, border_color, border_width, border_style, label){
		var legend_item;
        if (document.getElementById && document.createElement) {
           
        	legend_item = document.createElement("DIV");        
        	legend_item.setAttribute("class", "legendItem");
            var item_symbol = document.createElement("DIV");
            item_symbol.setAttribute("class", "legendItemSymbol");
            var symbol_style;
            switch (geometry_type) {
	            case "point":
	            	symbol_style = {
	            		'background-color' : fill_color,	      
	            		'width': '10px',
		            	'height': '10px',
		            	'border-radius': '10px',
		            	'margin-top': '4px'
            	    };
	                break;
	            case "line":
	            	symbol_style = {
            	      'background-color' : fill_color,	      
            	      'width': '20px',
            	  	  'height': '4px',
            	  	  'margin-top': '6px'
            	    };
	            	break;
	            case "polygon":
	            	symbol_style = {
            	      'background-color' : fill_color,	  
            	      'border-color' : border_color,  
            	      'border-width' : border_width,
            	      'border-style' : border_style,   
            	      'width': '20px',
            	  	  'height': '20px'
            	    };
	            	break;
	            default:
	            	symbol_style = {
	            	    };
	            	break;
            }	            
            $(item_symbol).css(symbol_style);
            var item_label = document.createElement("SPAN");
            item_label.setAttribute("class", "legendItemLabel");
            $(item_label).append(label);
            legend_item.appendChild(item_symbol);
            legend_item.appendChild(item_label);
        }
        return legend_item;
	}
	
	
    this.createLegendItemDiv = function createLegendItemDiv(geometry_type, fill_color, label) {
        var legend_item;
        if (document.getElementById && document.createElement) {
            
        	legend_item = document.createElement("DIV");        
        	legend_item.setAttribute("class", "legendItem");
            var item_symbol = document.createElement("DIV");
            item_symbol.setAttribute("class", "legendItemSymbol");
            var symbol_style;
            switch (geometry_type) {
	            case "point":
	            	symbol_style = {
	            		'background-color' : fill_color,	      
	            		'width': '10px',
		            	'height': '10px',
		            	'border-radius': '10px',
		            	'margin-top': '4px'
            	    };
	                break;
	            case "line":
	            	symbol_style = {
            	      'background-color' : fill_color,	      
            	      'width': '20px',
            	  	  'height': '4px',
            	  	  'margin-top': '6px'
            	    };
	            	break;
	            case "polygon":
	            	symbol_style = {
            	      'background-color' : fill_color,	      
            	      'width': '20px',
            	  	  'height': '20px'
            	    };
	            	break;
	            default:
	            	symbol_style = {
	            	    };
	            	break;
            }	            
            $(item_symbol).css(symbol_style);
            var item_label = document.createElement("SPAN");
            item_label.setAttribute("class", "legendItemLabel");
            $(item_label).append(label);
            legend_item.appendChild(item_symbol);
            legend_item.appendChild(item_label);
        }
        return legend_item;
    };
    
    this.createLabelItemDiv = function createLabelItemDiv(label) {
        var item;
        if (document.getElementById && document.createElement) {
            
        	item = document.createElement("DIV");        
        	item.setAttribute("class", "labelItem");
        	
            var item_label = document.createElement("SPAN");
            item_label.setAttribute("class", "itemLabel");
            $(item_label).append(label);            
            item.appendChild(item_label);
        }
        return item;
    };
    
    this.createIdentifyFeatureItem = function createIdentifyFeatureItem(feature){
    	var frag = document.createDocumentFragment();
    	var feature_li =  document.createElement("li");
		feature_li.setAttribute("class", "type");		
		feature_li.innerHTML = "<b><a href=\"javascript:highlightFeature('" + feature.fid + "')\">Evidenzia l'elemento:</a></b>";
		var attributes =  document.createElement("ul");
		$.each(feature.attributes, function(att_name, att_value) {
			if(att_name!=null){
				var attribute_li =  document.createElement("li");
				attribute_li.setAttribute("class", "attribute");
				var num = Number(att_value);
				var att_lbl = att_name;
				
				var idx_field = -1;
				if(att_name===def_var_name){
					idx_field=0;
					att_lbl = def_var_lbl;
				}
				
				if((idx_field>=0)&&($.isNumeric(num))){
					attribute_li.innerHTML = "<b>" + att_lbl + "</b>: " + $.format.number(num, '#,##0.##');
				}
				else if($.isNumeric(num)){
					attribute_li.innerHTML = "<b>" + att_lbl + "</b>: " + num;
				}
				else{
					attribute_li.innerHTML = "<b>" + att_lbl + "</b>: " + att_value;
				}				
				attributes.appendChild(attribute_li);
			}
		});
    	feature_li.appendChild(attributes);
    	frag.appendChild(feature_li);
    	var hr =  document.createElement("HR");
    	frag.appendChild(hr);
    	return frag;    	
    };
};

SISTER.Utils.DOM.ExternalPagesManager = function ExternalPagesManager() {
    
    var _legend_panel = "components/legend.html #legendPanelContent";
    var _legend_border_panel = "components/legendBorder.html #legendBorderPanelContent";
    var _classify_frm = "components/frmClassify.html #classifyParams";
    var _identify_rslt = "components/identifyRslt.html #identifyRslt";    
    var _opacity_panel = "components/opacity.html #opacityPnl";
    
    this.getLegendPanel = function getLegendPanel() {
        return _legend_panel;
    };
    
    this.getLegendBorderPanel = function getLegendBorderPanel() {
        return _legend_panel;
    };
    
    this.getClassifyFormPanel = function getClassifyFormPanel() {
        return _classify_frm;
    };
    
    this.getIdentifyResultPanel = function getIdentifyResultPanel() {
        return _identify_rslt;
    };
    
    this.getOpactiyPanel = function getOpacityPanel() {
        return _opacity_panel;
    };
   
};