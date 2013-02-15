var strContent = "";
var dimensionVal="";	
/*GET FEATURE INFO*/
function getFeatureInfoRslt(response){	
	
	if (response.features && response.features.length) {		 
        highlightLayer.destroyFeatures();
        highlightLayer2.destroyFeatures(); 
		highlightLayer.addFeatures(response.features);
        highlightLayer.redraw();
    
		$("#identifyRslt").html("");
		var frag = document.createDocumentFragment();
		var featureInfo = document.createElement("DIV");
		featureInfo.setAttribute("id", "feature_info_result");
		
		var featInfoUl =  document.createElement("ul");
		featInfoUl.setAttribute("class", "external");		
		var infoSpan = document.createElement("SPAN");
		infoSpan.setAttribute("id","InfoSpan");
		getInfoTabsChildContent(response);			
		infoSpan.innerHTML = strContent;		
		featureInfo.appendChild(infoSpan);
		frag.appendChild(featureInfo);
				
		$("#identifyRslt").append(frag);
		$("#identifyRslt").dialog("open");		
                    
		showLayerFeatureDetails(0);		
	}
}

function highlightFeature(fid){
	if(fid!=null){
		var feature = highlightLayer.getFeatureByFid(fid);
		highlightLayer2.destroyFeatures(); 
		highlightLayer2.addFeatures([feature.clone()]);
		highlightLayer2.redraw();	
	}
}

function zoomToIdentifiedFeatures(){	
	map.zoomToExtent(highlightLayer.getDataExtent());
}

function getInfoTabsChildContent(response){
		
	if (response.features && response.features.length) {	
		
		strContent = "<div id='infoMainContainerDiv'>";		
        strContent += "<div id='infoContentDiv' class='infoContentDiv'>";
        strContent += "<div id='infoContent' class='divInfo'>";
		
         var objRes = "var objRes={"+response.features+"};";
         if (response.features.length > 0) {

            strContent += "<table class='tbInfo'>";
            
			strContent += "<tr><td colspan=2 class=\"ZoomTo\"><a href=\"javascript:zoomToIdentifiedFeatures()\"><img src=\"img/zoom_to.png\" draggable=\"false\" title=\"Zoom a tutti gli elementi identificati\"</a></td></tr>";            

             for (var j = 0, len = response.features.length; j < len; j++) {

                var elem = response.features[j];
                
                strContent +="<tr><td class='tdInfoElem' id='tdInfo_"+j+"'><a href=\"javascript:highlightFeature('" + elem.fid + "');showLayerFeatureDetails("+j+");\">elemento: " +(j+1)+ "</a></td></tr>"

            }
            strContent += "</table>";     
                  
        }
        strContent +="</div>";
        strContent +="</div>";
        strContent += "<div id='infoDetailContainerDiv' class='infoDetailContainerDiv'></div></div> ";                        

	}	
}

function showLayerFeatureDetails(pos){
	
    var strDetContent = "<table class='tbInfoDetail'>";

	var elem = highlightLayer.features[pos].attributes;
    var alternateRow = 0;
    var alias = "";
    var value = "";
    var lat=0;
    var lon=0;
        
	if (layerSchemaDict!=undefined)
  	{
		for (attr in elem)
  		{  			
  			alias = "";  			
  			value = elem[attr];
  						
  			if (typeof(layerSchemaDict[attr]) == 'undefined' )
  			{  				
	  			if(attr.toUpperCase()==elementCountField.toUpperCase())
  				{
  					alias = "Numero di elementi";
  				}
  				else if (descriptionFieldDict[attr] != undefined)
  				{
  					 alias = descriptionFieldDict[attr];	  				 
    		    }    	      				
  			}	
	  		else
  			{
  				if (layerSchemaDict[attr].alias == 'DO_X')
  				{
  					alias = 'Longitudine';
  				}
  				else if (layerSchemaDict[attr].alias == 'DO_Y'){
  					alias = 'Latitudine';  					
  				}  				
  				else{
  					alias = layerSchemaDict[attr].alias;
  				}  				
  				  			  				
  				if (layerSchemaDict[attr].type != 'GENERIC_COLUMN') {
  					
  					if (layerSchemaDict[attr].type == 'DIMENSION'){
  						var pl = new SOAPClientParameters();
    					pl.add("dataDimId",layerSchemaDict[attr].id);
    					
    					var strNumber = (Number(elem[attr])).toFixed(0);
    					pl.add("code",strNumber);
						
						dimensionVal="";
						
    					SOAPClient.invoke(wsOpenDataETLUrl, "getDimDescription", pl, false, getDimDescriptionHandler);
    					value = dimensionVal;
  					}  	
  					else{				  					
	  					switch (layerSchemaDict[attr].columnType ) {
  							case "NUMBER":
	  						case "FLOAT": 
  							case "CURRENCY": 
  								var strNumber = (Number(elem[attr]));
	  							if (layerSchemaDict[attr].decimals>=0 && layerSchemaDict[attr].decimals<=20){
    								strNumber = strNumber.toFixed(layerSchemaDict[attr].decimals);
    							}
    							value=addSeparatorsNF(strNumber,".",",",".");    						
		  						break; 
		
	  						default:  //STRING   						
						}
					}
				}
				if (layerSchemaDict[attr].type == 'GENERIC_COLUMN' || layerSchemaDict[attr].type == 'OTHERS'){
					value = getTextWithLinksAndMailFormatted(elem[attr]);
				}				
				 				 		
	  		}
	  		if (alias!="")
	  		{	  	
	  			strDetContent += "<tr class='trInfoDetail_"+alternateRow%2+"'>";		
		  		if (alias == def_var_lbl) {  					
					strDetContent += "<th class='thInfoDetailClass'>" + alias +"</th>";
				}
	  			else {
	  				strDetContent += "<th class='thInfoDetail'>" + alias +"</th>";
				}
	  			strDetContent += "<td>"+ value +"</td>";
	  			strDetContent += "</tr>";    
  				alternateRow ++;
  				if (alias == 'Longitudine'){lon=value;}
  				if (alias == 'Latitudine'){lat=value;}
  			}  									  			
  		}
  		strGoogleStreetRow = "";
  		if (lon>0 && lat>0){
  			strGoogleStreetRow = "<tr class='trInfoDetail_"+1+"'><th class='thInfoDetail'>Street View</th>";
  			strGoogleStreetRow += "<td><a alt='Vedi su Google Street View' title='Vedi su Google Street View' ";
  			strGoogleStreetRow += "href='http://maps.google.it/maps?q="+lat+","+lon+"&z=18&layer=c&cbll="+lat+","+lon+"&cbp=11,0,0,0,0' ";
  			strGoogleStreetRow += "target='_blank'><img height='25' width='25' style='vertical-align:middle' src='/sites/all/modules/spodata/metadata/viewer/multidimensional_viewer/img/streetView.png'></a></td></tr>";
  		} 
  		strDetContent =  strDetContent.replace("<table class='tbInfoDetail'>","<table class='tbInfoDetail'>"+strGoogleStreetRow);
	}    
	else
	{
		for (attr in elem)
  		{
  			alias = attr;
  			value = elem[attr];
  			
  			strDetContent += "<tr class='trInfoDetail_"+alternateRow%2+"'>";
  			if(attr.toUpperCase()==elementCountField.toUpperCase())
  			{
  				alias = "Numero di elementi";  				
  			}
  			
    		if (alias == def_var_lbl) {  					
				strDetContent += "<th class='thInfoDetailClass'>" + alias +"</th>";
			}
  			else {
  				strDetContent += "<th class='thInfoDetail'>" + alias +"</th>";
			}
			
  			strDetContent += "<td>"+ getTextWithLinksAndMailFormatted(value); +"</td>";  
  			
  		}
	}       
    strDetContent += "</table>";  
    $("#infoDetailContainerDiv").html(strDetContent);
    
    for (var i = 0; i < highlightLayer.features.length; i++) {
		var elem = $("#tdInfo_"+i);
        if (elem!=undefined) {
        	if (i==pos) {
            	elem.attr("class","tdInfoElemSel");
			}
            else {
            	elem.attr("class","tdInfoElem");
			}
        }
    }       
}

/**Fornisce l'etichetta di una dimensione 
 * dato il suo dataDimId e il codice.
 */
function getDimDescriptionHandler(r){
	var _X2JS = new X2JS();
    var obj = _X2JS.xml_str2json(r);
    var res = obj.Envelope.Body.getDimDescriptionResponse.getDimDescriptionReturn;
	dimensionVal = r;
}

/**
* Formatta le stringhe in numeri
* nStr=stringa da formattare
* inD=separatore decimale in ingresso
* outD=separatore decimale in uscita
* sep=separatore migliaia
* restituisce la stringa formattata
*/
function addSeparatorsNF(nStr, inD, outD, sep) {
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
* Formatta le stringhe contenenti link o indirizzi email aggiungendo l'href o il mail-to
* test=stringa da formattare
* restituisce la stringa formattata
*/
function getTextWithLinksAndMailFormatted(text) { 
	var linkExp = /(\b(https?|ftp|file):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
	text =  text.replace(linkExp,'<a style="color:blue;" target="_new" href="$1">$1</a>');

	var mailExp = /(\b(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,})))/;
	text =  text.replace(mailExp,'<a style="color:blue;" href="mailto:$1">$1</a>');
	
	return text;
};

