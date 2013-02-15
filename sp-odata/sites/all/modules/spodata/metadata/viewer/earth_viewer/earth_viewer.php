<?php

if(isset($_GET['id'])) {		
		$id=trim($_GET['id']);
?>

<script type="text/javascript">
	jQuery(document).ready(function() {StatPortalOpenData.EarthViewer.initEarthFromDrupal('<?php echo $id;?>');});
</script>

<div>
<!-- 
<img src="https://www.google.it/logos/2012/Howard_Carter-2012-res.png" alt="" height="32" width="32" />
<img src="https://www.google.it/logos/2012/Howard_Carter-2012-res.png" alt="" height="32" width="32" />
<img src="https://www.google.it/logos/2012/Howard_Carter-2012-res.png" alt="" height="32" width="32" />
<img src="https://www.google.it/logos/2012/Howard_Carter-2012-res.png" alt="" height="32" width="32" />
 -->
</div>

<div id="earth-viewer-container">
	<div id="toolbarHearthContainerDummy">
	</div>
	<div id="map3d-viewer" style="width: 930px;">
		<div id="map3d" style="width: 930px;">
		</div>
		<div id="earth_viewer_loading" class="viewer_loading" style="display:inline;"></div>
	</div>
</div>
<?php 
}else{
?>
<div id="map3d-viewer">
	Visualzzazione non disponibile (non si è passato nessun id).
</div>
<?php
}
?>