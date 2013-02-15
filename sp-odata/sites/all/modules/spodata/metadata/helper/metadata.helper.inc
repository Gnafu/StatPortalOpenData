<?php 
/**
 * MetadataHelper class.
 *
 * @author a.mistretta
 */
class MetadataHelper
{
	const MD_BASE_URI = 'opendata/';

	/**
	 * Get all system authors.
	 *
	 * @param bool $only_approved if true return only approved authors.
	 * @param bool $search_form if true add author count span
	 * @return array of authors with key=id_author, value=author_name.
	 */
	public static function getAuthors($only_approved=true, $search_form=false) {
		// get all authors
		$query = db_select('author', 'a')
		->fields('a', array('name' , 'id'))
		->orderBy('name');
		// get only approved authors
		if($only_approved) {
			global $user;
			if(!user_access('administer')) {
				if($search_form) {
					$subquery = db_select('metadata', 'md')
					->fields('md', array('id_author'))
					->groupBy('md.id_author');

					// filter: only author related to visible and approved metadata or user created metadata
					$subquery->condition(db_or()
							->condition('md.id_publisher', $user->uid, '=')
							->condition(db_and()
									->condition('md.status', TableMetadata::VISIBLE, '=')
									->condition('md.visibility', TableMetadata::APPROVED, '=')));

					$query->innerJoin($subquery, 'M', ' a.id=M.id_author ');
				}
				else {
					// [metadata creation form]
					// filter: only approved authour or user created author
					$query->condition(
							db_or()
							->condition('a.status', 1, '=')
							->condition('a.uid', $user->uid, '='));
				}
			}
			else if ($search_form){
				// user has admin roles -> show author only if exists a metadata with id_authour=author.
				$subquery = db_select('metadata', 'md')
				->fields('md', array('id_author'))
				->groupBy('md.id_author');

				$query->innerJoin($subquery, 'M', ' a.id=M.id_author ');
			}
		}

		// execute query -> fetch result -> prepare output
		$rows = $query->execute();
		$authors = array();
		foreach ($rows as $author) {
			if($search_form) {
				$authors[$author->id] = $author->name . '<span class="term-count" id="author-' . $author->id . '">(0/0)</span>';
			}
			else {
				$authors[$author->id] = $author->name;
			}
		}

		return $authors;
	}


	/**
	 * Get author row by name.
	 *
	 * @param string $name author name
	 * @return array contained a author row.
	 */
	public static function getAuthorByName($name) {
		// get authors options
		$row = db_select('author', 'a')
		->fields('a')
		->condition('name', $name, 'LIKE')
		->execute();
		$result = array();

		foreach ($row as $author) {
			$result['id'] = $author->id;
		}

		return $result;
	}

	/**
	 * Get author row by name.
	 *
	 * @param string $name author name
	 * @return array contained a author row.
	 */
	public static function getAuthorByLink($link) {
		// get authors options
		$row = db_select('author', 'a')
		->fields('a')
		->condition('website', $link, 'LIKE')
		->execute();
		$result = array();

		foreach ($row as $author) {
			$result['id'] = $author->id;
		}

		return $result;
	}

	/**
	 * Get author row by name.
	 *
	 * @param string $name author name
	 * @return array contained a author row.
	 */
	public static function getAuthorById($id_author) {
		// get authors options
		$row = db_select('author', 'a')
		->fields('a')
		->condition('id', $id_author)
		->execute();
		$result = NULL;

		foreach ($row as $author) {
			$result = $author;
		}

		return $result;
	}

	/**
	 * Get metadata identifier.
	 *
	 * @param object $node a metadata node.
	 * @return string unique identifier.
	 */
	public static function getMDIdentifier($node) {
		//si crea un alias univoco seguendo questi passi:
		//se il nodo ha un titolo si parte dal titolo, altrimenti dall'id
		//nel caso del titolo si ripulisce lasciando solo i caratteri [a-zA-Z0-9]
		//tutti gli altri vengono rimpiazzati da _
		//se l'identificativo così creato è già in uso se ne crea uno univoco aggiungendo
		//_{id-nodo}
		$identifier = MetadataHelper::MD_BASE_URI;
		if(!empty($node->title))
		{
			$truncated = truncate_utf8($node->title, 100,FALSE);
			$identifier .= preg_replace("/[^\\w^0-9]+/", "_", $node->title);
		}
		else
		{
			$identifier .= $node->nid;
		}
		if(drupal_lookup_path("source", $identifier)!=FALSE)
		{
			$identifier .= "_".$node->nid;
		}
		return $identifier;
	}

	/**
	 *
	 */
	public static function getTermsCount() {
		// 		SELECT tid, "count"(*) as count
		// 		FROM taxonomy_index ti INNER JOIN metadata m on ti.nid=m.nid
		// 		GROUP BY tid
		// 		ORDER BY count desc
		$query_res = NULL;

		try {
			$query = db_select('taxonomy_index', 'ti')
			->fields('ti', array('tid'))
			->groupBy('ti.tid');
			$query->leftJoin('metadata', 'm', 'ti.nid=m.nid');
			$query->addExpression('count(*)', 'md_count');
			$query->condition('m.status', 1, '=');
			$query_res = $query->execute();

		} catch (Exception $e) {
			watchdog_exception('metadata', $e, 'Get last voted authors!');
		}
		$terms_count = array();
		foreach ($query_res as $row) {
			$terms_count[$row->tid] = $row->md_count;
		}

		return $terms_count;
	}


	/**
	 *
	 */
	public static function getAuthorsCount() {
		// 		SELECT id, "count"(*) as count
		// 		FROM author a INNER JOIN metadata m on a.id=m.id_author
		// 		GROUP BY a.id
		$query_res = NULL;

		try {
			$query = db_select('view_most_active_author', 'a')
			->fields('a');

			$query_res = $query->execute();

		} catch (Exception $e) {
			watchdog_exception('metadata', $e, 'Get last voted authors!');
		}
		$authors_mdcount = array();
		foreach ($query_res as $row) {
			$authors_mdcount[$row->id] = $row->tot;
		}

		return $authors_mdcount;
	}

	/**
	 * Get sercives info by odata_services table.
	 * @return db query results contained active services.
	 */
	public static function get_services($service_name=NULL) {
		$query = db_select('odata_services', 'OS')
		->fields('OS', array());

		if(!is_null($service_name)) {
			$query->condition('service_name', $service_name, 'ILIKE');
		}

		$result = $query->execute();
		return $result;
	}

	/**
	 * Get sercives info by odata_services table as javascript object string.
	 * @return string rappresentation of odata services js object.
	 */
	public static function get_js_services() {
		// get services info rows
		$services = MetadataHelper::get_services();
		// build js object
		$js_prop = '';
		$first = true;
		// featch all services
		foreach ($services as $service_info) {
			$js_prop .= ($first ? '' : ',' );
			$js_prop .= $service_info->service_name . ' : "' . $service_info->service_url . '"';
			$first = false;
		}

		return "{ $js_prop }";
	}

	/**
	 * Get metadata count grouped by term.
	 */
	public static function get_metadatacount_by_term() {
		$query = db_select('view_metadata_count_by_taxonomy_term', 'VMC')
		->fields('VMC', array());

		$result = $query->execute();
		return $result;
	}

	/**
	 * Get metadata count grouped by: visibility/status field.
	 */
	public static function get_metadatacount_by_status() {
		$query_visibility = db_select('metadata', 'M')
		// 			->fields('M', array('visibility'))
		->groupBy('visibility');
		$query_visibility->addExpression("CONCAT('visibility_', visibility)", '"type"');
		$query_visibility->addExpression('count(*)', 'tot');


		$query_status = db_select('metadata', 'M')
		// 		->fields('M', array('status'))
		->groupBy('status');
		$query_status->addExpression("CONCAT('status_', status)", '"type"');
		$query_status->addExpression('count(*)', 'tot');

		$result = $query_visibility->union($query_status)
		->execute();

		return $result;
		/*
		 select 'visibility' _type, visibility, count(*)
		from metadata
		GROUP BY visibility
		UNION
		select 'status' _type, status, count(*)
		from metadata
		GROUP BY status
		* */
	}

	/**
	 * Get vote count of metadata with id_publisher=$uid.
	 *
	 * @param int $uid user id
	 */
	public static function get_metadata_owner_votecount($uid) {
		// 		SELECT count(*) day_vote_count, to_char(to_timestamp(V.timestamp), 'YYYY-MM-DD') vdate
		// 		from metadata M INNER JOIN votingapi_vote V ON M.nid=V.entity_id
		// 		where M.id_publisher=61 AND
		// 		V.timestamp > (date_part('epoch'::text, now()))::integer - (date_part('epoch'::text, '6 months'::interval))::integer
		// 		GROUP BY vdate
		// 		ORDER BY vdate
		$data = array();
		try {
			$query = db_select('metadata', 'M');
			$query->innerJoin('votingapi_vote', 'V', ' M.nid=V.entity_id');
			$query->addExpression('count(*)', ' day_vote_count');
			$query->addExpression("to_char(to_timestamp(V.timestamp), 'YYYY-MM-DD')", 'vdate');
			$query->condition('M.id_publisher', $uid, '=')
			->condition('M.status', 1, '=')
			->where("V.timestamp > (date_part('epoch'::text, now()))::integer - (date_part('epoch'::text, '6 months'::interval))::integer")
			->groupBy('vdate')
			->orderBy('vdate', 'ASC');

			$result = $query->execute();

			// todo inizializzare con query per ottenere il numero di schede create precedentemente
			$count = 0;

			foreach ($result as $row) {
				$count += $row->day_vote_count;
				$data[] = array(
						'date' => $row->vdate,
						'tot' => $count
				);
			}
		} catch (Exception $e) {
			watchdog_exception('DB Error', $e);
		}

		return $data;
	}

	/**
	 * Get metadata page keywords (DO-990).
	 * @param stdClass $node node object.
	 * @return array of ordered keywords.
	 */
	public static function get_metadata_metakeywords($node) {
		$keywords = array();

		if(isset($node->metadata_type['und'])) {
			foreach ($node->metadata_type['und'] as $value) {
				if( isset($value['taxonomy_term']) && !in_array($value['taxonomy_term']->name, $keywords) )
					$keywords[] = $value['taxonomy_term']->name;
			}
		}

		$keywords[] = $node->author['name'];

		if(isset($node->metadata_category['und'])) {
			foreach ($node->metadata_category['und'] as $value) {
				if( isset($value['taxonomy_term']) && !in_array($value['taxonomy_term']->name, $keywords) )
					$keywords[] = $value['taxonomy_term']->name;
			}
		}

		if(isset($node->metadata_datasources['und'])) {
			foreach ($node->metadata_datasources['und'] as $value) {
				if( isset($value['taxonomy_term']) && !in_array($value['taxonomy_term']->name, $keywords) )
					$keywords[] = $value['taxonomy_term']->name;
			}
		}

		return $keywords;
	}

	/**
	 * Get metadata page description (DO-990).
	 * @param stdClass $node node object.
	 * @return string meta description.
	 */
	public static function get_metadata_metadescription($node) {
		$length = variable_get('metadata_metadescription_length', 200);

		if(isset($node->body[$node->language])) {
			return truncate_utf8(strip_tags($node->body[$node->language][0]['value']), $length, TRUE);
		}
		if(isset($node->body['und']))
			return truncate_utf8(strip_tags($node->body['und'][0]['value']), $length, TRUE);
		else
			return '';
	}

	/**
	 * Set metadata page header.
	 *
	 * @param stdClass $node
	 * @return void
	 */
	public static function set_metadata_page_header($node, $isODataDetailPage=FALSE) {

		// changing title tag (it depends from viewer type)
		$additionalKeyWord = "";
		$additionalKeyWordSuffix = "";
		$odataDetailtPageTitleSuffix = $isODataDetailPage ? " | Informazioni aggiuntive" : '';
		$DO_viewerType = isset($_GET["t"]) ?  $_GET["t"] : '';

		$additionalKeyWord = $DO_viewerType;
		switch($DO_viewerType){
			case 'Tabella':
				$additionalKeyWord = 'Tabella';
				break;
			case 'Mappa':
				$additionalKeyWord = 'Mappa';
				break;
			case 'Mappa3D':
				$additionalKeyWord = 'Mappa 3D';
				break;
			case 'Grafico':
				$additionalKeyWord = 'Grafico';
				break;
			case 'Scarica':
				$additionalKeyWord = 'Download';
				$additionalKeyWordSuffix = '(Scarica CSV, XML, JSON, OData)';
				break;
			case 'Commenti':
				$additionalKeyWord = 'Commenti';
				break;
			default:
				break;
		}

		$_title = trim($additionalKeyWord . ' ' . $node->title . ' ' . $additionalKeyWordSuffix . $odataDetailtPageTitleSuffix);
		$meta_pagetitle = array(
				'#type' => 'html_tag',
				'#tag' => 'title',
				'#value' => $_title,
		);
		drupal_add_html_head($meta_pagetitle, 'title');

		// add keywords to metadata page
		$default_keywords = variable_get('metadata_datapage_keywords', 'open data, dati aperti, dati');
		$meta_keywords = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'keywords',
						'content' => implode(',', self::get_metadata_metakeywords($node)) . ",$default_keywords",
				));
		drupal_add_html_head($meta_keywords, 'meta_keywords');

		$meta_description = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'description',
						'content' => $_title . '. ' . self::get_metadata_metadescription($node),		// append the title to description
				));

		drupal_add_html_head($meta_description, 'meta_description');

		$meta_ogtitle = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'og:title',
						'content' => $node->title,
				));
		drupal_add_html_head($meta_ogtitle, 'meta_ogtitle');

		$meta_ogsitename = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'og:site_name',
						'content' => variable_get('site_name', "datiopen.it") ,
				));
		drupal_add_html_head($meta_ogsitename, 'meta_ogsitename');

		$meta_ogtype = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'og:type',
						'content' => 'website',
				));
		drupal_add_html_head($meta_ogtype, 'meta_ogtype');

		$meta_ogurl = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'og:url',
						'content' => url($node->identifier, array('absolute'=>true)),
				));
		drupal_add_html_head($meta_ogurl, 'meta_ogurl');

		$meta_ogimage = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'og:image',
						'content' => file_create_url(drupal_get_path('module', 'metadata') .  '/img/authors/' . $node->author['logo']),
				));
		drupal_add_html_head($meta_ogimage, 'meta_ogimage');
	}


	public static function theme_metadata_adminfo($node) {
		$visibility_label = $status_label = $admin_properties = '';

		if($node->md_visibility==TableMetadata::VISIBLE) {
			$visibility_label = t('Visibile');
		}
		else if($node->md_visibility==TableMetadata::HIDDEN) {
			$visibility_label = t('Non visibile');
		}

		switch ($node->md_status) {
			case TableMetadata::APPROVED:
				$status_label = t('Approvato');
				break;
			case TableMetadata::NOT_APPROVED:
				$status_label = t('Non approvato');
				break;
			case TableMetadata::REJECTED:
				$status_label = t('Scartato');
				break;
		}

		if(!empty($visibility_label))
			$admin_properties .= '<span class="visibility">'.$visibility_label.'</span>';

		if(!empty($status_label))
			$admin_properties .= '<span class="status">'.$status_label.'</span>';

		return $admin_properties;
	}
}

