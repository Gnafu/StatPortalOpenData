<?php

if(isset($_GET['id'])) {		
		$id=trim($_GET['id']);
?>

<script type="text/javascript">
	jQuery(document).ready(function() {
		if(!StatPortalOpenData.ODataUtility.loadStateCompleted){
			jQuery('body').bind('loadStateCompleted', function() {
				initChartFromDrupal('<?php echo $id;?>');
				StatPortalOpenData.ODataUtility.removeInitialLoading();
			});
		}else{
			initChartFromDrupal('<?php echo $id;?>');
			StatPortalOpenData.ODataUtility.removeInitialLoading();
		}
	});
</script>
		<div id="chart-viewer" >
			<div style="background:white;">
				<span id="chart-controller">
					<span style="margin-left: 60px;">
						Grafico a  &nbsp;
						<select onChange="StatPortalOpenData.GraphViewer.resetGraphPagination();StatPortalOpenData.GraphViewer.uploadGraph();" id="graphType">
						  <option value="vertical_bar">Barre</option>
						  <option value="line">Curva</option>
						  <!-- <option value="horizontal_bar">Barre orizzontali</option>  -->
						  <option value="pie">Torta</option>
						  <option value="donut">Anello</option>
						  <option value="funnel">Imbuto</option>
						</select>
					</span>
					&nbsp;:&nbsp;
					<span>
						<select onChange="StatPortalOpenData.GraphViewer.graphMeasureChanged();" id="graphMeasures">
						</select>
					</span>
					&nbsp;per&nbsp;
					<span>
						<!-- 
						<select onChange="StatPortalOpenData.GraphViewer.resetGraphPagination();StatPortalOpenData.GraphViewer.uploadFilterKeyController(StatPortalOpenData.GraphViewer.uploadGraph);" id="graphDimensions">
						</select>
						 -->
						<!-- Secondo menu a tendina visibile solo se si seleziona "numero di elementi" -->
						<select class="graphFieldToCount" onChange="StatPortalOpenData.GraphViewer.graphFieldToCountChanged();" id="graphFieldToCount">
						</select>
					</span>
					<span id="graph-dimension-filter">
						(
						<span id="graph-dimensions-to-filter-key-container"></span>
						:
						<span id="graph-dimensions-to-filter-value-container"></span>
						)
					</span>
					<span id="graphViewerResetZoom" style="visibility:hidden;"><a href="javascript:void(0);" onclick="javascript:graphObject.resetZoom();jQuery('#graphViewerResetZoom').css('visibility', 'hidden');"><img src="/sites/all/modules/spodata/metadata/viewer/graph_viewer/img/zoom_minus.png" alt="Annulla Zoom" title="Annulla Zoom" height="16" width="16" /></a></span>
				</span>
				<div style="clear:both;"></div>
				<div id="infoGraphForUser" class="infoForUser"></div>
				<div id="dataChart" style="width:932px;"></div>
				<div id="backNextGraph"><a id="backLinkForGraph" class="backNextLinkDisabled" href="javascript:void(0);">&lt;</a> <span id="infoGraphPagination"></span> <a id="nextLinkForGraph" class="backNextLinkDisabled" href="javascript:void(0);">&gt;</a></div>
				<div id="graph_viewer_loading" class="viewer_loading" style="display:inline;"></div>
			</div>
		</div>
<?php 
}else{
?>
<div id="chart-viewer">
	Visualzzazione non disponibile (non si è passato nessun id).
</div>
<?php
}
?>
