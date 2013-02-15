<?php

/**
 * @file
 * Default theme implementation for displaying a single search result.
 *
 * This template renders a single search result and is collected into
 * search-results.tpl.php. This and the parent template are
 * dependent to one another sharing the markup for definition lists.
 *
 * Available variables:
 * - $url: URL of the result.
 * - $title: Title of the result.
 * - $snippet: A small preview of the result. Does not apply to user searches.
 * - $info: String of all the meta information ready for print. Does not apply
 *   to user searches.
 * - $info_split: Contains same data as $info, split into a keyed array.
 * - $module: The machine-readable name of the module (tab) being searched, such
 *   as "node" or "user".
 * - $title_prefix (array): An array containing additional output populated by
 *   modules, intended to be displayed in front of the main title tag that
 *   appears in the template.
 * - $title_suffix (array): An array containing additional output populated by
 *   modules, intended to be displayed after the main title tag that appears in
 *   the template.
 *
 * Default keys within $info_split:
 * - $info_split['type']: Node type (or item type string supplied by module).
 * - $info_split['user']: Author of the node linked to users profile. Depends
 *   on permission.
 * - $info_split['date']: Last update of the node. Short formatted.
 * - $info_split['comment']: Number of comments output as "% comments", %
 *   being the count. Depends on comment.module.
 *
 * Other variables:
 * - $classes_array: Array of HTML class attribute values. It is flattened
 *   into a string within the variable $classes.
 * - $title_attributes_array: Array of HTML attributes for the title. It is
 *   flattened into a string within the variable $title_attributes.
 * - $content_attributes_array: Array of HTML attributes for the content. It is
 *   flattened into a string within the variable $content_attributes.
 *
 * Since $info_split is keyed, a direct print of the item is possible.
 * This array does not apply to user searches so it is recommended to check
 * for its existence before printing. The default keys of 'type', 'user' and
 * 'date' always exist for node searches. Modules may provide other data.
 * @code
 *   <?php if (isset($info_split['comment'])): ?>
 *     <span class="info-comment">
 *       <?php print $info_split['comment']; ?>
 *     </span>
 *   <?php endif; ?>
 * @endcode
 *
 * To check for all available data within $info_split, use the code below.
 * @code
 *   <?php print '<pre>'. check_plain(print_r($info_split, 1)) .'</pre>'; ?>
 * @endcode
 *
 * @see template_preprocess()
 * @see template_preprocess_metadata_search_result()
 * @see template_process()
 */


$module_base_path = drupal_get_path('module', 'metadata');

$url_separator = variable_get('clean_url', 0)===0 ? '&amp;' : '?';
$node = $result['#result']['node'];
global $user;
$is_admin = user_access('administrator');
$attributes .= $is_admin ? ' data-id="' . $node->nid . ' " ' : '';

?>
<li class="<?php print $classes; ?>"<?php print $attributes; ?> itemprop="itemListElement" itemscope itemtype="http://schema.org/CreativeWork">



<div class="search-result-dx">
  <?php 
  
  if(user_access('administrator')) : ?>
  	<div class="metadata-action-select">Seleziona</div>
  	<div class="metadata-action-deselect" style="display:none;">Annulla selezione</div>
  <?php endif; ?>
  <!-- http://support.google.com/webmasters/bin/answer.py?hl=en&answer=176035  -->
  <span itemprop="aggregateRating" itemscope itemtype="http://schema.org/AggregateRating">
  	<meta itemprop="description" content="Votato dagli utenti"/>	
  	<meta itemprop="bestRating" content="100"/>
  	<meta itemprop="ratingValue" content="<?php echo isset($fivestar_field['vote']['#values']['average']) ? $fivestar_field['vote']['#values']['average'] : 0;?>"/>
  	<meta itemprop="ratingCount" content="<?php echo isset($fivestar_field['vote']['#values']['count']) ? $fivestar_field['vote']['#values']['count'] : 0;?>"/>
  	<meta itemprop="reviewCount" content="<?php echo isset($fivestar_field['vote']['#values']['count']) ? $fivestar_field['vote']['#values']['count'] : 0;?>"/>
  	<meta itemprop="worstRating" content="0"/>
  </span>
  <?php 
  print render($fivestar_field);
  ?>
  <div class="search-result-author action-filter" data-vocabulary="metadata-author" data-input="<?php echo $author_info['id']; ?>">		    
		<?php if (!empty($author_info['logo']) && file_exists("$module_base_path/img/authors/".$author_info['logo'])): ?>
	  		<img alt="<?php print $author_info['name'];?>" title="<?php print $author_info['name'];?>" src="/sites/all/modules/spodata/metadata/img/authors/<?php echo $author_info['logo']; ?>" />
	  	<?php endif; ?>	  	  		  		
  </div>
  <?php 
	if(isset($licences[$license_info[0]['taxonomy_term']->name])) :
  		$license = $licences[$license_info[0]['taxonomy_term']->name];
  ?>
  <div class="search-result-license action-filter" data-vocabulary="metadata-lu-license" data-input="<?php echo $license_info[0]['taxonomy_term']->tid;?>">    
		<?php 
		if (!empty($license['logo']) && file_exists("$module_base_path/img/licences/".$license['logo'])): ?>
	  		<img alt="<?php print $license_info[0]['taxonomy_term']->name;?>" title="<?php print $license_info[0]['taxonomy_term']->name;?>" 
	  			src="/sites/all/modules/spodata/metadata/img/licences/<?php echo $license['logo']; ?>" />
	  	<?php endif; ?>	  	  		  		
  </div>
  <?php endif; ?>
</div>

<div class="search-result-sx">
  <?php print render($title_prefix); ?>  
  <h3 class="title"<?php print $title_attributes; ?> itemprop="name">
    <a href="<?php print $url; ?>" title="<?php print html_entity_decode($title); ?>"><?php print $min_title; ?></a>
  </h3>
  <?php print render($title_suffix); ?>
   
  	<div class="search-result-box">
  
		<div class="search-result-direct-link">
			<a href="<?php print $url; ?>"><img alt="Visualizza la descrizione della scheda" title="Visualizza la descrizione" src="/sites/all/modules/spodata/metadata/img/blue_icons/Description.png"/></a>
		
			<?php if($node->hasGrid) : ?>
				<a href="<?php print $url . $url_separator . "t=Tabella"; ?>"><img alt="Visualizza il dato in tabella" title="Visualizza in tabella" src="/sites/all/modules/spodata/metadata/img/blue_icons/Table.png"/></a>
			<?php endif; ?>
			<?php if($node->hasMap2D) : ?>
				<a href="<?php print $url . $url_separator . "t=Mappa"; ?>"><img alt="Visualizza il dato in mappa" title="Visualizza in mappa" src="/sites/all/modules/spodata/metadata/img/blue_icons/Map.png"/></a>
			<?php endif; ?>
			<?php if($node->hasMap3D) : ?>	
				<a href="<?php print $url . $url_separator . "t=Mappa3D"; ?>"><img alt="Visualizza il dato in mappa 3D" title="Visualizza in mappa 3D" src="/sites/all/modules/spodata/metadata/img/blue_icons/Map3D.png"/></a>
			<?php endif; ?>
			<?php if($node->hasGraph) : ?>
				<a href="<?php print $url . $url_separator . "t=Grafico"; ?>"><img alt="Visualizza il grafico del dato" title="Visualizza il grafico" src="/sites/all/modules/spodata/metadata/img/blue_icons/Graphics.png"/></a>
			<?php endif; ?>
		
			<a href="<?php print $url . $url_separator . "t=Scarica"; ?>"><img alt="Scarica il dato" title="Scarica il dato" src="/sites/all/modules/spodata/metadata/img/blue_icons/Download.png"/></a>
			<a href="<?php print $url . $url_separator . "t=Commenti"; ?>"><img alt="Visualizza i commenti" title="Visualizza i commenti" src="/sites/all/modules/spodata/metadata/img/blue_icons/Comment.png"/></a>
			
			<?php if(isset($node->admin_properties)) { 
				echo $node->admin_properties;
			} ?>
		</div>
	  
	  <div class="search-result-tag">
	  	<?php 

			print '<div itemprop="author" itemscope itemtype="http://schema.org/Organization"><span class="search-result-fonte">' . t('Fonte: ') . '</span>'.
				 '<span itemprop="name">' . truncate_utf8($author_info['name'], 60, FALSE, TRUE).'</span></div>';

	  		print '<div class="search-result-viewcount"><span>' . t('visite: ') . '</span>' . $view_count . '</div>';
	  		
	  		print '<div class="search-result-lastupdate"><span>' . t('aggiornato al: ') . '</span>' . $last_update . '</div>';	  		
	  		
	  		print theme('metadata_hierarchical_terms',
	  				array( 'terms' => $result['#result']['node']->metadata_format['und'],
	  						'vocabulary' => variable_get('metadata_lu_format'),
	  				));
	  		?>
	  </div>
	  
	  <div class="search-snippet-info">
	    <?php if ($snippet): ?>
	      <p class="search-snippet"<?php print $content_attributes; ?> itemprop="description"><?php print $snippet; ?></p>
	    <?php endif; ?>
	  </div>
	  
	  <?php
	  $tmp_keywords = array(); 
	  foreach($category_info as $term) {
	  	$tmp_keywords[] = $term['taxonomy_term']->name;
	  }
	  
	  foreach($type_info as $term) {
	  	$tmp_keywords[] = $term['taxonomy_term']->name;
	  }
	  
	  $keywords = implode(', ', $tmp_keywords);
	  ?>

	  <div class="search-item-tags">
	  		<div class="search-item-label">
	  			<?php print t('Tematiche:'); ?>
	  		</div>
	  		
	    	<?php 
	    	print theme('metadata-search-taxonomy-term',
	  				array( 'terms' => $category_info,
	  						'vocabulary' => variable_get('metadata_lu_category'),
	  				));
	    	?>
	   </div>
	   <div class="search-item-tags">
	    	<div class="search-item-label">
	  			<?php print t('Parole chiave:'); ?>
	  		</div>
	    	<?php 
	    	print theme('metadata-search-type',
	  				array( 'terms' => $type_info,
	  						'vocabulary' => variable_get('metadata_lu_type'),
	  				));
	    	?>
	    </div>   
	    <meta itemprop="keywords" content="<?php echo $keywords; ?>" />
  </div>
</div>

</li>
