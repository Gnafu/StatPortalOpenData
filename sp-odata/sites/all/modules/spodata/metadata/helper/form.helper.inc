<?php
/**
 * Metadata filters helper class.
 * Implements form utility function.
 *
 * @author a.mistretta
 */
class MetadataFilters {
	/**
	 * Add a tree filter to form (only hierarchical vocabulary).
	 *
	 * @param array the form reference (in/out).
	 * @param string form_name.
	 * @param object drupal vocabulary object.
	 * @param array ajax options.
	 */
	static function add_treefilter_form_array(&$form, $form_name, $voc, $ajax_options=array(), $isAccordion=false) {


		$h3_classes = $isAccordion ? '' : 'ui-accordion-header ui-helper-reset ui-state-hover ui-state-active ui-corner-top';
		$div_classes = $isAccordion ? '' : 'criterion ui-accordion-content ui-helper-reset ui-widget-content ui-corner-bottom ui-accordion-content-active' ;
		$h3_id = 'id="' . $voc->machine_name . 'box-header"';
		$div_id = 'id="' . $voc->machine_name . 'box-content"';
		$weight = $isAccordion ? 20 : 3;

		$js_terms = self::build_js_tree_object($voc);

		// add tree container to form
		$form[$form_name]['metadata_filter_' . $voc->machine_name] = array(
				'#type' => 'container',
				'#attributes' => array (
						'class' => array (
								'field-type-taxonomy-term-reference',
								'field-name-metadata-format',
								'field-widget-term-reference-tree',
						)
				),
				'#weight' => $weight,
				'#tree' => 1,
				'#required' => FALSE,
				'#collapsible' => TRUE,
				'#collapsed' => false,
				'#ajax' => $ajax_options,
				'#prefix' => "<h3 $h3_id class='$h3_classes'><a href='#'>" . $voc->name . "</a></h3><div $div_id class='$div_classes'>",
				'#suffix' => '</div>',
		);

		// add tree to tree container.
		$form[$form_name]['metadata_filter_' . $voc->machine_name]['tree'] = array (
				'#prefix' => '<div class="sub-criterion">',
				'#suffix' => '</div>',
				'#type' => 'checkbox_tree',
				'#max_choices' => -1,
				'#max_depth' => 5,
				'#start_minimized' => false,
				'#leaves_only' => false,
				'#select_parents' => true,
				'#cascading_selection' => true,
				'#field_name' => $voc->name,
				'#vocabulary' => $voc->vid,
				'#columns' => Array ( 'tid' ),
				'#value_key' => 'tid',
				'#filter_view' => '',
				'#parent_tid' => 0,
				'#token_display' => '',
				'#ajax' => $ajax_options,
				'#default_value' => array(27),
				// additional js e css file.
				'#attached' => Array (
						'js' => Array (
								'sites/all/modules/term_reference_tree/term_reference_tree.js',
								$js_terms => array( 'type' => 'inline'),
						),
						'css' => Array (
								'sites/all/modules/term_reference_tree/term_reference_tree.css' => array('weight' => -20, 'group' => 100),
						)
				),
		);
	}

	/**
	 * Build a javascript object that describes a taxonomy tree hierarchy.
	 */
	static protected function build_js_tree_object($voc) {
		$tree = taxonomy_get_tree($voc->vid);
		$js_tree = 'var MetadataSearch = MetadataSearch || {}; MetadataSearch.Terms=MetadataSearch.Terms || {};
		MetadataSearch.Terms.' . $voc->machine_name . ' = { 0: { parents: [], childrens: [], tid: 0} };';


		foreach ($tree as $idx => $term) {
			$name = $term->name;
			$tid = $term->tid;
			$js_tree .= 'MetadataSearch.Terms.' . $voc->machine_name . '[' . $tid . '] = ' .
					"{ tid: $tid, name: '$name', childrens: [], parent: [";

			$first = true;
			foreach ($term->parents as $value) {
				$js_tree .= ($first ? '' : ', ') . $value;
				$first = false;
			}

			$js_tree .= "]};";
		}

		// set element childrens
		foreach ($tree as $idx => $term) {
			$tid = $term->tid;
			foreach ($term->parents as $parent_tid) {
				$js_tree .= 'MetadataSearch.Terms.' . $voc->machine_name . '[' . $parent_tid . "].childrens.push($tid);";
			}
		}

		return $js_tree;
	}

	/**
	 * Add a taxonomy filter to form.
	 *
	 * @param array $form reference (in/out).
	 * @param string $form_name The form name.
	 * @param object $voc drupal vocabulary object.
	 * @param array $options form item options.
	 * @param array $default_values selected form item options.
	 * @param array $ajax_options item ajax options.
	 */
	static function add_filter_form_array(&$form, $form_name, $voc, $options, $default_values=array(), $ajax_options=array(), $isAccordion=false) {

		$h3_classes = $isAccordion ? '' : 'ui-accordion-header ui-helper-reset ui-state-hover ui-state-active ui-corner-top';
		$div_classes = $isAccordion ? '' : 'criterion ui-accordion-content ui-helper-reset ui-widget-content ui-corner-bottom ui-accordion-content-active' ;
		$h3_id = 'id="' . $voc->machine_name . 'box-header"';
		$div_id = 'id="' . $voc->machine_name . 'box-content"';
		$weight = $isAccordion ? 20 : 2;

		// add item to form
		$form[$form_name]['metadata_filter_' . $voc->machine_name] = array(
		//'#title' => t($voc->name),
				'#type' => 'checkboxes',
				'#options' => $options ,
				'#required' => FALSE,
				'#prefix' => "<h3 $h3_id class='$h3_classes'><a href='#'>" . t($voc->name) . "</a></h3><div $div_id class='$div_classes'>",
				'#suffix' => '</div>',
				'#tree' => FALSE,
				'#ajax' => $ajax_options,
				'#weight' => $weight,
				'#default_value' => $default_values,
		);
	}

	/**
	 * Add author filter to form.
	 *
	 * @param array $form reference (in/out)
	 * @param string $form_name name of the form
	 * @param array $ajax_options array of item ajax options.
	 */
	static function add_authorfilter_form_array(&$form, $form_name, $ajax_options=array(), $isAccordion=false) {
		// initialized by query url parameter handler
		global $search_helper;
		// Get complete author options.
		$author_opts = MetadataHelper::getAuthors(true, true);// getActiveAuthors(true);
		$h3_classes = $isAccordion ? '' : 'ui-accordion-header ui-helper-reset ui-state-hover ui-state-active ui-corner-top';
		$div_classes = $isAccordion ? '' : 'criterion ui-accordion-content ui-helper-reset ui-widget-content ui-corner-bottom ui-accordion-content-active' ;
		$weight = $isAccordion ? 20 : 3;

		$form[$form_name]['metadata_author'] = array(
				'#type' => 'checkboxes',
				'#options' => $author_opts,
				'#required' => FALSE,
				'#prefix' => "<h3 class='$h3_classes'><a href='#'>" . t('Fonte') . "</a></h3><div class='$div_classes'>",
				'#suffix' => '</div>',
				'#ajax' => $ajax_options,
				'#weight' => $weight,
				'#default_value' => $search_helper->getAuthor(), 
		);

	}

	/**
	 * Get metadata count associated with tid.
	 *
	 * @param int $tid term id
	 * @return int metadata count for input tid.
	 */
	private static function _get_term_md_count($tid, $author=false) {
		static $__term_md_count;
		static $__author_md_count;
		// load count by db
		if(!isset($__term_md_count) && !$author) {
			$__term_md_count = MetadataHelper::getTermsCount();
		}
		else if(!isset($__author_md_count) && $author) {
			$__author_md_count = MetadataHelper::getAuthorsCount();
		}

		$count = 0;
		if(isset($tid) && isset($__term_md_count[$tid]) && !$author)
			$count=$__term_md_count[$tid];
		else if(isset($tid) && isset($__author_md_count[$tid]) && $author)
			$count = $__author_md_count[$tid];

		return $count;
	}
}