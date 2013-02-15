<?php

if(isset($_GET['id'])) {		
		$id=trim($_GET['id']);
?>

<script type="text/javascript">
	jQuery(document).ready(function() {
		if(!StatPortalOpenData.ODataUtility.loadStateCompleted){
			jQuery('body').bind('loadStateCompleted', function() {
				initMapFromDrupal('<?php echo $id;?>');
				StatPortalOpenData.ODataUtility.removeInitialLoading();
			});
		}else{
			initMapFromDrupal('<?php echo $id;?>');
			StatPortalOpenData.ODataUtility.removeInitialLoading();
		}
		//StatPortalOpenData.GeoViewer.initDimensionSelector();
	});
	StatPortalOpenData.GeoViewer.dataId = '<?php echo $id;?>';
</script>

<div id="map-viewer-container">
	<div id="map-viewer-toolbar">	 
		<span>
			<select onChange="StatPortalOpenData.GeoViewer.measureChangedHandler(StatPortalOpenData.GeoViewer.dataId);" id="geoviewerMeasures">
			</select>
		</span>
		<span id="map-for">
		&nbsp;per&nbsp;
		</span>
		<span>
			<select onChange="StatPortalOpenData.GeoViewer.territorialDimensionChangedHandler(StatPortalOpenData.GeoViewer.dataId);" id="geoviewerDimensions">
			</select>
		</span>
		<span id="temporalDimensionFilter">
			<span id="temporalDimensionFilterInternal">
				( <span id="temporalDimensionFilterLabel"></span> : 
				<span id="temporalDimensionFilterValue"></span> )
			</span>
		</span>
		<span id="show-map-as-img" class="viewer_img_link_button"><img alt="Vedi come immagine" title="Vedi come immagine" src="/sites/all/modules/spodata/metadata/viewer/commons/img/imageExport.png" onclick="document.getElementById('mapViewer').contentWindow.PrintMap();" /></span>
	</div>
	<div style="clear:both;"></div>
	<div id="infoMapForUser" class="infoForUser" style="display:none;"></div>
	<!-- <select id="geoViewerChange" style="display:none;" onchange="StatPortalOpenData.GeoViewer.mapTypeChange(id_data_da_cancellare);">
		<option selected="selected" value="shape">Puntuale</option>
	</select>
	 -->
	<!-- <div id="toolbarContainerDummy"></div> -->
	<div id="map-viewer">
		<iframe id="mapViewer" style="border: 0px inset;" width="932" height="" src=""></iframe>
		<div id="geo_viewer_loading" class="viewer_loading" style="display:inline;"></div>
	</div>
</div>
<?php 
}else{
?>
	<div id="map-viewer">
		Visualzzazione non disponibile (non si è passato nessun id).
	</div>
<?php
}
?>
