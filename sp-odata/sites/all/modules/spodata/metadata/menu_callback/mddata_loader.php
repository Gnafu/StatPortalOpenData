<?php
/**
 * @file
 * Module file for mddata loader menu callback.
 */

/**
 * Menu item callback function. This is an example.
 *
 * @param int $identifier
 * @return string html (themed) of this page region.
 */
function metadata_importer($args=NULL)
{
	$source_table = 'POSTGRESQL_IMPORT';
	
	// loading category terms
	_load_term_to_vocabulary($source_table, 'category', 'metadata_lu_category');
	// loading type terms	
	_load_term_to_vocabulary($source_table, 'type', 'metadata_lu_type');
	
	try {
		// get all rows.
		$query = db_select( '"POSTGRESQL_IMPORT"', 'D')
		->fields('D');
		$result = $query->execute();
	}
	catch (Exception $ex)
	{
		throw new Exception("Error get source rows", 1, $ex);
	}
	
	// get md format term id(const).
	$attach_term = taxonomy_get_term_by_name('Allegati', -1);
	$attach_tid;
	foreach ($attach_term as $key => $value) {
		$attach_tid = $value->tid;
	}
	$data_term = taxonomy_get_term_by_name('Dati', -1);
	$data_tid;
	foreach ($data_term as $key => $value) {
		$data_tid = $value->tid;
	}
	$md_term = taxonomy_get_term_by_name('Strutturati', -1);
	$md_tid;
	foreach ($md_term as $key => $value) {
		$md_tid = $value->tid;
	}
	$alphanumeric_term = taxonomy_get_term_by_name('alfanumerici', -1);
	$alphanumeric_tid;
	foreach ($alphanumeric_term as $key => $value) {
		$alphanumeric_tid = $value->tid;
	}
	// set md default format.
	$format = array(
			'und' => array(
					array(
							'tid' => $attach_tid,
					),
					array(
							'tid' => $data_tid,
					),
					array(
							'tid' => $md_tid,
					),
					array(
							'tid' => $alphanumeric_tid,
					),
			),
	);

	// get md license term id(const).
	$license_term = taxonomy_get_term_by_name('GPL2', -1);
	$license = array (
			'und' => array(
			// 					'tid' => $license_term->tid,
			),
	);
	foreach ($license_term as $key => $value) {
		$license['und'][] = array( 'tid' => $value->tid);
	}
	// get md author term id(const).
	$author = MetadataHelper::getAuthorByName('StatPortal');
	global $user;

	// Attach table handler
	$attach_table = new TableAttach();

	// fetch
	foreach($result as $row) {

		// set type
		$type_term = taxonomy_get_term_by_name($row->type, -1);
		$type = array (
				'und' => array(
				// 						'tid' => $type_term->tid,
				),
		);
		foreach ($type_term as $key => $value) {
			$type['und'][] = array( 'tid' => $value->tid);
		}

		// set category
		$category_term = taxonomy_get_term_by_name($row->category, -1);
		$category = array (
				'und' => array(
				// 						'tid' => $category_term->tid,
				),
		);
		foreach ($category_term as $key => $value) {
			$category['und'][] = array( 'tid' => $value->tid);
		}

		// prepare node array
		$node = array(
				'type' => 'metadata',
				'title' => $row->title,
				'language' => 'en',
				'metadata_format' => $format,
				'metadata_license' => $license,
				'metadata_type' => $type,
				'metadata_category' => $category,
				'metadata_author' => $author['id'],
				'note' => '',
				'created' => format_date(time(), 'custom', 'l j F Y'),
				'uid' => $user->uid,
				'body' => array(
						'und' => array(
								0 => array(
										'value' => isset($row->description) ? $row->description : '',
								),
						),
				),
				'relation' => '',
		);

		// save and submit new node
		$node = node_submit((object)$node);
		node_save($node);

		// add attach to metadata node.
		$url = 'http://localhost:9090/InMemoryProducerExample.svc/MdData(' . $row->id . ')/DataRows';
		$attach_table->addAttachToMetadata($node, TableAttach::TYPE_DATA_MD, $user->uid, $url);
	}
}

/**
 * @file
 * Module file for mddata loader menu callback.
 */

/**
 * Menu item callback function. This is an example.
 *
 * @param int $identifier
 * @return string html (themed) of this page region.
 */
function metadata_statlomb3_importer($args=NULL)
{
	$source_table = 'statlomb3_mddata';
	
	__load_authors_by_table_field($source_table, 'fonte');
	// al momento aggiungiamo solo gli autori.
	// return;

	// loading new category terms
	_load_term_to_vocabulary($source_table, 'macrocategoria', 'metadata_lu_category');
	_load_single_term_to_vocabulary('CISIS-CINSEDO', 'metadata_lu_category');
	
	
	// loading type terms
	_load_term_to_vocabulary($source_table, 'categoria', 'metadata_lu_type');
	_load_term_to_vocabulary($source_table, 'parola_chiave', 'metadata_lu_type');
	

	try {
		// get all rows.
		$query = db_select( $source_table, 'D')
		->fields('D');
		$result = $query->execute();
	}
	catch (Exception $ex)
	{
		throw new Exception("Error get source rows", 1, $ex);
	}

	$format = __get_default_format();

	// get default license.
	$license = __get_default_license();
	
	
	global $user;

	// Attach table handler
	$attach_table = new TableAttach();
	$default_category = taxonomy_get_term_by_name('CISIS-CINSEDO', -1);
	// fetch
	foreach($result as $row) {

		$fonte =  __get_link_by_field($row->fonte);
		$ext_link = __get_link_by_field($row->link_esterno);
		$keyword = __get_keyword_by_field($row->parola_chiave);
		$abstract = $row->abstract;
		
		// get md author term id(const).
		$author = MetadataHelper::getAuthorByLink($fonte['link']);
		
		// set type
		$type_term = taxonomy_get_term_by_name($row->categoria, -1);
		$type = array (
				'und' => array(
				// 						'tid' => $type_term->tid,
				),
		);
		foreach ($type_term as $key => $value) {
			$type['und'][] = array( 'tid' => $value->tid);
		}
		$type_term = taxonomy_get_term_by_name($keyword, -1);
		foreach ($type_term as $key => $value) {
			$type['und'][] = array( 'tid' => $value->tid);
		}

		// set category
		$category_term = taxonomy_get_term_by_name($row->macrocategoria, -1);
		$category = array (
				'und' => array(
				// 						'tid' => $category_term->tid,
				),
		);
		foreach ($category_term as $key => $value) {
			$category['und'][] = array( 'tid' => $value->tid);
		}
		foreach ($default_category as $key => $value) {
			$category['und'][] = array( 'tid' => $value->tid);
		}

		// prepare node array
		$node = array(
				'type' => 'metadata',
				'title' => check_plain($row->title),
				'language' => 'en',
				'metadata_format' => $format,
				'metadata_license' => $license,
				'metadata_type' => $type,
				'metadata_category' => $category,
				'metadata_author' => $author['id'],
				'note' => '',
				'created' => $row->data_creazione, // format_date($row->data_creazione, 'custom', 'l j F Y'),
				'uid' => $user->uid,
				'body' => array(
						'und' => array(
								0 => array(
										'value' => isset($row->abstract) ? $row->abstract . '<br>' . $row->link_esterno : '',
										'format' => 'full_html',
								),
						),
				),
				'relation' => array($ext_link),
		);

		// save and submit new node
		$node = node_submit((object)$node);
		node_save($node);

		// add attach to metadata node.
		$url = 'http://localhost:9090/InMemoryProducerExample.svc/MdData(' . $row->id . ')/DataRows';
		$attach_table->addAttachToMetadata($node, TableAttach::TYPE_DATA_MD, $user->uid, $url);
	}
}

/**
 * Load new author.
 * 
 * @param string $table_name
 * @param string $field field name
 * @throws Exception on sql errors.
 */
function __load_authors_by_table_field($table_name, $field) {
	
	$added = array();
	
	try {
		// get all rows.
		$query = db_select( $table_name, 'D')
			->fields('D', array($field))
			->distinct(true);
		$result = $query->execute();
	}
	catch (Exception $ex)
	{
		throw new Exception("Error get authors rows", 1, $ex);
	}
	
	// fetch
	foreach($result as $row) {
		$author_array = __get_link_by_field($row->$field);
		
		$author = MetadataHelper::getAuthorByLink($author_array['link']);
		
		// se l'autore non esiste lo aggiungiamo
		if(count($author)==0 && !isset($added[$author_array['link']])) {
			try {
				$new_id = db_insert('author')
					->fields(array('name', 'website', 'logo'), array($author_array['label'], $author_array['link'], 'default_logo.png'))
					->execute();
				
				$added[$author_array['link']] = $author_array['label'];
				}
				catch (Exception $ex)
				{
					throw new Exception("Error adding new author", 1, $ex);
				}
			
		}
		
	}
}


function __get_default_license() {
	// get md license term id(const).
	$license_term = taxonomy_get_term_by_name('GPL2', -1);
	$license = array (
			'und' => array(
			// 					'tid' => $license_term->tid,
			),
	);
	foreach ($license_term as $key => $value) {
		$license['und'][] = array( 'tid' => $value->tid);
	}
	
	return $license;
}

/**
 * Get metadata default format.
 * 
 * @return array default format
 */
function __get_default_format() {
	// get md format term id(const).
// 	$attach_term = taxonomy_get_term_by_name('Allegati', -1);
// 	$attach_tid;
// 	foreach ($attach_term as $key => $value) {
// 		$attach_tid = $value->tid;
// 	}
	$data_term = taxonomy_get_term_by_name('Dati', -1);
	$data_tid;
	foreach ($data_term as $key => $value) {
		$data_tid = $value->tid;
	}
	$md_term = taxonomy_get_term_by_name('Strutturati', -1);
	$md_tid;
	foreach ($md_term as $key => $value) {
		$md_tid = $value->tid;
	}
	$alphanumeric_term = taxonomy_get_term_by_name('alfanumerici', -1);
	$alphanumeric_tid;
	foreach ($alphanumeric_term as $key => $value) {
		$alphanumeric_tid = $value->tid;
	}
	// set md default format.
	$format = array(
			'und' => array(
// 					array(
// 							'tid' => $attach_tid,
// 					),
					array(
							'tid' => $data_tid,
					),
					array(
							'tid' => $md_tid,
					),
					array(
							'tid' => $alphanumeric_tid,
					),
			),
	);
	
	return $format;
}


function __get_link_by_field($fonte) {
	$str1 = "href='";
	$str2 = "'>";
	$str3 = "</a>";
	$pos1 = strrpos($fonte, $str1);
	$pos2 = strrpos($fonte, $str2);
	$pos3 = strrpos($fonte, $str3);
	
	$link = substr($fonte, ($pos1 + strlen($str1)), $pos2 - ($pos1 + strlen($str1)));
	$label = substr($fonte, $pos2 + strlen($str2), $pos3 - ($pos2 + strlen($str2)));
	
	return array('link' => $link, 'label' => check_plain($label));
}

function __get_keyword_by_field($fonte) {
	$str1 = "Cinsedo";
	$str2 = "(Sor";
	$pos1 = strrpos($fonte, $str1);
	$pos2 = strrpos($fonte, $str2);
	
	$parolaChiave = substr($fonte, $pos1, $pos2 - ($pos1));
	
	return check_plain($parolaChiave);
}

/**
 * Load term to vocabulary.
 * 
 * @param string $source_table.
 * @param string $source_field.
 * @param string $vocabulary_name.
 */
function _load_term_to_vocabulary($source_table, $source_field, $vocabulary_name) 
{
	$result;
	try {
		$query = db_select( "\"$source_table\"", 'D')
		->fields('D', array($source_field))
		->distinct('true');

		$result = $query->execute();
	}
	catch (Exception $ex)
	{
		throw new Exception("Fail to get $source_field list by $source_table", 1, $ex);
	}
	
	// get vocabulary id
	$format_vid = variable_get($vocabulary_name, -1);
	
	// fetch result
	foreach($result as $row) {
		
		if($source_field=='parola_chiave')
			$field_value = __get_keyword_by_field($row->$source_field);
		else
			$field_value = $row->$source_field;
		
		$matched = taxonomy_get_term_by_name($field_value);
		if(empty($matched))
		{
			//taxonomy_term_save
			$term_obj = array(
					'vid' => $format_vid,
					'name' => check_plain($field_value),
					'parent' => 0,
			);
			// add term to vocabulary
			taxonomy_term_save((object)$term_obj);
		}
	}
}


/**
 * Load term to vocabulary.
 * 
 * @param string $term to add.
 * @param string $vocabulary_name vocabulary name.
 */
function _load_single_term_to_vocabulary($term, $vocabulary_name) {
	$matched = taxonomy_get_term_by_name($term);
	$vid = variable_get($vocabulary_name, -1);
	
	if(empty($matched) && $vid>=0)
	{
		//taxonomy_term_save
		$term_obj = array(
				'vid' => $vid,
				'name' => check_plain($term),
				'parent' => 0,
		);
		// add term to vocabulary
		taxonomy_term_save((object)$term_obj);
	}
}