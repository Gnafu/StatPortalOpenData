var borderDict = [
    {serverWidth:"0", clientWidth:"0px"},
    {serverWidth:"0.5", clientWidth:"1px"},
    {serverWidth:"1", clientWidth:"2px"},
    {serverWidth:"1.5", clientWidth:"2px"},
    {serverWidth:"2", clientWidth:"3px"}
    ]

/* LEGEND */
function handleGetLegend(style){
	var _X2JS = new X2JS();
	$("#legendClasses").html("");
    var stile = _X2JS.xml_str2json( style );
    var rules = stile.StyledLayerDescriptor.NamedLayer.UserStyle.FeatureTypeStyle.Rule;
	if(!$.isArray(rules)){
		var rule = rules;
		rules = [rule];	
	}
	
   	$("#classified_var").text("Geometria");
    if (def_var_lbl!=""){
		$("#classified_var").text(def_var_lbl);
   	}
    //disable autocomplete
    $("#legendPanelContent .ui-autocomplete-input").attr('disabled', true);
    
    //disable autocomplete
    $("#legendBorderPanelContent .ui-autocomplete-input").attr('disabled', true);
    
	$.each(rules, function(i, val) {
		if(val!=null){
			var fill_color,geom_type,border_color,border_width;
			if(val.PointSymbolizer!=null){
				geom_type="point";
				if (val.PointSymbolizer.Graphic.Mark!=undefined)
				{
					fill_color = val.PointSymbolizer.Graphic.Mark.Fill.CssParameter[0].toString();
				}
				else
					fill_color = "#FFFFFF";	
			}else if(val.LineSymbolizer!=null){
				geom_type="line";				
				if(val.LineSymbolizer.Stroke.CssParameter[0])
					fill_color = val.LineSymbolizer.Stroke.CssParameter[0].toString();
				else if(val.LineSymbolizer.Stroke.CssParameter_asArray[0])
					fill_color = val.LineSymbolizer.Stroke.CssParameter_asArray[0].toString();
				else
					fill_color = "#FFFFFF";
			}else if(val.PolygonSymbolizer != null){
				geom_type="polygon";
				if(val.PolygonSymbolizer.Fill.CssParameter[0])
					fill_color = val.PolygonSymbolizer.Fill.CssParameter[0].toString();
				else if(val.PolygonSymbolizer.Fill.CssParameter_asArray[0])
					fill_color = val.PolygonSymbolizer.Fill.CssParameter_asArray[0].toString();
				else
					fill_color = "#FFFFFF";									
			}else{
				return false;
			}
			var label="";
			if (val.Title__text!=undefined){
				label="da ";
			}
			var lblitems = val.Title.__text.split("..");
			var itemsCount = lblitems.length;
			for(var i=0;i<itemsCount;i++){
				if(i>0){
					label = label + " a ";
				}
				var num = Number(lblitems[i]);
				if(!isNaN(num)){					
					label = label + $.format.number(num, '#,##0.00');
				}				
			}
			
			var item;
			
			if (def_border_color != undefined){
				item = domManager.createLegendAndBorderItemDiv(geom_type, fill_color, getClientBorderColor(def_border_color), getClientBorderWidth(def_border_width), def_border_style, label);
			} 
			else
			{
				item = domManager.createLegendItemDiv(geom_type, fill_color, label);
			}			
			$("#legendClasses").append(item);
		}
	});	
	
	if(rules.length==1 && rules[0]==undefined){
		var item = domManager.createLabelItemDiv("Non ci sono valori per la classificazione scelta");			
		$("#legendClasses").append(item);
	}
	
	
	$("#spinner").hide();
		
	if(!($('#legendPanel').is(':visible'))) {
		showLegendPanel();
	}
		
	if(!($('#legendBorderPanel').is(':visible'))) {
		showLegendBorderPanel();
	}

	setClassifyParameters();	
}

function attachLegendListener(){
	$('#legendClasses').bind('click', function() {
  		//open classify popup
		openClassifyParameters();
	});	
}

function attachLegendBorderListener(){
	$('#legendBorderClasses').bind('click', function() {
  		//open classify popup
		openClassifyParameters();
	});	
}

function legendDoClassify(){
	//do classify				
	var class_var=$("#vars_legend").val()? $("#vars_legend").val() : def_var_name;	
	var class_var_lbl=$("#vars_legend").val()? $("#vars_legend").find('option:selected').html() : def_var_lbl;
	if(class_var!=def_var_name){
		doLegendClassify(class_var,class_var_lbl);
	}
}

function hideOpacityPanel(){
	$('#legendOpacityPanel').hide();	
}

function hideLegendPanel(){
	$('#legendPanel').hide();	
}

function showOpacityPanel(){
	$('#legendOpacityPanel').show();	
}

function showLegendPanel(){
	$('#legendPanel').show();
}

function hideLegendBorderPanel(){	
	$('#legendBorderPanel').hide();	
}

function showLegendBorderPanel(){	
	$('#legendBorderPanel').show();
}

function getClientBorderColor(serverBorderColor){	
	return "rgb("+serverBorderColor.replace(/_/gi,', ')+ ")";
}

function getClientBorderWidth(serverBorderWidth){
	for ( var i=0 ; i < borderDict.length; i++) {
		if (borderDict[i].serverWidth == serverBorderWidth) {
			return borderDict[i].clientWidth;
		}
	}
}
