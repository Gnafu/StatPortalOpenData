<?php
/**
 * @file Metadata module include file.
 * Implements Metadata blocks.
 * 
 */

/**
 * hook_block_info implementation.
 *
 * @return An associative array whose keys define the delta for each block and
 * 			whose values contain the block descriptions.
 *
 *  @see hook_block_info
 */
function metadata_block_info()
{
	$blocks['catalog_filter_box'] = array(
			'info' => t('Metadata search'),
			'cache' => DRUPAL_CACHE_PER_USER,
			'status' => TRUE,
			'region' => 'sidebar_first',
			'visibility' => 1,
			'visibility' => BLOCK_VISIBILITY_LISTED,
			'pages' => 'catalog\ncatalog/*',
	);

	$blocks['metadata_statistics'] = array(
			'info' => t('Metadata statistics'),
			'cache' => DRUPAL_CACHE_PER_USER,
			'status' => TRUE,
			'region' => 'home_bottom_right',
			'visibility' => 1,
			'visibility' => BLOCK_VISIBILITY_LISTED,
			'pages' => '<front>',
	);

	$blocks['metadata_catalog_shortcut'] = array(
			'info' => t('Catalog shortcut'),
			'cache' => DRUPAL_CACHE_PER_USER,
			'status' => TRUE,
			'region' => 'home_top_right',
			'visibility' => 1,
			'visibility' => BLOCK_VISIBILITY_LISTED,
			'pages' => '<front>',
	);
	
	$blocks['powered_by'] = array(
			'info' => t('Powered by'),
			'weight' => '10',
			'cache' => DRUPAL_NO_CACHE,
			'region' => 'footer',
			'status' => true,
	);
	
	$blocks['search_data'] = array(
			'info' => t('Cerca nei dati'),
			'weight' => '10',
			'cache' => DRUPAL_NO_CACHE,
			'region' => 'header',
			'status' => true,
	);
	
	$blocks['metadata_news_scroller'] = array(
			'info' => t('Front news'),
			'cache' => DRUPAL_CACHE_GLOBAL,
			'status' => TRUE,
			'region' => 'home_news_barousel',
			'visibility' => 1,
			'visibility' => BLOCK_VISIBILITY_LISTED,
			'pages' => '<front>',
	);
	
	return $blocks;
}

/**
 * Implements hook_block_configure.
 * Show block configuration options.
 *
 * @param array $delta Which block is being configured.
 * 				This is a unique identifier for the block within the module.
 * @return A configuration form, if one is needed for your block beyond the
 * 			standard elements that the block module provides (block title, visibility, etc.).
 *
 *  @see hook_block_configure
 */
function metadata_block_configure($delta) 	{

	$form = array();
	$module_path = drupal_get_path('module', 'metadata');
	if($delta==='catalog_filter_box') {

		$vocabularies = array();
		foreach (taxonomy_get_vocabularies() as $key => $value) {
			if ( strcmp($value->module, MODULE)==0)
				$vocabularies[$value->vid] = t($value->name . ' filter');
		}

		// se non è mai stato configurato imposta la configurazione di default.
		$default = variable_get('catalog_filter_box_configuration', -1);
		if($default===-1 || empty($default)) {
			$default = array (
					variable_get('metadata_lu_category', -1),
					variable_get('metadata_lu_format', -1),
					'author',
			);
		}
		else
		{
			$default = explode(';', $default);
		}

		$vocabularies['author'] = t('Author filter');

		$form['catalog_filter_box_config'] = array(
				'#type' => 'checkboxes',
				'#title' => t('Showed Taxonomies of catalog filter box'),
				'#description' => t('Choose your filter options visibility.'),
				'#options' => $vocabularies,
				'#default_value' => $default,
				'#weight' => 20,
		);
	}
	else if ($delta==='powered_by') {
		// default power-by configuration
		$default_powby_config = array( 'Copiright' => array('label' => t('&copy; Copyright 2012, Sistemi Territoriali S.r.l.'), 'link' => 'http://www.sister.it'),
				'Credits'  => array('label' => t('StatPortal OpenData'), 'link' => 'http://www.sister.it'),
				'Disclaimer' => array('label' => t('Condizioni d\'uso'), 'link' => 'http://www.sister.it'),
				'Privacy' => array('label' => t('Privacy'), 'link' => 'http://www.sister.it'),
				);
		
		$powby_config = variable_get('metadata_footer_config', $default_powby_config);
		
		$form['pow_by']['copiright'] = array(
				'#type' => 'fieldset',
				'#title' => t('Copiright'),
				'#description' => t('Copiright configuration.'),
				'#collapsible' => true,
		);
		
		$form['pow_by']['copiright']['label_copy'] = array(
				'#type' => 'textfield',
				'#title' => t('Label'),
				'#default_value' => $powby_config['Copiright']['label'],
				'#element_validate' => array('_metadata_label_validation'),
		);
		
		$form['pow_by']['copiright']['link_copy'] = array(
				'#type' => 'textfield',
				'#title' => t('Link'),
				'#default_value' => $powby_config['Copiright']['link'],
				'#element_validate' => array('_metadata_link_validation'),
		);
		
		
		$form['pow_by']['credits'] = array(
				'#type' => 'fieldset',
				'#title' => t('Credits'),
				'#description' => t('Credits configuration.'),
				'#collapsible' => true,
				'#collapsed' => true,
		);
		
		$form['pow_by']['credits']['label_credit'] = array(
				'#type' => 'textfield',
				'#title' => t('Label'),
				'#default_value' => $powby_config['Credits']['label'],
				'#element_validate' => array('_metadata_label_validation'),
		);
		
		$form['pow_by']['credits']['link_credit'] = array(
				'#type' => 'textfield',
				'#title' => t('Link'),
				'#default_value' => $powby_config['Credits']['link'],
				'#element_validate' => array('_metadata_link_validation'),
		);
		
		$form['pow_by']['disclaimer'] = array(
				'#type' => 'fieldset',
				'#title' => t('Disclaimer'),
				'#description' => t('Disclaimer configuration.'),
				'#collapsible' => true,
				'#collapsed' => true,
		);
		
		$form['pow_by']['disclaimer']['label_disclaimer'] = array(
				'#type' => 'textfield',
				'#title' => t('Label'),
				'#default_value' => $powby_config['Disclaimer']['label'],
				'#element_validate' => array('_metadata_label_validation'),
		);
		
		$form['pow_by']['disclaimer']['link_disclaimer'] = array(
				'#type' => 'textfield',
				'#title' => t('Link'),
				'#default_value' => $powby_config['Disclaimer']['link'],
				'#element_validate' => array('_metadata_link_validation'),
		);
		
		$form['pow_by']['privacy'] = array(
				'#type' => 'fieldset',
				'#title' => t('Privacy'),
				'#description' => t('Privacy configuration.'),
				'#collapsible' => true,
				'#collapsed' => true,
		);
		
		$form['pow_by']['privacy']['label_privacy'] = array(
				'#type' => 'textfield',
				'#title' => t('Label'),
				'#default_value' => $powby_config['Privacy']['label'],
				'#element_validate' => array('_metadata_label_validation'),
		);
		
		$form['pow_by']['privacy']['link_privacy'] = array(
				'#type' => 'textfield',
				'#title' => t('Link'),
				'#default_value' => $powby_config['Privacy']['link'],
				'#element_validate' => array('_metadata_link_validation'),
		);
	}
	else if ($delta==='metadata_statistics') {

		$form['metadata_stat'] = array(
				'#type' => 'fieldset',
				'#title' => t('Numero elementi visibili'),
				'#description' => t('Indica il numero massimo di elementi visibili per ciascuna sezione del blocco.'),
				'#collapsible' => true,
		);

		$last_update_count = variable_get('metadatastat_last_update_count', 5);
		$form['metadata_stat']['last_update'] = array(
				'#type' => 'textfield',
				'#title' => t('Ultimi aggiornamenti'),
				'#default_value' => $last_update_count,
				'#element_validate' => array('_stat_block_save_validation'),
		);

		$most_active_count = variable_get('metadatastat_most_active_count', 5);
		$form['metadata_stat']['most_active'] = array(
				'#type' => 'textfield',
				'#title' => t('Autori pi&ugrave; attivi'),
				'#default_value' => $most_active_count,
				'#element_validate' => array('_stat_block_save_validation'),
		);

		$most_voted_count = variable_get('metadatastat_most_voted_count', 5);
		$form['metadata_stat']['most_voted'] = array(
				'#type' => 'textfield',
				'#title' => t('Autori pi&ugrave; votati'),
				'#default_value' => $most_voted_count,
				'#element_validate' => array('_stat_block_save_validation'),
		);

		$most_viewed_count = variable_get('metadatastat_most_viewed_count', 5);
		$form['metadata_stat']['most_viewed'] = array(
				'#type' => 'textfield',
				'#title' => t('Schede pi&ugrave; visualizzate'),
				'#default_value' => $most_viewed_count,
				'#element_validate' => array('_stat_block_save_validation'),
		);

		$most_searched_count = variable_get('metadatastat_most_searched_count', 5);
		$form['metadata_stat']['most_searched'] = array(
				'#type' => 'textfield',
				'#title' => t('Schede pi&ugrave; cercate'),
				'#default_value' => $most_searched_count,
				'#element_validate' => array('_stat_block_save_validation'),
		);

		$most_downloaded_count = variable_get('metadatastat_most_downloaded_count', 5);
		$form['metadata_stat']['most_downloaded'] = array(
				'#type' => 'textfield',
				'#title' => t('Schede pi&ugrave; scaricate'),
				'#default_value' => $most_downloaded_count,
				'#element_validate' => array('_stat_block_save_validation'),
		);

		$show_metadatastat_stat = variable_get('metadatastat_stat_visibility', 1);
		$form['metadata_stat']['stat_visibility'] = array(
				'#type' => 'radios',
				'#title' => t('Visualizza statistiche'),
				'#description' => t('Scegli se rendere visibile o meno le statistiche.'),
				'#options' => array( '0' => t('No'), '1' => t('Si') ),
				'#default_value' => $show_metadatastat_stat,
				'#weight' => 18,
		);
		$stat_title = variable_get('metadatastat_title', t('Statistiche'));
		$form['metadata_stat']['stat_title'] = array(
				'#type' => 'textfield',
				'#description' => t('Specifica un titolo da associare alla sezione delle statistiche.'),
				'#title' => t('Titolo sezione statistiche sito'),
				'#default_value' => $stat_title,
				'#weight' => 20,
				'#prefix' => '<div id="show-title" ' . ($show_metadatastat_stat==0 ? 'style="display: none">' : '>'),
				'#suffix' => '</div>',
				'#attached' => array (
						'js' => array("jQuery(document).ready(function() { 
										if(jQuery('input[name=\"stat_visibility\"]:checked').val()==0) {
											jQuery('#show-title').css('display', 'none');
										}
										jQuery('input[name=\"stat_visibility\"]').change(function() {
										 jQuery('#show-title').toggle();});
									});"
									=> array('type' => 'inline', 'weight' => 20)), 
						),
		);
		
			
		
	}
	else if ($delta==='metadata_catalog_shortcut') {
		
		$config = variable_get('metadata_shortcut_block_config', array('items_count'=>0));
		
		$form['max_link'] = array(
				'#type' => 'radios',
				'#title' => t('Numero collegamenti'),
				'#default_value' => $config['items_count'],
				'#options' => array(4 => t('4'), 8 => t('8')),
				'#description' => t('Seleziona il numero di elementi da visualizzare'),
				'#element_validate' => array('shortcut_block_config_validation'),

		);
		
		$form['shortcut_block'] = array(
				'#type' => 'fieldset',
				'#title' => t('Configurazione'),
				'#collapsible' => TRUE,
				'#collapsed' => TRUE,
				'#attached' => array(
						'css' => array(
								$module_path . '/css/block_config.css',
						),
						'js' => array(
								$module_path . '/js/block_config.js',
						),
				),
		);
		
		
		$voc_id = variable_get('metadata_lu_category', -1);
		$terms = array();
		
		if($voc_id>0) {
			foreach (taxonomy_get_tree($voc_id) as $value) {
				$terms[$value->tid] = $value->name;
			}
		}
		
		$default_cat = -1;
		$default_label = '';
		$default_image = '';
		for($i=0;$i<8;$i++) {
			
			if($config['items_count'] > $i) {
				$default_cat = isset($config['items'][$i]['category']) ? $config['items'][$i]['category'] : -1;
				$default_label = isset($config['items'][$i]['label']) ? $config['items'][$i]['label'] : '';
				$default_image = isset($config['items'][$i]['image']) ? $config['items'][$i]['image'] : '';
			}
			
			
			$form['shortcut_block']["item-$i"] = array(
				'#type' => 'container',
				'#weight' => $i,
				'#attributes' => array( 'class' => array('config-item')),
			);
	
			$form['shortcut_block']["item-$i"]["category-$i"] = array(
				'#type' => 'select',
				'#title' => t('Seleziona la categoria'),
				'#options' => $terms,
				'#default_value' => $default_cat,
			);
			
			$form['shortcut_block']["item-$i"]["link_label-$i"] = array(
					'#type' => 'textfield',
					'#title' => t('Inserisci un\'etichetta'),
					'#default_value' => $default_label,
			);
			
			$form['shortcut_block']["item-$i"]["image-$i"] = array(
					'#type' => 'managed_file',
					'#title' => t('Seleziona un\'immagine'),
					'#default_value' => $default_image,
					'#upload_location' => 'public://gallery_images/',
					'#upload_validators' => array(
							'file_validate_extensions' => array('gif png jpg jpeg'),
							// Pass the maximum file size in bytes
							'file_validate_size' => array(10*1024*1024),
					),
			);
			
			
		}
	}
	else if($delta='home_news_barousel') {
		
		$config = variable_get('metadata_fron_news_block_config', array('items_count'=>7, 'items' => array()));
		
		$form['max_item'] = array(
					'#type' => 'textfield',
					'#title' => t('Numero di news'),
					'#description' => t('Inserisci un numero da 0 a 20'),
					'#default_value' => $config['items_count'],
					'#size' => 2,
					'#maxlength' => 2,
				);
		
		$form['items'] = array(
					'#type' => 'fieldset',
					'#title' => t('News item'),
					'#collapsible' => TRUE,
					'#collapsed' => TRUE,
					'#attached' => array(
							'css' => array(
									$module_path . '/css/block_news_config.css',
							),
							'js' => array(
									$module_path . '/js/block_config.js',
							),
					),
				);
		
		for ($i = 0; $i < 20; $i++) {
					
			$form['items']["item-$i"] = array(
					'#type' => 'container',
					'#weight' => $i,
					'#attributes' => array( 'class' => array('config-item')),
			);
			
			if($i>=$config['items_count'])
				$form['items']["item-$i"]['#attributes']['class'][] = 'invisible';
			
			$news_idx = $i+1;
			
			$form['items']["item-$i"]["Title-$i"] = array(
					'#type' => 'textfield',
					'#title' => t("$news_idx. Imposta un titolo per la news "),
					'#default_value' => isset($config['items'][$i]) ? $config['items'][$i]['title'] : '',
			);
			
			$form['items']["item-$i"]["news-$i"] = array(
					'#type' => 'text_format',
					'#title' => t('Inserisci il contenuto della news'),
					'#default_value' => isset($config['items'][$i]) ? $config['items'][$i]['news'] : '',
					'#format' => filter_default_format(),
			);
			
			
			$form['items']["item-$i"]["image-$i"] = array(
					'#type' => 'managed_file',
					'#title' => t('Seleziona l\'immagine di sfondo'),
					'#default_value' => isset($config['items'][$i]) ? $config['items'][$i]['image'] : '',
					'#upload_location' => 'public://metadata_images/',
					'#upload_validators' => array(
							'file_validate_extensions' => array('gif png jpg jpeg'),
							// Pass the maximum file size in bytes
							'file_validate_size' => array(10*1024*1024),
					),
			);
		}
	}

	return $form;
}


/**
 * hook_block_save impplementation.
 * Save block configurations.
 *
 * @param string $delta Which block is being configured. This is a unique identifier
 * 			for the block within the module, defined in hook_block_info().
 * @param array $edit The submitted form data from the configuration form.
 */
function metadata_block_save($delta='', $edit=array()) {
	
	switch ($delta) {
		case 'catalog_filter_box':
			$active_filters = '';
			foreach ($edit['catalog_filter_box_config'] as $key => $value) {
				if($value!==0 || strcmp($value, 'author')===0)
					$active_filters .= $key . ';';
			}

			variable_set('catalog_filter_box_configuration', $active_filters);
			break;
		case 'metadata_statistics':
			// save new config
			variable_set('metadatastat_most_downloaded_count', $edit['most_downloaded']);
			variable_set('metadatastat_most_searched_count', $edit['most_searched']);
			variable_set('metadatastat_most_voted_count', $edit['most_voted']);
			variable_set('metadatastat_most_active_count', $edit['most_active']);
			variable_set('metadatastat_last_update_count', $edit['last_update']);
			variable_set('metadatastat_most_viewed_count', $edit['most_viewed']);
			variable_set('metadatastat_stat_visibility', $edit['stat_visibility']);
			variable_set('metadatastat_stat_title', $edit['title']);
			variable_set('metadatastat_title', $edit['stat_title']);
			break;
		case 'metadata_catalog_shortcut':
			$new_config = array( 'items_count' => $edit['max_link'], 'items' => array());
			for($i=0;$i<$edit['max_link']; $i++) {
				
				$new_config['items'][$i]['category'] = $edit["category-$i"];
				$new_config['items'][$i]['label'] = $edit["link_label-$i"];
				$new_config['items'][$i]['image'] = $edit["image-$i"];
				
			}
			// serialize and save on db configuration 
			variable_set('metadata_shortcut_block_config', $new_config);
			break;
		case 'powered_by' :
			// default power-by configuration
			$powby_config = array( 
					'Copiright' => array('label' => $edit['label_copy'], 'link' => $edit['link_copy']),
					'Credits'  => array('label' => $edit['label_credit'], 'link' => $edit['link_credit']),
					'Disclaimer' => array('label' => $edit['label_disclaimer'], 'link' => $edit['link_disclaimer']),
					'Privacy' =>  array('label' => $edit['label_privacy'], 'link' => $edit['link_privacy']));
			
			
			
			$config = variable_set('metadata_footer_config', $powby_config);
			break;
		case 'metadata_news_scroller':
			if(isset($edit['max_item']) && is_numeric($edit['max_item'])) {
			
				$config = array('items_count'=> $edit['max_item'], 'items' => array()); 
				global $theme;
				$block = db_query('SELECT bid FROM {block} WHERE module = :module AND delta = :delta AND theme = :theme', 
						array(':module' => 'metadata', ':delta' => 'metadata_news_scroller', 'theme' => $theme))
						->fetchObject();
				
				
				for($i=0; $i<$edit['max_item']; $i++) {
					
					// Load the file via file.fid.
					$file = file_load($edit["image-$i"]);
					if($file && $file->status != FILE_STATUS_PERMANENT) {
						// Change status to permanent.
						$file->status = FILE_STATUS_PERMANENT;
						// Save.
						file_save($file);
						// Record that the module (in this example, user module) is using the file.
						file_usage_add($file, 'metadata', 'block', $block->bid);
					}
					
					$config['items'][] = array(
						'title' => $edit["Title-$i"],
						'news' => $edit["news-$i"]['value'],
						'image' => $edit["image-$i"], 
					);
				}
				
				variable_set('metadata_fron_news_block_config', $config);
			}
			break;
	}
}

/**
 * hook_block_view implementation.
 *
 * @param string $delta Which block to render. This is a unique identifier
 * 		 for the block within the module, defined in hook_block_info().
 * @return array An array containing subject and content.
 */
function metadata_block_view($delta='') {
	$block = array();
	
	switch ($delta) {
		case 'catalog_filter_box':
			// form md search init
			$form_state['build_info']['args'] = array( 'module' => MODULE . '_catalog');
			$search_form = drupal_build_form('search_form', $form_state);
			$block['subject'] = t('Cerca dati');
			$block['content'] = render($search_form);
			break;
		case 'metadata_statistics':
			// get config
			$most_downloaded_count = variable_get('metadatastat_most_downloaded_count', 5);
			$most_searched_count = variable_get('metadatastat_most_searched_count', 5);
			$most_voted_count = variable_get('metadatastat_most_voted_count', 5);
			$most_active_count = variable_get('metadatastat_most_active_count', 5);
			$last_update_count = variable_get('metadatastat_last_update_count', 5);
			$most_viewed_count = variable_get('metadatastat_most_viewed_count', 5);
			$stat_visibility = variable_get('metadatastat_stat_visibility', 1);
			$stat_title = variable_get('metadatastat_title', t('Statistiche'));
			// get items
			$last_update = MetadataStatisticsHelper::getLastUpdatedMetadata($last_update_count);
			$most_active = MetadataStatisticsHelper::getMostActiveAuthors($most_active_count);
			$most_voted = MetadataStatisticsHelper::getMostVotedAuthors($most_voted_count);
			$most_viewed = MetadataStatisticsHelper::getMostViewedMetadata($most_viewed_count);
			$most_downloaded = MetadataStatisticsHelper::getMostDownloadedMetadata($most_downloaded_count);
			$most_searched = MetadataStatisticsHelper::getMostSearchedMetadata($most_searched_count);
			if($stat_visibility>0)
				$evt_stat = MetadataStatisticsHelper::getMainStatistics();
			else
				$evt_stat = NULL;

			// write block. $block['subject'] = t('Statistics');
			$block['content'] = theme('metadata_statistics_block', array( 'last_update' => $last_update, 'most_active' => $most_active,
					'most_voted' => $most_voted, 'most_viewed' => $most_viewed,
					'most_searched' => $most_searched,	'most_downloaded' => $most_downloaded,
					'events_stat' => $evt_stat, 'custom_title' => $stat_title )); //theme_metadata_statistics();
			break;
		case 'metadata_catalog_shortcut':
			$block['content'] = theme('metadata_catalog_shortcut_block', array( 'var' => 'metadata_shortcut_block_config'));
			break;
		case 'search_data':
			$url = url('catalog/');
			$block['content'] ="<div id='search-bar'>
								<form action='$url' method='get'>
									<label id='main-search-label'></label>
									<input id='main-search-bar' name='keys' type='text' value='" . t("Inserisci il testo per la ricerca") . "'/>
									<input id='main-search-button' type='submit' value='" . t("Cerca dati") . "' />
								</form>
								</div>";			
			break;
		case 'powered_by':
			// default power-by configuration
			$default_powby_config = array( 'Copiright' => array('label' => '&copy; Copyright 2012, Sistemi Territoriali S.r.l.', 'link' => 'http://www.sister.it'),
					'Credits'  => array('label' => 'StatPortal OpenData', 'link' => 'http://www.sister.it'),
					'Disclaimer' => array('label' => 'Condizioni d\'uso', 'link' => 'http://www.sister.it'),
					'Privacy' => array('label' => 'Privacy', 'link' => 'http://www.sister.it'), );
			
			$config = variable_get('metadata_footer_config', $default_powby_config);
			$block['content'] = theme('metadata_footer_block', array( 'config' => $config ));
			
			break;
		case 'metadata_news_scroller':
			$default_news_block_config = array('items_count'=>0, 'items' => array());
			$config = variable_get('metadata_fron_news_block_config', $default_news_block_config);
			$block['content'] = theme('metadata_news_scroller_block', array('config' => $config));
			
			break;
	}

	return $block;
}

/**
 * Form validation function. Check if user input is a numeric value.
 *
 * @param unknown_type $element
 */
function _stat_block_save_validation($element) {
	$last_update = $element['#value'];

	if(!is_numeric($last_update) || $last_update<0) {
		form_set_error('lat_update', $element['#title'] . t(': valore non valido, inserisci un valore numerico maggiore o uguale a 0'));
		$result = false;
	}
}

/**
 * Form label validation function. Check if user input is a valid label.
 *
 * @param array $element.
 */
function _metadata_link_validation($element) {

	$value = $element['#value'];
	
	if(!valid_url($value, true))
		form_set_error('powby', t('Il link non &egrave; nel formato corretto.'));
	else
		$element['#value'] = $value;
}

/**
 * Form link validation function. Check if user input is a valid link.
 * @param array $element
 */
function _metadata_label_validation($element) {
	$value = $element['#value'];
	
	$value = check_plain($value);
	if(empty($value))
		form_set_error('powby', t('L\'Etichetta non pu&ograve; essere vuota'));
	else 
		$element['#value'] = $value;
}

/**
 * Shortcut block config validation.
 * 
 * @param array $form form array 
 * @param array $form_state form state array
 */
function shortcut_block_config_validation($element, &$form_state, $form) {
	$link_count = $form_state['values']['max_link'];
	
	for($i=0;$i<$link_count; $i++) {
		$selected_cat = $form_state['values']["category-$i"];
		$label = $form_state['values']["link_label-$i"];
		$image = $form_state['values']["image-$i"];
		
		if(!isset($selected_cat) || is_null($selected_cat) || !is_numeric($selected_cat))
			form_set_error('shortcut_block', t("Selezionare una categoria per l'elemento n. $i."));
		
		if(!isset($label) || is_null($label) || !is_string($label) || empty($label))
			form_set_error('shortcut_block', t("Specificare un'etichetta per l'elemento n. $i."));
		
		if(!isset($image) || !isset($image['fid']) || $image['fid']==0)
			form_set_error('shortcut_block', t("Scegli un'immagine per l'elemento n. $i."));
		else {
			// Load the file via file.fid.
			$file = file_load($image['fid']);
			// Change status to permanent.
			$file->status = FILE_STATUS_PERMANENT;
			// Save.
			file_save($file);
			// Record that the module (in this example, user module) is using the file.
			file_usage_add($file, 'metadata', 'block', $selected_cat);
		}
		
	}
}