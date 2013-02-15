StatPortalOpenData.MultidimensionalViewer = StatPortalOpenData.MultidimensionalViewer || {};

var MIN_TABLE_HEIGHT = 200;
function initGridFromDrupal(id) {
	
	try{
		var dimensionsPATCH = state.getDimensions();
		var measuresPATCH = state.getMeasures();
		var columnsPATCH = state.getColumns();
		var i = 0;
		for( i = 0; i < dimensionsPATCH.length; i++) {
			dimensionsPATCH[i].setAggregated(false);
		}
		for( i = 0; i < measuresPATCH.length; i++) {
			measuresPATCH[i].setVisibleAndAggFun(true, 'SUM');
		}
		for( i = 0; i < columnsPATCH.length; i++) {
			columnsPATCH[i].setVisible(true);
		}
	}catch(Exception){
		// non fare niente. PATCH FALLITA
	}
	
	state.setNewAndOnlyModuleActive(StatPortalOpenData.Enums.ModuleId.GRID);
	StatPortalOpenData.ODataUtility.initViews();
	
	jQuery(window).unbind('resize', StatPortalOpenData.MultidimensionalViewer.windowResizeHandlerForMultidimensionalGrid);
	jQuery(window).bind('resize', StatPortalOpenData.MultidimensionalViewer.windowResizeHandlerForMultidimensionalGrid);
}


StatPortalOpenData.MultidimensionalViewer.windowResizeHandlerForMultidimensionalGrid = function windowResizeHandlerForMultidimensionalGrid(){
	var newHeight = Math.max(MIN_TABLE_HEIGHT, jQuery(window).height() - StatPortalOpenData.utility.getResidualTableHeight());
	if (jQuery("#list").height() !== newHeight) {
		jQuery("#list").setGridHeight(newHeight);
	}
};

StatPortalOpenData.MultidimensionalViewer.windowResizeHandlerForMultidimensionalGridFullScreen = function windowResizeHandlerForMultidimensionalGridFullScreen(){
	var fullDimensions = StatPortalOpenData.MultidimensionalViewer.getDimensionsForFullScreen();
	jQuery("#list").setGridHeight(fullDimensions.height);
	jQuery("#list").setGridWidth(fullDimensions.width);
};


var oldMultidimensionalGridWidth;

/**
 * Ottimizza le dimensioni della griglia per permettere una visualizzazione ottimale a schermo intero
 */
StatPortalOpenData.MultidimensionalViewer.toFullScreen = function toFullScreen(){

	var fullDimensions = StatPortalOpenData.MultidimensionalViewer.getDimensionsForFullScreen();
	// colonne selezionate
	try{
		var selectedColumns = jQuery('.ui-multiselect-checkboxes').find('input').filter('[checked]').filter(':enabled').length;
		var numColumnsForShrink = Math.floor(fullDimensions.width/150);
		if(selectedColumns < numColumnsForShrink){
			jQuery('#list').setGridParam({shrinkToFit:true});
		}
			
	}catch(Exception){
		StatPortalOpenData.utility.log('ERROR: impossibile recuperare il numero di colonne checkate (per il fullscreen)');
	}
	jQuery(window).unbind('resize', StatPortalOpenData.MultidimensionalViewer.windowResizeHandlerForMultidimensionalGrid);
	jQuery(window).bind('resize', StatPortalOpenData.MultidimensionalViewer.windowResizeHandlerForMultidimensionalGridFullScreen);
	
	jQuery('#table-viewer').addClass('multidimensionalFullScreen');
	jQuery('#controllerContainer').addClass('controllerContainerFullScreen');
	//oldMultidimensionalGridHeight = jQuery('.ui-jqgrid-bdiv').height();
	oldMultidimensionalGridWidth = jQuery('#ODataGridContainer').width();
	jQuery('#table-viewer').append('<img id="closeFullscreen" class="closeFullscreen" onclick="javascript:StatPortalOpenData.ODataUtility.toNormalSize();" src="/sites/all/modules/spodata/metadata/viewer/commons/img/close.png" />');
	
	jQuery("#list").setGridHeight(fullDimensions.height);
	jQuery("#list").setGridWidth(fullDimensions.width);
	state.setFullScreenMode(StatPortalOpenData.Enums.ModuleId.GRID, true);
};

/**
 * Restituisce le dimensioni disponibili per una visualizzazione della griglia a tutto schermo
 * @returns {___anonymous3083_3167}
 */
StatPortalOpenData.MultidimensionalViewer.getDimensionsForFullScreen = function getDimensionsForFullScreen(){
	return {
		height: jQuery(window).height() - 120,
		width:jQuery(window).width() - 10 
	};
};

/**
 * Funzione chiude il fullscreen nella griglia
 */
StatPortalOpenData.MultidimensionalViewer.toNormalSize = function toNormalSize(){	
	state.setFullScreenMode(StatPortalOpenData.Enums.ModuleId.GRID, false);
	jQuery('#table-viewer').removeClass('multidimensionalFullScreen');
	jQuery('#controllerContainer').removeClass('controllerContainerFullScreen');
  	StatPortalOpenData.MultidimensionalViewer.windowResizeHandlerForMultidimensionalGrid();
	jQuery("#list").setGridWidth(oldMultidimensionalGridWidth);
	jQuery(window).unbind('resize', StatPortalOpenData.MultidimensionalViewer.windowResizeHandlerForMultidimensionalGridFullScreen);
	jQuery(window).bind('resize', StatPortalOpenData.MultidimensionalViewer.windowResizeHandlerForMultidimensionalGrid);
};





