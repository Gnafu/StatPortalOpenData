(function($) {
/*
 * @namespace StatPortalOpenData
 * @class State
 *
 * @param {Object}	options configuration {Configuration} configurazioni generali
 * 							modules {Array} insieme di moduli accessibili
 * 							dimensions {Array<Dimension>} insieme delle dimensioni (se si tratta di un dato strutturato)
 * 							measures {Array<Measure>} insieme delle misure (se si tratta di un dato strutturato)
 * 							columns {Array<Column>} insieme delle colonne (se NON si tratta di un dato strutturato)
 * 							filters {} eventuali filtri presenti
 *
 */
StatPortalOpenData.State = function State(options) {

	// self invoking constructor
	if(!(this instanceof State)) {
		return new State(options);
	}

	var defaults = {
		modules : [],
		dimensions : [],
		measures : [],
		columns : [],
		filters : [],
		columnStructure : []
	};
	options = $.extend(defaults, options);

	// inizializzazione
	var _configuration = options.configuration, _modules = options.modules, _dataInfo = {
		dimensions : options.dimensions,
		measures : options.measures,
		columns : options.columns
	}, _columnStructure = options.columnStructure, 
	_filters = options.filters,
	_dataType = options.dataType;

	// esempio di funzione privata
	/*
	 var private_function = function()
	 {
	 myPrivateVar = "I can set this here!";
	 }
	 */
	
	this.setDataType = function setDataType(dataType){
		_dataType = dataType;
	};
	
	this.getDataType = function getDataType(){
		return _dataType;
	};
	
	this.setFullScreenMode = function setFullScreenMode(moduleId, isFullScreen){
		var i = 0;
		for (i = 0; i < _modules.length; i++) {
			if (_modules[i].getId() === moduleId) {
				_modules[i].setFullScreenMode(isFullScreen);
			}
		}
	};
	
	this.isFullScreenMode = function isFullScreenMode(moduleId){
		var i = 0;
		for (i = 0; i < _modules.length; i++) {
			if (_modules[i].getId() === moduleId) {
				return _modules[i].isFullScreenMode();
			}
		}
	};
	
	
	this.setNewAndOnlyModuleActive = function setNewAndOnlyModuleActive(moduleId){
		_modules = [ new StatPortalOpenData.Module({
			id : moduleId,
			enabled : true
		}) ];
	}; 

	this.isModuleActive = function isModuleActive(moduleId) {
		var i = 0;
			for (i = 0; i < _modules.length; i++) {
				if (_modules[i].getId() === moduleId
						&& _modules[i].isEnabled() === true) {
					return true;
				}
			}
			return false;
	};
	
	this.isAggregation = function isAggregation(){
		var idx, struct = _dataInfo.dimensions, structLen = struct.length, countDimAggregated = 0;
		
		for(idx=0;idx<structLen; idx++){
			if(struct[idx].getAggregated()){
				countDimAggregated++;
			}
		}
		
		if(countDimAggregated === 0 || structLen === countDimAggregated){
			return false;
		}else{
			return true;
		}
		
		
	};
  
	this.isStructuredData = function isStructuredData() {
		return (_configuration.getDataType() === StatPortalOpenData.Enums.DataType.MD_DATA);
	};

	this.areIncludedNavigationProperties = function areIncludedNavigationProperties() {
		return _configuration.areIncludedNavigationProperties();
	};

	this.getResourcePath = function getResourcePath() {
		return _configuration.getResourcePath();
	};
	
	this.getServiceUri = function getServiceUri() {
		return _configuration.getServiceUri();
	};

	this.getCrossDomainProxy = function getCrossDomainProxy() {
		return _configuration.getCrossDomainProxy();
	};
	
	this.getDataId = function getDataId(){
		return _configuration.getDataId();
	};

	this.addDimension = function addDimension(dimension) {
		// si crea una copia della dimensione
		/*
		var dimClone = new StatPortalOpenData.Dimension({
			alias : dimension.getAlias(),
			name : StatPortalOpenData.ODataUtility.getUniqueIdentifier(dimension.getAlias()),
			aggregated : dimension.getAggregated(),
			id : dimension.getId()
		});
		_dataInfo.dimensions.push(dimClone);
		*/
		_dataInfo.dimensions.push(dimension);
	};

	this.getDimensions = function getDimensions() {
		return _dataInfo.dimensions;
		/*
		var dimensionsClone = new Array();
		var dimensionsLength = _dataInfo.dimensions.length;
		var i, max;
		for( i = 0, max = dimensionsLength; i < max; i++) {
			dimensionsClone.push(_dataInfo.dimensions[i]);
		}
		//var dimensionsClone = jQuery.extend(true, {}, _dataInfo.dimensions);
		return dimensionsClone;
		*/
	};
	
	this.containsAlmostOneTerritorialDimension = function containsAlmostOneTerritorialDimension(){
		var dimCounter = 0, dimensionsLength = _dataInfo.dimensions.length;
		for( dimCounter = 0; dimCounter < dimensionsLength; dimCounter++) {
			if(_dataInfo.dimensions[dimCounter].getDimType() == 'TERRITORIALE'){
				return true;
			}
		}
		
		return false;
	};

	this.getDimensionsCount = function getDimensionsCount() {
		return _dataInfo.dimensions.length;
	};
	
	this.getColumnStructure = function getColumnStructure(){
		return _columnStructure;
	};
	
	this.setColumnStructure = function setColumnStructure(columnStructure){
		_columnStructure = columnStructure;
	};

	/*
	this.setDimensions = function setDimensions(dimensions) {
		var dimensionsClone = new Array();
		var dimensionsLength = dimensions.length;
		var i, max;
		for( i = 0, max = dimensionsLength; i < max; i++) {
			dimensionsClone.push(dimensions[i]);
		}
		//var dimensionsClone = jQuery.extend(true, {}, _dataInfo.dimensions);
		_dataInfo.dimensions = dimensionsClone;
	}
	*/

	this.addColumn = function addColumn(column) {
		// si crea una copia della colonna
		/*
		var columnClone = jQuery.extend(true, {}, column);
		_dataInfo.columns.push(columnClone);
		*/
		_dataInfo.columns.push(column);
	};

	this.getColumns = function getColumns() {
		return _dataInfo.columns;
		/*
		var columnsClone = new Array();
		var columnsLength = _dataInfo.columns.length;
		var i, max;
		for( i = 0, max = columnsLength; i < max; i++) {
			columnsClone.push(_dataInfo.columns[i]);
		}
		return columnsClone;
		*/
	};
	
	this.getColumnByName = function getColumnByName(name){
		var res = this.getColumnByNameAndColumnType(name, StatPortalOpenData.Enums.ColumnType.MEASURE);
		if(res != null)
			return res;
		res = this.getColumnByNameAndColumnType(name, StatPortalOpenData.Enums.ColumnType.DIMENSION);
		if(res != null)
			return res;
		res = this.getColumnByNameAndColumnType(name, StatPortalOpenData.Enums.ColumnType.COLUMN);
		return res;
	};
	
	this.getColumnByNameAndColumnType = function getColumnByNameAndColumnType(name, columnType){
		
		var dataStructure, dataStructureLen, index;
		
		if(columnType === StatPortalOpenData.Enums.ColumnType.MEASURE){
			dataStructure = _dataInfo.measures;
		}else if(columnType === StatPortalOpenData.Enums.ColumnType.DIMENSION){
			dataStructure = _dataInfo.dimensions;
		}else if(columnType === StatPortalOpenData.Enums.ColumnType.COLUMN){
			dataStructure = _dataInfo.columns;
		}else{
			return null;
		}
		
		dataStructureLen = dataStructure.length;

		for(index=0; index < dataStructureLen; index++){
			var iesimo = dataStructure[index];
			if(iesimo.getName() === name){
				return iesimo;
			}
		}
		return null;
	};
	
	this.setDimensionAggregation = function setDimensionAggregation(name, aggregated){
		var result = false;
		var dim = this.getColumnByNameAndColumnType(name, StatPortalOpenData.Enums.ColumnType.DIMENSION);
		if(dim !== null){
			dim.setAggregated(aggregated);
		}
		return result;
	};
	
	this.setMeasureVisibilityAndAggregation = function setMeasureVisibilityAndAggregation(name, visible, aggFun){
		var result = false;
		var measure = this.getColumnByNameAndColumnType(name, StatPortalOpenData.Enums.ColumnType.MEASURE);
		if(measure !== null){
			measure.setVisibleAndAggFun(visible, aggFun);
		}
		return result;
	};
	
	this.setColumnVisibility = function setColumnVisibility(name, visible){
		var result = false;
		var column = this.getColumnByNameAndColumnType(name, StatPortalOpenData.Enums.ColumnType.COLUMN);
		if(column !== null){
			column.setVisible(visible);
		}
		return result;		
	};

	this.getColumnsCount = function getColumnsCount() {
		return _dataInfo.columns.length;
	};

	/*
	this.setColumns = function setColumns(columns) {
		var columnsClone = new Array();
		var columnsLength = columns.length;
		var i, max;
		for( i = 0, max = columnsLength; i < max; i++) {
			columnsClone.push(columns[i]);
		}
		_dataInfo.columns = columnsClone;
	}
	*/

	this.addMeasure = function addMeasure(measure) {
		/*
		// si crea una copia della misura
		var measClone = new StatPortalOpenData.Measure({
			alias : measure.getAlias(),
			name : StatPortalOpenData.ODataUtility.getUniqueIdentifier(measure.getAlias()),
			visible : measure.getVisible(),
			aggFun : measure.getAggFun(),
			id : measure.getId()
		});
		
		_dataInfo.measures.push(measClone);
		*/
		
		_dataInfo.measures.push(measure);
	};

	this.getMeasures = function getMeasures() {
		return _dataInfo.measures;
		/*
		var measuresClone = new Array();
		var measuresLength = _dataInfo.measures.length;
		var i, max;
		for( i = 0, max = measuresLength; i < max; i++) {
			measuresClone.push(_dataInfo.measures[i]);
		}
		return measuresClone;
		*/
	};

	this.getMeasuresCount = function getMeasuresCount() {
		return _dataInfo.measures.length;
	};

	/*
	this.setMeasures = function setMeasures(measures) {
		var measuresClone = new Array();
		var measuresLength = measures.length;
		var i, max;
		for( i = 0, max = measuresLength; i < max; i++) {
			measuresClone.push(measures[i]);
		}
		_dataInfo.measures = measuresClone;
	}
	*/

	this.getJsonMetadata = function getJsonMetadata() {
		return _configuration.getJsonMetadata();
	};
	/**
	 * Restituisce una copia dell'oggetto di configurazione
	 *
	 * @return {Configuration}	Oggetto di configurazione
	 */
	this.getConfiguration = function getConfiguration() {
		return jQuery.extend(true, {}, _configuration);
	};
	/**
	 * Setta una copia dell'oggetto di configurazione
	 *
	 * @param {Configuration}	Oggetto di configurazione
	 */
	this.setConfiguration = function setConfiguration(config) {
		_configuration = jQuery.extend(true, {}, config);
	};
};




/**
 * @namespace StatPortalOpenData
 * @class Configuration
 *
 * @param 	options {Object} parametri di configurazione. Obbligatori: crossDomainProxy, dataType {StatPortalOpenData.Enums.DataType},
 * 		  	serviceUri, resourcePath, includeNavigationProperties, geoserverUrl, layerName, workspaceName, classificationMethod,
 * 			extentMethod, publishMethod, crossDomainProxy, dataType
 *
 */

StatPortalOpenData.Configuration = function Configuration(options) {

	if(!(this instanceof Configuration)) {
		return new Configuration(options);
	}

	var defaults = {
		includeNavigationProperties : false
	};
	options = $.extend(defaults, options);

	// validazione
	if( typeof (options) === 'undefined' || typeof (options.crossDomainProxy) === 'undefined' || typeof (options.dataType) === 'undefined') {
		alert("Inizializzazione dell'oggetto Configuration fallita: sono obbligatori i parametri crossDomainProxy e dataType.");
	} else {
		var _odata, _geodata, _general;
		_odata = {
			serviceUri : options.serviceUri,
			resourcePath : options.resourcePath,
			jsonMetadata : options.jsonMetadata,
			dataId : options.dataId,
			includeNavigationProperties : options.includeNavigationProperties
		};
		_geodata = {
			geoserverUrl : options.geoserverUrl,
			layerName : options.layerName,
			workspaceName : options.workspaceName,
			classificationMethod : options.classificationMethod,
			extentMethod : options.extentMethod,
			publishMethod : options.publishMethod
		};
		_general = {
			crossDomainProxy : options.crossDomainProxy,
			dataType : options.dataType//,StatPortalOpenData.Enums.DataType
		};
	}
	
	this.getDataId = function getDataId(){
		return _odata.dataId;
	};

	this.areIncludedNavigationProperties = function areIncludedNavigationProperties() {
		return (_odata.includeNavigationProperties === true);
	};

	this.getServiceUri = function getServiceUri() {
		return _odata.serviceUri;
	};

	this.getResourcePath = function getResourcePath() {
		return _odata.resourcePath;
	};

	this.getJsonMetadata = function getJsonMetadata() {
		return _odata.jsonMetadata;
	};

	this.getDataType = function getDataType() {
		return _general.dataType;
	};

	this.getCrossDomainProxy = function getCrossDomainProxy() {
		return _general.crossDomainProxy;
	};
};




StatPortalOpenData.Module = function Module(options) {
	if(!(this instanceof Module)) {
		return new Module(options);
	}
	// validazione

	var _id = options.id/*StatPortalOpenData.Enums.ModuleId*/, _enabled = options.enabled, _fullScreenMode = false;
  
  this.getId = function getId() {
		return _id;
	};

  this.isEnabled = function isEnabled() {
		return _enabled;
	};
	
	this.setFullScreenMode = function setFullScreenMode(isFullScreen){
		_fullScreenMode = isFullScreen;
	};
	
	this.isFullScreenMode = function isFullScreenMode(){
		return _fullScreenMode;
	};

};



StatPortalOpenData.Dimension = function Dimension(options) {
	
	if(!(this instanceof StatPortalOpenData.Dimension)) {
		return new StatPortalOpenData.Dimension(options);
	}
	
	var _id = options.id;
	var _alias = options.alias;
	var _name = options.name;
	var _aggregated = options.aggregated;
	var _cardinality = options.cardinality;
	var _dimType = options.dimType;
	var _differentDistinctCount = options.differentDistinctCount;
	
	this.getId = function getId(){
		return _id;
	};
	
	this.getAlias = function getAlias(){
		return _alias;
	};
	
	this.getName = function getName(){
		return _name;
	};
	
	this.getAggregated = function getAggregated(){
		return _aggregated;
	};
	
	this.getVisible = function getVisible(){
		return !_aggregated;
	};
	
	this.getCardinality = function getCardinality(){
		return _cardinality;
	};
	
	this.getDimType = function getDimType(){
		return _dimType;
	};
	
	this.getDifferentDistinctCount = function getDifferentDistinctCount(){
		return _differentDistinctCount;
	};
	
	this.setAggregated = function setAggregated(aggregated){
		_aggregated = aggregated;
	};
};



StatPortalOpenData.Measure = function Measure(options) {
	
	if(!(this instanceof StatPortalOpenData.Measure)) {
		return new StatPortalOpenData.Measure(options);
	}
	
	var _id = options.id;
	var _alias = options.alias;
	var _name = options.name;
	var _visible = options.visible;
	var _aggFun = options.aggFun;
	
	
	this.getId = function getId(){
		return _id;
	};
	
	this.getAlias = function getAlias(){
		return _alias;
	};
	
	this.getName = function getName(){
		return _name;
	};
	
	this.getVisible = function getVisible(){
		return _visible;
	};
	
	this.getAggFun = function getAggFun(){
		return _aggFun;
	};
	
	this.setVisibleAndAggFun = function setVisibleAndAggFun(visible, aggFun){
		_visible = visible;
		_aggFun = aggFun;
	};
};



StatPortalOpenData.Column = function Column(options) {

	if(!(this instanceof StatPortalOpenData.Column)) {
		return new StatPortalOpenData.Column(options);
	}
	
	var _id = options.id;
	var _name = options.name;
	var _alias = options.alias;
	var _visible = options.visible;
	var _differentDistinctCount = options.differentDistinctCount;
	
	this.getId = function getId(){
		return _id;
	};
	
	this.getAlias = function getAlias(){
		return _alias;
	};
	
	this.getName = function getName(){
		return _name;
	};
	
	this.getVisible = function getVisible(){
		return _visible;
	};
	
	this.setVisible = function setVisible(visible){
		_visible = visible;
	};
	
	this.getDifferentDistinctCount = function getDifferentDistinctCount(){
		return _differentDistinctCount;
	};
	
};

/* ENUM che identifica la tipologia di dato (TODO: da integrare con i nuovi id (lu_data_type) */
StatPortalOpenData.Enums.DataType = {
	MD_DATA : 0,
	RAW : 1
};
//if(dataType == StatPortalOpenData.Enums.DataType.MD_DATA) {...}
StatPortalOpenData.Enums.ModuleId = {
	GRID : 0,
	MAP : 1, 
	GRAPH : 2,
	MAP3D : 3
};

StatPortalOpenData.Enums.ColumnType = {
		MEASURE : 0,
		DIMENSION : 1, 
		COLUMN : 2
};

StatPortalOpenData.Enums.MdLuDataType = {
		 TERRITORIALE: 1,
		 TEMPORALE: 2, 
		 GENERICO: 3, 
		 TERRITORIALE_KML: 4
	};

})(jQuery);