<?php

/**
 * @file
 * Default theme implementation for displaying search results.
 *
 * This template collects each invocation of theme_search_result(). This and
 * the child template are dependent to one another sharing the markup for
 * definition lists.
 *
 * Note that modules may implement their own search type and theme function
 * completely bypassing this template.
 *
 * Available variables:
 * - $search_results: All results as it is rendered through
 *   search-result.tpl.php
 * - $module: The machine-readable name of the module (tab) being searched, such
 *   as "node" or "user".
 *
 *
 * @see template_preprocess_search_results()
 */
//<h2><?php print t('Search results');? ></h2>
$basepath = drupal_get_path('module', 'metadata');
drupal_add_js($basepath .'/js/metadata.filterinfo.widget.js');
?>

<div id='metadata-catalog'>
<?php if ($search_results): ?>
	
	<?php 
	 	print "<input type='hidden' value='$page_count' name='page_count'/>";
	 	print "<input type='hidden' value='$match_count' name='match_count'/>";
  ?>
  
  <ol class="search-results <?php print $module; ?>-results" itemscope itemtype="http://schema.org/ItemList">
  	<span style="display:none;" itemprop="name">Catalogo dati open</span>
    <?php print $search_results; ?>
  </ol>
  <?php //print $pager; ?>
<?php else : ?>
	<input type='hidden' value='0' name='page_count'/>
	<input type='hidden' value='0' name='match_count'/>
  <h2><?php print t('Your search yielded no results');?></h2>
  <?php print search_help('search#noresults', drupal_help_arg()); ?>
<?php endif; ?>
<?php print $pager; ?>
</div>

