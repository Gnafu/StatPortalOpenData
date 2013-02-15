/* CLASSIFY */
function openClassifyParameters(){
	//set default or last value		
	setClassifyParameters();	
	$("#numClasses").combobox();
	//disable all autocomplete inputs
	$("#classifyParams .ui-autocomplete-input").keypress(function(evt) { evt.preventDefault(); }); //.attr('disabled', true);
    
    if (def_var_lbl!= ""){    
    	$("#classifyParams #classifyParamsCl").show();
    	$("#classifyParams").dialog({title: 'Imposta parametri per la classificazione', height: 350, width: 410});    	
    }
    else {
    	$("#classifyParams #classifyParamsCl").hide();
    	$("#classifyParams").dialog({title: 'Imposta parametri', height: 150, width: 410});
    }
	$("#classifyParams").dialog("open");
		
}

function setClassifyParameters(){
	if (def_var_lbl!= ""){
		$("#var_name").text(def_var_lbl);
		$("#numClasses").find("option[value='"+def_num_classes+"']").attr('selected',true);	
		SetCheckInvertedValueAndPalette();	
		$("#palettes").find("input[value='"+def_palette_name+"']").attr('checked',true);
	}
	
	$("#borderWidth").find("option[value='"+def_border_width+"']").attr('selected',true);
	$("#styleB").find("input[value='"+def_border_style+"']").attr('checked',true);	
	
	//set color
	valFromRBG = def_border_color.split("_");
	if (valFromRBG.length==3 && document.getElementById('borderColor')!=undefined){		
		(new jscolor.color(document.getElementById('borderColor'), {})).fromRGB(valFromRBG[0]/255 , valFromRBG[1]/255 , valFromRBG[2]/255);		
	}
}
		
function doClassify(isFirstTime) {	
	$("#spinner").show();
	var token = username + ":" + password;
	var authCode = "Basic " + $.base64.encode(token);
	//set def_inverted_palette and def_palette_name
	var num_classes = def_num_classes;	
	if (!isFirstTime){
		def_inverted_palette = $('input:checkbox[name=invertedClasses]:checked').val() ? true : false;
		def_palette_name =$('input:radio[name=palettes]:checked').val() ?  $('input:radio[name=palettes]:checked').val() : def_palette_name;
		num_classes=$("#numClasses").val()? $("#numClasses").val() : def_num_classes;
	}
		
	var palette_name = def_inverted_palette ?  def_palette_name+"Inverted" : def_palette_name;
	
	var _methodUrl = classificationRESTMethodURL + "?" + workspaceName + "," + layerName;
	 
	if (def_var_name!="")
	{
		_methodUrl = classificationRESTMethodURL + "?" + workspaceName + "," + layerName + "," + def_var_name + "," + num_classes + "," + palette_name;
		
		if(filter && filter!=""){
			_methodUrl += "," + filter;
		}	
	}
		
	//set border configuration
	def_border_color = $("#borderColor").val()? hexToServerRgb($("#borderColor").val()) : def_border_color;
	def_border_width = $("#borderWidth").val()? $("#borderWidth").val() : def_border_width;	
	def_border_style = $('input:radio[name=styleB]:checked').val() ?  $('input:radio[name=styleB]:checked').val() : def_border_style ;//$("#borderStyle").val()? $("#borderStyle").val() : def_border_style;	
	
	_methodUrl += ","+def_border_color+"," +def_border_width+","+def_border_style;

	var jqxhr = $.ajax( {
		url: _methodUrl,
		type : "GET",			
		crossDomain : true,
	 	headers : {
			"Authorization" : authCode
		},
		dataType: "xml"
	})
	.done(classifyComplete)
	.fail(classifyError); 	
	//apply to default values	
	def_num_classes = num_classes;		
		
}

function classifyComplete(xmlStyle) {		
	//from xml to string
	var style = operationManager.unparseXml(xmlStyle);	
	if(style!=null){
		openDataLayer.mergeNewParams({
			"sld_body" : style
		});
		openDataLayer.redraw();
		handleGetLegend(style);
	}
	
	if($('#geo_viewer_loading',window.parent.document).length > 0){$('#geo_viewer_loading',window.parent.document).hide();}
}

function classifyError(data){
	
}

function doInitialClassify(){	
	$("#spinner").show();
	var token = username + ":" + password;
	var authCode = "Basic " + $.base64.encode(token);		
	var _methodUrl = classificationRESTMethodURL + "?" + workspaceName + "," + layerName + "," + def_var_name + "," + def_num_classes + "," + def_palette_name+","+def_border_color+","+def_border_width+","+def_border_style;
	if(filter && filter!=""){
		_methodUrl += "," + filter;
	}
	
	$.ajax( {
			url: _methodUrl,
			type : "GET",			
	 		crossDomain : true,
	 		headers : {
				"Authorization" : authCode
			},
			dataType: "xml"
		})
    	.done(initialClassifyComplete)
    	.fail(classifyError)
		.always(createAndShowMap);
}

function initialClassifyComplete(xmlStyle){	
	//from xml to string
	var style = operationManager.unparseXml(xmlStyle);
	if(style!=null){
		openDataLayer.mergeNewParams({
			"sld_body" : style
		});
		handleGetLegend(style);
	}	
}

function doLegendClassify(classification_var,classification_var_lbl){	
	$("#spinner").show();
	var token = username + ":" + password;
	var authCode = "Basic " + $.base64.encode(token);
	var palette_name = def_palette_name;
	if (def_inverted_palette){
		palette_name = palette_name +"Inverted";
	}
	var _methodUrl = classificationRESTMethodURL + "?" + workspaceName + "," + layerName + "," + classification_var + "," + def_num_classes + "," + palette_name + ","+def_border_color+","+def_border_width+","+def_border_style;
	
	if(filter && filter!=""){
		_methodUrl += "," + filter;
	}	
	
	var jqxhr = $.ajax( {
		url: _methodUrl,
		type : "GET",			
	 	crossDomain : true,
	 	headers : {
			"Authorization" : authCode
		},
		dataType: "xml"
	})
	.done(classifyComplete)
	.fail(classifyError); 	
	//apply to default values
	def_var_name = classification_var;
	def_var_lbl = classification_var_lbl;	
		
}

function doColorInvert(obj){	
	if(obj!=undefined && obj.checked!=undefined){
		if (obj.checked){
			jQuery('#sequential').find('img').each(function() { 
					var srcAttr = jQuery(this).attr('src');				
					var arrSrc = srcAttr.split(".");
					if (arrSrc.length==2){
						if(srcAttr.indexOf("Inverted")==-1){											
							srcAttr = arrSrc[0] + "Inverted." + arrSrc[1];					
							jQuery(this).attr('src', srcAttr);
						}
					} 
				}
			);
		}
		else
		{
			jQuery('#sequential').find('img').each(function() { 
				var srcAttr = jQuery(this).attr('src');
				srcAttr = srcAttr.replace("Inverted","");			
				jQuery(this).attr('src', srcAttr); 
				}
			);
		}
	}
};

function SetCheckInvertedValueAndPalette(){	
	if (def_inverted_palette){
		 $('input:checkbox[name=invertedClasses]').attr('checked',true);		 	
	}
	else {
		$('input:checkbox[name=invertedClasses]').attr('checked',false);	
	}
	doColorInvert($('input:checkbox[name=invertedClasses]')[0]);
	
};

function hexToServerRgb(hex) {
    var bigint = parseInt(hex, 16);
    var r = (bigint >> 16) & 255;
    var g = (bigint >> 8) & 255;
    var b = bigint & 255;

    return r + "_" + g + "_" + b;
};
