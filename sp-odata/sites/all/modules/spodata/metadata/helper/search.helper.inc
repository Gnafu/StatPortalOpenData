<?php
/**
 * Metadata search helper class.
 *
 * @author a.mistretta
 */
class MetadataSearchHelper
{
	const STRINGFILTER_KEY = 'filter_string';
	/**
	 * Search string.
	 * @var String $keys 
	 */
	private $keys = ''; 
	/**
	 * Order type.
	 * @var int 
	 */
	private $orderBy = ORDER_BY_RELEVANCE; 
	/**
	 * Author filter.
	 * @var array 
	 */	
	private $authors = array();
	/**
	 * Category filter.
	 * @var array 
	 */
	private $category = array();
	/**
	 * License filter.
	 * @var array
	 */
	private $license = array();
	/**
	 * Type filter.
	 * @var array
	 */
	private $type = array();
	/**
	 * Filter filter.
	 * @var array
	 */
	private $format = array();
	/**
	 * Datasource filter.
	 * @var array 
	 */
	private $datasources = array();
	/**
	 * Page to show.
	 * @var int
	 */
	private $current_page = 0;
	/**
	 * Administrator filter.
	 * @var array
	 */
	private $admFilter = array('approved', 'visible'); 
	/**
	 * Author filter.
	 * @var object
	 */
	private $current_user=NULL;
	/**
	 * Search type.
	 * @var bool
	 */
	private $search = TRUE;
	/**
	 * Search has pagination.
	 * @var bool
	 */
	private $pagination=true;
	/**
	 * Search page title.
	 * @var string
	 */
	public $search_page_title='Catalogo Open Data';
	/**
	 * Search page keywords
	 * @var array
	 */
	public $search_page_keywords = array();
	
	/**
	 * MetadataSearchHelper constructor. 
	 * @param array $search_params array of search parameters. 
	 */
	public function __construct(array $search_params=array()) {
		if(count($search_params)>0) {
			foreach ($search_params as $key => $value) {
				if(property_exists($this, $key) && isset($value)) {
					// override default search options
					$this->$key = $value;
				}
				else if(strcmp($key, self::STRINGFILTER_KEY)==0) {
					// parse search params and init istance properties
					$this->parse_search_params($value);
				}
			}		
		}
		else {
			$this->get_search_params_by_request();
		}
	}
	
	/**
	 * Return MetadataSearchQuery object.
	 */
	public function getSearchQuery() {
		$query;
		$hasKeyTerms = $this->keys!='';
	
		// Get visibility and status filter value
		$status = TableMetadata::APPROVED;
		$visibility=TableMetadata::VISIBLE;
		if(isset($this->admFilter) && !empty($this->admFilter)) {
			// get visibility filter value
			if(isset($this->admFilter['visible']) && isset($this->admFilter['not_visible']))
				$visibility = NULL; // tutte le schede visibili e non
			else if(isset($this->admFilter['not_visible']))
				$visibility = TableMetadata::HIDDEN;
	
			// get status filter value
			if( (isset($this->admFilter['approved']) && isset($this->admFilter['not_approved']) && isset($this->admFilter['rejected'])) ||
					(!isset($this->admFilter['approved']) && !isset($this->admFilter['not_approved']) && !isset($this->admFilter['rejected'])) ) {
				$status = NULL; // tutte le schede visibili e non
			}
			else {
				$status = array();
	
				if(isset($this->admFilter['approved']))
					$status[] = TableMetadata::APPROVED;
	
				if(isset($this->admFilter['not_approved']))
					$status[] = TableMetadata::NOT_APPROVED;
	
				if(isset($this->admFilter['rejected']))
					$status[] = TableMetadata::REJECTED;
	
				if(count($status)==1)
					$status = $status[0]; // evitiamo l'operatore IN
			}
		}
	
		// output array
		$results = array();
	
		// keys missed?
		if(!$hasKeyTerms)
		{
			$_condition = db_and();
			// set query to get all metadata ->addExpression('SUM(i.score * t.count)', 'calculated_score')
			$query = db_select('metadata', 'M', array())
				->fields('M', array('nid'));
			// set status and visibility conditions
			if(!is_null($status))
				$_condition->condition('M.status', $status);
			if(!is_null($visibility))
				$_condition->condition('M.visibility', $visibility);
	
			$score_name = $query->addExpression('1', 'calculated_score');
			$sid_name = $query->addExpression('M.nid', 'sid');
			
			// user metadata only.
			if(isset($this->current_user))
				$_condition->condition('M.id_publisher', $this->current_user);
	
			if(user_access('edit own metadata content') && !user_access('administrator')) {
				global $user;
				$this->current_user = $user->uid;
				$_condition = db_or()
				->condition('M.id_publisher', $this->current_user, '=')
				->condition($_condition);
			}
			if($_condition->count()>0) {
				$query->condition($_condition);
			}
		}
		else
		{
			$_condition = db_and();
			// Build matching conditions
			$query = db_select('search_index', 'i', array('target' => 'slave'))->extend('MetadataSearchQuery');//->extend('PagerDefault');
			$query->join('node', 'n', 'n.nid = i.sid');
			$query->join('metadata', 'M', 'M.nid = i.sid');
	
			// set status and visibility conditions
			if(!is_null($status))
				$_condition->condition('M.status', $status);
			if(!is_null($visibility))
				$_condition->condition('M.visibility', $visibility);
	
			if(user_access('edit own metadata content') && !user_access('administrator')) {
				global $user;
				$_condition = db_or()
				->condition('M.id_publisher', $user->uid, '=')
				->condition($_condition);
			}
	
			if($_condition->count()>0) {
				$query->condition($_condition);
			}
	
			$query
				->addTag('node_access')
				->searchExpression($this->keys, 'node');
	
			// Only continue if the first pass query matches.
			if (!$query->executeFirstPass()) {
				return NULL;
			}
	
			// Add the ranking expressions.
			_node_rankings($query);
		}
	
		// set advanced search condition to query
		$this->set_search_query_advanced_filters($query);
	
		return $query;
	}
	
	/**
	 * Set page header (title,keyword,description,OpenGraph)
	 * @param int $pagecount
	 */
	public function setPageHeader($pagecount) {
		// set page meta info
		$page_title = t('@title - Pagina @current_page di @page_count', array( '@title' => $this->search_page_title,'@current_page'=>$this->current_page+1, '@page_count'=>$pagecount+1));
		drupal_set_title($page_title);
		$keywords = count($this->search_page_keywords)>0 ? implode(',', $this->search_page_keywords) . ", dati, dati aperti, open data, catalogo" : "dati, dati aperti, open data, catalogo";
		$meta_keywords = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'keywords',
						'content' =>  $keywords,
				));
		drupal_add_html_head($meta_keywords, 'meta_keywords');
		
		// set title
		if(count($this->search_page_keywords)>0)
			$title = t("Il catalogo dati mostra l'elenco degli open data pubblicati filtrati per @keywords.", array('@keywords'=>implode(',', $this->search_page_keywords)));
		else
			$title = t("Il catalogo dati mostra l'elenco completo degli open data pubblicati.");
		
		$meta_description = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'description',
						'content' => $title),
		);
		drupal_add_html_head($meta_description, 'meta_description');
		
		$meta_ogtitle = array(
				'#type' => 'html_tag',
				'#tag' => 'meta',
				'#attributes' => array(
						'name' => 'og:title',
						'content' => $page_title,
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
	}
	
	/**
	 * Get author filter.
	 * @return array Author array
	 */
	public function getAuthor() {	return $this->authors;	}
	
	/**
	 * Get search string (keywords).
	* @return string Search keys
	 */
	public function getKeys() {		return $this->keys;		}
	
	/**
	 * Get order.
	 * @return int order type
	 */
	public function getOrderBy() {	return $this->orderBy;	}
	
	/**
	 * Get admin filter options.
	 * @return array Administrator filter array
	 */
	public function getAdminFilter() {	return $this->admFilter;	}
	
	public function getTaxonomyFilter($vocabulary_name) {
		$type = str_replace(MODULE . '_lu_', '', $vocabulary_name);
		if(property_exists($this, $type)) {
			return $this->$type;
		}
		return array();
	}
	/**
	 * Add order statement to search_query object.
	 *
	 * @param object $query search query.
	 *
	 * @return object query with order by statement
	 */
	private function set_search_query_orderby_options($query) {
		$hasKeyTerms = !empty($this->keys);
		// aggiungo l'ordinamento selezionato dall'utente alla query.
		switch ($this->orderBy) {

			// return query to get results ordered by rating.
			case ORDER_BY_RATING: //OK
				// add type sid to select statement
				if($hasKeyTerms)
					$query=$query->fields('i', array('type', 'sid'));

				$ord_query = db_select('votingapi_cache', 'V', array());

				/**
				 * INNER JOIN (
				 SELECT value as vote_count, entity_id
				 FROM votingapi_cache
				 WHERE "function" LIKE 'count'
				 ) VCOUNT ON V.entity_id = VCOUNT.entity_id
				 */
				$sub_query = db_select('votingapi_cache', 'VCOUNT')
					->fields('VCOUNT', array('entity_id'))
					->condition('function', 'count', 'LIKE');
				$sub_query->addExpression('value', 'vote_count');

				$ord_query->innerJoin($sub_query, 'VCOUNT', 'V.entity_id = VCOUNT.entity_id');
				$alias = $ord_query->rightJoin($query, 'O', 'V.entity_id=O.sid AND function LIKE \'average\'');
				$ord_query->fields($alias);
				$ord_query->addExpression('CASE WHEN V.value IS NULL THEN 0 ELSE 10^6*vote_count+V.value END', 'rating');

				$ord_query->orderBy('rating', 'DESC');

				if($hasKeyTerms) {
					$ord_query->orderBy('word_score', 'DESC');
				}

				return $ord_query;
				// return query to get results ordered by title.
			case ORDER_BY_TITLE: //OK
				if($hasKeyTerms)
					$query=$query->fields('i', array('type', 'sid'));

				$ord_query = db_select('metadata', 'M', array());
				$ord_query->rightJoin($query, 'O', 'M.nid=O.sid');
				$ord_query->fields('O');
				$ord_query
					->orderBy('M.title', 'ASC');

				return $ord_query;
				// return query to get results ordered by type
			case ORDER_BY_TYPE: //OK

				if($hasKeyTerms)
				{
					$query=$query->fields('i', array('type', 'sid'));
					$ord_query = db_select('view_metadata_type_term', 'V', array());
					$ord_query->rightJoin($query, 'O', 'V.entity_id=O.sid');
					$ord_query->fields('O');
					$ord_query
						->orderBy('V.min_term', 'DESC')
						->orderBy('V.max_term', 'DESC')
						->orderBy('word_score', 'DESC');
					return $ord_query;
				}
				else {

					$query->innerJoin('view_metadata_type_term', 'VTerm', 'VTerm.entity_id=M.nid');
					$query->orderBy('VTerm.min_term', 'DESC')
						->orderBy('VTerm.max_term', 'DESC');
					return $query;
				}
				break;
			case ORDER_BY_LAST_UPDATE: // OK
				if($hasKeyTerms)
					$query=$query->fields('i', array('type', 'sid'));

				$ord_query = db_select('metadata', 'M', array());
				$ord_query->rightJoin($query, 'O', 'M.nid=O.sid');
				$ord_query->fields('O');
				$ord_query
					->orderBy('M.last_update', 'DESC');

				if($hasKeyTerms) {
					$ord_query->orderBy('word_score', 'DESC');// a parità di data aggiornamento uso il word_score
				}


				return $ord_query;
				break;

			case ORDER_BY_RELEVANCE: // OK
				if(!$hasKeyTerms) {
					$query->innerJoin('view_metadata_relevance', 'vmr', 'vmr.nid=M.nid');
					$query->addExpression('vmr.view_count*0.3 + vmr.search_count*0.6 + vmr.download_count*0.9', 'RELEVANCE');
					$query->orderBy('RELEVANCE', 'DESC');
					return $query;
				} else {
					$query->fields('i', array('type', 'sid')); // select i.type, i.sid as nid

					$ord_query = db_select('view_metadata_relevance', 'vmr', array());
					$ord_query->rightJoin($query, 'O', 'vmr.nid=O.sid');
					$ord_query->addExpression('vmr.view_count*0.3 + vmr.search_count*0.6 + vmr.download_count*0.9', 'RELEVANCE');
					$ord_query->fields('O')
						->orderBy('word_score', 'DESC')
						->orderBy('RELEVANCE', 'DESC');
					return $ord_query;
				}
				break;
				// return query to get results order score.
			case ORDER_BY_MOST_DOWNLOADED:
				if(!$hasKeyTerms) {
					$query->innerJoin('view_metadata_relevance', 'vmr', 'vmr.nid=M.nid');
					$query->orderBy('vmr.download_count', 'DESC');
					return $query;
				} else {
					$query->fields('i', array('type', 'sid')); // select i.type, i.sid as nid

					$ord_query = db_select('view_metadata_relevance', 'vmr', array());
					$ord_query->rightJoin($query, 'O', 'vmr.nid=O.sid');
					$ord_query->fields('O')
					->orderBy('vmr.download_count', 'DESC')
					->orderBy('word_score', 'DESC');
					return $ord_query;
				}
				break;
			case ORDER_BY_FORMAT: // OK
				if($hasKeyTerms)
				{
					$query=$query->fields('i', array('type', 'sid'));
					$ord_query = db_select('view_metadata_format_term', 'V', array());
					$ord_query->rightJoin($query, 'O', 'V.entity_id=O.sid');
					$ord_query->fields('O');
					$ord_query->orderBy('V.term', 'DESC');
					$ord_query->orderBy('word_score', 'DESC');
					return $ord_query;
				}
				else {

					$query->innerJoin('view_metadata_format_term', 'VTerm', 'VTerm.entity_id=M.nid');
					$query->orderBy('VTerm.term', 'DESC');
					
					return $query;
				}
				break;

				break;
			case ORDER_BY_MOST_VIEWED:
				if(!$hasKeyTerms) {
					$query->innerJoin('view_metadata_relevance', 'vmr', 'vmr.nid=M.nid');
					$query->orderBy('vmr.view_count', 'DESC');
					return $query;
				} else {
					$query->fields('i', array('type', 'sid')); // select i.type, i.sid as nid

					$ord_query = db_select('view_metadata_relevance', 'vmr', array());
					$ord_query->rightJoin($query, 'O', 'vmr.nid=O.sid');
					$ord_query->fields('O');
					$ord_query
					->orderBy('vmr.view_count', 'DESC')
					->orderBy('word_score', 'DESC');
					return $ord_query;
				}
				break;
			case ORDER_BY_MOST_SEARCHED:
				if(!$hasKeyTerms) {
					$query->innerJoin('view_metadata_relevance', 'vmr', 'vmr.nid=M.nid');
					$query->orderBy('vmr.search_count', 'DESC');
					return $query;
				} else {
					$query->fields('i', array('type', 'sid')); // select i.type, i.sid as nid

					$ord_query = db_select('view_metadata_relevance', 'vmr', array());
					$ord_query->rightJoin($query, 'O', 'vmr.nid=O.sid');
					$ord_query->fields('O');
					$ord_query->orderBy('vmr.search_count', 'DESC')
					->orderBy('word_score', 'DESC');
					return $ord_query;
				}
				break;
			default:
				if(!$hasKeyTerms) {
				$query->orderBy('calculated_score', 'ASC');
				return $query;
			} else {
				$ord_query = db_select('metadata', 'M', array());
				$ord_query->rightJoin($query, 'O', 'M.nid=O.sid');
				$ord_query->fields('M');
				$ord_query->fields('O');
				
				return $ord_query;
			}

			break;
		}
	}
	
	/**
	 * Get a metadata simple/advanced search query.
	 *
	 **/
	private function set_search_query_advanced_filters($query) {
		if(!empty($this->authors))
		{
			// set author filter
			$query->innerJoin( 'author', 'A', 'A.id = M.id_author');
			$query->condition('A.id', $this->authors);
		}
		
		if(!empty($this->category) || !empty($this->license) || !empty($this->type) || 
			!empty($this->format) || !empty($this->datasources) )
		{
			$cond = db_and();
			if(!empty($this->category))
			{
				// add catagories filter
				self::_add_taxonomy_filter($query, $this->category, 'TF_CAT');
			}
			// add license filter
			if(!empty($this->license))
			{
				// add lecenses filter
				self::_add_taxonomy_filter($query, $this->license, 'TF_LIC');
			}
			// add type filter
			if(!empty($this->type))
			{
				// add type filter
				self::_add_taxonomy_filter($query, $this->type, 'TF_TYP');
			}
			// add format filter
			if(!empty($this->format))
			{
				// add format filter
				self::_add_taxonomy_filter($query, $this->format, 'TF_FOR', true);
			}
			// add format filter
			if(!empty($this->datasources))
			{
				// add format filter
				self::_add_taxonomy_filter($query, $this->datasources, 'TF_DSOURCE', false);
			}
		}
	}
	
	/**
	 * Add a complete INNER JOIN statement to search query
	 * to filter metadata by taxonomy
	 *
	 * @param object $query query object ref
	 * @param array $term_ids term id array
	 * @param string $alias subquery alias
	 */
	private static function _add_taxonomy_filter(&$query, $term_ids = array(), $alias, $tree=false)
	{
		if(!empty($term_ids))
		{
			// prepare subquery taxonomy filter
			$sub_query = db_select('taxonomy_index', 'TI', array())
			->fields('TI', array('nid'))
			->groupBy('TI.nid');

			// set taxonomy condition
			if($tree && isset($term_ids['tree'])) {
				$sub_query->condition(_set_tree_searchquery_condition($term_ids['tree'][0], $sub_query));
			}
			else {
				$sub_query->condition('TI.tid', $term_ids);
			}

			$sub_query->addExpression('COUNT(*)', 'weight');

			// add subquery
			$query->innerJoin($sub_query, $alias, $alias . '.nid=M.nid');
		}
	}

	/**
	 * Execute a metadata simple/advanced search.
	 *
	 * @return array of search_results.
	 */
	public function search() {
		$query;
		$hasKeyTerms = $this->keys!='';
		
		// output array
		$results = array();
		// get search query
		$query = $this->getSearchQuery();
		
		// empty result on first step => retun empty result
		if(is_null($query))
			return array();
		
		//	add order by to search query
		$query = $this->set_search_query_orderby_options($query);
		
		if($this->pagination) {
			$num_per_page = variable_get('metadata_num_per_page', 5);
			$query = $query->extend('PagerDefault');
			$query->limit($num_per_page);
		
			// count query result
			$query_count = $query->getCountQuery();
			$result_count = $query_count->execute()->fetchField();
		
			// 	$page = pager_default_initialize($result_count, $num_per_page);
			$page = max(0, min($this->current_page, ((int) ceil($result_count/$num_per_page)) - 1));//
		
		}
		else {
			$num_per_page = variable_get('metadata_num_per_page', 5);
			$query->range(0, $num_per_page);
		}
		
		$find = $query->execute();
		$result_id = array();
		// fetch results.
		foreach ($find as $item) {
			// Render the node.
			if(isset($item->sid))
				$node = node_load($item->sid);
			else
				$node = node_load($item->nid);
		
			if ($this->search) {
				$build = node_view($node, 'search_result');
				unset($build['#theme']);
		
				// hide licence field label
				if(isset($build['metadata_license']))
					$build['metadata_license']['#label_display'] = true;
		
				$node->rendered = drupal_render($build);
		
				// Fetch comments for snippet.
				$node->rendered .= ' ' . module_invoke('comment', 'node_update_index', $node);
		
				$extra = module_invoke_all('node_search_result', $node);
		
				$uri = entity_uri('node', $node);
				$results[] = array(
						'link' => url($uri['path'], array_merge($uri['options'], array('absolute' => TRUE))),
						'type' => check_plain(node_type_get_name($node)),
						'title' => $node->title,
						'user' => theme('username', array('account' => $node)),
						'date' => $node->changed,
						'node' => $node,
						'extra' => $extra,
						'score' => isset($item->calculated_score) ? $item->calculated_score : -1,
						'snippet' => search_excerpt($this->keys, $node->rendered),
						'language' => $node->language,
				);
			}
			else {
				$uri = entity_uri('node', $node);
				$results[] = array(
						'link' => url($uri['path'], array_merge($uri['options'], array('absolute' => TRUE))),
						'type' => check_plain(node_type_get_name($node)),
						'title' => $node->title,
						'user' => theme('username', array('account' => $node)),
						'date' => $node->changed,
						'node' => $node,
						'score' => isset($item->calculated_score) ? $item->calculated_score : -1,
						//'snippet' => search_excerpt($keys, $node->rendered),
						'language' => $node->language,
				);
			}
		
			// to add event
			$result_id[] = $node->nid;
		}
		
		if($this->pagination) {
			// add result count and result page count to output
			$results['match_count'] = $result_count;
			$results['page_count'] = (int)($result_count/$num_per_page);
		}
		
		if ($this->search) {
			_metadata_add_event(TableMetadataEvents::SEARCH_EVTTYPE, $result_id);
		}
		
		return $results;
	}
	
	/**
	 * Build catalog filters.
	 * 
	 * @param string $filter search filter string
	 * @param string $keys(optional) search keywords string
	 * @param int $orderBy(optional) search order 
	 * @param int $page(optional) page 
	 */
	private function parse_search_params($filter) {
		
		if(!empty($filter)) {
			// filtro autore
			if(strpos($filter, 'authors=')!==false) {
				$all_author = MetadataHelper::getAuthors();
				$authors_string = trim(str_ireplace('authors=', '', $filter));
				
				$authors = explode('::', $authors_string);
				// set filter array
				foreach ($authors as $author_id) {
					$author_id = trim($author_id);
					$this->authors[$author_id] = $author_id;
					
					if(isset($all_author[$author_id])) {
						$this->search_page_title = t('Elenco dati elaborati da @author', array( '@author' => $all_author[$author_id]));
						$this->search_page_keywords[] = $all_author[$author_id];
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
						$this->category[$key] = $key;
						$this->search_page_title = t('Dati @category',array('@category' => $cat));
						if(!in_array($cat, $this->search_page_keywords)) {
							$search_page_keywords[] = $cat;
						}
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
					if(property_exists($this, $voc)) {
						array_push($this->$voc, $term);
					}
					
					$this->search_page_keywords[] = $obj_term->name;
					
					switch ($obj_term->vocabulary_machine_name) {
						case 'metadata_lu_datasources':
							$this->search_page_title = t('Elenco dati della banca dati @term', array('@term' => $obj_term->name));
						break;
						case 'metadata_lu_format':
							$this->search_page_title = t('Elenco dati con formato @term', array('@term' => $obj_term->name));
							break;
						case 'metadata_lu_license':
							$this->search_page_title = t('Elenco dati con licenza @term', array('@term' => $obj_term->name));
							break;
						case 'metadata_lu_category':
							$this->search_page_title = t('Elenco dati sulla tematica @term', array('@term' => $obj_term->name));
							break;
						case 'metadata_lu_type':
							$this->search_page_title = t('Elenco dati con la parola chiave @term', array('@term' => $obj_term->name));
							break;
						default:
						break;
					}
				}
			}	
		}
	}

	private function get_search_params_by_request() {
		$req = drupal_get_query_parameters($_POST);
		// simple search parameter
		$this->keys = isset($req['keys']) ? $req['keys'] : '';
		$this->orderBy = isset($req['order_by']) ? $req['order_by'] : '';
		
		// advanced search parameter
		$this->authors = isset($req['metadata_author']) ? $req['metadata_author'] : array();
		$this->category = isset($req['metadata_filter_metadata_lu_category']) ? $req['metadata_filter_metadata_lu_category'] : array();
		$this->license = isset($req['metadata_filter_metadata_lu_license']) ? $req['metadata_filter_metadata_lu_license'] : array();
		$this->type = isset($req['metadata_filter_metadata_lu_type']) ? $req['metadata_filter_metadata_lu_type'] : array();
		$this->format = isset($req['metadata_filter_metadata_lu_format']) ? $req['metadata_filter_metadata_lu_format'] : array();
		$this->datasources = isset($req['metadata_filter_metadata_lu_datasources']) ? $req['metadata_filter_metadata_lu_datasources'] : array();
		$this->current_page = isset($req['page']) ? $req['page'] : 0;
		
		$this->admFilter = array('visible'=>'visible','approved'=>'approved');
		
		// admin filters
		global $user;
		if(user_access('administrator')) {
			if(isset($req['metadata_filter_administration_tools'])) {
				$this->admFilter = $req['metadata_filter_administration_tools'];
			}
		}
		
		// 	$params['visible']
		
		// check page number
		if(!is_numeric($this->current_page) || $this->current_page<0)
			$this->current_page = 0;
		
		$format_term_list = array();
		if(!empty($this->format))
			_get_form_seleted_hierarchy_terms($this->format['tree'], $format_term_list);
	}
}
