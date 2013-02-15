<?php 
/**
 * @file
 * Module file for mddata catalog menu callback.
 */
 
/**
 * Catalog menu item callback function.
 *
 * @param string $keys user search keys
 * @param string $filter taxonomy+author filters
 * @param int $orderby search results order
 * @param int $page page to show
 * @return string html (themed) of this page region.
 */
function metadata_catalog($keys = '', $filter = '', $orderby = ORDER_BY_RELEVANCE, $page=0){
	global $search_helper;
	// page filter
	if (strpos($page, 'page=')!==false) {
		$page = trim(str_ireplace('page=', '', $page));
		$_REQUEST['page'] = $page-1;
	}
	else if(isset($_REQUEST['page']) && $_REQUEST['page']>0) {
		$_REQUEST['page'] -= 1;
		$page = $_REQUEST['page']; 
	}
	
	try {
		// compute search
		$search_opt = array(MetadataSearchHelper::STRINGFILTER_KEY => $filter, 'current_page' => $page, 'keys' => $keys, 'orderBy' => $orderby, );
		$search_helper = new MetadataSearchHelper($search_opt);
		$nodes = $search_helper->search();
	}
	catch (Exception $ex) {
		// set empty results
		$nodes = array();
		// log error
		watchdog_exception('Metadata - Search', $ex, 'Error executing catalog query.');
	}
	
	$match_count = isset($nodes['match_count']) ? $nodes['match_count'] : 0;
	$page_count = isset($nodes['page_count']) ? $nodes['page_count'] : 0;
	
	unset($nodes['match_count']);
	unset($nodes['page_count']);
	
	$search_helper->setPageHeader($page_count);
	
	// get search result page
	$search_result = metadata_search_page($nodes);
	$search_form = array();
 	// form md search init
	$search_result['#page_count'] = $page_count;
	$search_result['#match_count'] = $match_count;
	$search_result['#page_order_type'] = $orderby;
	$search_result['#current_page'] = $page;
	
	// add filter selection (if exists) on search form
	global $user;
	$admin_menu = NULL;
	if(user_access('administrator')) {
		$admin_menu = array( t('Approva'), t('Invia richiesta correzione'), t('Cancella'));
	}
	
	// catalog page theming
	return theme('catalog', array('rows' => $search_result,
			'searchform' => $search_form,
			'adm_menu' => $admin_menu,
	));
}


/**
 * User catalog menu item callback.
 *
 * @param string $filter (not used)
 * @return string html (themed) of this page region.
 */
function metadata_personal_catalog($filter = NULL) {
	global $user;

	try {
		// get metadata published by current user
		$search_opt = array( 
				'current_page' => 0,
				'current_user'=>$user->uid,
		);
		$helper = new MetadataSearchHelper($search_opt);
		$nodes = $helper->search();
	}
	catch (Exception $ex) {
		// set empty results
		$nodes = array();
		// log error
		watchdog_exception('Metadata -  Search', $ex, 'Error executing personal catalog query.');
	}
	
	
	$search_result = metadata_search_page($nodes);

	// search results theming.
	return theme('catalog', array('rows' => $search_result));
}

/**
 * Metadata catalog management page callback.
 * 
 * @param unknown_type $param
 */
function metadata_catalog_management() {
	$i=0;
	$i++;
	
	// TODO implementare catalogo metadata
	
	// catalog page theming
	return theme('metadata_catalog_management', array( 'results' => array(0,1))); //'ciao ciao.';
}

/**
 * Build catalog filters.
 * Al momento supporta soltanto il parsing del filtro per categoria e autore.
 * 
 * @param string $filter 
 * @return array 
 */
function _catalog_parse_filter($filter) {
	
	$out_filter = array('category' => array(), 'authors' => array(), 'type' => array(), 'datasources' => array(), 'format' => array(), 
				'metadata_filter_administration_tools' => array(), 'title' => 'Catalogo Open Data ', 'keywords' => array());
	
	if(!empty($filter)) {
		// filtro autore
		if(strpos($filter, 'authors=')!==false) {
			$all_author = MetadataHelper::getAuthors();
			$authors_string = trim(str_ireplace('authors=', '', $filter));
			
			$authors = explode('::', $authors_string);
			// set filter array
			foreach ($authors as $author_id) {
				$author_id = trim($author_id);
				$out_filter['authors'][$author_id] = $author_id;
				
				if(isset($all_author[$author_id])) {
					$out_filter['title'] = 'Elenco dati elaborati da ' . $all_author[$author_id];
					$out_filter['keywords'][] = $all_author[$author_id];
				}
			}
		}
		// filtro categoria
		else if (strpos($filter, 'category=')!==false) { 
			$cat_string = trim(str_ireplace('category=', '', $filter));
			
			$categories = explode('::', $cat_string);
			// set filter array
			foreach ($categories as $cat) {
				$cat = trim($cat);
				foreach (taxonomy_get_term_by_name($cat) as $key => $value) {
					$out_filter['category'][$key] = $key;
					$out_filter['title'] = 'Dati ' . $cat;
					if(!in_array($cat, $out_filter['keywords']))
						$out_filter['keywords'][] = $cat;
				}
			}
		}
		// filtro categoria
		else if (strpos($filter, 'term=')!==false) {
			$terms_string = trim(str_ireplace('term=', '', $filter));
			
			$terms = explode('::', $terms_string);
			// set filter array
			foreach ($terms as $term) {
				
				$obj_term = taxonomy_term_load($term);
				$voc = str_replace('metadata_lu_', '', $obj_term->vocabulary_machine_name);
				
				$out_filter[$voc][] = $term;
				
				$out_filter['keywords'][] = $obj_term->name;
				
				switch ($obj_term->vocabulary_machine_name) {
					case 'metadata_lu_datasources':
						$out_filter['title'] = 'Elenco dati della banca dati ' . $obj_term->name;
					break;
					case 'metadata_lu_format':
						$out_filter['title'] = 'Elenco dati con formato ' . $obj_term->name;
						break;
					case 'metadata_lu_license':
						$out_filter['title'] = 'Elenco dati con licenza ' . $obj_term->name;
						break;
					case 'metadata_lu_category':
						$out_filter['title'] = 'Elenco dati sulla tematica ' . $obj_term->name;
						break;
					case 'metadata_lu_type':
						$out_filter['title'] = 'Elenco dati con la parola chiave ' . $obj_term->name;
						break;
					default:
					break;
				}
			}
		}	
	}
	
	return $out_filter;
}
