<?php

if(isset($_GET['id'])) {		
		$id=trim($_GET['id']);
?>
 
<script type="text/javascript">
	jQuery(document).ready(function() {
		if(!StatPortalOpenData.ODataUtility.loadStateCompleted){
			jQuery('body').bind('loadStateCompleted', function() {
				initGridFromDrupal('<?php echo $id;?>');
				StatPortalOpenData.ODataUtility.removeInitialLoading();
			});
		}else{
			initGridFromDrupal('<?php echo $id;?>');
			StatPortalOpenData.ODataUtility.removeInitialLoading();
		}
	});
</script>
 
<!-- <link rel="stylesheet" type="text/css" href="/sites/all/modules/spodata/metadata/viewer/commons/jquery_ui/css/custom-theme/gray/jquery-ui-1.8.18.custom.css" /> -->

<div id="table-viewer">
	<div id="controllerContainer">
	<span id="controller"></span> <span id="applyFieldChange"
			class="button"
			onclick="StatPortalOpenData.ODataUtility.applyControllerChange();">Applica</span>
	</div>
	<br /> <br />
	<div id="ODataGridContainer"
		class="odataGridContainer">
		<table id="list"></table>
		<div id="pscrolling"></div>
	</div>
	<div id="obscure"></div>
	<div id="multidimensional_viewer_loading" class="viewer_loading" style="display:inline;"></div>
</div>
<?php 
}else{
?>
<div id="table-viewer">
	Visualzzazione non disponibile (non si è passato nessun id).
</div>
<?php
}
?>
