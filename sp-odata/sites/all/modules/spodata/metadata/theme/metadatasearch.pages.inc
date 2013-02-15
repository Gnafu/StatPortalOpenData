<?php

/**
 * @file
 * User page callbacks for the search module.
 */

/**
 * Process variables for search-results.tpl.php.
 *
 * The $variables array contains the following arguments:
 * - $results: Search results array.
 * - $module: Module the search results came from (module implementing
 *   hook_search_info()).
 *
 * @see metadata-search-results.tpl.php
 */
function template_preprocess_metadata_search_results(&$variables) {
  $variables['search_results'] = '';
  $def_aut_info = $def_licence = NULL;
  $def_view_count = 0;
  $licenses = metadata_get_licenses();
  $tmpl_variables = array(); 
  if (!empty($variables['module'])) {
    $variables['module'] = check_plain($variables['module']);
  }
  foreach ($variables['results'] as $result) {
  	
  		$tmpl_variables['view_count'] = isset($result['#result']['node']->view_count) ? $result['#result']['node']->view_count : $def_view_count;
  		$tmpl_variables['author_info'] = isset($result['#result']['node']->author) ? $result['#result']['node']->author : $def_aut_info;
  		$tmpl_variables['license_info'] = isset($result['#result']['node']->metadata_license['und']) ? $result['#result']['node']->metadata_license['und'] : $def_licence;
  		$tmpl_variables['fivestar_field'] = field_view_value('node', $result['#result']['node'], 'metadata_fivestars', $result['#result']['node']->metadata_fivestars);
  		$tmpl_variables['category_info'] = isset($result['#result']['node']->metadata_category['und']) ? $result['#result']['node']->metadata_category['und'] : array();
  		$tmpl_variables['type_info'] = $result['#result']['node']->metadata_type['und'];
  		$tmpl_variables['last_update'] = isset($info_split['date']) ? $info_split['date'] : '-';
  		$tmpl_variables['licences'] = $licenses;
  		$tmpl_variables['result'] = $result;
  		  		
  		if(isset($result['#result']['title']))
  			$tmpl_variables['min_title'] = html_entity_decode(truncate_utf8($result['#result']['title'], 90, true, true));
  	
  		
    $variables['search_results'] .= theme('metadata_search_result', $tmpl_variables);
  }
  $variables['pager'] = theme('pager', array('tags' => NULL));
  $variables['theme_hook_suggestions'][] = 'search_results__' . $variables['module'];
  
}

/**
 * Process variables for search-result.tpl.php.
 *
 * The $variables array contains the following arguments:
 * - $result
 * - $module
 *
 * @see search-result.tpl.php
*/
function template_preprocess_metadata_search_result(&$variables) {
  global $language;

  $result = $variables['result']['#result'];
  $variables['url'] = check_url($result['link']);
  $variables['title'] = check_plain($result['title']);
  if (isset($result['language']) && $result['language'] != $language->language && $result['language'] != LANGUAGE_NONE) {
    $variables['title_attributes_array']['xml:lang'] = $result['language'];
    $variables['content_attributes_array']['xml:lang'] = $result['language'];
  }

  $info = array();
  if (!empty($result['module'])) {
    $info['module'] = check_plain($result['module']);
  }
  if (!empty($result['user'])) {
    $info['user'] = $result['user'];
  }
  if (!empty($result['date'])) {
    $info['date'] = format_date($result['date'], 'short');
  }
  if (isset($result['extra']) && is_array($result['extra'])) {
    $info = array_merge($info, $result['extra']);
  }
  // Check for existence. User search does not include snippets.
  $variables['snippet'] = isset($result['snippet']) ? trim($result['snippet']) : '...';
  
  if(empty($variables['snippet']) || strcmp( $variables['snippet'], '...')==0) {
  	// todo sistemare questione della lingua
  	$variables['snippet'] = truncate_utf8(strip_tags($variables['result']['#result']['node']->body['und'][0]['value']), 200, TRUE);
  }
  
  
  // Provide separated and grouped meta information..
  //$variables['info_split'] = $info;
  $variables['last_update'] = isset($info['date']) ? $info['date'] : '-';
  $variables['info'] = implode(' - ', $info);
  $variables['theme_hook_suggestions'][] = 'search_result__' . $variables['module'];
} 
