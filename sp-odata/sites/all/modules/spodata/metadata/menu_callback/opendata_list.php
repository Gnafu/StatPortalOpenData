<?php

/**
 * Menu item callback
 * Prepare data to renderer and call content theme.
 */
function metadata_opendata_list() {
	$cond = db_or()->condition('source', 'catalog//term=%', 'like')
				->condition('source', 'catalog//authors=%', 'like');
	
	$query_res = db_select('url_alias', 'A')
		->fields('A')
		->condition($cond)
		->execute();
	
	$query_taxonomy = db_select('taxonomy_index', 'TI');
	$query_taxonomy->innerJoin('metadata', 'M', 'TI.nid=M.nid and M.visibility=1 and M.status=1');
	$query_taxonomy = $query_taxonomy->fields('TI', array('tid'))
		->distinct(true)
		->execute();

	$taxonomy = $query_taxonomy->fetchCol();
	
	$query_authors = db_select('metadata', 'M')
		->fields('M', array('id_author'))
		->condition('visibility', 1, '=')
		->condition('status', 1, '=')
		->distinct(true)
		->execute();
	
	$authors = $query_authors->fetchCol();
		
// 	while($row=$query_res->fetch()) {
		
// 	}
	
	
	return theme('metadata_odata_list', array( 
			'rows'=>$query_res,
			'active_taxonomies'=> $taxonomy, 
			'active_authors'=> $authors));
}