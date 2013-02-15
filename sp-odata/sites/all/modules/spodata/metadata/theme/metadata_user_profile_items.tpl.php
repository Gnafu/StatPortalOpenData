<?php
if(isset($activity)) : 

	$base_path = drupal_get_path('module', 'metadata');

	drupal_add_js('var StatPortalOpenData = StatPortalOpenData || {}; StatPortalOpenData.Userprofile = ' . 
			json_encode($activity), 'inline');
	drupal_add_library('metadata', 'jqplot');
?>

	<div class="user-activity">
		<div id="created-chart" style="height: 200px; width: 200px; color: #666; display: inline-block;"></div>
		<div id="voted-chart" style="height: 200px; width: 200px; color: #666; display: inline-block;"></div>
		<div id="relevance-chart" style="height: 200px; width: 200px; color: #666; display: inline-block;"></div>
	</div>	
<?php endif; 
