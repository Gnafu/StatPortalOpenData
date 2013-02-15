<?php

/**
 * Menu item callback.
 */
function metadata_edit_authors() {
	$form_state = array();
	
	$form = drupal_build_form('metadata_edit_author_form', $form_state);
	
	$form_state_delete = array();
	$form_delete = drupal_build_form('metadata_delete_author_form', $form_state_delete);
	
	// get authors
	$query = db_select('author', 'a')
		->fields('a')
		->orderBy('name');
	
	$rows = $query->execute();
	
	return theme('metadata_edit_authors', array( 'rows' => $rows, 'form' => $form, 'form_delete' => $form_delete));
}


/**
 * Create author edit/add form.
 * 
 * @param array $form Nested array of form elements that comprise the form.
 * @param array $form_state A keyed array containing the current state of the form. 
 * 			The arguments that drupal_get_form() was originally called with are available in the 
 * 			array $form_state['build_info']['args'].
 * 
 * @return array Edit author form array.
 */
function metadata_edit_author_form($form, $form_state){
	
	$metadata_module = drupal_get_path('module', 'metadata');
	
	$form['authors'] = array(
			'#type' => 'container',
			'#attached' => array(
					'js' => array(
							$metadata_module . '/js/metadata.authors.js',
							// dependencies
							$metadata_module . '/js/ui/jquery.ui.core.js',
							$metadata_module . '/js/ui/jquery.ui.widget.min.js',
							$metadata_module . '/js/ui/jquery.ui.mouse.min.js',
							$metadata_module . '/js/ui/jquery.ui.button.min.js',
							$metadata_module . '/js/ui/jquery.ui.draggable.min.js',
							$metadata_module . '/js/ui/jquery.ui.position.min.js',
							$metadata_module . '/js/ui/jquery.ui.resizable.min.js',
							// end dependencies
							$metadata_module . '/js/ui/jquery.ui.dialog.min.js',
					),
					'css' => array(
							$metadata_module . '/css/metadata.authors.css',
					),
			),
			'#attributes' => array ( 'style' => 'display:block;', 'id' => 'form-container'),
	);
	
	$form['authors']['name'] = array(
		'#type' => 'textfield',
		'#title' => t('Fonte'),
		'#description' => t('Il nome della fonte'),
		'#default_value' => '',
		'#size' => 60,
		'#maxlength' => 150, 
		'#required' => TRUE,
		'#attributes' => array( 'id' => 'author-name'),
		'#prefix' => '<img id="image-preview" title="Clicca sull\'immagine per sostituirla." class="logo" src=""/>',
	);
	
	$form['authors']['website'] = array(
			'#type' => 'textfield',
			'#title' => t('Url sito web'),
			'#description' => t('L\'indirizzo web del sito della fonte'),
			'#default_value' => '',
			'#size' => 60,
			'#maxlength' => 150,
			'#required' => TRUE,
			'#attributes' => array( 'id' => 'author-website'),
	);
	
	$form['authors']['email'] = array(
			'#type' => 'textfield',
			'#title' => t('Email'),
			'#description' => t('Indirizzo di posta elettronica'),
			'#default_value' => '',
			'#size' => 60,
			'#maxlength' => 150,
			'#required' => TRUE,
			'#attributes' => array( 'id' => 'author-email'),
	);
	
// 	$form['authors']['logo'] = array(
// 			'#type' => 'hidden',
// 			'#title' => t('Logo'),
// 			'#description' => t('Logo della fonte'),
// 			'#default_value' => '',
// 			'#size' => 60,
// 			'#maxlength' => 150,
// 			'#required' => TRUE,
// 			'#attributes' => array( 'id' => 'author-logo'),
// 	);
	
	$form['authors']['id'] = array(
			'#type' => 'hidden',
			'#default_value' => -1,
			'#size' => 60,
			'#required' => false,
			'#attributes' => array( 'id' => 'author-id'),
	);
	
	$form['authors']['logo2'] = array(
			'#title' => t('Logo'),
			'#type' => 'managed_file',
			'#required' => false,
			'#default_value' => variable_get('logo', ''),
			'#upload_location' => "public://files/metadata_images",//"$metadata_module/img/authors/",
	// 			'#element_validate' => array(
	// 					'file_validate_extensions' => array('gif png jpg jpeg'),
	// 					'file_validate_size' => array(1*1024*1024),
// 		),
		'#attributes' => array( 'id' => 'author-logo-file'),
		'#weight' => -20,
	);
	
	$form['authors']['go'] = array(
			'#type' => 'submit',
			'#value' => t('Salva le modifiche'),
			'#attributes' => array( 'id' => 'author-edit-submit'),
			'#default_value' => '',
	);
	
	return $form;
}


/**
 * Author form validation.
 * @param array $form Nested array of form elements that comprise the form.
 * @param array $form_state A keyed array containing the current state of the form. 
 * 			The arguments that drupal_get_form() was originally called with are available in the 
 * 			array $form_state['build_info']['args'].
 * 
 */
function metadata_edit_author_form_validate($form, $form_state){

	if(!valid_email_address(trim($form_state['values']['email']))) {
		form_set_error('metadata_edit_author_form', t('Formato dal campo Email non valido.'));
	}
	
	if(!valid_url(trim($form_state['values']['website']), true)) {
		form_set_error('metadata_edit_author_form', t('Il formato dell\'indirizzo web della Fonte non &egrave; corretto.'));
	}
	
	if(strlen($form_state['values']['name'])>250) {
		form_set_error('metadata_edit_author_form', t('Il nome della Fonte supera il limite massimo di 250 caratteri.'));
	}

	if(strlen($form_state['values']['website'])>250) {
		form_set_error('metadata_edit_author_form', t('L\'indirizzo di web della Fonte supera il limite massimo di 250 caratteri.'));
	}
	
	// image validation
	if($form_state['values']['logo2']<=0 && $form_state['values']['fid']<0)
		form_set_error('metadata_edit_author_form', t('Devi caricare un immagine per la Fonte da inserire.'));
	
	$fid = $form_state['values']['logo2'];
	$file = file_load($fid);
	if($file) {
		$fvalidate_output = file_validate_is_image($file);
		if(is_array($fvalidate_output ) && count($fvalidate_output)>0) {
			$message = '';
			
			foreach ($fvalidate_output as $key => $value) {
				$message .= " $value";
			}
			
			form_set_error('metadata_edit_author_form', t('Il logo caricato non &egrave; un\'immagine. ') . $value);
		}
		
		$fsize_validate = file_validate_size($file, 2*1024*1024); 
		if(is_array($fsize_validate) && count($fsize_validate)>0) {
			$message = '';
			
			foreach ($fsize_validate as $key => $value) {
				$message .= " $value";
			}
			
			form_set_error('metadata_edit_author_form', 'L\'immagine caricata supera le dimensioni massime (2MB).' .  $message);
		}
			
	}
}

/**
 * Author form submit
 * @param array $form Nested array of form elements that comprise the form.
 * @param array $form_state A keyed array containing the current state of the form. 
 * 			The arguments that drupal_get_form() was originally called with are available in the 
 * 			array $form_state['build_info']['args'].
 * 
 */
function metadata_edit_author_form_submit($form, $form_state){
	
	$fid = $form_state['values']['logo2'];
	$file = file_load($fid);
	if($file) {
		// move file 
		$file = file_unmanaged_copy($file->uri, drupal_get_path('module', 'metadata')."/img/authors");
		
		$file = $file ? basename($file) : false;
	}

	// try to insert/update table row 
	try {
		// editing
		if($form_state['values']['id'] > 0) {
			if(!TableAuthor::update_or_insert($form_state['values']['name'], $file, $form_state['values']['website'], 
					$form_state['values']['email'], $form_state['values']['id'])) {
				drupal_set_message(t('Authore non aggiunto'), 'error');
			}
			else {
				drupal_set_message(t('Modifiche salvate'), 'status');
			}
		}
		// insert
		else {
			if(!TableAuthor::update_or_insert($form_state['values']['name'], $file, 
					$form_state['values']['website'], $form_state['values']['email'])) {
				drupal_set_message(t('Authore non aggiunto'), 'error');
			}
			else {
				drupal_set_message(t('Modifiche salvate'), 'status');
			}
		}
		
		// update alias
		db_query('select * from create_taxonomy_aliases();');
	}catch (PDOException $ex) {
		drupal_set_message(t('Errore salvando il record'), 'error');
	}
	
}

/**
 * Create author delete form.
 * 
 * @param array $form Nested array of form elements that comprise the form.
 * @param array $form_state A keyed array containing the current state of the form. 
 * 			The arguments that drupal_get_form() was originally called with are available in the 
 * 			array $form_state['build_info']['args'].
 * 
 * @return array Edit author form array.
 */
function metadata_delete_author_form($form, $form_state){
	
	$form['#id'] = 'delete-author-handler';
	$form['author_id'] = array(
		'#type' => 'hidden',
		'#attributes' => array( 'id' => 'author-id-to-delete'),
	);
	
// 	$form['author_name'] = array(
// 		'#type' => 'textfield',
// 		'#attributes' => array(
// 			'disabled' => 'disabled',
// 		),
// 		'#default_value' => '',
// 	);
	
	$form['replacement_type_options'] = array(
			'#type' => 'value',
			'#value' => array(
			'replace_with_other' => t('Elimina la Fonte selezionata e mappa i contenuti ad essa associati alla Fonte "Altro".'),
			'replace_with' => t('Elimina la Fonte selezionata e mappa i contenuti ad essa associati alla Fonte selezionata.'),
			'delete_content' => t('Elimina la Fonte e tutti i contenuti ad essa associati.'),
		),
	); 
	
	$form['replacement_type'] = array(
		'#type' => 'radios',
    	'#title' => t('Tipo di cancellazione'),
		'#default_value' => 'replace_with_other',
		'#options' => $form['replacement_type_options']['#value'],
	);
	
	$authors = MetadataHelper::getAuthors(false);
	$form['author_options'] = array(
			'#type' => 'value',
			'#value' => $authors + array(-1=>t('Seleziona la fonte')),
	);

	$form['metadata_author'] = array(
			'#title' => t('Rimpiazza la Fonte con'),
			'#type' => 'select',
			'#description' => t("Selezionare la Fonte a cui associare i contenuti in relazione con la Fonte da cancellare." ),
			'#options' => $form['author_options']['#value'],
			'#required' => TRUE,
			'#default_value' => !isset($row_metadata->id_author) ? -1 : $row_metadata->id_author,
	);
	
	$form['authors']['go'] = array(
			'#type' => 'submit',
			'#value' => t('Elimina la fonte'),
			'#attributes' => array( 'id' => 'author-delete-submit'),
			'#default_value' => '',
			'#weight' => 20,
	);
	
	return $form;
}

/**
 * Delete author submit handler.
 * 
 * @param array $form Nested array of form elements that comprise the form.
 * @param array $form_state A keyed array containing the current state of the form. 
 * 			The arguments that drupal_get_form() was originally called with are available in the 
 * 			array $form_state['build_info']['args'].
 * 
 */
function metadata_delete_author_form_submit($form, $form_state){
	try {
		
		// replacement type
		switch ($form_state['values']['replacement_type']) {
			case 'replace_with_other':
				$default_author = variable_get('metadata_default_author', 'Altro');
				$default_author_id = -1;
				// search default author id
				foreach ($form_state['values']['author_options'] as $id => $name) {
					if(strcmp($name, $default_author)==0) {
						$default_author_id = $id;
						break;
					}
				}
				
				if($default_author_id<0) {
					drupal_set_message(t('Impossibile trovare la Fonte di default.'), 'error');
				}
				else {
					TableAuthor::delete_and_replace_with($form_state['values']['author_id'], $default_author_id);
				}
				
				break;
		
			case 'delete_content':
				TableAuthor::delete_author_and_related_metadata($form_state['values']['author_id']);
				break;
		
			case 'replace_with':
				TableAuthor::delete_and_replace_with($form_state['values']['author_id'], $form_state['values']['metadata_author']);
			break;
		}
		
		// update alias
		db_query('select * from create_taxonomy_aliases();');
	
	} catch (Exception $e) {
		drupal_set_message(t('Errore %code nella cancellazione della Fonte: %msg', array('%code'=>$e->getCode(), '%msg' => $e->getMessage())), 'error' );
		return;
	}
	
	drupal_set_message('Cancellazione fonte eseguita con successo.');
}

/**
 * Delete author form validation handler.
 * 
 * @param array $form Nested array of form elements that comprise the form.
 * @param array $form_state A keyed array containing the current state of the form. 
 * 			The arguments that drupal_get_form() was originally called with are available in the 
 * 			array $form_state['build_info']['args'].
 * 
 */
function metadata_delete_author_form_validate($form, $form_state){
	// author to delete
	if(!isset($form_state['values']['author_id']) || !is_numeric($form_state['values']['author_id']) 
		|| $form_state['values']['author_id']<0 ) {
		form_set_error('metadata_delete_author_form', t('Non hai selezionato la Fonte da cancellare.'));
	}
	else if(!in_array($form_state['values']['author_id'], array_keys($form_state['values']['author_options']))) {
		form_set_error('metadata_delete_author_form', t('La Fonte selezionata non esiste: ') . 'id=' . $form_state['values']['author_id']);
	}
	
	// replacement type
	switch ($form_state['values']['replacement_type']) {
		case 'replace_with_other':
		case 'delete_content':
			;
		break;
			
		case 'replace_with':
			// replace md author with ...
			if(!isset($form_state['values']['metadata_author']) || !is_numeric($form_state['values']['metadata_author']) 
			  || $form_state['values']['metadata_author']<0 || 
			   !in_array($form_state['values']['metadata_author'], array_keys($form_state['values']['author_options']))) {
				form_set_error('metadata_delete_author_form', t('Devi selezionare la nuova Fonte per le schede associate alla Fonte da eliminare.'));
			}
			
			break;
				
		default:
			form_set_error('metadata_delete_author_form', t('Metodo di cancellazione non supportato'));
		break;
	}
	
	
	

}