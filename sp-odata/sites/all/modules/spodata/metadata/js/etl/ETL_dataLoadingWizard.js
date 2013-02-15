var StatPortalOpenData = StatPortalOpenData || {};
StatPortalOpenData.ETL = StatPortalOpenData.ETL || {};
StatPortalOpenData.ETL.Enums = StatPortalOpenData.ETL.Enums || {};

StatPortalOpenData.ETL.EXTRACT_INFO_INDEX = 0;
StatPortalOpenData.ETL.DETAILS_INFO_INDEX = 1;
StatPortalOpenData.ETL.LOAD_DATA_INDEX = 2;

StatPortalOpenData.ETL.Enums.ColumnType = {
		CHAR : 'CHAR',
		CURRENCY : 'CURRENCY',
		DATE : 'DATE',
		FLOAT : 'FLOAT',
		BOOLEAN : 'BOOLEAN',
		STRING : 'STRING',
		NUMBER : 'NUMBER',
		PICTURE : 'PICTURE',
		UNKNOWN : 'UNKNOWN'
};

StatPortalOpenData.ETL.Enums.ColumnDimension = {
		UNKNOWN : '-1',
		CITY : 'CITY',
		POSTAL_CODE : 'POSTAL_CODE',
		YEAR : '79894',
		PROVINCE : '53532',		
		REGION : '108147' 
};

var etlServiceUrl = StatPortalOpenData.ODataServices.OPENDATA_ETL;
StatPortalOpenData.ETL._columnInfo;


StatPortalOpenData.ETL.extractInfo = function extractInfo(){
	var filePath = StatPortalOpenData.ETL.getFilePath();
	if(filePath != null){
		var pl = new SOAPClientParameters();		
		pl.add('filePath', filePath);
		var extraArguments = StatPortalOpenData.ETL.getExtraArguments();
		pl.add('extraArguments', extraArguments);
		SOAPClient.invoke(etlServiceUrl, "extractInfo", pl, true, StatPortalOpenData.ETL.extractInfoHandler);
	}else{
		alert('Prima di andare avanti caricare un file');
	}
};


var visibleFileInfo = '#visibleFileInfo';
var visibleDetailsFileInfo = '#visibleDetailsFileInfo';
var editingLabel = '#labels-to-edit';
var etlWarningsContainer = '#etl-warnings-container';

StatPortalOpenData.ETL.idNameMapping;
StatPortalOpenData.ETL.nameIdMapping;

StatPortalOpenData.ETL.extractInfoHandler = function extractInfoHandler(r){
	
	try{
		var _X2JS = new X2JS();
	    var obj = _X2JS.xml_str2json(r);
	    var res = obj.Envelope.Body.extractInfoResponse.extractInfoReturn;
	    StatPortalOpenData.ETL._columnInfo = jQuery.parseJSON(res);
	    
	    
	    StatPortalOpenData.ETL.idNameMapping = new Object();
	    StatPortalOpenData.ETL.nameIdMapping = new Object();
	    
	    // si fa l'escape per gli id
	    var columnInfoLen = StatPortalOpenData.ETL._columnInfo.columnInfo.length;
	    var idxColumn = 0;
	    for(idxColumn=0; idxColumn<columnInfoLen; idxColumn++){
	    	var newId = StatPortalOpenData.ETL.setIdForName(StatPortalOpenData.ETL._columnInfo.columnInfo[idxColumn].name, idxColumn);
	    	StatPortalOpenData.ETL._columnInfo.columnInfo[idxColumn].idName = newId;
		    // si aggiunge l'indice alle colonne
	    	StatPortalOpenData.ETL._columnInfo.columnInfo[idxColumn].idx = ((idxColumn<9) ? '&nbsp;&nbsp;' : '') + (idxColumn + 1);
	    }
	    
	    // se l'esito è positivo
	    if(StatPortalOpenData.ETL._columnInfo.statusCode === 4){
		    //jQuery('#filePath').attr('readonly', 'readonly');
		    //jQuery('#filePath').css('background-color', 'whiteSmoke');
		    jQuery(visibleFileInfo).html('');
		    
		  	jQuery('#columnInfoTemplate').tmpl(StatPortalOpenData.ETL._columnInfo.columnInfo).appendTo(visibleFileInfo);
		  	jQuery('#customEventStepsETL').trigger('CustomEventStepsETL', StatPortalOpenData.ETL.EXTRACT_INFO_INDEX);
	    }else{
	    	//altrimenti viene mostrato l'errore
	    	alert(StatPortalOpenData.ETL._columnInfo.statusDescription);
	    	StatPortalOpenData.ETL.removeLoading();
	    }
	}catch(Exception){
		alert('Si e\' verificato un errore inaspettato nel recupero delle informazioni dal file');
		StatPortalOpenData.ETL.removeLoading();
		return;
	}
};

StatPortalOpenData.ETL.verifyUniquenessHandler = function verifyUniquenessHandler(r){
	
	try{
		var _X2JS = new X2JS();
	    var obj = _X2JS.xml_str2json(r);
	    var res = obj.Envelope.Body.verifyUniquenessResponse.verifyUniquenessReturn;
	    var jsonRes = jQuery.parseJSON(res);
	    
	    if(jsonRes.code === 0){
	    	// la colonna non contiene valori unici, 
	    	// quindi deve essere invitato l'utente a selezionare un valore corretto
	    	alert('La colonna non contiene valori unici. Selezionare un altro campo descrittivo');    	
	    	StatPortalOpenData.ETL.removeLoading();
	    }else{
	    	// la colonna contiene valori unici
	    	// si passa al caricamento dei dati
	    	StatPortalOpenData.ETL.loadData();
	    }
	}catch(Exception){
		alert('Si e\' verificato un errore inaspettato nella verifica dell\'univocità del campo descrittivo');
		StatPortalOpenData.ETL.removeLoading();
		return;
	}
};

StatPortalOpenData.ETL.columnDetailsChoice = function columnDetailsChoice(){
	jQuery.template("columnDetailsTemplate", columnDetailsTemplate);
	jQuery(visibleDetailsFileInfo).html('');
	
	var _options = {
		    isShapeFile: (StatPortalOpenData.ETL.getFilePath().indexOf('zip', this.length - 'zip'.length) !== -1)
		};
	jQuery('#columnDetailsTemplate').tmpl(columnDetailsForViewer, _options).appendTo(visibleDetailsFileInfo);
	StatPortalOpenData.ETL.setAllHtmlInfoBox();
	jQuery('#customEventStepsETL').trigger('CustomEventStepsETL', StatPortalOpenData.ETL.DETAILS_INFO_INDEX);
	
};


var columnDetailsForViewer;
var columnDetails;

var endOfCSV = 'csv';

StatPortalOpenData.ETL.showCsvDelimiterAndNextButtonIfNecessary = function showCsvDelimiterAndNextButtonIfNecessary(){
	// se si tratta di file csv si fa scegliere il delimitatore
	var filePath = StatPortalOpenData.ETL.getFilePath();
	/*
	if(filePath != null && filePath.indexOf(endOfCSV, this.length - endOfCSV.length) !== -1){
		jQuery('#csv-delimiter-container').show();
	}else{
		jQuery('#csv-delimiter-container').hide();
	}
	*/
	
	if(filePath != null){
		jQuery('.etl-wizard-step-buttons').show();
	}else{
		jQuery('.etl-wizard-step-buttons').hide();
	}
	
	StatPortalOpenData.ETL.checkFormatFileSelection();
};

StatPortalOpenData.ETL.checkFormatFileSelection = function checkFormatFileSelection(){
	var filePath = StatPortalOpenData.ETL.getFilePath();
	
	if(filePath == null){
		// se non è stato caricato nessun tipo di file si nasconde qualsiasi messaggio di errore
		jQuery('#message-error-etl-formatFile').text('').hide();
	}else{
		var formatFile = jQuery('#fileFormatSelection').val();
		
		// si controlla se il path termina correttamente
		if(filePath.indexOf(formatFile, this.length - formatFile.length) !== -1 || (formatFile == 'csv' && filePath.indexOf('txt', this.length - 'txt'.length) !== -1) || (formatFile == 'kml' && filePath.indexOf('kmz', this.length - 'kmz'.length) !== -1)){
			jQuery('.etl-wizard-step-buttons').show();
			jQuery('#message-error-etl-formatFile').text('').hide();
		}else{
			jQuery('#message-error-etl-formatFile').text('Il formato del file non corrisponde a quello selezionato').show();
			jQuery('.etl-wizard-step-buttons').hide();
		}
	}
};

StatPortalOpenData.ETL.getExtraArguments = function getExtraArguments(){
	return ((StatPortalOpenData.ETL.getFilePath().indexOf(endOfCSV, this.length - endOfCSV.length) !== -1) || (StatPortalOpenData.ETL.getFilePath().indexOf('txt', this.length - 'txt'.length) !== -1) ) ? jQuery('#csv-delimiter').val() : '';
};

StatPortalOpenData.ETL.initializeDataEditing = function initializeDataEditing(){
	// se c'è un file già caricato si rende visibile e si inizializza il menu a tendina
	var filePath = StatPortalOpenData.ETL.getFilePath();  
	if(filePath != null && filePath != '') {
		jQuery('#metadata-dataupload-form').show();
		StatPortalOpenData.ETL.selectFileFormatDetails(StatPortalOpenData.ETL.getFileFormat(filePath));
	}else{
		jQuery('#metadata-dataupload-form').hide();
	}
};

StatPortalOpenData.ETL.selectFileFormatDetails = function selectFileFormatDetails(format){
	
	var selectionValue = "empty"; 
	
	if(format != null && typeof(format) != 'undefined' && format != ''){
		if(format == 'csv' || format == 'txt'){
			selectionValue = 'csv';
		}else{
			selectionValue = format;
		}
	}
	
	// si seleziona l'item corretto
	jQuery('#fileFormatSelection').val(selectionValue).change();
	
};

StatPortalOpenData.ETL.getFileFormat = function getFileFormat(filePath){
	var splitted = filePath.split('.');
	var splittedLen = splitted.length;
	var format = null;
	if(splittedLen > 0){
		format = splitted[splittedLen - 1];
	}
	
	return format;
};

StatPortalOpenData.ETL.passToColumnsDetails = function passToColumnsDetails(){
	
	// si cancellano eventuali warnings (dovuti al passaggio indietro e poi avanti)
	jQuery(etlWarningsContainer).html('');
	
	// variabili di utilità generale
	var checkFieldPrefix = 'cb_';
	var labelPrefix = 'tb_';
	
	// variabili utili per il controllo sui campi duplicati
	mapLabel = new Object();
	var labelDuplicate = false;
	var labelsDuplicated = '';
	
	// array per la renderizzazione dei valori checkati da vedere
	columnDetailsForViewer = new Array();
	// si riempie l'oggetto columnDetails come se fosse un hashmap
	// indicizzato per name
	columnDetails = new Object();
	var idx = 0;
	var allColInfo = StatPortalOpenData.ETL._columnInfo.columnInfo;
	for(idx=0;idx<allColInfo.length;idx++){
		//StatPortalOpenData.ETL._columnInfo.columnInfo
		columnDetails[allColInfo[idx].name] = allColInfo[idx];
	}
	var almostOneSelected = false;
	var aliasProtected = false;
	var aliasProtectedStr = '';
	var labelsTooLong = false;
	var labelsTooLongStr = '';
	
	// si scorrono tutti i campi
	jQuery('.etl_checkField').each(function(index) {
		// si considerano solo quelli checkati
		
		// si recupera l'id
		var id = this.id.replace(checkFieldPrefix,'');
		var idName = StatPortalOpenData.ETL.getNameFromId(id);
		if(this.checked){
			almostOneSelected = true;
			// INIZIO CONTROLLO SU ETICHETTE DUPLICATE
			var label = jQuery('#' + labelPrefix + id);
			if(label.val().length > 100){
				labelsTooLong = true;
				labelsTooLongStr = label.val() + ', ';
			}
			if(typeof(mapLabel[label.val().toLowerCase()]) !== 'undefined'){			
				labelDuplicate = true;
				labelsDuplicated += label.val() + ', ';
			}else{
				mapLabel[label.val().toLowerCase()] = true;
			}
			// FINE CONTROLLO SU ETICHETTE DUPLICATE
			
			// INIZIO CONTROLLO SU ALIAS PROTETTI
			var labelToLowerCase = label.val().toLowerCase();
			if(labelToLowerCase == 'the_geom' || labelToLowerCase == 'do_x' || labelToLowerCase == 'do_y' || labelToLowerCase == 'do_ind' || labelToLowerCase == 'gid'){
				aliasProtectedStr += label.val() + ', ';
				aliasProtected = true;
			}
			// FINE CONTROLLO SU ALIAS PROTETTI
			
			// INIZIO COMPOSIZIONE NUOVA STRUTTURA DATI 
			// columnDetails
			columnDetails[idName].alias = label.val();
			columnDetailsForViewer.push(columnDetails[idName]);
			// FINE COMPOSIZIONE NUOVA STRUTTURA DATI
		}else{
			columnDetails[idName] = undefined;
		}
	});
	
	if(!almostOneSelected){
		alert('Deve essere selezionata almeno una colonna da importare');
		StatPortalOpenData.ETL.removeLoading();
	}else if(labelDuplicate){
		labelsDuplicated = labelsDuplicated.substring(0, labelsDuplicated.length - 2);
		alert('Non sono ammesse etichette duplicate (' + labelsDuplicated + ')');
		StatPortalOpenData.ETL.removeLoading();
	}else if(aliasProtected){
		aliasProtectedStr = aliasProtectedStr.substring(0, aliasProtectedStr.length - 2);
		alert('Le seguenti etichette non sono ammesse: ' + aliasProtectedStr);
		StatPortalOpenData.ETL.removeLoading();
	}else if(labelsTooLong){
		labelsTooLongStr = labelsTooLongStr.substring(0, labelsTooLongStr.length - 2);
		alert('Etichette troppo lunghe (oltre il limite di 100 caratteri): ' + labelsTooLongStr);
		StatPortalOpenData.ETL.removeLoading();
	}else{
		StatPortalOpenData.ETL.columnDetailsChoice();
	}	
};

StatPortalOpenData.ETL.typeColumnChange = function typeColumnChange(elem){
	var id = elem.id.replace('ddl_columnType_','');
	var prefixDdlDecimal = 'ddl_numberOfDecimal_';
	var ddlToShowOrHide = jQuery('#' + prefixDdlDecimal + id);
	var value = elem.value;
	
	if(elem.value === 'statistica'){
		ddlToShowOrHide.show();
	}else{
		ddlToShowOrHide.hide();
	}
	
	if(value === 'descrittiva'){
		// controllare se tra tutti gli altri menu a tendina 
		// c'è già un'altra colonna descrittiva selezionata
		var valSelected;
		jQuery('.ddl_columnType').each(function(index) {
			valSelected = jQuery(this).val();
			if(id !== this.id.replace('ddl_columnType_','') && valSelected === 'descrittiva'){
				alert('Impossibile selezionare due campi descrittivi per lo stesso file');
				jQuery(elem).val('generica');
				return false;
			}
		});
	}
	
	if(value === 'statistica' && jQuery(elem).attr('data-type') != 'FLOAT' && jQuery(elem).attr('data-type') != 'NUMBER' && jQuery(elem).attr('data-type') != 'UNKNOWN'){
		jQuery(elem).siblings(".spanColumnErrorMessage").show().text('Il tipo della colonna del file non risulta essere numerico');
	}else if(StatPortalOpenData.ETL.isFilePathShapeFile() && !isNaN(value) && jQuery(elem).attr('data-type') != 'FLOAT' && jQuery(elem).attr('data-type') != 'NUMBER' && jQuery(elem).attr('data-type') != 'UNKNOWN'){
		jQuery(elem).siblings(".spanColumnErrorMessage").show().text('Il tipo della colonna deve necessariamente essere numerico');
	}else{
		jQuery(elem).siblings(".spanColumnErrorMessage").hide().text('');
	}
	
	// se si tratta di shape e si è selezionato una dimensione, ma la tipologia della colonna non è numerica
	
	
	// TODO: da gestire anche l'inizializzazione con l'inferenza
	var selectedSelectorStr = '#' + elem.id + ' option:selected';
	var selectedText = jQuery(selectedSelectorStr).text();
	
	var infoHtml = StatPortalOpenData.ETL.getHtmlInfoBox(value, selectedText);
	
	(infoHtml !== '' ) ? jQuery(elem).siblings(".spanColumnInfoMessage").show().html(infoHtml) : jQuery(elem).siblings(".spanColumnInfoMessage").hide().html('');

	
	// se c'è almeno un errore visibile non si fa andare avanti l'utente nel wizard
	if(jQuery('.spanColumnErrorMessage').is(":visible")){
		jQuery('#error-info').text('Risolvere i problemi segnalati per andare avanti').show();
		jQuery('#step2Next').hide();
	}else{
		jQuery('#error-info').hide();
		jQuery('#step2Next').show();
	}
	
};

StatPortalOpenData.ETL.setAllHtmlInfoBox = function setAllHtmlInfoBox(){
	// si scorrono tutte le righe e si aggiorna l'infobox
	jQuery('.rowColumnDetailsContainer').each(function(index) {
		var selectElement = jQuery(this).children('.ddl_columnType');
		var optionElement = selectElement.children("option").filter(":selected");
		var selectedText = optionElement.text();
		var value = optionElement.val();
		
		var infoHtml = StatPortalOpenData.ETL.getHtmlInfoBox(value, selectedText);
		
		(infoHtml !== '' ) ? jQuery(selectElement).siblings(".spanColumnInfoMessage").show().html(infoHtml) : jQuery(selectElement).siblings(".spanColumnInfoMessage").hide().html('');
	});
	
};

StatPortalOpenData.ETL.getHtmlInfoBox = function getHtmlInfoBox(value, selectedText){
	var infoHtml = '';
	if(value == '1517' || value == '1516' || value == '277147' || value == '53532' || value == '838163' || value == '838164' || value == '1515' || value == '79894' || value == '1818'){
		infoHtml = '<a style="color:blue;" target="_blank" href="' + StatPortalOpenData.ODataServices.PATH_FOR_DOWNLOAD_NODE_TABLE + value + '.xls' + '">Clicca qui</a> per scaricare l\'elenco dei valori ammessi per il tipo <b><i>' + selectedText + '</i></b>';
	}else if(value == 'generica'){
		// non si scrive niente
	}else if(value == 'statistica'){
		infoHtml = 'Per i valori di questa colonna verranno fatte statistiche, visualizzati grafici e/o mappe tematiche';
	}else if(value == 'latitudine' || value == 'longitudine'){
		infoHtml = 'Sistema di coordinate Geografico (WGS84)';
	}
	
	return infoHtml;
};


var measureInfoCollection;
var dimensionInfoCollection;
var genericColumnInfoCollection;
// campo utilizzato per aggiungere info sul dato (nelle note)
var detailsDataLoaded; 
StatPortalOpenData.ETL.verifyUniquenessAndLoadData = function verifyUniquenessAndLoadData(){
	
	detailsDataLoaded = 'Il dato contiene le seguenti colonne: ';
	
	// si scorrono tutti
	var iLoad = 0, maxLoadLen = columnDetailsForViewer.length;
	measureInfoCollection = [];
	dimensionInfoCollection = [];
	genericColumnInfoCollection = [];
	var almostOneUniqueChecked = false, columnNameUniqueChecked = '';
	
	for(iLoad = 0; iLoad < maxLoadLen; iLoad++){
		var iesimo = columnDetailsForViewer[iLoad];
		var iName = iesimo.name;
		var idName = iesimo.idName;
		
		var iColumnType = jQuery('#ddl_columnType_' + idName).val();
		var iColumnTypeText = jQuery('#ddl_columnType_' + idName + ' option:selected').text(); 
		
		if(iColumnType === 'statistica'){
			// si leggono anche il numero di decimali
			iDecimals = jQuery('#ddl_numberOfDecimal_' + idName).val();
			
			detailsDataLoaded += iesimo.alias + ' (tipo: numero ' + ((iDecimals == 0) ? 'intero' : (iDecimals + ' decimali')) + ')';
			
			measureInfoCollection.push({
				columnName : iName,
				alias : iesimo.alias,
				description : '',
				measureUnit : '',
				decimals : iDecimals,
				pos : iLoad
			});
		}else if(iColumnType === 'generica' || iColumnType === 'descrittiva'){
			var isDescriptive = (iColumnType === 'descrittiva');
			if(isDescriptive){
				almostOneUniqueChecked = true;
				columnNameUniqueChecked = iName;
			}
			
			if(iColumnType === 'generica'){
				detailsDataLoaded += iesimo.alias + ' (tipo: generica)';
			}else if(iColumnType === 'descrittiva'){
				detailsDataLoaded += iesimo.alias + ' (tipo: descrittiva)';
			}
			
			genericColumnInfoCollection.push({
				alias : iesimo.alias,
				columnName : iName,
				descriptiveField : isDescriptive,
				pos : iLoad,
				isLatitude : false,
				isLongitude : false
			});
		}else if(iColumnType === 'latitudine' || iColumnType === 'longitudine'){
			var latLonAlias = '';
			var isLatitude = false;
			var isLongitude = false;
			
			if(iColumnType === 'latitudine'){
				latLonAlias = 'DO_Y';
				isLatitude = true;
				detailsDataLoaded += iesimo.alias + ' (tipo: latitudine)';
			}else if(iColumnType === 'longitudine'){
				latLonAlias = 'DO_X';
				isLongitude = true;
				detailsDataLoaded += iesimo.alias + ' (tipo: longitudine)';
			}
			
			genericColumnInfoCollection.push({
				alias : latLonAlias,
				columnName : iName,
				descriptiveField : false,
				pos : iLoad,
				isLatitude : isLatitude,
				isLongitude : isLongitude
			});

		}else{
			// si tratta di una dimensione
			detailsDataLoaded += iesimo.alias + ' (tipo: ' + iColumnTypeText + ')';
			
			dimensionInfoCollection.push({
				alias : iesimo.alias,
				description : '',
				idHierNode : iColumnType,
				columnName : iName,
				pos : iLoad
			});
		}
		detailsDataLoaded += '; ';
	}
	
	// cancellazione ultimo punto e virgola
	//if(detailsDataLoaded.match("; $") != null){
	//	detailsDataLoaded = detailsDataLoaded.substring(0, detailsDataLoaded.lastIndexOf("; "));
	//}
	
	// se ci sono misure ma non ci sono dimensioni 
	// non ha senso caricare il dato
	
	// CONTROLLO DA CANCELLARE: DOVRA' ESSERE PERMESSO..
//	if(measureInfoCollection.length > 0 && dimensionInfoCollection.length == 0){
//		alert('Se si seleziona almeno una colonna di tipo \'Statistica\', deve essere selezionata almeno una dimension (TOCHANGE)');
//		StatPortalOpenData.ETL.removeLoading();
//		return;
//	}
	
	if(almostOneUniqueChecked){
		// se c'è almeno una colonna 'descrittiva' checkata si verifica che non contenga valori duplicati
		var pl = new SOAPClientParameters();
		pl.add('filePath', StatPortalOpenData.ETL.getFilePath());
		pl.add('columnName', columnNameUniqueChecked);
		var extraArguments = StatPortalOpenData.ETL.getExtraArguments();
		pl.add('extraArguments', extraArguments);
		SOAPClient.invoke(etlServiceUrl, "verifyUniqueness", pl, true, StatPortalOpenData.ETL.verifyUniquenessHandler);		
	}else{
		// si passa direttamente al caricamento dei dati
		StatPortalOpenData.ETL.getWarnings();
	}
	
};

StatPortalOpenData.ETL.myEscape = function myEscape(str){
	return escape(str).replace(/%/g,'-percent-');
};

StatPortalOpenData.ETL.myUnescape = function myUnescape(str){
	return unescape(str.replace(/-percent-/g,'%'));
};

StatPortalOpenData.ETL.setIdForName = function setIdForName(str, idx){
	// creo l'id
	var newId = 'etl-column-info-' + idx;
	
	// salvo l'associazione nome, id
    StatPortalOpenData.ETL.idNameMapping[newId] = str;
    StatPortalOpenData.ETL.nameIdMapping[str] = newId;
    
    return newId;
};

StatPortalOpenData.ETL.getIdForName = function getIdForName(str){
	return StatPortalOpenData.ETL.nameIdMapping[str];
};

StatPortalOpenData.ETL.getNameFromId = function getNameFromId(id){
	return StatPortalOpenData.ETL.idNameMapping[id];
};

StatPortalOpenData.ETL.addLoading = function addLoading(){
	jQuery('#loadDataFormLoading').addClass('loadDataFormLoading');
};

StatPortalOpenData.ETL.removeLoading = function removeLoading(){
	jQuery('#loadDataFormLoading').removeClass('loadDataFormLoading');
};

StatPortalOpenData.ETL.getFilePath = function getFilePath(){
	try{
		var fileUrl = jQuery("#edit-data-file a").attr('href');
		if(typeof(fileUrl) === 'undefined'){
			StatPortalOpenData.ETL.removeLoading();
			return null;
		}else{
			var lastIndexOfSlash = fileUrl.lastIndexOf("/");
			var fileName = (lastIndexOfSlash !== -1) ? fileUrl.substring(lastIndexOfSlash, fileUrl.length) : "";
			return decodeURI(StatPortalOpenData.UPLOAD_PATH + fileName);
		}
	}catch(Exception){
		return null;
	}
};

StatPortalOpenData.ETL.isFilePathShapeFile = function isFilePathShapeFile(){
	return (StatPortalOpenData.ETL.getFilePath().indexOf('zip', this.length - 'zip'.length) !== -1);
};

StatPortalOpenData.ETL.getDbName = function getDbName(){
    var pl = new SOAPClientParameters();
    SOAPClient.invoke(etlServiceUrl, "getDbName", pl, true, StatPortalOpenData.ETL.getDbNameHandler);
};

StatPortalOpenData.ETL.getDbNameHandler = function getDbNameHandler(r){
	try{
		var _X2JS = new X2JS();
	    var obj = _X2JS.xml_str2json(r);
	    var res = obj.Envelope.Body.getDbNameResponse.getDbNameReturn;
	    alert(res);
	}catch(Exception){
		alert('Si e\' verificato un errore inaspettato');
	}
};

StatPortalOpenData.ETL.loadData = function loadData(){

	var filePath = StatPortalOpenData.ETL.getFilePath();
	if(filePath != null){
		var name = jQuery('#edit-title').val();
		var description = jQuery('#edit-body-und-0-value').val();
		var tableName = '';
		
		var dataInfo = {
				name: name, 
				description: description,
				tableName: tableName
		};
		
		dataInfo = JSON.stringify(dataInfo);
		var dimensionInfoCollectionStr = JSON.stringify(dimensionInfoCollection);
		var measureInfoCollectionStr = JSON.stringify(measureInfoCollection);
		var genericColumnInfoCollectionStr = JSON.stringify(genericColumnInfoCollection);
		
		// INFO: da togliere quando si decommenta la chiamata al ws (o no?)
		//jQuery('#customEventStepsETL').trigger('CustomEventStepsETL', StatPortalOpenData.ETL.LOAD_DATA_INDEX);
		var pl = new SOAPClientParameters();
		pl.add('filePath', filePath);
		pl.add('dataInfo', dataInfo);
		pl.add('dimensionInfoCollection', dimensionInfoCollectionStr);
		pl.add('measureInfoCollection', measureInfoCollectionStr);
		pl.add('genericColumnInfoCollection', genericColumnInfoCollectionStr);
		var extraArguments = StatPortalOpenData.ETL.getExtraArguments();
		pl.add('extraArguments', extraArguments);

		SOAPClient.invoke(etlServiceUrl, "importData", pl, true, StatPortalOpenData.ETL.loadDataHandler);	
	}
};

StatPortalOpenData.ETL.getWarnings = function getWarnings(){
	var filePath = StatPortalOpenData.ETL.getFilePath();
	if(filePath != null){
		var dimensionInfoCollectionStr = JSON.stringify(dimensionInfoCollection);
		var measureInfoCollectionStr = JSON.stringify(measureInfoCollection);
		var genericColumnInfoCollectionStr = JSON.stringify(genericColumnInfoCollection);
		var extraArguments = StatPortalOpenData.ETL.getExtraArguments();
		var pl = new SOAPClientParameters();
		pl.add('filePath', filePath);
		pl.add('dimensionInfoCollection', dimensionInfoCollectionStr);
		pl.add('measureInfoCollection', measureInfoCollectionStr);
		pl.add('genericColumnInfoCollection', genericColumnInfoCollectionStr);
		pl.add('extraArguments', extraArguments);
		
		SOAPClient.invoke(etlServiceUrl, "getWarnings", pl, true, StatPortalOpenData.ETL.getWarningsHandler);
	}
};

StatPortalOpenData.ETL.getWarningsHandler = function getWarningsHandler(r){
	
	try{
		var _X2JS = new X2JS();
	    var obj = _X2JS.xml_str2json(r);
	    
	    var res = obj.Envelope.Body.getWarningsResponse.getWarningsReturn;
	    
	    var resWarningObj = jQuery.parseJSON(res);
		var warnings = resWarningObj.warnings;
		
		var zeroWarnings = (warnings.length == 0);
		var canForceImport = resWarningObj.canForceImport;	
		
		
		if(zeroWarnings){
			StatPortalOpenData.ETL.loadData();
			jQuery(etlWarningsContainer).html('');
			jQuery(etlWarningsContainer).hide();
		}else{
			// si nascondono i bottoni del wizard esterno e si fa apparire un 'indietro' e 'avanti' "fittizi"
			jQuery('.etl-wizard-step-buttons').hide();
			jQuery('#etl-button-for-warnings').show();
			jQuery('#visibleDetailsFileInfo').hide();
			// si visualizzano i warnings
			jQuery(etlWarningsContainer).html('');
			var warningDetails = '';
			if(canForceImport){
				warningDetails += '<div class="messages error">Le seguenti righe contengono errori, se si prosegue verranno scartate. </div><br />';
			}else{
				warningDetails += '<div class="messages error">Le seguenti righe contengono errori che non permettono il caricamento del dato. </div><br />';
			}
			warningDetails += '<div id="etl-warnings-details"></div>';
			var solveDetails = '<br /><b>Per risolvere il problema provare a:</b><ul><li>modificare la tipologia delle colonne<li>modificare il file, ricaricarlo e seguire il wizard</ul><br />';
			jQuery(warningDetails).appendTo(etlWarningsContainer);
			jQuery(solveDetails).appendTo(etlWarningsContainer);
			jQuery('#warningInfoTemplate').tmpl(warnings).appendTo('#etl-warnings-details');
			
		    jQuery(etlWarningsContainer).scrollTop(0);
		    
		    jQuery(etlWarningsContainer).show();
		    
		    jQuery('#columnTypeInfoBox').hide();
		    
		    // se non si può forzare il caricamento si fa sparire il tasto avanti
		    if(!canForceImport){
		    	jQuery('#stepFittNext').hide();
		    }else{
		    	jQuery('#stepFittNext').show();
		    }
		    
		    StatPortalOpenData.ETL.removeLoading();
		}
	}catch(Exception){
		alert('Si e\' verificato un errore inaspettato nella richiesta di errori sul file');
		StatPortalOpenData.ETL.removeLoading();
		return;
	}
};

StatPortalOpenData.ETL.loadDataHandler = function loadDataHandler(r){
	
	try{
		var _X2JS = new X2JS();
	    var obj = _X2JS.xml_str2json(r);
	    var res = obj.Envelope.Body.importDataResponse.importDataReturn;
	    
	    var result = jQuery.parseJSON(res);
	    //statusCode":3,"statusDescription":"Struttura estratta correttamente","visualizationFormats":["Table","Chart"],"isGeographic":false}"
	    
	    if(result.statusCode == 3){    	
	    	// il dato caricato se andato a buon fine sarà sempre alfanumerico
	    	
	    	// si imposta il formato del file
	    	if(typeof(StatPortalOpenData.ETL._columnInfo) != 'undefined' && typeof(StatPortalOpenData.ETL._columnInfo.fileType) != 'undefined'){
	    		jQuery('#file-format').val(StatPortalOpenData.ETL._columnInfo.fileType).trigger('change');
	    	}
	    	
	    	jQuery('#cb-type-alphanumeric, #cb-type-geographic, #cb-type-structured, #cb-type-raw, #cb-viewer-table, #cb-viewer-map2d, #cb-viewer-map3d, #cb-viewer-graph').prop("checked", false).trigger('change');
	    	
	    	jQuery('#cb-type-alphanumeric').prop("checked", true).trigger('change');
	    	
	    	// si imposta il formato 
	    	if(result.isGeographic){
	    		jQuery('#cb-type-geographic').prop("checked", true).trigger('change');
	    	}
	    	
	    	if(result.isStructured){
	    		jQuery("#cb-type-structured").prop("checked", true).trigger('change');	
	    	}else{
	    		jQuery("#cb-type-raw").prop("checked", true).trigger('change');
	    	}
	    	
	    	jQuery("#tb-odata-url").val(result.odataLink).trigger('change');
	    	
	    	// si leggono i viewer disponibili
	    	var viewers = result.visualizationFormats;
	    	if(typeof(viewers) != 'undefined'){
	    		var idxViewer = 0, viewersLen = viewers.length;
	    		for(idxViewer= 0; idxViewer < viewersLen; idxViewer++){
		    		switch(viewers[idxViewer]){
		    			case 'Table' :
		    				jQuery("#cb-viewer-table").prop("checked", true).trigger('change');
		    				jQuery('#cb-viewer-table-container').show();
		    				break;
		    			case 'Map2D' :
		    				jQuery("#cb-viewer-map2d").prop("checked", true).trigger('change');
		    				jQuery('#cb-viewer-map2d-container').show();
		    				break;
		    			case 'Map3D' :
		    				jQuery("#cb-viewer-map3d").prop("checked", true).trigger('change');
		    				jQuery('#cb-viewer-map3d-container').show();
		    				break;
		    			case 'Chart' :
		    				jQuery("#cb-viewer-graph").prop("checked", true).trigger('change');
		    				jQuery('#cb-viewer-graph-container').show();
		    				break;
		    		}
		    	}
	    	}
	    	
	    	// si imposta l'input hidden che poi andrà accodato alle note
	    	jQuery('#extra-notes').val(detailsDataLoaded);
	    	
	    	// è andato tutto bene, si fa sparire il loading e anche i bottoni
	    	jQuery('#customEventStepsETL').trigger('CustomEventStepsETL', StatPortalOpenData.ETL.LOAD_DATA_INDEX);
	    	jQuery('.etl-wizard-step-buttons').hide();
	    	jQuery('#etl-button-for-warnings').hide();
	    }else{
	    	// qualcosa è andato storto. Si cancella il valore dell'input hidden
	    	detailsDataLoaded = '';
	    	jQuery('#extra-notes').val('');
	    	
	    	StatPortalOpenData.ETL.removeLoading();
	    	if(typeof(result.statusDescription) == 'undefined'){
	    		result.statusDescription = 'Impossibile caricare il dato (Codice errore: ' + result.statusCode + ')';
	    	}
	    	
	    	alert(result.statusDescription);
	    }
	}catch(Exception){
		alert('Si e\' verificato un errore inaspettato nella fase di caricamento del dato');
		StatPortalOpenData.ETL.removeLoading();
		return;
	}
};

StatPortalOpenData.ETL.getAliases = function getAliases(){
	jQuery('#loadDataFormLoadingForLabel').addClass('loadDataFormLoading');	
	var dataId = jQuery('#uid_odata_attach').val();
	if(dataId != '-1'){
		var pl = new SOAPClientParameters();
		pl.add('dataUid', dataId);
		SOAPClient.invoke(etlServiceUrl, "getAliases", pl, true, StatPortalOpenData.ETL.getAliasesHandler);
	}
};

StatPortalOpenData.ETL.getAliasesHandler = function getAliasesHandler(r){
	try{
		var _X2JS = new X2JS();
	    var obj = _X2JS.xml_str2json(r);
	    var res = obj.Envelope.Body.getAliasesResponse.getAliasesReturn;
		
	    StatPortalOpenData.ETL._aliases = jQuery.parseJSON(res);
	    
		jQuery(editingLabel).html('');
	    jQuery('#labelInfoTemplate').tmpl(StatPortalOpenData.ETL._aliases.aliases).appendTo(editingLabel);
	}catch(Exception){
		alert('Si e\' verificato un errore inaspettato nel recupero delle etichette');
	}
	jQuery('#loadDataFormLoadingForLabel').removeClass('loadDataFormLoading');
};

StatPortalOpenData.ETL.setAliases = function setAliases(){
	jQuery('#loadDataFormLoadingForLabel').addClass('loadDataFormLoading');
	var error = false;
	var labelDictionary = {};
	var labelDuplicateStr = '';
	var aliasProtectedStr = '';
	
	jQuery('.etl_alias').each(function(index) {
		var id = jQuery(this)[0].id.replace('tb_alias_', '');
		var value = jQuery(this).val();
		var dataType = jQuery(this).attr('data-type');
		
		// se non si tratta di latitudine o longitudine si cambia l'alias nella struttura dati
		if(dataType != 'latitude' && dataType != 'longitude'){
			if(typeof(labelDictionary[value]) != 'undefined'){
				error = true;
				labelDuplicateStr += value + '  ';
			}else if(false){
				var labelToLowerCase = value.toLowerCase();
				if(labelToLowerCase == 'the_geom' || labelToLowerCase == 'do_x' || labelToLowerCase == 'do_y' || labelToLowerCase == 'do_ind' || labelToLowerCase == 'gid'){
					aliasProtectedStr += value + ', ';
					error = true;
				}
			}else{
				labelDictionary[value] = true;
			}
			var alsIdx = 0, found = false;
			for(alsIdx = 0; alsIdx < StatPortalOpenData.ETL._aliases.aliases.length; alsIdx++){
				if(StatPortalOpenData.ETL._aliases.aliases[alsIdx].id == id){
					found = true;
					StatPortalOpenData.ETL._aliases.aliases[alsIdx].alias = value;
					break;
				}
			}
			if(!found){
				error = true;
			}
		}
	});
	
	if(error){
		if(labelDuplicateStr != ''){
			alert('Non sono ammesse etichette duplicate (' + labelDuplicateStr + ')');
		}else if(aliasProtectedStr != ''){
			aliasProtectedStr = aliasProtectedStr.substring(0, aliasProtectedStr.length - 2);
			alert('Le seguenti etichette non sono ammesse: ' + aliasProtectedStr);
		}else{
			alert('Si e\' verificato un errore inaspettato');
		}
		jQuery('#loadDataFormLoadingForLabel').removeClass('loadDataFormLoading');
	}else{
		// si chiama il ws
		var pl = new SOAPClientParameters();
		var dataId = jQuery('#uid_odata_attach').val();
		pl.add('dataId', dataId.split("@")[0]);
		pl.add('aliases', JSON.stringify(StatPortalOpenData.ETL._aliases));
		SOAPClient.invoke(etlServiceUrl, "setAliases", pl, true, StatPortalOpenData.ETL.setAliasesHandler);
	}
};

StatPortalOpenData.ETL.setAliasesHandler = function setAliasesHandler(r){
	var editLabelErrorMessage = 'Si e\' verificato un errore inaspettato nella modifica delle etichette';
	try{
		var _X2JS = new X2JS();
		var obj = _X2JS.xml_str2json(r);
		var res = obj.Envelope.Body.setAliasesResponse.setAliasesReturn;
		// non c'è in risposta se è tutto ok
		if(res == 'true'){
			jQuery('#apply-result').show();
		}else{
			alert(editLabelErrorMessage);
		}
	}catch(Exception){
		alert(editLabelErrorMessage);
	}
	jQuery('#loadDataFormLoadingForLabel').removeClass('loadDataFormLoading');
};

StatPortalOpenData.ETL.provaCancellazione = function provaCancellazione(){
	var pl = new SOAPClientParameters();
	pl.add('dataUid', 'ce3d75f3-8e81-4612-a2ed-d0a9805e4edc');
	SOAPClient.invoke(etlServiceUrl, "deleteData", pl, true, StatPortalOpenData.ETL.provaCancellazioneHandler);	
};

StatPortalOpenData.ETL.setAllColumnsAsGenerics = function setAllColumnsAsGenerics(){
	jQuery('#visibleDetailsFileInfo select.ddl_columnType').val('generica').trigger('change');
};

StatPortalOpenData.ETL.dummyPrevious = function dummyPrevious(){
	jQuery('#columnTypeInfoBox').show();
	StatPortalOpenData.ETL.addLoading();
	jQuery('#visibleDetailsFileInfo').show(); 
	jQuery('#etl-warnings-container').html(''); 
	jQuery('#etl-warnings-container').hide(); 
	jQuery('#etl-button-for-warnings').hide(); 
	jQuery('.etl-wizard-step-buttons').show(); 
	StatPortalOpenData.ETL.removeLoading();
};

StatPortalOpenData.ETL.dummyNext = function dummyNext(){
	StatPortalOpenData.ETL.addLoading();
	StatPortalOpenData.ETL.loadData();
};


StatPortalOpenData.ETL.provaCancellazioneHandler = function provaCancellazioneHandler(r){
	var _X2JS = new X2JS();
    var obj = _X2JS.xml_str2json(r);
    var res = obj.Envelope.Body.deleteDataResponse.deleteDataReturn;
	
    var uff = jQuery.parseJSON(res);
    
};

StatPortalOpenData.ETL.onChangeFileFormatInfoBox = function onChangeFileFormatInfoBox(selection){
	var value = selection[selection.selectedIndex].value;
	jQuery('.fileFormatInfoBox').hide();
	if(value == 'empty'){
		jQuery('#metadata-dataupload-form').hide();
	}else{
		jQuery('#metadata-dataupload-form').show();
		var idFormatInfoBox = '#fileFormatInfoBox-' + value;
		jQuery(idFormatInfoBox).show();
		
		if(value == 'csv'){
			jQuery('#csv-delimiter-container').show();
		}else{
			jQuery('#csv-delimiter-container').hide();
		}
	}
	
	StatPortalOpenData.ETL.checkFormatFileSelection();
};


//implement JSON.stringify serialization
JSON.stringify = JSON.stringify || function (obj) {
    var t = typeof (obj);
    if (t != "object" || obj === null) {
        // simple data type
        if (t == "string") obj = '"'+obj+'"';
        return String(obj);
    }
    else {
        // recurse array or object
        var n, v, json = [], arr = (obj && obj.constructor == Array);
        for (n in obj) {
            v = obj[n]; t = typeof(v);
            if (t == "string") v = '"'+v+'"';
            else if (t == "object" && v !== null) v = JSON.stringify(v);
            json.push((arr ? "" : '"' + n + '":') + String(v));
        }
        return (arr ? "[" : "{") + String(json) + (arr ? "]" : "}");
    }
};