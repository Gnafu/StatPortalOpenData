<?php
/**
 * @file
 * Shortcut metadata block.
 *
 * Available variables:
 * - $var: block settings.
 *
 */

if(isset($var)) {
// get configuration
$conf = variable_get($var,array());
$voc_id = variable_get('metadata_lu_category', -1);
$terms = array();

if($voc_id>0) {
	foreach (taxonomy_get_tree($voc_id) as $value) {
		$terms[$value->tid] = $value->name;
	}
}

if(isset($conf['items_count'])) {
	if($conf['items_count']>0) {
	
	for ($i = 0; $i < $conf['items_count']; $i++) {
		
		$cat = $conf['items'][$i]['category'];
		$label = $conf['items'][$i]['label'];
		$img_id = $conf['items'][$i]['image'];
		$img_obj = file_load($img_id);
		$img_path = file_create_url($img_obj->uri);
		global $language;
        $url = url(drupal_get_path_alias("catalog//term=$cat"), array('absolute' => TRUE));
		
		
		
		echo "<a href='$url'>
		<div class='category-link' style='background-image: url($img_path)'>		
		<div class='label'>$label</div>
		</div></a>";
		
	}
	}
	else {
		global $user;
		if( in_array('administrator', $user->roles) )
			print t('Configurare i link al catalogo o disabilita il blocco');
		
	}
}
}