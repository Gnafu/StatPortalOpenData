// this assumes that the Map object is a JavaScript variable named "map"
var print_wait_win = null;
var max_google_h = 480;
var max_google_w = 640;
var mapImageName = "";
var mapImageHeight = 480;
var mapImageWidth = 640;

function mapimageexportComplete(url) {
	var token = username + ":" + password;    
	var authCode = "Basic " + $.base64.encode(token);
    var _methodUrl =  geoServerRESTUrl + "mapImageExport?"+mapImageName;			
	var jqrmiep = $.ajax( {
		url: _methodUrl,
		type : "GET",		
		dataType: "json",	
		crossDomain : true,		
	 	headers : {
			"Authorization" : authCode
		}
	})
	.done(mapimageexportPathComplete)
	.fail(mapimageexportPathError); 	
}

function mapimageexportError(data){
	printedMap();
	alert("Si è verificato un errore durante l'export");
}

function mapimageexportPathComplete(url) {
	printedMap();
	window.open(url.fileName,"_blank","menubar=0,scrollbars=0,location=0,toolbar=0,resizable=0,width="+mapImageWidth+",height="+mapImageHeight);	
}

function mapimageexportPathError(data){
	printedMap();
	alert("Si è verificato un errore durante lo scaricamento dell'immagine");
}

function PrintMap() {
    printingMap();
    // go through all layers, and collect a list of objects
    // each object is a tile's URL and the tile's pixel location relative to the viewport
    var offsetX = parseInt(map.layerContainerDiv.style.left);
    var offsetY = parseInt(map.layerContainerDiv.style.top);
    var size  = map.getSize();    
    
    var offsetSizeX =0;
    if (size.w>max_google_w){offsetSizeX=size.w-max_google_w};
    var offsetSizeY =0;
    if (size.h>max_google_h){offsetSizeY=size.h-max_google_h};    
        
    var tiles = [];
    for (layername in map.layers) {
        // if the layer isn't visible at this range, or is turned off, skip it
        var layer = map.layers[layername];
        if (!layer.getVisibility()) continue;
        if (!layer.calculateInRange()) continue;
        // iterate through their grid's tiles, collecting each tile's extent and pixel location at this moment        
        if (layer.name!=undefined && layer.name.indexOf("Google")>=0){ 
        	var urlGoogleMap = "http://maps.googleapis.com/maps/api/staticmap?";
        	urlGoogleMap += "maptype="+ layer.type;
        	var center = layer.getMapObjectCenter();
        	urlGoogleMap += "&center="+ center.lat() +","+center.lng();
        
        	urlGoogleMap += "&zoom="+ layer.getMapObjectZoom();
        	urlGoogleMap += "&size="
        	urlGoogleMap+=(size.w>max_google_w)? max_google_w : size.w;
        	urlGoogleMap+="x";
        	urlGoogleMap+=(size.h>max_google_h)? max_google_h : size.h;
        	urlGoogleMap += "&sensor=false";        	
	        var urlG = urlGoogleMap;			
			var opacityG  = 255;
	        tiles[tiles.length] = {alpha:opacityG, url:urlG, requestType:'GET'};
        }
        else {	        
	    	if (layer.grid != undefined && layer.grid.length>0) {
	        	var tile = layer.grid[0][0];
	            var url = layer.getURL(tile.bounds);
				if (offsetSizeX>0 || offsetSizeY>0){					
	                var idxB = url.indexOf("&BBOX=");
	                if (idxB>=0){
	                	var bbox_str = url.substring(idxB+1);
	                	idxB = bbox_str.indexOf("&");
	                	if (idxB>=0){
	                		bbox_str = bbox_str.substring(0,idxB);
	                	}
	                	
	                	var arrBBox= bbox_str.substring(5).split(",");	                	
	                	var pMin = map.getLonLatFromPixel(new OpenLayers.Pixel(offsetSizeX/2,offsetSizeY/2));	                		
	                	var pMax = map.getLonLatFromPixel(new OpenLayers.Pixel(size.w-(offsetSizeX/2),size.h-(offsetSizeY/2)));	                	
	                	url = url.replace("&"+bbox_str, "&BBOX="+pMin.lon+","+pMax.lat+","+pMax.lon+","+pMin.lat);
	                	if(offsetSizeX>0){
	                		
	                		var idxW = url.indexOf("&WIDTH=");
	                		if (idxW>=0){
	                			var width_str = url.substring(idxW+1);
	                			idxW = width_str.indexOf("&");
	                			if (idxW>=0){
	                				width_str = width_str.substring(0,idxW);
	                			}
	                			url = url.replace("&"+width_str,"&WIDTH="+max_google_w);
	                		}
	                	}
	                	if(offsetSizeY>0){
	                		
	                		var idxH = url.indexOf("&HEIGHT=");	                		
	                		if (idxH>=0){
	                			var height_str = url.substring(idxH+1);
	                			idxH = height_str.indexOf("&");
	                			if (idxH>=0){
	                				height_str = height_str.substring(0,idxH);
	                			}
	                			url = url.replace("&"+height_str,"&HEIGHT="+max_google_h);
	                		}
	                	} 	                	
	                }
	                
	                var opacity  = layer.opacity ? parseInt(255*layer.opacity) : 255;
	                tiles[tiles.length] = {alpha:opacity, url:url, requestType:'POST'};
	            }
	        }
        }
    }
    
    // hand off the list to our server-side script, which will do the heavy lifting       
    var token = username + ":" + password;   
	var authCode = "Basic " + $.base64.encode(token);
    var _methodUrl =  geoServerRESTUrl + "mapImageExport";
    mapImageName = GUID()+".png";
    
    mapImageWidth = (size.w-offsetSizeX);
    mapImageHeight = (size.h-offsetSizeY);
	var params = {fileName:mapImageName,width:mapImageWidth,height:mapImageHeight,layers:tiles};
	var paramJson = JSON.stringify(params);
			
	var jqrmie = $.ajax( {
		beforeSend: function (xhr) { xhr.setRequestHeader ("Authorization", authCode); },
		type : "POST",	
		data: paramJson,
		url: _methodUrl,
		dataType: "text",
		contentType: "application/json",			
	 	crossDomain : true		
	})
	.done(mapimageexportComplete)
	.fail(mapimageexportError); 	    
}

function GUID ()
{
    var S4 = function ()
    {
        return Math.floor(
                Math.random() * 0x10000 /* 65536 */
            ).toString(16);
    };

    return (
            S4() + S4() + "-" +
            S4() + "-" +
            S4() + "-" +
            S4() + "-" +
            S4() + S4() + S4()
        );
}

function printingMap() {
	$("#loadingImg").center();
	$("#loadingPanel").show();    	    
};

function printedMap(){	
	$("#loadingPanel").hide();
};