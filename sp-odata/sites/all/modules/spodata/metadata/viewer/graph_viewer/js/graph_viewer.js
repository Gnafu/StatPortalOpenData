StatPortalOpenData.GraphViewer = StatPortalOpenData.GraphViewer || {};
StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS = 12;
StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM = 0;
StatPortalOpenData.GraphViewer.PAGE_FOR_GRAPH_RESULTS = 0;
StatPortalOpenData.GraphViewer.NUM_GRAPH_RESULTS = 0;
StatPortalOpenData.GraphViewer.ACTUAL_PAGE = 0;
StatPortalOpenData.GraphViewer.MIN_SCALE_GRAPH_VALUE;
StatPortalOpenData.GraphViewer.MAX_SCALE_GRAPH_VALUE;
// numero massimo di elementi da caricare nel menu a tendina
StatPortalOpenData.GraphViewer.GRAPH_MAX_DISTINCT_FOR_DROPDOWN = 5;
StatPortalOpenData.GraphViewer.timerForAutocomplete;

StatPortalOpenData.GraphViewer.TEXT_IF_TEXTBOX_FILTER_IS_EMPTY = 'Inserisci filtri';

StatPortalOpenData.GraphViewer.MAX_X_AXIS_LEN = 40;

var graphObject;
var graphTimer;
var MIN_GRAPH_HEIGHT = 400;

var PRESUMED_DECIMAL_CALCULATED;

function initChartFromDrupal(id) {
	StatPortalOpenData.GraphViewer.resetGraphPagination();
	// si inizializzano le dimensioni per il grafico
	jQuery('#dataChart').height(Math.max(MIN_GRAPH_HEIGHT, jQuery(window).height() - StatPortalOpenData.utility.getResidualTableHeight('GRAPH')));

	state.setNewAndOnlyModuleActive(StatPortalOpenData.Enums.ModuleId.GRAPH);	
	StatPortalOpenData.ODataUtility.initViews();
	//jQuery.jqplot.config.enablePlugins = true;
	
	jQuery(window).unbind('resize', StatPortalOpenData.GraphViewer.windowResizeHandlerForGraphViewer);
	jQuery(window).bind('resize', StatPortalOpenData.GraphViewer.windowResizeHandlerForGraphViewer);
	
	jQuery('#dataChart').unbind('jqplotPostRedraw', StatPortalOpenData.GraphViewer.zoomedHandler);
	jQuery('#dataChart').bind('jqplotPostRedraw', StatPortalOpenData.GraphViewer.zoomedHandler);
	
}

(function($) {
StatPortalOpenData.GraphViewer.updateInfoGraphPagination = function updateInfoGraphPagination(actual, tot) {
	jQuery('#infoGraphPagination').html(actual + ' di ' + tot);
	if(tot == 1){
		jQuery('#backNextGraph').hide();
	}else{
		jQuery('#backNextGraph').show();
	}
};

StatPortalOpenData.GraphViewer.axisFormatter = function axisFormatter(formatString, value) {
	
	if(arguments.callee.caller.name == ''){
		return StatPortalOpenData.ODataUtility.formatNumber(value.toFixed(0));
	}
	
	// se c'è il punto si arrotonda a due cifre decimali, altrimenti si lascia intero
	if(value.toString().indexOf('.') !== -1){
		if(typeof value !== "number"){
			value = Number(value);
		}
		value = value.toFixed(2);
	}
	
	// se c'è il numero presunto di decimali si formatta a quello con il toFixed
	if(typeof(PRESUMED_DECIMAL_CALCULATED) != 'undefined'){
		try{
			var splitted = value.toString().split('.');
			// se si tratta di un double
			if(splitted.length == 2){
				var realDecimals = splitted[1].length;
				if(PRESUMED_DECIMAL_CALCULATED > realDecimals){
					value = value.toFixed(PRESUMED_DECIMAL_CALCULATED);
				}
			}else if(splitted.length == 1){
				if(PRESUMED_DECIMAL_CALCULATED > 0){
					value = value.toFixed(PRESUMED_DECIMAL_CALCULATED);
				}
			}
		}catch(err){
			// non si fa niente, è solo una questione di formattazione del numero
		}		
	}
	
	return StatPortalOpenData.ODataUtility.formatNumber(value);
};

StatPortalOpenData.GraphViewer.uploadFilterKeyController = function uploadFilterKeyController(callback) {
	// è stata selezionata una dimensione. Si deve aggiungere il menu a tendina
	// per tutte le altre dimensioni
	// se c'è più di una sola dimensione si prendono tutte tranne quella 'eventualmente' selezionata
	var selectedCountOptionsName = jQuery('#graphFieldToCount :selected').attr('data-id');
	
	var dimensionFilterKeyController = '<select id="dimensionFilterKeyController" name="dimensionFilterKeyController" onChange="StatPortalOpenData.GraphViewer.resetGraphPagination();StatPortalOpenData.GraphViewer.uploadFilterValueController(StatPortalOpenData.GraphViewer.uploadGraph);" >';
	
	var dimensions = state.getDimensions();
	
	jQuery.each(dimensions, function() {
		// l'ho commentata perchè alla fine può essere utile anche fare il filtro sulla dimensione selezionata
		//if(selectedCountOptionsName !== this.getId()){
			dimensionFilterKeyController += '<option value="' + this.getName() + '"' + '>' + this.getAlias() + '</option>';
		//}
	});
	
	dimensionFilterKeyController += '</select>';
	
	jQuery('#graph-dimensions-to-filter-key-container').html(dimensionFilterKeyController);
	
	if(callback){
		StatPortalOpenData.GraphViewer.uploadFilterValueController(callback);
	}
};

StatPortalOpenData.GraphViewer.uploadFilterValueController = function uploadFilterValueController(callback) {
	// è cambiata la dimensione da filtrare selezionata. Vanno aggiornati
	// tutti i possibili valori che questa può prendere
	
	// in base alla dimensione selezionata si fa vedere un menu a tendina oppure una textbox con autocompletamento
	var dimensionFilterKeySelected = jQuery('#dimensionFilterKeyController').val();
	
	var dimType = '';
	var dimCardinality = 1;
	var orderDirection = 'ASC';
	// se si tratta di una serie temporale si ordina in ordine alfabetico, altrimenti in ordine decrescente rispetto alla misura
	var dimensionList = state.getDimensions();
	jQuery.each(dimensionList, function() {
	    if(this.getName().toString() === dimensionFilterKeySelected){
	    	dimType = this.getDimType();
	    	dimCardinality = this.getCardinality();
	    }
	});
	// si compone la richiesta OData
	if(dimType === 'TEMPORALE'){
		orderDirection = 'DESC';
	}
	
	// si controlla la cardinalità
	if(dimCardinality > StatPortalOpenData.GraphViewer.GRAPH_MAX_DISTINCT_FOR_DROPDOWN){
		// si fa vedere una textbox con autocompletamento
		var dimensionFilterValueControllerTb = '<input type="text" id="dimension-filter-value-tb" name="dimension-filter-value-tb" value="' + StatPortalOpenData.GraphViewer.TEXT_IF_TEXTBOX_FILTER_IS_EMPTY + '" />';
		jQuery('#graph-dimensions-to-filter-value-container').html(dimensionFilterValueControllerTb);
		jQuery('#dimension-filter-value-tb').css('color', 'gray');
		jQuery.fn.extend({
			 propAttr: jQuery.fn.prop || jQuery.fn.attr
			});
		jQuery('#dimension-filter-value-tb').autocomplete({
			source: function( request, response ) {
				
				if(jQuery('#dimension-filter-value-tb').val() !== StatPortalOpenData.GraphViewer.TEXT_IF_TEXTBOX_FILTER_IS_EMPTY){
					var filterStr = jQuery('#dimension-filter-value-tb').val().replace(new RegExp("\'", "g"),"\'\'");
					
					var urlOdata = state.getServiceUri() + state.getResourcePath().replace('DataRows', 'DistinctCountRows') + "?distinctField=" + dimensionFilterKeySelected + "&top=5&$skip=0&fieldToSort=LABEL&sortingDirection=ASC&$filter=substringof('" + filterStr + "',xx)";
					OData.defaultHttpClient.enableJsonpCallback = true;
					StatPortalOpenData.utility.log(urlOdata);
					OData.read(urlOdata, function(data) {
						response( jQuery.map( data.results, function( item ) {
							if(item.label.toLowerCase().indexOf(filterStr.toLowerCase()) == 0){
								return {
									label: item.label,
									value: item.label
								};
							}
						}));	
					});
				}else{
					return {};
				}
			},
			minLength: 1,
			select: function( event, ui ) {
				//alert(ui.item ? "Selected: " + ui.item.label : "Nothing selected, input was " + this.value);
				StatPortalOpenData.GraphViewer.resetGraphPagination();
				StatPortalOpenData.GraphViewer.uploadGraph();
			},
			open: function() {
				jQuery( this ).removeClass( "ui-corner-all" ).addClass( "ui-corner-top" );
			},
			close: function() {
				jQuery( this ).removeClass( "ui-corner-top" ).addClass( "ui-corner-all" );
			}
		});
		
		jQuery('#dimension-filter-value-tb').unbind('focusout'); 
		jQuery('#dimension-filter-value-tb').bind('focusout', function() {
			// se c'è il testo di default
			if(jQuery(this).val() === ''){
				jQuery(this).css('color', 'gray');
				jQuery(this).val(StatPortalOpenData.GraphViewer.TEXT_IF_TEXTBOX_FILTER_IS_EMPTY);
			}
		});
		
		jQuery('#dimension-filter-value-tb').unbind('focus'); 
		jQuery('#dimension-filter-value-tb').bind('focus', function() {
			// se c'è il testo di default
			if(jQuery(this).val() === StatPortalOpenData.GraphViewer.TEXT_IF_TEXTBOX_FILTER_IS_EMPTY){
				jQuery(this).val('');
				jQuery(this).css('color', 'black');
			}
		});
		
		jQuery('#dimension-filter-value-tb').unbind('keyup');
		jQuery('#dimension-filter-value-tb').bind('keyup', function(e) {
			//var code = (e.keyCode ? e.keyCode : e.which);
			if(/*code == 13 && */jQuery('#dimension-filter-value-tb').val() == '') {
				StatPortalOpenData.GraphViewer.resetGraphPagination();
				StatPortalOpenData.GraphViewer.uploadGraph();
			}else{
//				if(typeof(StatPortalOpenData.GraphViewer.timerForAutocomplete) != 'undefined'){
//					clearTimeout(StatPortalOpenData.GraphViewer.timerForAutocomplete);
//				}
//				StatPortalOpenData.GraphViewer.timerForAutocomplete = setTimeout('StatPortalOpenData.GraphViewer.resetGraphPagination();StatPortalOpenData.GraphViewer.uploadGraph();', 1500);
			}
		});
		
		StatPortalOpenData.GraphViewer.resetGraphPagination();
		StatPortalOpenData.GraphViewer.uploadGraph();
		
	}else{
		// si compone il menu a tendina
		var distinctODataRequest = state.getServiceUri() + state.getResourcePath().replace('DataRows', 'DistinctCountRows') + '?distinctField=' + dimensionFilterKeySelected + '&$skip=0&top=' + StatPortalOpenData.GraphViewer.GRAPH_MAX_DISTINCT_FOR_DROPDOWN + '&fieldToSort=LABEL&sortingDirection=' + orderDirection;
		OData.defaultHttpClient.enableJsonpCallback = true;
		OData.read(distinctODataRequest, function(data) {
			
			var dimensionFilterValueController = '<select id="dimensionFilterValueController" name="dimensionFilterValueController" onChange="StatPortalOpenData.GraphViewer.resetGraphPagination();StatPortalOpenData.GraphViewer.uploadGraph();" >';
			
			var res = data.results, resLen = res.length;
			if(resLen > 1){
				dimensionFilterValueController += '<option value="-1"' + '>Tutti</option>';
			}
			for(var z=0; z<resLen; z++){
				var a = res[z]['label'];
				dimensionFilterValueController += '<option value="' + res[z]['id'] + '"' + '>' + res[z]['label'] + '</option>';
			}
			
			// Se c'è più di un elemento ci deve essere anche l'opzione tutti. 
			// Nel caso sia uno solo invece i filtri devono essere disabilitati
			// Nel caso invece non ci sia nessun elemento andrebbe selezionata un'altra dimensione (TODO)
			jQuery('#dimensionFilterKeyController').removeAttr("disabled");
			jQuery('#dimensionFilterValueController').removeAttr("disabled");
			
			dimensionFilterValueController += '</select>';
			
			jQuery('#graph-dimensions-to-filter-value-container').html(dimensionFilterValueController);
			
			if(resLen === 0){
				showMessage('info', 'Nessun risultato per i filtri impostati');
			}else if(resLen === 1){
				try{
					//TODO: da togliere il try catch!! L'ho fatto solo per evitare problemi visto che non verrà testata 
					if(state.getDimensions().length < 3){
						jQuery('#dimensionFilterKeyController').attr("disabled", true);
						jQuery('#dimensionFilterValueController').attr("disabled", true);	
					}
				}catch(Exception){
					jQuery('#dimensionFilterKeyController').attr("disabled", true);
					jQuery('#dimensionFilterValueController').attr("disabled", true);
				}
			}
			
			if(callback){
				callback();
			}
			
		}, function(err) {
			showMessage('error', 'Impossibile visualizzare il grafico');
			jQuery('#graph_viewer_loading').hide();
		});
	}
	

};

/**
 * Gestione degli errori del grafico
 */
StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage = function printGraphGeneralErrorMessage(errorNumber){
	showMessage('error', 'Impossibile visualizzare il grafico');
};

/**
 * Calcola il minimo e massimo valore per la scala del grafico
 */
StatPortalOpenData.GraphViewer.getAxisScaleFromRange = function getAxisScaleFromRange(min, max){
	
	min = Number(min);
	max = Number(max);
	var percentualRange = 0.1;
	var gap = (max - min)*percentualRange;
	
	var newMin = Math.round(min - gap);
	var newMax = Math.round(max + gap);

	if(newMax == max){
		newMax = max + 1;
	}
	
	if(newMin == min){
		newMin = min -1;
	}
	
	if(min >= 0 && newMin < 0){
		newMin = 0;
	}
	
	if(min == max){
		newMin = 0;
		newMax = newMax*(1 + percentualRange);
	}
	
	return {
		scaleMin : newMin,
		scaleMax : newMax
	};
	
};

/**
 * Resetta la paginazione del grafico
 */
StatPortalOpenData.GraphViewer.resetGraphPagination = function resetGraphPagination(){
	StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM = 0;
	var graphTypeVal = jQuery('#graphType').val();
	if((graphTypeVal == 'vertical_bar' || graphTypeVal == 'line')){
		jQuery('#backNextGraph').show();
	}else{
		jQuery('#backNextGraph').hide();
	}
};

/**
 * Disabilita il bottone 'avanti'
 */
StatPortalOpenData.GraphViewer.disableNextGraphButton = function disableNextGraphButton(){
	jQuery('#nextLinkForGraph').css('color', 'gray');
	jQuery('#nextLinkForGraph').attr('href', 'javascript:void(0);');
	jQuery('#nextLinkForGraph').css('cursor', 'default');
	jQuery('#nextLinkForGraph').addClass('backNextLinkDisabled');
};

/**
 * Abilita il bottone 'avanti'
 */
StatPortalOpenData.GraphViewer.enableNextGraphButton = function enableNextGraphButton(){
	jQuery('#nextLinkForGraph').css('color', 'blue');
	jQuery('#nextLinkForGraph').css('cursor', 'pointer');
	jQuery('#nextLinkForGraph').removeClass('backNextLinkDisabled');
	jQuery('#nextLinkForGraph').attr('href', 'javascript:StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM += StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS; StatPortalOpenData.GraphViewer.uploadGraph();');
};

/**
 * Disabilita il bottone 'indietro'
 */
StatPortalOpenData.GraphViewer.disableBackGraphButton = function disableBackGraphButton(){
	jQuery('#backLinkForGraph').css('color', 'gray');
	jQuery('#backLinkForGraph').css('cursor', 'default');
	jQuery('#backLinkForGraph').attr('href', 'javascript:void(0);');
	jQuery('#backLinkForGraph').addClass('backNextLinkDisabled');
};

/**
 * Abilita il bottone 'indietro'
 */
StatPortalOpenData.GraphViewer.enableBackGraphButton = function enableBackGraphButton(){
	jQuery('#backLinkForGraph').css('color', 'blue');
	jQuery('#backLinkForGraph').css('cursor', 'pointer');
	jQuery('#backLinkForGraph').removeClass('backNextLinkDisabled');
	jQuery('#backLinkForGraph').attr('href', 'javascript:StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM -= StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS; StatPortalOpenData.GraphViewer.uploadGraph();');
};

/**
 * Rende visibile il tasto dello zoom
 */
StatPortalOpenData.GraphViewer.zoomedHandler = function zoomedHandler(){
	jQuery('#graphViewerResetZoom').css('visibility', 'visible');
};

/**
 * ridisegna il grafico
 */
StatPortalOpenData.GraphViewer.replotGraphAfterResize = function replotGraphAfterResize(){
	jQuery('#graph_viewer_loading').show();
	var newHeight = Math.max(MIN_GRAPH_HEIGHT, jQuery(window).height() - StatPortalOpenData.utility.getResidualTableHeight('GRAPH'));
	if(jQuery('#dataChart').height() !== newHeight){
		jQuery('#dataChart').height(newHeight);
		graphObject.replot({resetAxes:true});
	}
	jQuery('#graph_viewer_loading').hide();
};

/**
 * Gestione del ridimensionamento della finestra
 */
StatPortalOpenData.GraphViewer.windowResizeHandlerForGraphViewer = function windowResizeHandlerForGraphViewer(){
    if(typeof(graphObject) != 'undefined'){
    	if(typeof(graphTimer) !== 'undefined'){
    		clearTimeout(graphTimer);
    	}
    	graphTimer = setTimeout("StatPortalOpenData.GraphViewer.replotGraphAfterResize();",800);
    }
};

/**
 * Gestione del ridimensionamento della finestra in presenza del fullscreen 
 */
StatPortalOpenData.GraphViewer.windowResizeHandlerForGraphViewerFullScreen = function windowResizeHandlerForGraphViewerFullScreen(){
    var dimFullScreen = StatPortalOpenData.GraphViewer.getDimensionsForFullScreen();
    jQuery('#dataChart').height(dimFullScreen.height);
	jQuery('#dataChart').width(dimFullScreen.width);
	graphObject.replot({clear: true, resetAxes:true});
	// il codice seguente serve per ridimensionare la larghezza delle singole barre
	jQuery.each(graphObject.series, function(index, series) { series.barWidth = undefined; });
};

var oldGraphViewerWidth;

/**
 * Gestione del fullscreen
 */
StatPortalOpenData.GraphViewer.toFullScreen = function toFullScreen(){
	jQuery('#chart-viewer').addClass('multidimensionalFullScreen');
	jQuery('#chart-viewer').append('<img id="closeFullscreen" class="closeFullscreen" onclick="javascript:StatPortalOpenData.GraphViewer.toNormalSize();" src="/sites/all/modules/spodata/metadata/viewer/commons/img/close.png" />');
	jQuery(window).unbind('resize', StatPortalOpenData.GraphViewer.windowResizeHandlerForGraphViewer);
	jQuery(window).bind('resize', StatPortalOpenData.GraphViewer.windowResizeHandlerForGraphViewerFullScreen);
	oldGraphViewerWidth = jQuery('#dataChart').width();
	jQuery('#dataChart').height(StatPortalOpenData.GraphViewer.getDimensionsForFullScreen().height);
	jQuery('#dataChart').width(StatPortalOpenData.GraphViewer.getDimensionsForFullScreen().width);
	graphObject.replot({resetAxes:true});
};

/**
 * Calcolo delle dimensioni per il fullscreen
 */
StatPortalOpenData.GraphViewer.getDimensionsForFullScreen = function getDimensionsForFullScreen(){
	return {
		height: jQuery(window).height() - 35,
		width:jQuery(window).width() - 130 
	};
};

/**
 * Gestione del ridimensionamento (da fullscreen a dimensioni normali)
 */
StatPortalOpenData.GraphViewer.toNormalSize = function toNormalSize(){
	jQuery('#chart-viewer').removeClass('multidimensionalFullScreen');
	jQuery('#dataChart').width(oldGraphViewerWidth);
	StatPortalOpenData.GraphViewer.replotGraphAfterResize();
	jQuery(window).bind('resize', StatPortalOpenData.GraphViewer.windowResizeHandlerForGraphViewer);
	jQuery(window).unbind('resize', StatPortalOpenData.GraphViewer.windowResizeHandlerForGraphViewerFullScreen);
};

/**
 * Gestione del cambio di misure
 */
StatPortalOpenData.GraphViewer.measureChangedHandler = function measureChangedHandler(){
	jQuery('#graphFieldToCount').css('display', 'inline');
	jQuery('#graph-dimension-filter').css('display', 'none');
	
	StatPortalOpenData.GraphViewer.uploadGraph();
};

var significativeFieldList = new Object();
significativeFieldList['provincia'] = 1;
significativeFieldList['siglaprovi'] = 0;
significativeFieldList['prov_1'] = 0;
significativeFieldList['tipologia'] = 3;
significativeFieldList['localita'] = 0;
significativeFieldList['comune'] = 0;
significativeFieldList['PROVINCIA'] = 1;

/**
 * Inizializza il controller del grafico (per permettere la scelta di misure, dimensioni ed eventualmente filtri)
 */
StatPortalOpenData.GraphViewer.initGraphController = function initGraphController(){
	
	// inizializza i controlli (menu a tendina)
	var dimensions = state.getDimensions();
	var measures = state.getMeasures();
	var columns = state.getColumns();
	var measureOptions = $('#graphMeasures');
	

	$.each(measures, function() {
		var optionToAppend = $("<option />").val(this.getId()).text(this.getAlias()).attr('data-type', 'measure');
		measureOptions.append(optionToAppend);
	});	
	
	// booleano che indica se esiste almeno una colonna per la quale ha senso 
	// avere il grafico
	var almostOneFieldForGraph = false;
	
	// si aggiungono i campi al menu a tendina relativo ai campi sui quali può essere fatta la count
	var countOptions = $('#graphFieldToCount');
	
	$.each(dimensions, function() {		
		var differentDistinctCount = this.getDifferentDistinctCount(); 
		countOptions.append($("<option />").val(this.getName()).text(this.getAlias()).attr('data-type', 'dimension').attr('data-id', this.getId()).attr('data-ddc', differentDistinctCount));
		if(differentDistinctCount != null && differentDistinctCount != 'null' && differentDistinctCount != 1){
			almostOneFieldForGraph = true;
		}
	});
	
	var nameToSelect = null;
	var alreadySelectedPriority = -1;
	
	$.each(columns, function() {
		var differentDistinctCount = this.getDifferentDistinctCount(); 
		countOptions.append($("<option />").val(StatPortalOpenData.ODataUtility.getUniqueIdentifier(this.getAlias())).text(this.getAlias()).attr('data-type', 'column').attr('data-ddc', differentDistinctCount));
		if(differentDistinctCount != null && differentDistinctCount != 'null' && differentDistinctCount != 1){
			almostOneFieldForGraph = true;
		}
		
		var priorityInSignificativeFieldList = significativeFieldList[this.getAlias().toLowerCase()];
		if(typeof(priorityInSignificativeFieldList) !== 'undefined' && priorityInSignificativeFieldList > alreadySelectedPriority){
			nameToSelect = this.getName();
			alreadySelectedPriority = priorityInSignificativeFieldList;
		}
		
	});
	// si seleziona l'eventuale colonna scelta tra le priorità
	if(nameToSelect !== null){
		countOptions.val(nameToSelect).attr('selected',true);
	}
	
	// si deve far vedere il numero di elementi solo se tutte le colonne presenti in 
	// graphFieldToCount non hanno la proprietà differentDistinctCount = 1
	if(almostOneFieldForGraph){		
		measureOptions.append($("<option />").val('ELEMENT_COUNT').text('Numero di elementi').attr('data-type', 'elementCount'));
	}
	
	// si preparano i filtri
	
	// se è selezionato il numero di elementi i filtri devono essere disabilitati
	if(measureOptions.val() === 'ELEMENT_COUNT'){
		//$('#graphDimensions').css('display', 'none');
		//$('#graphFieldToCount').css('display', 'inline');
		$('#graph-dimension-filter').css('display', 'none');
		
		// si devono disabilitare le colonne non selezionabili
		StatPortalOpenData.GraphViewer.disableColumnNotInteresting();
		
		StatPortalOpenData.GraphViewer.initGraph();
	}else{
		var groupedSelected = $('#graphFieldToCount :selected');
		// se c'è almeno una dimensione ed è stata selezionata una dimensione
		if(dimensions.length > 0 && groupedSelected.attr('data-type') == 'dimension'){
			$('#graph-dimension-filter').css('display', 'inline');
			StatPortalOpenData.GraphViewer.uploadFilterKeyController();
			StatPortalOpenData.GraphViewer.uploadFilterValueController(StatPortalOpenData.GraphViewer.initGraph); 
		}else{
			$('#graph-dimension-filter').css('display', 'none');
			StatPortalOpenData.GraphViewer.initGraph();
		}
	}
		
};

/**
 * Disabilita le colonne per le quali verrebbe un grafico piatto
 */
StatPortalOpenData.GraphViewer.disableColumnNotInteresting = function disableColumnNotInteresting(){
	$('#graphFieldToCount option').each(function(){
		if($(this).attr('data-ddc') == 1){
			$(this).prop("disabled", true);
		}
	});
};

/**
 * Riattiva la selezione di tutte le colonne
 */
StatPortalOpenData.GraphViewer.activeAllColumns = function activeAllColumns(){
	$('#graphFieldToCount option').each(function(){
			$(this).prop("disabled", false);
	});
};

/**
 * Mostra un particolare messaggio all'utente
 */
StatPortalOpenData.GraphViewer.showMessage = function showMessage(type, msg){
	StatPortalOpenData.ODataUtility.showMessage('#infoGraphForUser', type, msg);
};

/**
 * Nasconde eventuali messaggi di info per gli utenti
 */
StatPortalOpenData.GraphViewer.hideMessage = function hideMessage(){
	StatPortalOpenData.ODataUtility.hideMessage('#infoGraphForUser');
};

StatPortalOpenData.GraphViewer.callbackGraphUploading = function callbackGraphUploading(data){
	StatPortalOpenData.GraphViewer.hideMessage();
	
	if(data.results.length === 0){
		StatPortalOpenData.GraphViewer.showMessage('info', 'Nessun risultato per la visualizzazione richiesta');		
		if(typeof(graphObject) !== 'undefined'){
			graphObject.destroy();
		}
		StatPortalOpenData.GraphViewer.disableNextGraphButton();
	}else{
		// disegna il grafico
		var renderer = $.jqplot.LineRenderer;
		var rendererOptions = {};
		var legend = {};
		var graphType = $('#graphType :selected').val();
		
		var points = [];
		var res = data.results;
		var resLen = res.length;
		var others = 0;

		
		// CHANGED
		/*var dimText = $('#graphDimensions :selected').text();*/
		var dimText = $('#graphFieldToCount :selected').text();
		var measText = $('#graphMeasures :selected').text();
		var dimTextUnique = StatPortalOpenData.ODataUtility.getUniqueIdentifier(dimText);
		var measTextUnique = StatPortalOpenData.ODataUtility.getUniqueIdentifier(measText);
		
		
		// se si tratta di un count personalizzato dimTextUnique sarà 'label' e measTextUnique sarà 'count'
		// CHANGED
		var isCountDistinct = (jQuery('#graphMeasures').val() === 'ELEMENT_COUNT')/*($('#graphDimensions').css('display') === 'none')*/; 
		if(isCountDistinct){
			dimTextUnique = 'label';
			measTextUnique = 'count';
		}

				
		// cerchiamo di capire se si tratta di raggruppamento su dimensione o colonna generica
		var meas = $('#graphMeasures').val();
		var graphFieldToCountEl2 = $('#graphFieldToCount :selected');
		var dimOrFieldToCountType2 = graphFieldToCountEl2.attr('data-type');		
		if(meas !== 'ELEMENT_COUNT' && dimOrFieldToCountType2 != 'dimension'){
			dimTextUnique = 'column';
			measTextUnique = 'variable';
		}
		
		// si ottengono i presunti decimali del risultato
		var presumedDecimals = -1;
		try{
			presumedDecimals = res[0][measTextUnique].split('.')[1].length;
			PRESUMED_DECIMAL_CALCULATED = presumedDecimals;
		}catch(err){
			
		}
		
		switch(graphType){
			case 'line' :
				renderer = jQuery.jqplot.LineRenderer;
				break;
			case 'vertical_bar' :
				renderer = jQuery.jqplot.BarRenderer;
				break;
			case 'horizontal_bar' :
				renderer = jQuery.jqplot.BarRenderer;
				rendererOptions = {
				        barDirection: 'horizontal'
			    };
				break;
			case 'pie' :
				renderer = jQuery.jqplot.PieRenderer;
				break;
			case 'funnel':
				renderer = jQuery.jqplot.FunnelRenderer;
				break;
			case 'donut' :
				renderer = jQuery.jqplot.DonutRenderer;
				break;
		}
		
		var highlighter = {showMarker: false};
		var axes = {};
		var cursor = {};
		var axesDefaults = {};
		var seriesDefaults = {};
		
		if(graphType === 'vertical_bar' || graphType === 'line'){
			
			if(graphType === 'vertical_bar'){
				seriesDefaults = {
					rendererOptions: {
						fillToZero: true
					}
                 };
			}
			
			highlighter = {
				show: true, 
				tooltipSeparator: ' : ',
	            showTooltip: true,
	            showMarker: true,
	            numberOfDecimals: PRESUMED_DECIMAL_CALCULATED/*,
	            tooltipLocation: 'nw',  
	            tooltipAxes: 'both',    
	            lineWidthAdjust: 2.5,
	            
	            useAxesFormatters:true*/
	          };
			
			  axes = {
	            xaxis: {
	              renderer: jQuery.jqplot.CategoryAxisRenderer
	            },
	            yaxis: {
	                tickOptions:{
	                    formatter: StatPortalOpenData.GraphViewer.axisFormatter
	                }/*,
	            	formatString: "%.5f"*/
	            }
	          };
			  
			  if(typeof(StatPortalOpenData.GraphViewer.MIN_SCALE_GRAPH_VALUE) != 'undefined' && typeof(StatPortalOpenData.GraphViewer.MAX_SCALE_GRAPH_VALUE) != 'undefined'){
				  var minMax = StatPortalOpenData.GraphViewer.getAxisScaleFromRange(StatPortalOpenData.GraphViewer.MIN_SCALE_GRAPH_VALUE, StatPortalOpenData.GraphViewer.MAX_SCALE_GRAPH_VALUE);
				  axes.yaxis.min = minMax.scaleMin; 
				  axes.yaxis.max = minMax.scaleMax;
			  }
			  
			  cursor = {
	              show: true,
	              zoom:true,
	              showTooltip:false // sono i tooltip in basso a destra relativi al cursore
	          };
			  
			  axesDefaults = {
	              tickRenderer: jQuery.jqplot.CanvasAxisTickRenderer ,
	              tickOptions: {
	                angle: 30,
	                fontFamily:'Segoe UI',
	                fontSize: '8pt',
	                enableFontSupport: false,
	                pt2px: 4
	              }
	          };
		}
		
		if(graphType === 'pie' || graphType === 'funnel' || graphType === 'donut'){
			legend = {
	                show: true,
	                location: 'e',     // compass direction, nw, n, ne, e, se, s, sw, w.
	                rendererOptions:{
	                    numberRows:null,
	                    numberColumns:1
	                },
	                placement: 'outsideGrid'
	            };
			
			rendererOptions = {
					showDataLabels: true
		    };
			
			if(graphType !== 'funnel'){
				highlighter = { 
					show: true, 
		            showTooltip: true,
		            useAxesFormatters:false,
		            tooltipLocation:'custom',
		            formatString:'%s',
		            showMarker: false,
		            numberOfDecimals: PRESUMED_DECIMAL_CALCULATED
		          };
			}
		}

		var negativePointsPresents = false;
		var errorOnPoints = false;
		
		// se si tratta di count distinct si aggiungono tutti i punti al risultato
		// perchè la logica di raggruppare in altro gli elementi è lato server
		var pointCounter = 0;
		if(isCountDistinct){
			for(var c=0; c<resLen; c++){
				var a = res[c][dimTextUnique];
				var b = res[c][measTextUnique];
				points.push([StatPortalOpenData.GraphViewer.formatLabelForXaxis(a), Number(b)]);
				pointCounter++;
			}
			
			if(pointCounter < 3){
				rendererOptions.barWidth = 60;
			}
			
			//StatPortalOpenData.GraphViewer.updateInfoGraphPagination(StatPortalOpenData.GraphViewer.ACTUAL_PAGE, StatPortalOpenData.GraphViewer.PAGE_FOR_GRAPH_RESULTS);
			
			if(StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM + StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS < StatPortalOpenData.GraphViewer.NUM_GRAPH_RESULTS){
				if(pointCounter < StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS){
					StatPortalOpenData.GraphViewer.disableNextGraphButton();
				}else{
					StatPortalOpenData.GraphViewer.enableNextGraphButton();
				}
			}else{
				StatPortalOpenData.GraphViewer.disableNextGraphButton();
			}

			
			if(StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM == 0){
				StatPortalOpenData.GraphViewer.disableBackGraphButton();
			}else{
				StatPortalOpenData.GraphViewer.enableBackGraphButton();
			}
			
		}else{
		
			if(graphType === 'pie' || graphType === 'funnel' || graphType === 'donut'){
				
				for(var d=0; d<resLen; d++){
					var a = res[d][dimTextUnique];
					var b = res[d][measTextUnique];
					var bNum = Number(b);
					
					if(bNum < 0){
						negativePointsPresents = true;
						errorOnPoints = true;
						break;
					}
					points.push([StatPortalOpenData.GraphViewer.formatLabelForXaxis(a), bNum]);
				}
			
			}else{
				
				// se si tratta di una paginazione successiva alla prima pagina
				// si fa vedere il tasto indietro
				if(StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM > 0){
					StatPortalOpenData.GraphViewer.enableBackGraphButton();
				}else{
					StatPortalOpenData.GraphViewer.disableBackGraphButton();
				}
				
				StatPortalOpenData.GraphViewer.updateInfoGraphPagination(StatPortalOpenData.GraphViewer.ACTUAL_PAGE, StatPortalOpenData.GraphViewer.PAGE_FOR_GRAPH_RESULTS);
				
				var maxLen = Math.min(resLen, StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS);
				if(StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM + StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS < StatPortalOpenData.GraphViewer.NUM_GRAPH_RESULTS){
					
					if(resLen >= StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS){
						StatPortalOpenData.GraphViewer.enableNextGraphButton();
					}else{
						// dovrebbe essere il caso in cui alcuni valori sono null..
						StatPortalOpenData.GraphViewer.disableNextGraphButton();
					}
					
				}else{
					StatPortalOpenData.GraphViewer.disableNextGraphButton();
				}
				
				var pointCounter = 0;
				for(var w=0; w<maxLen; w++){
					if(res[w][measTextUnique] !== ''){
						var a = res[w][dimTextUnique];
						var b = res[w][measTextUnique];
						points.push([StatPortalOpenData.GraphViewer.formatLabelForXaxis(a), Number(b)]);
						pointCounter++;
					}
				}
				
				if(pointCounter < StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS){
					StatPortalOpenData.GraphViewer.disableNextGraphButton();
				}
				
				if(pointCounter < 3){
					rendererOptions.barWidth = 60;
				}
			}
		}
		
		//var seriesColors = ['#2E9AB7', '#F18124', '#6A8DC3', '#C86B69', '#C66A68', '#A3C16F', '#8B76A8', '#589BB1', '#F89C60', '#A1B1D0', '#CF9F9F', '#C0D2A7', '#2A5A94', '#942A27', '#6F8C31', '#583E77', '#237B92', '#227990', '#C0661B'];
		var seriesColors = ['#4C557C','#698093','#9CA44A','#B29641','#BB9D45','#825343','#6C5249','#5E6996','#829FB7','#84A0B9','#C0C85C','#EAC558','#A76C58','#806157','#8289AB','#9FB6CA','#D8E085','#F9DA80','#B7897C','#ACB0C2','#D9DEAD'];
		
		if(typeof(graphObject) !== 'undefined'){
			graphObject.destroy();
		}
		
		if(points.length !== 0 && !errorOnPoints){
		
		            graphObject = jQuery.jqplot('dataChart', [points], {
		            	seriesDefaults: seriesDefaults,
		                 series:[
		                         {
		                        	 renderer: renderer,
		                	 		 rendererOptions: rendererOptions,
		                	 		 useNegativeColors:true,
		                	 		 color:'#018FE2'
		                 }],
		                 highlighter: highlighter,
		                 axesDefaults: axesDefaults,
		                 axes: axes,
		                 cursor:cursor,
		                 legend: legend, 
		                 grid: {background: '#FFFFFF', shadow: false},
		                 seriesColors: seriesColors,
		                 negativeSeriesColors: ["#B3D8FF", "#F94545"]
		               });
		}else if(negativePointsPresents){
			showMessage('info', 'Impossibile visualizzare la tipologia di grafico richiesta per la presenza di valori negativi');
		}else{
			showMessage('info', 'Non ci sono dati visualizzabili nella tipologia di grafico richiesta');
		}
	}
	$('#graph_viewer_loading').hide();
};

/**
 * Formatta le label dell'asse delle ascisse
 */
StatPortalOpenData.GraphViewer.formatLabelForXaxis = function formatLabelForXaxis(label){
	var newLabel = label;
	if(label.length > StatPortalOpenData.GraphViewer.MAX_X_AXIS_LEN){
		newLabel = label.substring(0, StatPortalOpenData.GraphViewer.MAX_X_AXIS_LEN) + '...'; 
	}
	
	return newLabel;
};

/**
 * Gerstione del cambio di misura selezionata
 */
StatPortalOpenData.GraphViewer.graphMeasureChanged = function  graphMeasureChanged(){

	// nel caso sia una count si disabilitano le colonne non interessanti, altrimenti si abilitano tutte
	if($('#graphMeasures').val() == 'ELEMENT_COUNT'){
		StatPortalOpenData.GraphViewer.disableColumnNotInteresting();
	}else{
		StatPortalOpenData.GraphViewer.activeAllColumns();
	}
	
	StatPortalOpenData.GraphViewer.resetGraphPagination();
	StatPortalOpenData.GraphViewer.measureChangedHandler();	
};

/**
 * Gestione del cambio di colonna selezionata
 */
StatPortalOpenData.GraphViewer.graphFieldToCountChanged = function graphFieldToCountChanged(){
	
	// se si è selezionata una dimensione si deve aggiornare il controllo dei filtri (nel caso in cui non sia una count)
	if($('#graphFieldToCount :selected').attr('data-type') == 'dimension' && $('#graphMeasures').val() != 'ELEMENT_COUNT'){
		$('#graph-dimension-filter').css('display', 'inline');
		StatPortalOpenData.GraphViewer.uploadFilterKeyController(StatPortalOpenData.GraphViewer.uploadGraph);
	}else{
		$('#graph-dimension-filter').css('display', 'none');
	}
	
	StatPortalOpenData.GraphViewer.resetGraphPagination();
	StatPortalOpenData.GraphViewer.uploadGraph();
};

/**
 * Upload del grafico
 */
StatPortalOpenData.GraphViewer.uploadGraph = function uploadGraph(){
	$('#graph_viewer_loading').show();
	/*$.jqplot.enablePlugins=true;*/
	var graphType = $('#graphType :selected').val();
	var dim;
	var meas = $('#graphMeasures').val();
	var measures = state.getMeasures();
	var measAlias = meas;
	$.each(measures, function() {
	    if(this.getId().toString() === meas){
	    	measAlias = this.getAlias();
	    }
	});
	
	// cerchiamo di capire se si tratta di raggruppamento su dimensione o colonna generica
	var graphFieldToCountEl = $('#graphFieldToCount :selected'); 
	var dimOrFieldToCount = graphFieldToCountEl.val();
	var dimOrFieldToCountId = graphFieldToCountEl.attr('data-id');
	var dimOrFieldToCountType = graphFieldToCountEl.attr('data-type');
	if(dimOrFieldToCountType == 'dimension'){
		dim = dimOrFieldToCountId;
	}
	
	if(meas === 'ELEMENT_COUNT'){
		var fieldToCount = $('#graphFieldToCount :selected').val();
		// si deve fare la chiamata OData per avere la count distinct
		
		var otherParametersForCount = '';
		//top, othersValue, minResults, maxResults, minPercentage
		if(graphType === 'pie' || graphType === 'funnel' || graphType === 'donut'){
			otherParametersForCount = 'othersValue=true&minResults=5&maxResults=20&minPercentage=3';
		}else if(graphType === 'vertical_bar' || graphType === 'line'){
			var requestGap = '&top=' + StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS + '&$skip=' + StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM;
			otherParametersForCount = 'othersValue=false' + requestGap;
		}
		
		var countRequest = state.getServiceUri() + state.getResourcePath().replace('DataRows', 'DistinctCountRows') + '?distinctField=' + fieldToCount + '&' + otherParametersForCount;
		
		StatPortalOpenData.GraphViewer.ACTUAL_PAGE = (StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM/StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS)+1;
		
		// solo in caso di prima richiesta per le tipologie di grafico a barre e linee
		if(StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM === 0 && (graphType === 'vertical_bar' || graphType === 'line')){
			var countOfcountRequest = state.getServiceUri() + state.getResourcePath() + '?cardinalityForField=' + fieldToCount;
			
			StatPortalOpenData.utility.log(countOfcountRequest);
			OData.defaultHttpClient.enableJsonpCallback = true;
			OData.read(countOfcountRequest, function(count) {
				StatPortalOpenData.GraphViewer.NUM_GRAPH_RESULTS = count.results[0].Tot;	
				StatPortalOpenData.GraphViewer.PAGE_FOR_GRAPH_RESULTS = Math.ceil(StatPortalOpenData.GraphViewer.NUM_GRAPH_RESULTS/StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS);
				
				var minMaxRequest = state.getServiceUri() + state.getResourcePath().replace('DataRows', 'DistinctCountRows') + '?distinctField=' + fieldToCount + '&' + '&minMax=true';
				StatPortalOpenData.utility.log(minMaxRequest);

				OData.defaultHttpClient.enableJsonpCallback = true;
				OData.read(minMaxRequest, function(minMaxResponse) {
					try{
						StatPortalOpenData.GraphViewer.MIN_SCALE_GRAPH_VALUE = minMaxResponse.results[0].Min;
						StatPortalOpenData.GraphViewer.MAX_SCALE_GRAPH_VALUE = minMaxResponse.results[0].Max;
					}catch(Exception){
						StatPortalOpenData.GraphViewer.MIN_SCALE_GRAPH_VALUE = undefined;
						StatPortalOpenData.GraphViewer.MAX_SCALE_GRAPH_VALUE = undefined;
					}
					
					StatPortalOpenData.utility.log(countRequest);
					OData.defaultHttpClient.enableJsonpCallback = true;
					OData.read(countRequest, StatPortalOpenData.GraphViewer.callbackGraphUploading, function(err) {StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(60);});
					
				}, function(err) {
					StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(65);
				});
				
			}, function(err) {
				StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(61);
			});
		}else{
			OData.defaultHttpClient.enableJsonpCallback = true;
			OData.read(countRequest, function(data) {
				StatPortalOpenData.GraphViewer.callbackGraphUploading(data);
			}, function(err) {
				StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(62);
			});	
		}
		
	}else if(dimOrFieldToCountType == 'dimension'){
		var ordering = '';
		var dimType = '';
		// se si tratta di una serie temporale si ordina in ordine alfabetico, altrimenti in ordine decrescente rispetto alla misura
		var dimensions = state.getDimensions();
		$.each(dimensions, function() {
		    if(this.getId().toString() === dim){
		    	dimType = this.getDimType();
		    }
		});
		

		if(dimType === 'TEMPORALE'){
			// si ordina in maniera crescente rispetto alla dimensione visualizzata nel caso di grafici a barre e linea
			$.each(dimensions, function() {
			    if(this.getId().toString() === dim){
			    	ordering = '&$orderby=' + StatPortalOpenData.ODataUtility.getUniqueIdentifier(this.getAlias()) + ' asc';
			    }
			});
		}else{
			ordering = '&$orderby=' + StatPortalOpenData.ODataUtility.getUniqueIdentifier(measAlias) + ' desc';
		}
		
		var filtering = '';
		
		if($('#graph-dimension-filter').is(":visible")){
			var dimensionFilterKeySelected = $('#dimensionFilterKeyController option:selected').val();
			var dimensionFilterValueSelectedVal = '-1';
			var dimensionFilterValueSelectedText = '';
			if(jQuery('#dimensionFilterValueController').length > 0){
				dimensionFilterValueSelectedVal = $('#dimensionFilterValueController option:selected').val();
				dimensionFilterValueSelectedText = $('#dimensionFilterValueController option:selected').text();
			}else if(jQuery('#dimension-filter-value-tb').length > 0){
				dimensionFilterValueSelectedText = jQuery('#dimension-filter-value-tb').val();
				if(dimensionFilterValueSelectedText == '' || dimensionFilterValueSelectedText == StatPortalOpenData.GraphViewer.TEXT_IF_TEXTBOX_FILTER_IS_EMPTY) {
					dimensionFilterValueSelectedVal = '-1';
				}else{
					dimensionFilterValueSelectedVal = '';
				}
			}
			if(dimensionFilterValueSelectedVal !== '-1'){
				filtering = '&$filter=substringof(\'' + dimensionFilterValueSelectedText.replace(new RegExp("\'", "g"),"\'\'") + '\', ' + dimensionFilterKeySelected + ') eq true';
			}
		}
		
		var requestGap = '&$top=' + StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS + '&$skip=' + StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM;

		var request = state.getServiceUri() + state.getResourcePath() + '?dimensionList=' + dim + '&measureList=' + meas + ':SUM' + ordering + filtering + requestGap;
		if(graphType === 'pie' || graphType === 'funnel' || graphType === 'donut'){
			request = state.getServiceUri() + state.getResourcePath() + '?dimensionList=' + dim + '&measureList=' + meas + ':SUM' + ordering + filtering + '&othersValue=true&minResults=5&maxResults=20&minPercentage=3';
		}
		StatPortalOpenData.GraphViewer.ACTUAL_PAGE = (StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM/StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS)+1;
		// se è la prima richiesta si chiede prima il count degli elementi (nel caso di grafici a linee e barre)
		
		// si escludono i null dal risultato
		request = request + "&$filter=" + StatPortalOpenData.ODataUtility.getUniqueIdentifier(measAlias) + " ne null";
		if(StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM === 0 && (graphType === 'vertical_bar' || graphType === 'line')){
			var requestCount = request + '&count=true&excludeNulls=true';
			
			var minMaxRequest = request + '&minMax=true';
			StatPortalOpenData.utility.log(minMaxRequest);
			
			OData.defaultHttpClient.enableJsonpCallback = true;
			OData.read(minMaxRequest, function(minMaxResponse) {
				try{
					StatPortalOpenData.GraphViewer.MIN_SCALE_GRAPH_VALUE = minMaxResponse.results[0].Min;
					StatPortalOpenData.GraphViewer.MAX_SCALE_GRAPH_VALUE = minMaxResponse.results[0].Max;
				}catch(Exception){
					StatPortalOpenData.GraphViewer.MIN_SCALE_GRAPH_VALUE = undefined;
					StatPortalOpenData.GraphViewer.MAX_SCALE_GRAPH_VALUE = undefined;
				}
				
				StatPortalOpenData.utility.log(requestCount);
				OData.defaultHttpClient.enableJsonpCallback = true;
				OData.read(	requestCount, 
							function(data){
								StatPortalOpenData.GraphViewer.NUM_GRAPH_RESULTS = data.results[0].Tot;	
								StatPortalOpenData.GraphViewer.PAGE_FOR_GRAPH_RESULTS = Math.ceil(StatPortalOpenData.GraphViewer.NUM_GRAPH_RESULTS/StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS);
								
								StatPortalOpenData.utility.log(request);
								OData.defaultHttpClient.enableJsonpCallback = true;
								OData.read(request, StatPortalOpenData.GraphViewer.callbackGraphUploading, function(err) {
									showMessage('error', 'Impossibile visualizzare il grafico');
									$('#graph_viewer_loading').hide();
								});
							},
							function(err) {
								StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(63);
				});
				
			}, function(err) {
				StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(65);
			});
			
		}else{
			OData.defaultHttpClient.enableJsonpCallback = true;
			OData.read(request, StatPortalOpenData.GraphViewer.callbackGraphUploading, function(err) {
				StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(64);
			});
		}
	}else{
		// si tratta del nuovo caso in cui si vuole raggruppare per una colonna generica
		// si disabilitano i filtri
		jQuery('#graph-dimension-filter').css('display', 'none');
		var filtering = '';
		
		var commonRequestPrefix = state.getServiceUri() + state.getResourcePath().replace('DataRows', 'GenericColumnAggregationRows') + '?column=' + dimOrFieldToCount + '&measureList=' + meas + ':SUM' + filtering;
		
		if(graphType === 'pie' || graphType === 'funnel' || graphType === 'donut'){
			request = commonRequestPrefix + '&othersValue=true&minResults=5&maxResults=20&minPercentage=3';
			StatPortalOpenData.utility.log(request);
			OData.defaultHttpClient.enableJsonpCallback = true;
			OData.read(request, StatPortalOpenData.GraphViewer.callbackGraphUploading, function(err) {
				StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(67);
			});
		}else{
			
			if(StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM == 0){
			
				//dimOrFieldToCountId
				var minMaxRequest = commonRequestPrefix + '&minMax=true';
				
				StatPortalOpenData.utility.log(minMaxRequest);
				OData.defaultHttpClient.enableJsonpCallback = true;
				OData.read(minMaxRequest, function(minMaxResponse) {
					try{
						StatPortalOpenData.GraphViewer.MIN_SCALE_GRAPH_VALUE = minMaxResponse.results[0].Min;
						StatPortalOpenData.GraphViewer.MAX_SCALE_GRAPH_VALUE = minMaxResponse.results[0].Max;
					}catch(Exception){
						StatPortalOpenData.GraphViewer.MIN_SCALE_GRAPH_VALUE = undefined;
						StatPortalOpenData.GraphViewer.MAX_SCALE_GRAPH_VALUE = undefined;
					}
					
					// si compone la request con la count			
					var requestCount = commonRequestPrefix + '&count=true';
					
					StatPortalOpenData.utility.log(requestCount);
					OData.defaultHttpClient.enableJsonpCallback = true;
					OData.read(	requestCount, 
								function(data){
									StatPortalOpenData.GraphViewer.NUM_GRAPH_RESULTS = data.results[0].Tot;
									StatPortalOpenData.GraphViewer.PAGE_FOR_GRAPH_RESULTS = Math.ceil(StatPortalOpenData.GraphViewer.NUM_GRAPH_RESULTS/StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS);
									
									var requestGap = '&top=' + StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS + '&$skip=' + StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM + '&$orderby=' + StatPortalOpenData.ODataUtility.getUniqueIdentifier(measAlias) + ' desc';
									var finalRequest = commonRequestPrefix + requestGap;
									StatPortalOpenData.utility.log(finalRequest);
									
									StatPortalOpenData.GraphViewer.ACTUAL_PAGE = (StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM/StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS)+1;
									
									OData.defaultHttpClient.enableJsonpCallback = true;
									OData.read(finalRequest, StatPortalOpenData.GraphViewer.callbackGraphUploading, function(err) {
										showMessage('error', 'Impossibile visualizzare il grafico');
										$('#graph_viewer_loading').hide();
									});
								},
								function(err) {
									StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(63);
					});
	
				},
				function(err) {
					StatPortalOpenData.GraphViewer.printGraphGeneralErrorMessage(67);
				});
			}else{
				var requestGap = '&$top=' + StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS + '&$skip=' + StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM + '&$orderby=' + StatPortalOpenData.ODataUtility.getUniqueIdentifier(measAlias) + ' desc';
				var finalRequest = commonRequestPrefix + requestGap;
				StatPortalOpenData.utility.log(finalRequest);
				
				StatPortalOpenData.GraphViewer.ACTUAL_PAGE = (StatPortalOpenData.GraphViewer.GRAPH_INDEX_FROM/StatPortalOpenData.GraphViewer.MAX_NUMBER_ELEMENTS)+1;
				
				OData.defaultHttpClient.enableJsonpCallback = true;
				OData.read(finalRequest, StatPortalOpenData.GraphViewer.callbackGraphUploading, function(err) {
					showMessage('error', 'Impossibile visualizzare il grafico');
					$('#graph_viewer_loading').hide();
				});
			}
		}
	}
};

/**
 * Inizializzazione del grafico
 */
StatPortalOpenData.GraphViewer.initGraph = function initGraph(){
	StatPortalOpenData.GraphViewer.resetGraphPagination();
	StatPortalOpenData.GraphViewer.uploadGraph();
};

/**
 * Esporta l'immagine del grafico 
 * TODO: da completare e integrare
 */
StatPortalOpenData.GraphViewer.exportImageFromCanvas = function exportImageFromCanvas(){
//	var img = jQuery('#dataChart').jqplotToImageCanvas(50, 0); 
//	
//	var img2 = jQuery('#dataChart').jqplotToImageStr(50, 0); 
//	
//	var img3 = jQuery('#dataChart').jqplotToImageElem(50, 0); 
//	
//	
//	if (img) { 
//	  open(img.toDataURL("image/png")); 
//	} 
	
	var img3 = jQuery('#dataChart').jqplotToImageElem(50, 0); 
	
	jQuery(img3).dialog({ 
        modal: true, 
        overlay: { 
            opacity: 0.7, 
            background: "black",
            width: 1000
        } 
    });
	//window.location.href = img3.src.replace('image/png', 'image/octet-stream');
	
};

})(jQuery);
